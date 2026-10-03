package com.ber.nimblePattern.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.features.IPlayerRegistry;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftingLink;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import com.ber.nimblePattern.crafting.LoopCraftingController;
import com.ber.nimblePattern.crafting.LoopBatchDispatch;
import com.ber.nimblePattern.network.LoopSeedLostNotificationPacket;
import com.ber.nimblePattern.network.NimblePatternNetwork;
import com.ber.nimblePattern.pattern.NimbleEncodedPattern;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

// mod GTLCore will overwrite "executeCrafting", set higher priority to mixin later
@Mixin(value = CraftingCpuLogic.class, remap = false, priority = 1200)
public class CraftingCpuLogicMixin implements com.ber.nimblePattern.probability.ProbabilityTools {
    @Override public long nimble$toolsHeld(AEKey key) { return toolController == null ? 0 : toolController.held(key); }
    @Override public void nimble$reserveTools(KeyCounter tools) { if (!tools.isEmpty()) tools().reserve(tools, inventory); }
    @Override public boolean nimble$specialOutputPending(AEKey key) {
        return toolController != null && toolController.waiting(key) > 0 || loopController != null && loopController.isActive();
    }
    @Shadow
    @Final
    private CraftingCPUCluster cluster;
    @Shadow
    @Final
    private ListCraftingInventory inventory;
    @Shadow
    private @Nullable ExecutingCraftingJob job;

    @Unique
    private LoopCraftingController loopController;
    @Unique
    private com.ber.nimblePattern.crafting.ToolCraftingController toolController;
    @Unique private com.ber.nimblePattern.crafting.ToolCraftingController tools() {
        if (toolController == null) toolController = new com.ber.nimblePattern.crafting.ToolCraftingController();
        return toolController;
    }

    @Inject(method = "trySubmitJob", at = @At("RETURN"))
    private void reserveJobTools(appeng.api.networking.IGrid grid,
            appeng.api.networking.crafting.ICraftingPlan plan,
            appeng.api.networking.security.IActionSource source,
            appeng.api.networking.crafting.ICraftingRequester requester,
            CallbackInfoReturnable<appeng.api.networking.crafting.ICraftingSubmitResult> cir) {
        if (cir.getReturnValue().successful()) {
            var reserved = com.ber.nimblePattern.crafting.ToolCraftingPlan.reservations(plan.patternTimes());
            if (!reserved.isEmpty()) { tools().reserve(reserved, inventory); cluster.markDirty(); }
        }
    }

    @Inject(method = "tickCraftingLogic", at = @At("HEAD"))
    private void finishToolStep(IEnergyService energy, CraftingService service, CallbackInfo ci) {
        if (job == null || toolController == null || !toolController.isActive()) return;
        var results = new java.util.ArrayList<>(toolController.finishStep());
        if (results.isEmpty()) return;
        var cpu = (CraftingCpuLogic) (Object) this;
        var finalOutput = cpu.getFinalJobOutput();
        // Deliver container remainders before the final output can cause finishJob to release reserved tools.
        results.sort(java.util.Comparator.comparing(stack -> stack.what().matches(finalOutput)));
        for (var output : results) {
            if (job != null && output.what().matches(finalOutput)) {
                inventory.insert(output.what(), output.amount(), Actionable.MODULATE);
                long completed = Math.min(output.amount(),
                        ((ExecutingCraftingJobAccessorMixin) job).nimblePattern$getRemainingAmount());
                isLoopFinalOutput = true;
                try {
                    cpu.insert(output.what(), completed, Actionable.MODULATE);
                } finally {
                    isLoopFinalOutput = false;
                }
            } else {
                long accepted = cpu.insert(output.what(), output.amount(), Actionable.MODULATE);
                if (accepted < output.amount()) {
                    inventory.insert(output.what(), output.amount() - accepted, Actionable.MODULATE);
                }
            }
        }
        if (!results.isEmpty()) cluster.markDirty();
    }

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void receiveSpecialResults(AEKey key, long amount, Actionable mode, CallbackInfoReturnable<Long> cir) {
        if (job == null || amount <= 0 || isLoopFinalOutput) return;
        // Physical tool returns and loop intermediates own their receipts before probability supplies.
        receiveToolResults(key, amount, mode, cir);
        if (cir.isCancelled()) return;
        captureLoopOutput(key, amount, mode, cir);
        if (cir.isCancelled()) return;
        var probabilityCpu = (com.ber.nimblePattern.probability.ProbabilityCpu) this;
        if (!probabilityCpu.nimble$hasProbability() || nimble$specialOutputPending(key)) return;
        var probability = probabilityCpu.nimble$probability();
        long output = ((com.ber.nimblePattern.probability.ProbabilityJob) job).nimble$waiting()
                .extract(key, amount, Actionable.SIMULATE);
        if (output == 0) {
            long accepted = probability.collect(key, amount, mode, inventory);
            if (accepted > 0) {
                if (mode == Actionable.MODULATE) cluster.markDirty();
                cir.setReturnValue(accepted);
            }
        }
    }

