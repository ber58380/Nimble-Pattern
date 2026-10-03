package com.ber.nimblePattern.mixin.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.crafting.CraftBranchFailure;
import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingTreeProcess;
import appeng.crafting.inv.CraftingSimulationState;
import com.ber.nimblePattern.item.storage.LoopStorageCellAccess;
import com.ber.nimblePattern.pattern.NimbleEncodedPattern;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Validates the reserved loop seed without exposing it to AE as an ordinary consumable input. */
@Mixin(value = CraftingTreeProcess.class, remap = false)
public class CraftingTreeProcessMixin {
    @Shadow
    @Final
    private IPatternDetails details;
    @Shadow
    @Final
    private CraftingCalculation job;

    @Unique
    private boolean nimblePattern$missingSeedReported;

    @Inject(method = "request", at = @At("HEAD"))
    private void validateLoopSeed(CraftingSimulationState inventory, long times, CallbackInfo ci)
            throws CraftBranchFailure {
        if (!(details instanceof NimbleEncodedPattern pattern) || !pattern.isLoopComposite()) {
            return;
        }

        var data = pattern.getLoopData();
        var calculation = (CraftingCalculationAccessorMixin) job;
        var gridNode = calculation.nimblePattern$getSimulationRequester().getGridNode();
        boolean seedAvailable = gridNode != null
                && LoopStorageCellAccess.find(
                        gridNode.getGrid().getStorageService().getInventory(),
                        data.storageCellId(),
                        data.entry().what(),
                        data.seedAmount()) != null;
        if (seedAvailable) {
            return;
        }

        if (!job.isSimulation()) {
            throw new CraftBranchFailure(data.entry().what(), data.seedAmount());
        }
        if (!nimblePattern$missingSeedReported) {
            calculation.nimblePattern$addMissing(data.entry().what(), data.seedAmount());
            nimblePattern$missingSeedReported = true;
        }
    }
}
