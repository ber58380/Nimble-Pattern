package com.ber.nimblePattern.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftingLink;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.CraftingSubmitResult;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import com.ber.nimblePattern.crafting.*;
import com.ber.nimblePattern.network.LoopSeedLostNotificationPacket;
import com.ber.nimblePattern.network.WirelessNotification;
import com.ber.nimblePattern.pattern.PatternMapping;
import com.ber.nimblePattern.pattern.wrapper.NimbleCraftingPattern;
import com.ber.nimblePattern.pattern.wrapper.NimbleProcessingPattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

// mod GTLCore will overwrite "executeCrafting", set higher priority to mixin later
@Mixin(value = CraftingCpuLogic.class, remap = false, priority = 1200)
public class CraftingCpuLogicMixin implements LoopCpuAccess {
    @Shadow
    @Final
    private CraftingCPUCluster cluster;
    @Shadow
    @Final
    private ListCraftingInventory inventory;
    @Unique
    private LoopCraftingController loopController;
    @Unique
    private boolean isFinalOutput;

    @Unique
    private final FuzzyOutputLedger fuzzyOutputLedger = new FuzzyOutputLedger();
    @Unique
    private NimbleProcessingPattern currentPattern;
    @Unique
    private boolean isFakePattern;
    @Unique
    private boolean isFuzzyPattern;
    @Shadow
    private @Nullable ExecutingCraftingJob job;

    @Unique
    private ToolCraftingController toolController;

    @Unique
    private ProbabilityTrackingController probabilityTrackingController;

    @Unique
    private ProbabilityRetryController probabilityRetryController;

    @Unique
    private long lastProbabilityGameTick = -1;

    @Inject(method = "executeCrafting", at = @At("HEAD"))
    private void initializeFlags(CallbackInfoReturnable<Integer> cir) {
        CraftingBatchDispatch.clear();
        isFinalOutput = false;
        currentPattern = null;
        isFakePattern = false;
        isFuzzyPattern = false;
    }

    @Unique
    private ProbabilityTrackingController getOrCreateProbabilityTrackingController() {
        if (probabilityTrackingController == null) {
            probabilityTrackingController = new ProbabilityTrackingController();
            lastProbabilityGameTick = cluster.getLevel().getGameTime();
        }
        return probabilityTrackingController;
    }

    @Unique
    private ProbabilityRetryController getOrCreateProbabilityRetryController() {
        if (probabilityRetryController == null) {
            probabilityRetryController = new ProbabilityRetryController(cluster);
        }
        return probabilityRetryController;
    }

    // A failed extraction or push must not leave a count for a later dispatch.
    @Inject(method = "executeCrafting", at = @At("RETURN"))
    private void clearBatchDispatch(CallbackInfoReturnable<Integer> cir) {
        CraftingBatchDispatch.clear();
    }

    // record what is the current pushing pattern
    @Redirect(
            method = "executeCrafting",
            at = @At(value = "INVOKE",
                    target = "Lappeng/api/networking/crafting/ICraftingProvider;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z"))
    private boolean trackCurrentPattern(ICraftingProvider provider, IPatternDetails details, KeyCounter[] craftingContainer) {
        long operations = CraftingBatchDispatch.take(details);
        boolean result;
        if (details instanceof NimbleProcessingPattern npp && npp.isLoopComposite()) {
            if (loopController == null) {
                loopController = new LoopCraftingController(cluster);
            }
            if (toolController == null || !toolController.isActive()) {
                result = loopController.start(npp, craftingContainer, operations);
            } else {
                result = false;
            }
        } else if (details instanceof NimbleCraftingPattern ncp && ncp.isToolPattern()) {
            result = getOrCreateToolController().push(ncp, provider, craftingContainer, cluster.getLevel());
        } else {
            result = provider.pushPattern(PatternMapping.getOriginalPattern(provider, details), craftingContainer);
        }
        currentPattern = result && details instanceof NimbleProcessingPattern npp ? npp : null;

        // TODO:后续要处理概率样板和假样板、循环样板的组合关系
        if (currentPattern != null && currentPattern.isProbabilityPattern()) {
            getOrCreateProbabilityTrackingController().dispatched(
                    currentPattern.getDefinition(), new GenericStack[]{currentPattern.getPrimaryOutput()},
                    operations, currentPattern.getMaxWaitingTicks());
            cluster.markDirty();
        }
        return result;
    }

