package com.ber.nimblePattern.compat.gtceu;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.ber.nimblePattern.pattern.wrapper.NimbleProcessingPattern;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public final class GTRecipeMatcher {
    private GTRecipeMatcher() {
    }

    private static boolean matchesInput(GTRecipe recipe, AEKey key) {
        if (key instanceof AEItemKey itemKey) {
            var stack = itemKey.toStack();
            for (var content : recipe.getInputContents(ItemRecipeCapability.CAP)) {
                if (content.content instanceof Ingredient ingredient && ingredient.test(stack)) {
                    return true;
                }
                if (content.content instanceof ItemStack required && !required.isEmpty() && key.equals(AEItemKey.of(required))) {
                    return true;
                }
            }
            return false;
        } else if (key instanceof AEFluidKey fluidKey) {
            var stack = FluidStack.create(fluidKey.getFluid(), 1, fluidKey.getTag());
            for (var content : recipe.getInputContents(FluidRecipeCapability.CAP)) {
                if (content.content instanceof FluidIngredient ingredient && ingredient.test(stack)) {
                    return true;
                }
                if (content.content instanceof FluidStack required && !required.isEmpty() && key.equals(AEFluidKey.of(required.getFluid(), required.getTag()))) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private static boolean isSameOutput(AEKey expected, AEKey actual, boolean fuzzyMode) {
        return fuzzyMode ? expected.dropSecondary().equals(actual.dropSecondary()) : expected.equals(actual);
    }


    private static boolean matchesOutput(GTRecipe recipe, AEKey expected, boolean fuzzyMode) {
        if (expected instanceof AEItemKey) {
            for (var content : recipe.getOutputContents(ItemRecipeCapability.CAP)) {
                if (content.content instanceof Ingredient ingredient) {
                    for (var stack : ingredient.getItems()) {
                        if (!stack.isEmpty() && isSameOutput(expected, AEItemKey.of(stack), fuzzyMode)) {
                            return true;
                        }
                    }
                } else if (content.content instanceof ItemStack stack) {
                    if (!stack.isEmpty() && isSameOutput(expected, AEItemKey.of(stack), fuzzyMode)) {
                        return true;
                    }
                }
            }
            return false;
        } else if (expected instanceof AEFluidKey) {
            for (var content : recipe.getOutputContents(FluidRecipeCapability.CAP)) {
                if (content.content instanceof FluidIngredient ingredient) {
                    for (var stack : ingredient.getStacks()) {
                        if (!stack.isEmpty() && isSameOutput(expected, AEFluidKey.of(stack.getFluid(), stack.getTag()), fuzzyMode)) {
                            return true;
                        }
                    }
                } else if (content.content instanceof FluidStack stack) {
                    if (!stack.isEmpty() && isSameOutput(expected, AEFluidKey.of(stack.getFluid(), stack.getTag()), fuzzyMode)) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    /**
     * Check if the delivered inputs and primary output of the probability pattern matches a GT recipe or not
     */
    public static boolean matches(NimbleProcessingPattern pattern, List<GenericStack> deliveredInputs, GTRecipe recipe) {
        if (deliveredInputs.isEmpty()) {
            return false;
        }
        // Check the inputs of recipe contain the delivered inputs
        boolean hasInput = false;
        for (var input : deliveredInputs) {
            if (input.amount() <= 0) {
                continue;
            }
            hasInput = true;
            if (!matchesInput(recipe, input.what())) {
                return false;
            }
        }
        if (!hasInput) {
            return false;
        }

        // Check the outputs of recipe contain the primary outputs of the probability pattern
        return matchesOutput(recipe, pattern.getPrimaryOutput().what(), pattern.getFuzzyMode());
    }
}
