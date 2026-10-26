package com.ber.nimblePattern.mixin.ae2;

import appeng.api.networking.crafting.ICraftingPlan;
import appeng.crafting.CraftingCalculation;
import com.ber.nimblePattern.crafting.LoopSeedValidation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Apply after GTL's overwrite of run, covering both its fast calculator and vanilla AE2.
@Mixin(value = CraftingCalculation.class, remap = false, priority = 1200)
public class CraftingCalculationMixin {
    @Inject(method = "run", at = @At("RETURN"), cancellable = true)
    private void nimblePattern$validateSeeds(CallbackInfoReturnable<ICraftingPlan> cir) {
        var plan = cir.getReturnValue();
        if (plan == null) {
            return;
        }
        var node = ((CraftingCalculationAccessorMixin) this).getSimRequester().getGridNode();
        cir.setReturnValue(LoopSeedValidation.validate(plan,
                node == null ? null : node.getGrid().getStorageService().getInventory()));
    }
}
