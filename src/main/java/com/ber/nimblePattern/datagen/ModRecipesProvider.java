package com.ber.nimblePattern.datagen;

import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import com.ber.nimblePattern.item.ModItems;
import com.gregtechceu.gtceu.common.data.GTItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Consumer;

public class ModRecipesProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipesProvider(PackOutput packOutput) {
        super(packOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pWriter) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.PATTERN_TAG_TERMINAL.get())
                .requires(AEParts.PATTERN_ACCESS_TERMINAL.asItem())
                .requires(AEItems.LOGIC_PROCESSOR)
                .requires(AEItems.CALCULATION_PROCESSOR)
                .unlockedBy("has_access_terminal", has(AEParts.PATTERN_ACCESS_TERMINAL.asItem()))
                .save(pWriter);

        // loop storage cell
        registerLoopStorageCellRecipe(pWriter, ModItems.LOOP_STORAGE_CELL_1K.get(), AEItems.ITEM_CELL_1K, AEItems.FLUID_CELL_1K);
        registerLoopStorageCellRecipe(pWriter, ModItems.LOOP_STORAGE_CELL_4K.get(), AEItems.ITEM_CELL_4K, AEItems.FLUID_CELL_4K);
        registerLoopStorageCellRecipe(pWriter, ModItems.LOOP_STORAGE_CELL_16K.get(), AEItems.ITEM_CELL_16K, AEItems.FLUID_CELL_16K);
        registerLoopStorageCellRecipe(pWriter, ModItems.LOOP_STORAGE_CELL_64K.get(), AEItems.ITEM_CELL_64K, AEItems.FLUID_CELL_64K);
        registerLoopStorageCellRecipe(pWriter, ModItems.LOOP_STORAGE_CELL_256K.get(), AEItems.ITEM_CELL_256K, AEItems.FLUID_CELL_256K);
    }

    private void registerLoopStorageCellRecipe(Consumer<FinishedRecipe> pWriter, ItemLike loopCell, ItemLike itemCell, ItemLike fluidCell) {
        var recipeId = ForgeRegistries.ITEMS.getKey(loopCell.asItem());
        ConditionalRecipe.builder()
                .addCondition(modLoaded("gtceu"))
                .addRecipe(consumer ->
                        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, loopCell)
                                .requires(itemCell)
                                .requires(fluidCell)
                                .requires(GTItems.CRYSTAL_CENTRAL_PROCESSING_UNIT.get())
                                .unlockedBy("has_item_cell", has(itemCell))
                                .save(consumer, recipeId)
                )
                .addCondition(not(modLoaded("gtceu")))
                .addRecipe(consumer ->
                        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, loopCell)
                                .requires(itemCell)
                                .requires(fluidCell)
                                .requires(Items.AMETHYST_SHARD)
                                .unlockedBy("has_item_cell", has(itemCell))
                                .save(consumer, recipeId)
                )
                .generateAdvancement()
                .build(pWriter, recipeId);
    }
}
