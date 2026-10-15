package com.ber.nimblePattern.mixin.ae2;

import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingPlan;
import appeng.crafting.inv.CraftingSimulationState;
import com.ber.nimblePattern.crafting.ToolReservation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftingSimulationState.class, remap = false)
public class CraftingSimulationStateMixin {
    @Inject(method = "buildCraftingPlan", at = @At("RETURN"), cancellable = true)
    private static void reserveTool(CraftingSimulationState state, CraftingCalculation calculation, long calculatedAmount, CallbackInfoReturnable<CraftingPlan> cir) {
        var network = ((CraftingCalculationAccessorMixin) calculation).getNetworkInv();
        var snapshot = ((NetworkCraftingSimulationStateAccessorMixin) network).getSnapshot();
        var plan = cir.getReturnValue();

        var tools = ToolReservation.getToolReservation(plan.patternTimes());
        if (tools.isEmpty()) {
            return;
        }
        var used = new KeyCounter();
        used.addAll(plan.usedItems());
        var missing = new KeyCounter();
        missing.addAll(plan.missingItems());
        for (var tool : tools) {
            long total = Math.addExact(used.get(tool.getKey()), tool.getLongValue());
            used.set(tool.getKey(), total);
            long deficit = Math.max(0, total - snapshot.get(tool.getKey()));
            missing.add(tool.getKey(), deficit);
        }
        missing.removeZeros();
        cir.setReturnValue(new CraftingPlan(plan.finalOutput(), Math.addExact(plan.bytes(), tools.size() * 8L),
                plan.simulation() || !missing.isEmpty(), plan.multiplePaths(),
                used, plan.emittedItems(), missing, plan.patternTimes()));
    }
}
