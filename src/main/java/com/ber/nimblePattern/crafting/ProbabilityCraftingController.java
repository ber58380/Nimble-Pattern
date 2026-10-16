package com.ber.nimblePattern.crafting;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.inv.ListCraftingInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * The probability controller to track and update states of all probability patterns
 */
public final class ProbabilityCraftingController {
    // A large crafting task may contain several probability crafting tasks,
    // use list to record all tasks and support parallel crafting.
    private final List<ProbabilityAttempt> probabilityAttempts = new ArrayList<>();

    public boolean isActive() {
        return !probabilityAttempts.isEmpty();
    }

    /**
     * When a probability crafting task is dispatched successfully, register it to track.
     */
    public void dispatched(GenericStack[] outputs, long operations, long maxWaitingTicks) {
        if (operations <= 0 || maxWaitingTicks <= 0) {
            return;
        }
        var attempt = new ProbabilityAttempt(maxWaitingTicks);
        for (var output : outputs) {
            long amount = output.amount() * operations;
            if (amount > 0) {
                attempt.remaining.insert(output.what(), amount, Actionable.MODULATE);
            }
        }
        if (!attempt.remaining.list.isEmpty()) {
            probabilityAttempts.add(attempt);
        }
    }

    /**
     * Update the remaining based on returned output
     */
    public void received(AEKey key, long amount) {
        var iterator = probabilityAttempts.iterator();
        while (iterator.hasNext() && amount > 0) {
            var attempt = iterator.next();
            amount -= attempt.remaining.extract(key, amount, Actionable.MODULATE);
            if (attempt.remaining.list.isEmpty()) {
                iterator.remove();
            }
        }
    }

    /**
     * Call once per active CPU tick, before dispatching new work that tick.
     * Returns missing-output snapshots for attempts that timed out on this tick only.
     * Timed-out attempts remain in the ledger so late outputs can still settle them.
     */
    public List<List<GenericStack>> tick() {
        var expired = new ArrayList<List<GenericStack>>();
        for (var attempt : probabilityAttempts) {
            if (attempt.tick()) {
                expired.add(attempt.snapshot());
            }
        }
        return expired;
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        var entries = new ListTag();
        for (var attempt : probabilityAttempts) {
            entries.add(attempt.save());
        }
        tag.put("attempts", entries);
        return tag;
    }

    public void load(CompoundTag tag) {
        release();
        for (var value : tag.getList("attempts", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value;
            long maxWaitingTicks = entry.getLong("maxWaitingTicks");
            if (maxWaitingTicks <= 0) {
                continue;
            }
            var attempt = new ProbabilityAttempt(maxWaitingTicks);
            attempt.elapsedTicks = Math.max(0, Math.min(maxWaitingTicks, entry.getLong("elapsedTicks")));
            attempt.timeout = entry.getBoolean("timeout");
            attempt.remaining.readFromNBT(entry.getList("remaining", Tag.TAG_COMPOUND));
            if (!attempt.remaining.list.isEmpty()) {
                probabilityAttempts.add(attempt);
            }
        }
    }

    public void release() {
        probabilityAttempts.clear();
    }

    /**
     * For each probability crafting, record an attempt
     */
    private static final class ProbabilityAttempt {
        private final ListCraftingInventory remaining = new ListCraftingInventory(key -> {
        });
        private final long maxWaitingTicks;
        private long elapsedTicks;
        private boolean timeout;

        private ProbabilityAttempt(long maxWaitingTicks) {
            this.maxWaitingTicks = maxWaitingTicks;
        }

        /**
         * Returns true only on the transition to timeout.
         */
        private boolean tick() {
            if (timeout) {
                return false;
            }
            if (elapsedTicks < maxWaitingTicks) {
                elapsedTicks++;
            }
            timeout = elapsedTicks >= maxWaitingTicks;
            return timeout;
        }

        private List<GenericStack> snapshot() {
            var outputs = new ArrayList<GenericStack>();
            for (var entry : remaining.list) {
                if (entry.getLongValue() > 0) {
                    outputs.add(new GenericStack(entry.getKey(), entry.getLongValue()));
                }
            }
            return List.copyOf(outputs);
        }

        private CompoundTag save() {
            var tag = new CompoundTag();
            tag.putLong("maxWaitingTicks", maxWaitingTicks);
            tag.putLong("elapsedTicks", elapsedTicks);
            tag.putBoolean("timeout", timeout);
            tag.put("remaining", remaining.writeToNBT());
            return tag;
        }
    }
}