    @Redirect(
            method = "executeCrafting",
            at = @At(value = "INVOKE",
                    target = "Lappeng/crafting/inv/ListCraftingInventory;insert(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)V"))
    private void nimbleWaitingForInsert(ListCraftingInventory inv, AEKey what, long amount, Actionable mode) {
        var cpu = (CraftingCpuLogic) (Object) this;
        // fake pattern
        if (currentPattern != null && currentPattern.getFakeMode()) {
            var finalOutput = cpu.getFinalJobOutput();
            if (what.matches(finalOutput)) {
                inv.insert(what, amount, mode); // register output
                isFakePattern = true; // claim that a fake pattern invokes insert function
                try {
                    cpu.insert(what, amount, Actionable.MODULATE); // finish output immediately
                } finally {
                    isFakePattern = false;
                }
                return;
            }
        }

        // Supplementary work replaces an existing output debt instead of registering it twice.
        if (probabilityRetryController != null) {
            amount -= probabilityRetryController.reuseWaiting(what, amount, mode);
        }
        if (amount <= 0) {
            return;
        }

        // fuzzy pattern
        if (currentPattern != null && currentPattern.getFuzzyMode() && mode == Actionable.MODULATE) {
            fuzzyOutputLedger.add(what, amount);
        }
        // register output for normal and fuzzy patterns
        inv.insert(what, amount, mode);
    }

    @Redirect(
            method = "insert",
            at = @At(value = "INVOKE",
                    target = "Lappeng/crafting/CraftingLink;insert(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)J"))
    private long redirectFinalOutputInsert(CraftingLink link, AEKey what, long amount, Actionable mode) {
        // When insert function is invoked, might not invoke new executeCrafting, so currentPattern
        // might not latest. Use a boolean flag variable instead to clarify the source of invoke.
        if (isFakePattern) {
            // simulate the output is back to the network
            return amount;
        }
        if (isFinalOutput) {
            if (link.isStandalone()) {
                return amount;
            }
            long inserted = link.insert(what, amount, mode);
            if (mode == Actionable.MODULATE && inserted > 0) {
                inventory.extract(what, Math.min(inserted, amount), Actionable.MODULATE);
            }
            return inserted;
        }
        if (probabilityTrackingController != null && link.isStandalone()) {
            // Keep credited final products off the network until completion. Otherwise, the
            // next retry could extract and count the very same products a second time.
            inventory.insert(what, amount, mode);
            return amount;
        }
        return link.insert(what, amount, mode);
    }

    @Redirect(
            method = "insert",
            at = @At(value = "INVOKE",
                    target = "Lappeng/crafting/inv/ListCraftingInventory;extract(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)J"))
    private long settleWaitingOutputs(ListCraftingInventory inv, AEKey what, long amount, Actionable mode) {
        var receipt = fuzzyOutputLedger.extract(inv, what, amount, mode, (key, accepted) -> {
            if (probabilityTrackingController != null && probabilityTrackingController.isActive()) {
                probabilityTrackingController.received(key, accepted);
            }
        });
        isFuzzyPattern = receipt.fuzzyMode();
        return receipt.amount();
    }

    // check if the output is the final output
    @Redirect(
            method = "insert",
            at = @At(value = "INVOKE",
                    target = "Lappeng/api/stacks/AEKey;matches(Lappeng/api/stacks/GenericStack;)Z"))
    private boolean redirectFuzzyMatches(AEKey what, GenericStack finalOutput) {
        if (isFuzzyPattern) {
            isFuzzyPattern = false;
            return what.dropSecondary().equals(finalOutput.what().dropSecondary());
        }
        return what.matches(finalOutput);
    }

    @Unique
    private void stopLoop() {
        CraftingBatchDispatch.clear();
        if (loopController != null) {
            for (var failure : loopController.releaseSeeds(inventory)) {
                notifyLoopSeed(failure.key(), failure.returnFailed());
            }
        }
        loopController = null;
    }

    @Override
    public boolean nimblePattern$startLoop(NimbleProcessingPattern composite, KeyCounter[] externalInputs, long operations) {
        if (loopController == null) {
            loopController = new LoopCraftingController(cluster);
        }
        return loopController.start(composite, externalInputs, operations);
    }

