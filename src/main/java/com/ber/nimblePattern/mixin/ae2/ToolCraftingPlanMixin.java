package com.ber.nimblePattern.mixin.ae2;

import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingPlan;
import appeng.crafting.inv.CraftingSimulationState;
import com.ber.nimblePattern.crafting.ToolCraftingPlan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftingSimulationState.class, remap = false)
public class ToolCraftingPlanMixin {
    @Inject(method = "buildCraftingPlan", at = @At("RETURN"), cancellable = true)
    private static void reserveTools(CraftingSimulationState state, CraftingCalculation calculation, long amount,
            CallbackInfoReturnable<CraftingPlan> cir) {
        // Read the main-thread snapshot, never the live network from the calculation thread.
        var network = ((CraftingCalculationAccessorMixin) calculation).nimblePattern$getNetworkInventory();
        var snapshot = ((NetworkCraftingSnapshotAccessor) network).nimblePattern$getSnapshot();
        cir.setReturnValue(ToolCraftingPlan.reserve(cir.getReturnValue(), snapshot));
    }
}
