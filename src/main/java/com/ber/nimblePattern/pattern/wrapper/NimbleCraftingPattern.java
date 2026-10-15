package com.ber.nimblePattern.pattern.wrapper;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.crafting.pattern.AECraftingPattern;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;

public class NimbleCraftingPattern extends NimblePatternWrapper<AECraftingPattern> implements IMolecularAssemblerSupportedPattern {
    private final List<GenericStack> tools;
    private final IInput[] consumables;
    private final GenericStack[] netOutputs;

    public NimbleCraftingPattern(AECraftingPattern pattern, AEItemKey definition) {
        super(pattern, definition);
        var stack = definition.toStack();
        this.tools = NimblePatternTag.getTool(stack);
        this.consumables = Arrays.stream(pattern.getInputs())
                .filter(input -> tools.stream().noneMatch(tool ->
                        tool.what().equals(input.getPossibleInputs()[0].what())))
                .toArray(IInput[]::new);
        this.netOutputs = Arrays.stream(pattern.getOutputs()).map(output -> {
            // tool might be the output
            long toolOutput = tools.stream().filter(tool -> tool.what().equals(output.what())).mapToLong(GenericStack::amount).sum();
            if (toolOutput > 0) {
                // how much tool is returned through the way of remaining
                long returned = Arrays.stream(pattern.getInputs())
                        .filter(input -> output.what().equals(input.getPossibleInputs()[0].what())
                                && output.what().equals(input.getRemainingKey(output.what())))
                        .mapToLong(IInput::getMultiplier).sum();
                toolOutput = Math.max(0, toolOutput - returned);
            }
            // tool might return through the way of self-growing crafting
            return new GenericStack(output.what(), output.amount() - toolOutput);
        }).filter(output -> output.amount() > 0).toArray(GenericStack[]::new);
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

    public List<GenericStack> getTools() {
        return tools;
    }

    public boolean isToolPattern() {
        return !tools.isEmpty();
    }

    @Override
    public IInput[] getInputs() {
        return consumables;
    }

    @Override
    public GenericStack[] getOutputs() {
        return netOutputs;
    }
}
