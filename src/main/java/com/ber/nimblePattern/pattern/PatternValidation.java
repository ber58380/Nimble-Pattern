package com.ber.nimblePattern.pattern;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.menu.AutoCraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.BookItem;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * This class is used to validate a pattern whether is a fake pattern, tool pattern or not.
 */
public final class PatternValidation {
    private PatternValidation() {
    }

    /**
     * Determine whether the pattern is a fake pattern.
     * The fake pattern must be a processing pattern with only one renamed book as the solely output.
     */
    public static boolean isFakePattern(AEProcessingPattern pattern) {
        var outputs = pattern.getOutputs();
        if (outputs.length != 1) {
            return false;
        }
        if (!(outputs[0].what() instanceof AEItemKey key)) {
            return false;
        }
        var stack = key.toStack();
        if (!stack.hasCustomHoverName()) {
            return false;
        }
        var item = stack.getItem();
        return item instanceof BookItem;
    }

    /**
     * Determine whether the pattern is a tool pattern.
     * A tool pattern must be a crafting pattern with tools as input. The tools should not
     * have any changes after crafting and can't be damaged.
     * Return a list of all input items matching the tool rules.
     * When the list is not empty, it means the pattern is a tool pattern.
     * Called when coding pattern.
     */
    public static List<GenericStack> isToolPattern(AECraftingPattern pattern) {
        var inputs = pattern.getSparseInputs();
        var grid = new TransientCraftingContainer(new AutoCraftingMenu(), 3, 3);
        for (int slot = 0; slot < inputs.length; slot++) {
            var input = inputs[slot];
            if (input == null) {
                continue;
            }
            if (!(input.what() instanceof AEItemKey key) || input.amount() != 1) {
                return List.of();
            }
            grid.setItem(slot, key.toStack());
        }

        var remaining = pattern.getRemainingItems(grid);
        if (remaining.size() != grid.getContainerSize()) {
            return List.of();
        }
        var tools = new LinkedHashMap<AEItemKey, Long>();
        var consumed = new HashSet<AEItemKey>();
        for (int slot = 0; slot < inputs.length; slot++) {
            var input = inputs[slot];
            if (input == null) {
                continue;
            }
            var key = (AEItemKey) input.what();
            var original = key.toStack();
            var remainder = remaining.get(slot);
            if (input.amount() == remainder.getCount() &&
                    ItemStack.isSameItemSameTags(original, remainder) &&
                    !original.isDamageableItem()) {
                tools.merge(key, input.amount(), Math::addExact);
            } else {
                consumed.add(key);
            }
        }
        consumed.forEach(tools::remove);
        return tools.entrySet().stream()
                .map(entry -> new GenericStack(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * Determine a given tool is the tool in pattern or not. Called by pattern tag terminal.
     * This method could also detect the pattern that is self-growing.
     */
    public static List<GenericStack> isToolPattern(AECraftingPattern pattern, AEItemKey tool) {
        long needed = 0;
        for (var input : pattern.getInputs()) {
            var primaryInput = input.getPossibleInputs()[0];
            if (primaryInput.what().equals(tool)) {
                needed = Math.addExact(needed, Math.multiplyExact(primaryInput.amount(), input.getMultiplier()));
            }
        }
        if (needed == 0) {
            throw new IllegalArgumentException("tool_is_not_input");
        }

        // If the pattern is self-growing, consider it as a tool pattern.
        long produced = 0;
        for (var output : pattern.getOutputs()) {
            if (tool.equals(output.what())) {
                produced = Math.addExact(produced, output.amount());
            }
        }
        if (produced > needed) {
            return List.of(new GenericStack(tool, needed));
        }

        // Validate the tool would be remained after crafting
        if (tool.toStack().isDamageableItem()) {
            throw new IllegalArgumentException("tool_is_damageable");
        }
        for (var input : pattern.getInputs()) {
            var primaryInput = input.getPossibleInputs()[0];
            if (tool.equals(primaryInput.what()) && !tool.equals(input.getRemainingKey(tool))) {
                throw new IllegalArgumentException("tool_changed");
            }
        }
        return List.of(new GenericStack(tool, needed));
    }

}
