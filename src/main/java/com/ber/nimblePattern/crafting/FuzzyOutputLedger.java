package com.ber.nimblePattern.crafting;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.inv.ListCraftingInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ObjLongConsumer;

public final class FuzzyOutputLedger {
    // Ledger of all crafting outputs, {fuzzyKey: {exactKey: amount}}
    private final Map<AEKey, Map<AEKey, Long>> ledger = new HashMap<>();

    public void add(AEKey key, long amount) {
        if (amount > 0) {
            ledger.computeIfAbsent(key.dropSecondary(), k -> new HashMap<>()).merge(key, amount, Math::addExact);
        }
    }

    public Receipt extract(ListCraftingInventory waiting, AEKey key, long amount, Actionable mode, ObjLongConsumer<AEKey> received) {
        // exact: call ae2 logic to match exactly and get the amount of key that matches exactly
        long exact = waiting.extract(key, amount, mode);
        var fuzzyKey = key.dropSecondary();
        var candidates = ledger.get(fuzzyKey);
        // match exactly first
        if (exact > 0) {
            if (mode == Actionable.MODULATE) {
                if (candidates != null) {
                    candidates.computeIfPresent(key, (k, n) -> n <= exact ? null : n - exact);
                    if (candidates.isEmpty()) {
                        ledger.remove(fuzzyKey);
                    }
                }
                received.accept(key, exact);
            }
            // If matches exactly, don't execute the following fuzzy match.
            return new Receipt(exact, false);
        }
        if (candidates == null) {
            return new Receipt(0, false);
        }
        // fuzzy match
        long remaining = amount;
        var iterator = candidates.entrySet().iterator();
        while (iterator.hasNext() && remaining > 0) {
            var entry = iterator.next();
            long available = Math.min(entry.getValue(), waiting.extract(entry.getKey(), Long.MAX_VALUE, Actionable.SIMULATE));
            long accepted = waiting.extract(entry.getKey(), Math.min(remaining, available), mode);
            // remaining: the remaining amount of the returned output that has not been accepted
            remaining -= accepted;
            if (mode == Actionable.MODULATE) {
                // left: the remaining fuzzy debt for the current candidate key
                long left = available - accepted;
                if (left == 0) {
                    iterator.remove();
                } else {
                    entry.setValue(left);
                }
                if (accepted > 0) {
                    received.accept(entry.getKey(), accepted);
                }
            }
        }
        if (mode == Actionable.MODULATE && candidates.isEmpty()) {
            ledger.remove(fuzzyKey);
        }
        return new Receipt(amount - remaining, amount > remaining);
    }

    public void clear() {
        ledger.clear();
    }

    public ListTag save() {
        var tags = new ListTag();
        ledger.values().forEach(group -> group.forEach(
                (key, amount) -> tags.add(
                        GenericStack.writeTag(new GenericStack(key, amount))
                )
        ));
        return tags;
    }

    public void load(ListTag tags) {
        clear();
        for (var tag : tags) {
            var stack = GenericStack.readTag((CompoundTag) tag);
            if (stack != null && stack.amount() > 0) {
                add(stack.what(), stack.amount());
            }
        }
    }

    public record Receipt(long amount, boolean fuzzyMode) {
    }
}
