package com.ber.nimblePattern.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Exact tool keys, including durability/NBT; marking never makes a breakable tool unbreakable. */
public final class ToolPatternData {
    private ToolPatternData() {}

    public static List<GenericStack> read(ItemStack stack) {
        var root = stack.getTagElement("nimble_pattern");
        if (root == null) return List.of();
        var tools = new ArrayList<GenericStack>();
        for (var tag : root.getList("tools", Tag.TAG_COMPOUND)) {
            var tool = GenericStack.readTag((net.minecraft.nbt.CompoundTag) tag);
            if (tool != null && tool.what() instanceof AEItemKey && tool.amount() > 0) tools.add(tool);
        }
        return List.copyOf(tools);
    }

    public static void write(ItemStack stack, List<GenericStack> tools) {
        var list = new ListTag();
        tools.forEach(tool -> list.add(GenericStack.writeTag(tool)));
        NimblePatternTag.removeLoopTag(stack);
        stack.getOrCreateTagElement("nimble_pattern").put("tools", list);
    }

    public static List<GenericStack> validate(IPatternDetails pattern, List<AEItemKey> selected) {
        if (!(pattern instanceof appeng.crafting.pattern.AECraftingPattern)) {
            throw new IllegalArgumentException("crafting_only");
        }
        var result = new ArrayList<GenericStack>();
        for (var key : selected) {
            long needed = 0;
            for (var input : pattern.getInputs()) {
                var primary = input.getPossibleInputs()[0];
                if (primary.what().equals(key)) {
                    needed = Math.addExact(needed, Math.multiplyExact(primary.amount(), input.getMultiplier()));
                }
            }
            if (needed == 0) throw new IllegalArgumentException("missing_input");
            long produced = 0;
            for (var output : pattern.getOutputs()) {
                if (output.what().equals(key)) produced = Math.addExact(produced, output.amount());
            }
            var item = key.toStack();
            boolean unbreakable = item.hasTag() && item.getTag().getBoolean("Unbreakable");
            if (!unbreakable && produced <= needed) throw new IllegalArgumentException("invalid_tool");
            if (produced <= needed) {
                for (var input : pattern.getInputs()) {
                    if (input.getPossibleInputs()[0].what().equals(key)
                            && !key.equals(input.getRemainingKey(key))) {
                        throw new IllegalArgumentException("not_returned");
                    }
                }
            }
            result.add(new GenericStack(key, needed));
        }
        return List.copyOf(result);
    }
}
