package com.ber.nimblePattern.mixin.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.crafting.CraftBranchFailure;
import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingTreeProcess;
import appeng.crafting.inv.CraftingSimulationState;
import com.ber.nimblePattern.item.storage.LoopStorageCellAccess;
import com.ber.nimblePattern.pattern.wrapper.NimbleProcessingPattern;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftingTreeProcess.class, remap = false)
public class CraftingTreeProcessMixin {
    @Shadow
    @Final
    private IPatternDetails details;
    @Shadow
    @Final
    private CraftingCalculation job;

    @Unique
    private boolean missingLoopSeed;

    @Inject(method = "request", at = @At("HEAD"))
    private void validateLoopSeed(CraftingSimulationState inventory, long times, CallbackInfo ci) throws CraftBranchFailure {
        if (!(details instanceof NimbleProcessingPattern pattern) || !pattern.isLoopComposite()) {
            return;
        }
        var loopData = pattern.getLoopData();
        var calculation = (CraftingCalculationMixin) job;
        var gridNode = calculation.getSimRequester().getGridNode();
        boolean seedAvailable = gridNode != null
                && LoopStorageCellAccess.find(
                gridNode.getGrid().getStorageService().getInventory(),
                loopData.storageCellId(),
                loopData.entry().what(),
                loopData.seedAmount()
        ) != null;
        if (seedAvailable) {
            return;
        }
        if (!job.isSimulation()) {
            throw new CraftBranchFailure(loopData.entry().what(), loopData.seedAmount());
        }
        if (!missingLoopSeed) {
            calculation.invokeAddMissing(loopData.entry().what(), loopData.seedAmount());
            missingLoopSeed = true;
        }
    }
}
