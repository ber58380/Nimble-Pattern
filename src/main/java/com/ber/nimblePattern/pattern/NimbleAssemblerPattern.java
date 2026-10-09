package com.ber.nimblePattern.pattern;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NimbleAssemblerPattern extends NimblePatternWrapper<IMolecularAssemblerSupportedPattern> implements IMolecularAssemblerSupportedPattern {

    public NimbleAssemblerPattern(IMolecularAssemblerSupportedPattern pattern, AEItemKey definition) {
        super(pattern, definition);
    }

    @Override
    public ItemStack assemble(Container container, Level level) {
        return pattern.assemble(container, level);
    }

    @Override
    public boolean isItemValid(int slot, AEItemKey key, Level level) {
        return pattern.isItemValid(slot, key, level);
    }

    @Override
    public boolean isSlotEnabled(int slot) {
        return pattern.isSlotEnabled(slot);
    }

    @Override
    public void fillCraftingGrid(KeyCounter[] table, CraftingGridAccessor gridAccessor) {
        pattern.fillCraftingGrid(table, gridAccessor);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        return pattern.getRemainingItems(container);
    }
}
