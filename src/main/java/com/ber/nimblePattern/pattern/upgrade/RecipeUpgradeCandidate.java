package com.ber.nimblePattern.pattern.upgrade;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

/**
 * The recommending upgrade recipe for the old recipe.
 */
public record RecipeUpgradeCandidate(Recipe<?> originalRecipe, Recipe<?> recipe, ItemStack replacement,
                                     UpgradePreview preview) {
}
