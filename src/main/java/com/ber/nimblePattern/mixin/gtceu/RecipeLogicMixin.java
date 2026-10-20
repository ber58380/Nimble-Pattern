package com.ber.nimblePattern.mixin.gtceu;

import com.ber.nimblePattern.compat.gtceu.GTMachineTracking;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applied after GTL's overwrites. Both versions share these successful execution boundaries.
 */
@Mixin(value = RecipeLogic.class, remap = false, priority = 1500)
public abstract class RecipeLogicMixin {
    @Shadow
    protected GTRecipe lastRecipe;

    @Shadow
    protected abstract boolean handleRecipeIO(GTRecipe recipe, IO io);

    @Redirect(method = "setupRecipe", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/api/machine/trait/RecipeLogic;handleRecipeIO(Lcom/gregtechceu/gtceu/api/recipe/GTRecipe;Lcom/gregtechceu/gtceu/api/capability/recipe/IO;)Z"))
    private boolean recordStart(RecipeLogic logic, GTRecipe recipe, IO io) {
        boolean accepted = handleRecipeIO(recipe, io);
        if (accepted && io == IO.IN) {
            GTMachineTracking.start(logic, recipe);
        }
        return accepted;
    }

    @Inject(method = "onRecipeFinish", at = @At(value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/api/machine/feature/IRecipeLogicMachine;afterWorking()V"))
    private void recordFinish(CallbackInfo ci) {
        if (lastRecipe != null) {
            GTMachineTracking.finished((RecipeLogic) (Object) this);
        }
    }

    @Inject(method = {"interruptRecipe", "resetRecipeLogic", "inValid"}, at = @At("HEAD"))
    private void invalidateTracking(CallbackInfo ci) {
        GTMachineTracking.invalidate((RecipeLogic) (Object) this);
    }
}
