package com.ber.nimblePattern.mixin.ae2;

import com.ber.nimblePattern.probability.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.gregtechceu.gtceu.api.machine.trait.RecipeLogic", remap = false, priority = 1500)
public class ProbabilityGtCompletionMixin implements ProbabilityMachines.Tracked {
    @Unique private ResourceLocation nimble$finishedRecipe;
    @Unique private long nimble$finishedOperations;
    @Inject(method = "onRecipeFinish", at = @At("HEAD"), require = 0)
    private void capture(CallbackInfo ci) {
        nimble$finishedRecipe = null;
        if (!ProbabilityMachines.hasTickets(this)) return;
        try {
            Object origin = ProbabilityRecipes.call(this, "getLastOriginRecipe");
            Object actual = ProbabilityRecipes.call(this, "getLastRecipe");
            Object identity = origin instanceof Recipe<?> ? origin : actual;
            if (identity instanceof Recipe<?> recipe && actual != null) {
                nimble$finishedRecipe = recipe.getId();
                nimble$finishedOperations = Math.max(1, ((Number) actual.getClass().getField("parallels").get(actual)).longValue());
            }
        } catch (ReflectiveOperationException ignored) { }
    }
    @Inject(method = "onRecipeFinish", at = @At("TAIL"), require = 0)
    private void finished(CallbackInfo ci) {
        ProbabilityMachines.completed(this, nimble$finishedRecipe, nimble$finishedOperations);
    }
}