    @Unique
    private void receiveToolResults(AEKey key, long amount, Actionable mode, CallbackInfoReturnable<Long> cir) {
        if (job == null || toolController == null) return;
        long accepted = toolController.accept(key, amount, mode);
        if (accepted > 0) {
            if (mode == Actionable.MODULATE) cluster.markDirty();
            cir.setReturnValue(accepted);
        }
    }

    @Inject(method = "getWaitingFor", at = @At("RETURN"), cancellable = true)
    private void toolWaiting(AEKey key, CallbackInfoReturnable<Long> cir) {
        if (toolController != null) cir.setReturnValue(Math.max(cir.getReturnValue(), toolController.waiting(key)));
    }

    @Inject(method = "getAllWaitingFor", at = @At("TAIL"))
    private void allToolWaiting(java.util.Set<AEKey> keys, CallbackInfo ci) { if (toolController != null) toolController.addWaiting(keys); }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void saveTools(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        if (toolController != null) tag.put("nimble_tools", toolController.save());
        else tag.remove("nimble_tools");
        tag.put("nimble_fuzzy", fuzzyOutputs.save());
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void loadTools(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        toolController = null;
        if (tag.contains("nimble_tools", 10)) tools().load(tag.getCompound("nimble_tools"));
        fuzzyOutputs.load(tag.getList("nimble_fuzzy", 10));
        if (job == null) fuzzyOutputs.clear();
        if (job == null && toolController != null) toolController.release(inventory);
    }
    @Unique
    // {fuzzyKey: {exactKey: amount}}
    private final com.ber.nimblePattern.crafting.FuzzyOutputLedger fuzzyOutputs = new com.ber.nimblePattern.crafting.FuzzyOutputLedger();
    @Unique
    private NimbleEncodedPattern currentPattern;
    @Unique
    private boolean isFakePattern;
    @Unique
    private boolean isFuzzyPattern;
    @Unique
    private boolean isLoopFinalOutput;

    @Inject(method = "executeCrafting", at = @At("HEAD"))
    private void initializeFlags(CallbackInfoReturnable<Integer> cir) {
        LoopBatchDispatch.clear();
        currentPattern = null;
        isFakePattern = false;
        isFuzzyPattern = false;
        isLoopFinalOutput = false;
    }

    @Inject(method = "executeCrafting", at = @At("HEAD"), cancellable = true)
    private void executeActiveLoop(int maxPatterns, CraftingService craftingService, IEnergyService energyService,
            Level level, CallbackInfoReturnable<Integer> cir) {
        if (loopController != null && loopController.isActive()) {
            cir.setReturnValue(loopController.tick(craftingService, energyService, level));
        }
    }

    // record what is the current pushing pattern
    @Redirect(
            method = "executeCrafting",
            at = @At(value = "INVOKE",
                    target = "Lappeng/api/networking/crafting/ICraftingProvider;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z"))
    private boolean trackCurrentPattern(ICraftingProvider provider, IPatternDetails details, KeyCounter[] craftingContainer) {
        long operations = LoopBatchDispatch.take(details);
        boolean result;
        if (details instanceof com.ber.nimblePattern.pattern.NimbleAssemblerPattern tool && tool.isToolPattern()) {
            result = tools().push(tool, provider, craftingContainer, cluster.getLevel());
        } else if (details instanceof NimbleEncodedPattern npp && npp.isLoopComposite()) {
            if (loopController == null) loopController = new LoopCraftingController(cluster);
            result = (toolController == null || !toolController.isActive())
                    && loopController.start(npp, craftingContainer, operations);
        } else if (details instanceof NimbleEncodedPattern npp && npp.getLoopMode()) {
            // A direct request for an item inside the loop first runs the collapsed full loop above. The remaining
            // prefix patterns then use AE's normal output accounting so the requested item reaches its requester.
            result = provider.pushPattern(com.ber.nimblePattern.pattern.ProviderPatternIndex.nativePattern(provider, details), craftingContainer);
        } else if (details instanceof NimbleEncodedPattern npp
                && npp.metadata().probability()) {
            result = provider.pushPattern(com.ber.nimblePattern.pattern.ProviderPatternIndex.nativePattern(provider, details), craftingContainer);
        } else {
            result = provider.pushPattern(com.ber.nimblePattern.pattern.ProviderPatternIndex.nativePattern(provider, details), craftingContainer);
        }
        currentPattern = result && details instanceof NimbleEncodedPattern npp ? npp : null;
        if (result && (currentPattern != null ? currentPattern.metadata().probability()
                : com.ber.nimblePattern.probability.ProbabilityEncoding.marked(details.getDefinition().toStack())))
            ((com.ber.nimblePattern.probability.ProbabilityCpu) this).nimble$probability()
                .dispatched(details, provider, cluster, operations);
        return result;
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
                    isLoopFinalOutput = true;
                    try {
                        ((CraftingCpuLogic) (Object) this).insert(output.what(), completed, Actionable.MODULATE);
                    } finally {
                        isLoopFinalOutput = false;
                    }
                }
            } else {
                // Intermediate loop outputs remain available to the rest of the crafting plan.
                ((CraftingCpuLogic) (Object) this).insert(output.what(), output.amount(), Actionable.MODULATE);
            }
        }
        cir.setReturnValue(acceptance.accepted());
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
        // fuzzy pattern
        if (currentPattern != null && currentPattern.getFuzzyMode()) {
            // record fuzzy outputs by fuzzy keys
            if (mode == Actionable.MODULATE) fuzzyOutputs.add(what, amount);
        }
        // register output for normal and fuzzy patterns
        long credit = mode == Actionable.MODULATE && ((com.ber.nimblePattern.probability.ProbabilityCpu) this).nimble$hasProbability()
                ? ((com.ber.nimblePattern.probability.ProbabilityCpu) this).nimble$probability().consumeCredit(what, amount) : 0;
        inv.insert(what, amount - credit, mode);
    }

    @Redirect(
            method = "insert",
            at = @At(value = "INVOKE",
                    target = "Lappeng/crafting/CraftingLink;insert(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)J"))
    private long fakeInsert(CraftingLink link, AEKey what, long amount, Actionable mode) {
        // When insert function is invoked, might not invoke new executeCrafting, so currentPattern
        // might not latest. Use a boolean flag variable instead to clarify the source of invoke.
        if (isFakePattern) {
            // simulate the output is back to the network
            return amount;
        }
        if (isLoopFinalOutput) {
            // A player-started job has no requester. Acknowledge it here and let finishJob store every physical loop
            // output from the CPU inventory. For crafting-card requesters, retain AE's delivery behavior and remove
            // only the amount the requester actually accepted from that inventory.
            if (link.isStandalone()) {
                return amount;
            }
            long inserted = link.insert(what, amount, mode);
            if (mode == Actionable.MODULATE && inserted > 0) {
                inventory.extract(what, Math.min(inserted, amount), Actionable.MODULATE);
            }
            return inserted;
        }
        if (((com.ber.nimblePattern.probability.ProbabilityCpu) this).nimble$hasProbability() && link.isStandalone()) {
            // Own the physical final output until finishJob stores it. Otherwise AE's standalone link returns zero
            // and a second CPU can count the very same incoming items toward its own task.
            inventory.insert(what, amount, mode);
            return amount;
        }
        return link.insert(what, amount, mode);
    }

    @Redirect(
            method = "insert",
            at = @At(value = "INVOKE",
                    target = "Lappeng/crafting/inv/ListCraftingInventory;extract(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)J"))
    private long fuzzyExtract(ListCraftingInventory inv, AEKey what, long amount, Actionable mode) {
        var receipt = fuzzyOutputs.extract(inv, what, amount, mode,
                this::receivedProbabilityOutput);
        isFuzzyPattern = receipt.fuzzy();
        return receipt.amount();
    }

    @Unique private void receivedProbabilityOutput(AEKey key, long amount) {
        var probabilityCpu = (com.ber.nimblePattern.probability.ProbabilityCpu) this;
        if (probabilityCpu.nimble$hasProbability()) probabilityCpu.nimble$probability().received(key, amount);
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

    @Inject(method = "finishJob", at = @At("HEAD"))
    private void injectFinishJob(boolean success, CallbackInfo ci) {
        // Also clean up when AE finishes a job through a path other than cancel().
        injectCancel(ci);
        fuzzyOutputs.clear();
    }

    @Inject(method = "cancel", at = @At("HEAD"))
    private void injectCancel(CallbackInfo ci) {
        if (toolController != null) toolController.release(inventory);
        toolController = null;
        if (loopController != null && loopController.isActive()) {
            var lostSeed = loopController.cancelAndReturnSeed(inventory);
            if (lostSeed != null && job != null) {
                var playerId = ((ExecutingCraftingJobAccessorMixin) job).nimblePattern$getPlayerId();
                var server = cluster.getLevel().getServer();
                var player = playerId != null && server != null
                        ? IPlayerRegistry.getConnected(server, playerId)
                        : null;
                if (player != null) {
                    NimblePatternNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new LoopSeedLostNotificationPacket(lostSeed));
                }
            }
        }
        LoopBatchDispatch.clear();
        fuzzyOutputs.clear();
    }
}
