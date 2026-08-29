package com.ber.nimblePattern.datagen;

import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import com.ber.nimblePattern.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;

import java.util.function.Consumer;

public class ModRecipesProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipesProvider(PackOutput packOutput) {
        super(packOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pWriter) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.PATTERN_UPGRADE_TERMINAL.get())
                .requires(AEParts.PATTERN_ACCESS_TERMINAL.asItem())
                .requires(AEItems.LOGIC_PROCESSOR)
                .requires(AEItems.CALCULATION_PROCESSOR)
                .unlockedBy("has_access_terminal", has(AEParts.PATTERN_ACCESS_TERMINAL.asItem()))
                .save(pWriter);
    }
}