    @Override
    public boolean nimblePattern$hasActiveLoop() {
        return loopController != null && loopController.isActive();
    }

    @Override
    public void nimblePattern$tickLoop(CraftingService service, IEnergyService energy) {
        if (loopController != null && loopController.isActive()) {
            loopController.tick(service, energy, cluster.getLevel());
        }
    }

    @Override
    public LoopCraftingController.Acceptance nimblePattern$acceptLoopOutput(AEKey key, long amount, Actionable mode) {
        return loopController == null ? LoopCraftingController.Acceptance.NOT_HANDLED
                : loopController.acceptOutput(key, amount, mode);
    }

    @Override
    public void nimblePattern$deliverLoopRemainder(AEKey key, long amount) {
        if (amount <= 0) {
            return;
        }
        var cpu = (CraftingCpuLogic) (Object) this;
        long accepted = cpu.insert(key, amount, Actionable.MODULATE);
        if (accepted < amount) {
            inventory.insert(key, amount - accepted, Actionable.MODULATE);
        }
    }

    @Override
    public void nimblePattern$loadGraphLoop(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        if (loopController != null) {
            loopController.releaseSeeds(inventory);
        }
        loopController = null;
        try {
            var controller = new LoopCraftingController(cluster);
            controller.load(tag);
            loopController = controller;
        } catch (RuntimeException ignored) {
            // Keep the graph CPU loadable even if a saved loop batch is no longer valid. Its job can
            // still be inspected or cancelled instead of failing the whole CPU NBT read.
            loopController = null;
        }
    }

    @Override
    public void nimblePattern$finishGraphLoop() {
        stopLoop();
    }

    @Unique
    private void stopTool() {
        if (toolController != null) {
            toolController.release(inventory);
            toolController = null;
        }
    }

    @Unique
    private void stopProbability() {
        lastProbabilityGameTick = -1;
        if (probabilityTrackingController != null) {
            probabilityTrackingController.release();
            probabilityTrackingController = null;
        }
        if (probabilityRetryController != null) {
            probabilityRetryController.release();
            probabilityRetryController = null;
        }
    }

    @Inject(method = "finishJob", at = @At("HEAD"))
    private void injectFinishJob(boolean success, CallbackInfo ci) {
        stopTool();
        stopLoop();
        stopProbability();
        fuzzyOutputLedger.clear();
    }

