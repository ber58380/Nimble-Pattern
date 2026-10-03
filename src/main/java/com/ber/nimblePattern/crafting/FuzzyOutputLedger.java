package com.ber.nimblePattern.crafting;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.inv.ListCraftingInventory;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ObjLongConsumer;

/** Candidate index only. AE's waiting inventory remains the authority for every receipt. */
public final class FuzzyOutputLedger {
    private final Map<AEKey, Map<AEKey, Long>> groups = new HashMap<>();
    public record Receipt(long amount, boolean fuzzy) {}

    public void add(AEKey key, long amount) {
        if (amount > 0) groups.computeIfAbsent(key.dropSecondary(), k -> new HashMap<>())
                .merge(key, amount, Math::addExact);
    }
    public Receipt extract(ListCraftingInventory waiting, AEKey key, long amount, Actionable mode,
                           ObjLongConsumer<AEKey> received) {
        long exact = waiting.extract(key, amount, mode);
        var groupKey = key.dropSecondary();
        var candidates = groups.get(groupKey);
        if (exact > 0) {
            if (mode == Actionable.MODULATE) {
                if (candidates != null) {
                    candidates.computeIfPresent(key, (k, n) -> n <= exact ? null : n - exact);
                    if (candidates.isEmpty()) groups.remove(groupKey);
                }
                received.accept(key, exact);
            }
            // Keep exact and fuzzy receipts separate, including simulation and modulation.
            return new Receipt(exact, false);
        }
        if (candidates == null) return new Receipt(0, false);
        long remaining = amount;
        var iterator = candidates.entrySet().iterator();
        while (iterator.hasNext() && remaining > 0) {
            var entry = iterator.next();
            long available = Math.min(entry.getValue(), waiting.extract(entry.getKey(), Long.MAX_VALUE, Actionable.SIMULATE));
            long accepted = waiting.extract(entry.getKey(), Math.min(remaining, available), mode);
            remaining -= accepted;
            if (mode == Actionable.MODULATE) {
                long left = available - accepted;
                if (left == 0) iterator.remove(); else entry.setValue(left);
                if (accepted > 0) received.accept(entry.getKey(), accepted);
            }
        }
        if (mode == Actionable.MODULATE && candidates.isEmpty()) groups.remove(groupKey);
        return new Receipt(amount - remaining, amount > remaining);
    }
    public void clear() { groups.clear(); }
    public ListTag save() {
        var tags = new ListTag();
        groups.values().forEach(group -> group.forEach((key, amount) ->
                tags.add(GenericStack.writeTag(new GenericStack(key, amount)))));
        return tags;
    }
    public void load(ListTag tags) {
        clear();
        for (var tag : tags) {
            var stack = GenericStack.readTag((CompoundTag) tag);
            if (stack != null && stack.amount() > 0) add(stack.what(), stack.amount());
        }
    }
}
