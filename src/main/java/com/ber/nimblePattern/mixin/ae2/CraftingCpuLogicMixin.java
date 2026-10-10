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
import com.ber.nimblePattern.crafting.FuzzyOutputLedger;
import com.ber.nimblePattern.crafting.LoopCraftingController;
import com.ber.nimblePattern.crafting.LoopBatchDispatch;
import com.ber.nimblePattern.network.LoopSeedLostNotificationPacket;
import com.ber.nimblePattern.network.NimblePatternNetwork;
import com.ber.nimblePattern.pattern.PatternMapping;
import com.ber.nimblePattern.pattern.wrapper.NimbleProcessingPattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
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

// mod GTLCore will overwrite "executeCrafting", set higher priority to mixin later
@Mixin(value = CraftingCpuLogic.class, remap = false, priority = 1200)
public class CraftingCpuLogicMixin {
    @Shadow
    @Final
    private CraftingCPUCluster cluster;
    @Shadow
    @Final
    private ListCraftingInventory inventory;
    @Unique
    private LoopCraftingController loopController;
    @Unique
    private boolean isLoopFinalOutput;

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

    @Inject(method = "executeCrafting", at = @At("HEAD"))
    private void initializeFlags(CallbackInfoReturnable<Integer> cir) {
        LoopBatchDispatch.clear();
        isLoopFinalOutput = false;
        currentPattern = null;
        isFakePattern = false;
        isFuzzyPattern = false;
    }

    // A failed extraction or push must not leave a count for a later dispatch.
    @Inject(method = "executeCrafting", at = @At("RETURN"))
    private void clearBatchDispatch(CallbackInfoReturnable<Integer> cir) {
        LoopBatchDispatch.clear();
    }

    // record what is the current pushing pattern
    @Redirect(
            method = "executeCrafting",
            at = @At(value = "INVOKE",
                    target = "Lappeng/api/networking/crafting/ICraftingProvider;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z"))
    private boolean trackCurrentPattern(ICraftingProvider provider, IPatternDetails details, KeyCounter[] craftingContainer) {
        long operations = LoopBatchDispatch.take(details);
        boolean result;
        if (details instanceof NimbleProcessingPattern npp && npp.isLoopComposite()) {
            if (loopController == null) loopController = new LoopCraftingController(cluster);
            result = loopController.start(npp, craftingContainer, operations);
        } else {
            result = provider.pushPattern(PatternMapping.getOriginalPattern(provider, details), craftingContainer);
        }
        currentPattern = result && details instanceof NimbleProcessingPattern npp ? npp : null;
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
        if (isLoopFinalOutput) {
            if (link.isStandalone()) return amount;
            long inserted = link.insert(what, amount, mode);
            if (mode == Actionable.MODULATE && inserted > 0) {
                inventory.extract(what, Math.min(inserted, amount), Actionable.MODULATE);
            }
            return inserted;
        }
        return link.insert(what, amount, mode);
    }

    @Redirect(
            method = "insert",
            at = @At(value = "INVOKE",
                    target = "Lappeng/crafting/inv/ListCraftingInventory;extract(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)J"))
    private long fuzzyExtract(ListCraftingInventory inv, AEKey what, long amount, Actionable mode) {
        var receipt = fuzzyOutputLedger.extract(inv, what, amount, mode, (key, accepted) -> {
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

    @Inject(method = "finishJob", at = @At("HEAD"))
    private void injectFinishJob(boolean success, CallbackInfo ci) {
        stopLoop();
        fuzzyOutputLedger.clear();
    }

    @Inject(method = "cancel", at = @At("HEAD"))
    private void injectCancel(CallbackInfo ci) {
        stopLoop();
        fuzzyOutputLedger.clear();
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void injectWriteToNBT(CompoundTag tag, CallbackInfo ci) {
        tag.put("fuzzyOutputLedger", fuzzyOutputLedger.save());
        if (loopController != null) tag.put("loopController", loopController.save());
        else tag.remove("loopController");
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void injectReadFromNBT(CompoundTag tag, CallbackInfo ci) {
        fuzzyOutputLedger.load(tag.getList("fuzzyOutputLedger", Tag.TAG_COMPOUND));
        loopController = null;
        if (tag.contains("loopController", Tag.TAG_COMPOUND)) {
            loopController = new LoopCraftingController(cluster);
            loopController.load(tag.getCompound("loopController"));
            if (job == null) stopLoop();
        }
        if (job == null) {
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

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void receiveLoopResults(AEKey key, long amount, Actionable mode, CallbackInfoReturnable<Long> cir) {
        if (job == null || amount <= 0 || isLoopFinalOutput) return;
        captureLoopOutput(key, amount, mode, cir);
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

    @Unique
    private void stopLoop() {
        LoopBatchDispatch.clear();
        if (loopController != null) {
            for (var failure : loopController.releaseSeeds(inventory)) {
                notifyLoopSeed(failure.key(), failure.returnFailed());
            }
        }
        loopController = null;
    }
    @Unique
    private void notifyLoopSeed(AEKey key, boolean returnFailed) {
        if (job == null) return;
        var playerId = ((ExecutingCraftingJobAccessorMixin) job).nimblePattern$getPlayerId();
        var server = cluster.getLevel().getServer();
        var player = playerId != null && server != null
                ? IPlayerRegistry.getConnected(server, playerId) : null;
        if (player != null) {
            NimblePatternNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new LoopSeedLostNotificationPacket(key, returnFailed));
        }
    }
}
