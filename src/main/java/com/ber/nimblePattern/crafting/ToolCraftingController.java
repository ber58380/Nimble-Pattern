package com.ber.nimblePattern.crafting;

import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.crafting.inv.ListCraftingInventory;
import com.ber.nimblePattern.pattern.PatternMapping;
import com.ber.nimblePattern.pattern.wrapper.NimbleCraftingPattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ToolCraftingController {
    // The tools reserved in CPU that can be used for crafting
    private final ListCraftingInventory reserved = new ListCraftingInventory(key -> {
    });
    // The tools used by crafting and haven't been returned yet
    private final ListCraftingInventory borrowed = new ListCraftingInventory(key -> {
    });
    // The waiting output and remainder of the crafting
    private final ListCraftingInventory expected = new ListCraftingInventory(key -> {
    });
    // Returned items but haven't been settled
    private final ListCraftingInventory received = new ListCraftingInventory(key -> {
    });
    private boolean active;

    public boolean isActive() {
        return active;
    }

    /**
     * Reserve target amount of tools for crafting
     */
    public void reserve(KeyCounter tools, ListCraftingInventory cpu) {
        for (var tool : tools) {
            var key = tool.getKey();
            // tool.getLongValue() is the required amount of tool
            long extracted = cpu.extract(tool.getKey(), Math.max(0, tool.getLongValue() - reserved.list.get(key) - borrowed.list.get(key)), Actionable.MODULATE);
            reserved.insert(key, extracted, Actionable.MODULATE);
        }
    }

    /**
     * Provide tools for the pattern and push the pattern to execute crafting.
     */
    public boolean push(NimbleCraftingPattern pattern, ICraftingProvider provider, KeyCounter[] consumables, Level level) {
        if (active) {
            return false;
        }
        // Build inventory of the pattern
        var available = new ListCraftingInventory(key -> {
        });
        for (var consumable : consumables) {
            available.list.addAll(consumable);
        }
        for (var tool : pattern.getTools()) {
            // Check if there are enough tools have been reserved already
            if (reserved.extract(tool.what(), tool.amount(), Actionable.SIMULATE) != tool.amount()) {
                return false;
            }
            available.insert(tool.what(), tool.amount(), Actionable.MODULATE);
        }

        // Rebuild the complete inputs of the pattern with tools
        var outputs = new KeyCounter();
        var remainders = new KeyCounter();
        var inputs = CraftingCpuHelper.extractPatternInputs(pattern.getPattern(), available, level, outputs, remainders);
        if (inputs == null || !available.list.isEmpty()) {
            return false;
        }
        for (var tool : pattern.getTools()) {
            reserved.extract(tool.what(), tool.amount(), Actionable.MODULATE);
            borrowed.insert(tool.what(), tool.amount(), Actionable.MODULATE);
        }
        expected.list.addAll(outputs);
        expected.list.addAll(remainders);
        active = true;

        if (!provider.pushPattern(PatternMapping.getOriginalPattern(provider, pattern), inputs)) {
            reserved.list.addAll(borrowed.list);
            borrowed.list.clear();
            expected.list.clear();
            active = false;
            return false;
        }
        for (var consumable : consumables) {
            consumable.clear();
        }
        return true;
    }

    /**
     * Accept output from the crafting if it is in waiting list
     */
    public long accept(AEKey key, long amount, Actionable mode) {
        if (!active) {
            return 0;
        }
        long accepted = expected.extract(key, amount, mode);
        if (accepted > 0 && mode == Actionable.MODULATE) {
            received.insert(key, accepted, Actionable.MODULATE);
        }
        return accepted;
    }

    /**
     * Count the returned items after crafting, including outputs and remainders
     */
    public List<GenericStack> settle() {
        expected.list.removeZeros();
        if (!active || !expected.list.isEmpty()) {
            return List.of();
        }
        for (var tool : borrowed.list) {
            long returnedAmount = received.extract(tool.getKey(), tool.getLongValue(), Actionable.MODULATE);
            reserved.insert(tool.getKey(), returnedAmount, Actionable.MODULATE);
        }
        var returned = new ArrayList<GenericStack>();
        for (var stack : received.list) {
            var amount = stack.getLongValue();
            if (amount > 0) {
                returned.add(new GenericStack(stack.getKey(), amount));
            }
        }
        borrowed.list.clear();
        received.list.clear();
        active = false;
        return returned;
    }

    public long getWaitingAmount(AEKey key) {
        return expected.list.get(key);
    }

    public void addWaitingList(Set<AEKey> keys) {
        for (var entry : expected.list) {
            if (entry.getLongValue() > 0) {
                keys.add(entry.getKey());
            }
        }
    }

    public void release(ListCraftingInventory cpu) {
        cpu.list.addAll(reserved.list);
        cpu.list.addAll(received.list);
        reserved.list.clear();
        received.list.clear();
        borrowed.list.clear();
        expected.list.clear();
        active = false;
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putBoolean("active", active);
        tag.put("reserved", reserved.writeToNBT());
        tag.put("expected", expected.writeToNBT());
        tag.put("received", received.writeToNBT());
        tag.put("borrowed", borrowed.writeToNBT());
        return tag;
    }

    public void load(CompoundTag tag) {
        active = tag.getBoolean("active");
        reserved.readFromNBT(tag.getList("reserved", Tag.TAG_COMPOUND));
        expected.readFromNBT(tag.getList("expected", Tag.TAG_COMPOUND));
        received.readFromNBT(tag.getList("received", Tag.TAG_COMPOUND));
        borrowed.readFromNBT(tag.getList("borrowed", Tag.TAG_COMPOUND));
    }
}