    @Inject(method = "cancel", at = @At("HEAD"))
    private void injectCancel(CallbackInfo ci) {
        stopTool();
        stopLoop();
        stopProbability();
        fuzzyOutputLedger.clear();
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void injectWriteToNBT(CompoundTag tag, CallbackInfo ci) {
        tag.put("fuzzyOutputLedger", fuzzyOutputLedger.save());
        if (toolController != null) {
            tag.put("toolController", toolController.save());
        } else {
            tag.remove("toolController");
        }
        if (loopController != null) {
            tag.put("loopController", loopController.save());
        } else {
            tag.remove("loopController");
        }
        if (probabilityTrackingController != null) {
            tag.put("probabilityTrackingController", probabilityTrackingController.save());
        } else {
            tag.remove("probabilityTrackingController");
        }
        if (probabilityRetryController != null) {
            tag.put("probabilityRetryController", probabilityRetryController.save());
        } else {
            tag.remove("probabilityRetryController");
        }
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void injectReadFromNBT(CompoundTag tag, CallbackInfo ci) {
        // GTL graph jobs have no classic ExecutingCraftingJob and load their loop state from
        // GraphCpuController.read before this tail hook can run (when it runs at all).
        if (tag.contains("gtlcoreGraphJob", Tag.TAG_COMPOUND)) {
            return;
        }
        if (probabilityRetryController != null) {
            probabilityRetryController.release();
        }
        probabilityRetryController = null;
        fuzzyOutputLedger.load(tag.getList("fuzzyOutputLedger", Tag.TAG_COMPOUND));
        loopController = null;
        toolController = null;
        probabilityTrackingController = null;

        if (tag.contains("toolController", Tag.TAG_COMPOUND)) {
            getOrCreateToolController().load(tag.getCompound("toolController"));
        }
        if (tag.contains("loopController", Tag.TAG_COMPOUND)) {
            loopController = new LoopCraftingController(cluster);
            loopController.load(tag.getCompound("loopController"));
        }
        if (tag.contains("probabilityTrackingController", Tag.TAG_COMPOUND)) {
            getOrCreateProbabilityTrackingController().load(tag.getCompound("probabilityTrackingController"));
        }
        if (tag.contains("probabilityRetryController", Tag.TAG_COMPOUND)) {
            getOrCreateProbabilityRetryController().load(tag.getCompound("probabilityRetryController"));
        }
        if (job == null) {
            stopTool();
            stopLoop();
            stopProbability();
            fuzzyOutputLedger.clear();
        }
    }

    @Inject(method = "executeCrafting", at = @At("HEAD"), cancellable = true)
    private void executeActiveLoop(int maxPatterns, CraftingService craftingService, IEnergyService energyService,
                                   Level level, CallbackInfoReturnable<Integer> cir) {
        if (loopController != null && loopController.isActive()) {
            cir.setReturnValue(loopController.tick(craftingService, energyService, level));
        }
    }

    @Unique
    private void captureLoopOutput(AEKey what, long amount, Actionable mode, CallbackInfoReturnable<Long> cir) {
        if (loopController == null || !loopController.isActive()) {
            return;
        }
        var acceptance = loopController.acceptOutput(what, amount, mode);
        if (!acceptance.handled()) {
            return;
        }
        if (mode == Actionable.MODULATE && acceptance.completedOutput() != null) {
            // Transfer before the nested insert can finish the job and flush the CPU inventory.
            for (var remainder : acceptance.remainder()) {
                inventory.insert(remainder.what(), remainder.amount(), Actionable.MODULATE);
            }
            var output = acceptance.completedOutput();
            var finalOutput = ((CraftingCpuLogic) (Object) this).getFinalJobOutput();
            if (job != null && finalOutput != null && output.what().matches(finalOutput)) {
                // Keep every physical item produced by the loop in the CPU inventory. The nested insert only
                // settles the requested amount in AE's job accounting; its CraftingLink insertion is redirected
                // below so it cannot consume the same items a second time.
                inventory.insert(output.what(), output.amount(), Actionable.MODULATE);
                long completed = Math.min(output.amount(),
                        ((ExecutingCraftingJobAccessorMixin) job).nimblePattern$getRemainingAmount());
                if (completed > 0) {
                    isFinalOutput = true;
                    try {
                        ((CraftingCpuLogic) (Object) this).insert(output.what(), completed, Actionable.MODULATE);
                    } finally {
                        isFinalOutput = false;
                    }
                }
            } else {
                // Intermediate loop outputs remain available to the rest of the crafting plan.
                ((CraftingCpuLogic) (Object) this).insert(output.what(), output.amount(), Actionable.MODULATE);
            }
        }
        cir.setReturnValue(acceptance.accepted());
    }

    @Unique
    private void captureToolOutput(AEKey key, long amount, Actionable mode, CallbackInfoReturnable<Long> cir) {
        if (toolController == null) {
            return;
        }
        long accepted = toolController.accept(key, amount, mode);
        if (accepted > 0) {
            if (mode == Actionable.MODULATE) {
                cluster.markDirty();
            }
            cir.setReturnValue(accepted);
        }
    }

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void receiveSpecialResults(AEKey key, long amount, Actionable mode, CallbackInfoReturnable<Long> cir) {
        if (job == null || amount <= 0 || isFinalOutput) {
            return;
        }
        captureToolOutput(key, amount, mode, cir);
        if (cir.isCancelled()) {
            return;
        }
        captureLoopOutput(key, amount, mode, cir);
    }

    @Unique
    private void notifyLoopSeed(AEKey key, boolean returnFailed) {
        if (job == null) {
            return;
        }
        WirelessNotification.broadcast(cluster.getGrid(), new LoopSeedLostNotificationPacket(key, returnFailed));
    }

    @Unique
    private ToolCraftingController getOrCreateToolController() {
        if (toolController == null) {
            toolController = new ToolCraftingController();
        }
        return toolController;
    }

    @Inject(method = "trySubmitJob", at = @At("HEAD"), cancellable = true)
    private void validateLoopSeedsBeforeSubmit(IGrid grid, ICraftingPlan plan, IActionSource source,
                                               ICraftingRequester requester, CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        for (var missing : LoopSeedValidation.missing(plan, grid.getStorageService().getInventory())) {
            cir.setReturnValue(CraftingSubmitResult.missingIngredient(
                    new GenericStack(missing.getKey(), missing.getLongValue())));
            return;
        }
    }

    @Inject(method = "trySubmitJob", at = @At("RETURN"))
    private void reserveTools(IGrid grid, ICraftingPlan plan, IActionSource source, ICraftingRequester requester, CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        if (cir.getReturnValue().successful()) {
            var reserved = ToolReservation.getToolReservation(plan.patternTimes());
            if (!reserved.isEmpty()) {
                getOrCreateToolController().reserve(reserved, inventory);
                cluster.markDirty();
            }
        }
    }

    /**
     * submit the outputs collected by toolController
     */
    @Unique
    private void submitToolPatternOutput() {
        if (toolController == null || !toolController.isActive()) {
            return;
        }
        var outputs = new ArrayList<>(toolController.settle());
        if (outputs.isEmpty()) {
            return;
        }
        // Sort the outputs, move the final output to the end of the arrayList.
        // So the following steps would submit remainders and byproduct first.
        var cpu = (CraftingCpuLogic) (Object) this;
        var finalOutput = cpu.getFinalJobOutput();
        outputs.sort(Comparator.comparing(stack -> stack.what().matches(finalOutput)));

        for (var output : outputs) {
            if (job != null && output.what().matches(finalOutput)) {
                inventory.insert(output.what(), output.amount(), Actionable.MODULATE);
                long completed = Math.min(output.amount(), ((ExecutingCraftingJobAccessorMixin) job).nimblePattern$getRemainingAmount());
                isFinalOutput = true;
                try {
                    cpu.insert(output.what(), completed, Actionable.MODULATE);
                } finally {
                    isFinalOutput = false;
                }
            } else {
                long accepted = cpu.insert(output.what(), output.amount(), Actionable.MODULATE);
                if (accepted < output.amount()) {
                    inventory.insert(output.what(), output.amount() - accepted, Actionable.MODULATE);
                }
            }
        }
        if (!outputs.isEmpty()) {
            cluster.markDirty();
        }
    }

    /**
     * update the state of probability patterns every tick
     */
    @Unique
    private void tickProbability() {
        if (job == null || probabilityTrackingController == null) {
            return;
        }
        long gameTick = cluster.getLevel().getGameTime();
        long elapsed = lastProbabilityGameTick < 0 ? 0 : Math.max(0, gameTick - lastProbabilityGameTick);
        lastProbabilityGameTick = gameTick;
        if (probabilityTrackingController.isActive()) {
            probabilityTrackingController.reconcileWaiting(
                    ((ExecutingCraftingJobAccessorMixin) job).nimblePattern$getWaitingFor());
            probabilityTrackingController.tick(elapsed);
            cluster.markDirty();
        }
        var plan = getOrCreateProbabilityRetryController().tick(
                job, inventory, probabilityTrackingController, toolController);
        if (plan != null && job != null) {
            var tools = ToolReservation.getToolReservation(plan.patternTimes());
            if (!tools.isEmpty()) {
                getOrCreateToolController().reserve(tools, inventory);
            }
        }
    }

    @Inject(method = "tickCraftingLogic", at = @At("HEAD"))
    private void updateControllers(IEnergyService energy, CraftingService service, CallbackInfo ci) {
        if (job == null || !cluster.isActive() || ((ExecutingCraftingJobAccessorMixin) job).nimblePattern$getLink().isCanceled()) {
            lastProbabilityGameTick = -1;
            return;
        }
        submitToolPatternOutput();
        tickProbability();
    }

    @Inject(method = "getWaitingFor", at = @At("RETURN"), cancellable = true)
    private void getWaitingTool(AEKey key, CallbackInfoReturnable<Long> cir) {
        if (toolController != null) {
            cir.setReturnValue(Math.max(cir.getReturnValue(), toolController.getWaitingAmount(key)));
        }
    }

    @Inject(method = "getAllWaitingFor", at = @At("TAIL"))
    private void addWaitingTool(Set<AEKey> keys, CallbackInfo ci) {
        if (toolController != null) {
            toolController.addWaitingList(keys);
        }
    }
}
