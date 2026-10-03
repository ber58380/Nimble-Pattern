package com.ber.nimblePattern.pattern;

import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.stacks.GenericStack;
import java.util.List;
import java.util.Arrays;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Preserves the assembler capability when wrapping crafting, smithing or stonecutting patterns. */
public final class NimbleAssemblerPattern extends NimbleEncodedPattern implements IMolecularAssemblerSupportedPattern {
    private final IMolecularAssemblerSupportedPattern delegate;
    private final List<GenericStack> tools;
    private final IInput[] consumables;
    private final GenericStack[] netOutputs;

    public NimbleAssemblerPattern(IMolecularAssemblerSupportedPattern pattern, boolean fuzzy) {
        super(pattern, fuzzy);
        delegate = pattern;
        var tagged = metadata().tools();
        List<GenericStack> validated = List.of();
        if (!tagged.isEmpty()) {
            try {
                validated = ToolPatternData.validate(pattern,
                        tagged.stream().map(stack -> (AEItemKey) stack.what()).distinct().toList());
            } catch (IllegalArgumentException | ArithmeticException ignored) {
                // Old/edited tags cannot turn a consuming recipe into a reusable one.
            }
        }
        tools = validated;
        consumables = Arrays.stream(super.getInputs())
                .filter(input -> tools.stream().noneMatch(tool ->
                        tool.what().equals(input.getPossibleInputs()[0].what())))
                .toArray(IInput[]::new);
        netOutputs = Arrays.stream(super.getOutputs()).map(output -> {
            long reserved = tools.stream().filter(tool -> tool.what().equals(output.what()))
                    .mapToLong(GenericStack::amount).sum();
            if (reserved > 0) {
                long returned = Arrays.stream(pattern.getInputs())
                        .filter(input -> output.what().equals(input.getPossibleInputs()[0].what())
                                && output.what().equals(input.getRemainingKey(output.what())))
                        .mapToLong(IInput::getMultiplier).sum();
                reserved = Math.max(0, reserved - returned);
            }
            return new GenericStack(output.what(), Math.max(0, output.amount() - reserved));
        }).filter(output -> output.amount() > 0).toArray(GenericStack[]::new);
    }

    public List<GenericStack> getTools() { return tools; }
    public boolean isToolPattern() { return !tools.isEmpty(); }
    @Override public boolean getFakeMode() { return !isToolPattern() && super.getFakeMode(); }
    @Override public IInput[] getInputs() { return consumables; }
    @Override public GenericStack[] getOutputs() { return netOutputs; }

    @Override public ItemStack assemble(Container container, Level level) {
        return delegate.assemble(container, level);
    }
    @Override public boolean isItemValid(int slot, AEItemKey key, Level level) {
        return delegate.isItemValid(slot, key, level);
    }
    @Override public boolean isSlotEnabled(int slot) {
        return delegate.isSlotEnabled(slot);
    }
    @Override public void fillCraftingGrid(KeyCounter[] table, CraftingGridAccessor accessor) {
        delegate.fillCraftingGrid(table, accessor);
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        return delegate.getRemainingItems(container);
    }
}
