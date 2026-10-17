package com.ber.nimblePattern.crafting;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.inv.ListCraftingInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * The probability controller to track and update states of all probability patterns
 */
public final class ProbabilityTrackingController {
    // A large crafting task may contain several probability crafting tasks,
    // use list to record all tasks and support parallel crafting.
    private final List<ProbabilityAttempt> probabilityAttempts = new ArrayList<>();

    public boolean isActive() {
        return !probabilityAttempts.isEmpty();
    }

    /**
     * When a probability crafting task is dispatched successfully, register it to track.
     */
    public void dispatched(AEKey pattern, GenericStack[] outputs, long operations, long maxWaitingTicks) {
        if (operations <= 0 || maxWaitingTicks <= 0) {
            return;
        }
        ProbabilityAttempt attempt = null;
        for (var existing : probabilityAttempts) {
            if (!existing.timeout && pattern.equals(existing.pattern)) {
                attempt = existing;
                break;
            }
        }
        boolean newAttempt = attempt == null;
        if (newAttempt) {
            attempt = new ProbabilityAttempt(pattern, 0);
        }
        attempt.maxWaitingTicks += maxWaitingTicks * operations;
        for (var output : outputs) {
            long amount = output.amount() * operations;
            if (amount > 0) {
                attempt.remaining.insert(output.what(), amount, Actionable.MODULATE);
            }
        }
        if (newAttempt && !attempt.remaining.list.isEmpty()) {
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
     * After supplement crafting plan is created, there might be waiting outputs back to
     * the CPU. This method is to justify the tracker of supplement crafting plan.
     */
    public void reconcileWaiting(ListCraftingInventory waiting) {
        var accounted = new KeyCounter();
        var iterator = probabilityAttempts.iterator();
        while (iterator.hasNext()) {
            var attempt = iterator.next();
            for (var entry : attempt.remaining.list) {
                var key = entry.getKey();
                long available = Math.max(0, waiting.list.get(key) - accounted.get(key));
                long amount = Math.min(entry.getLongValue(), available);
                entry.setValue(amount);
                accounted.add(key, amount);
            }
            attempt.remaining.list.removeZeros();
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
    public List<List<GenericStack>> tick(long elapsedGameTicks) {
        var expired = new ArrayList<List<GenericStack>>();
        if (elapsedGameTicks <= 0) {
            return expired;
        }
        for (var attempt : probabilityAttempts) {
            if (attempt.tick(elapsedGameTicks)) {
                expired.add(attempt.snapshot());
            }
        }
        return expired;
    }

    public List<GenericStack> getTimedOutOutputs() {
        var missing = new KeyCounter();
        for (var attempt : probabilityAttempts) {
            if (attempt.timeout) {
                missing.addAll(attempt.remaining.list);
            }
        }
        var outputs = new ArrayList<GenericStack>();
        for (var entry : missing) {
            if (entry.getLongValue() > 0) {
                outputs.add(new GenericStack(entry.getKey(), entry.getLongValue()));
            }
        }
        return outputs;
    }

    public long getTimedOutAmount(AEKey key) {
        long amount = 0;
        for (var attempt : probabilityAttempts) {
            if (attempt.timeout) {
                amount += attempt.remaining.list.get(key);
            }
        }
        return amount;
    }

    /**
     * When a supplement crafting plan is started to craft timeout outputs,
     * remove the tracking for it.
     */
    public void receiveSupplementPlan(AEKey key, long amount) {
        var iterator = probabilityAttempts.iterator();
        while (iterator.hasNext() && amount > 0) {
            var attempt = iterator.next();
            if (!attempt.timeout) {
                continue;
            }
            amount -= attempt.remaining.extract(key, amount, Actionable.MODULATE);
            if (attempt.remaining.list.isEmpty()) {
                iterator.remove();
            }
        }
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
            var pattern = entry.contains("pattern", Tag.TAG_COMPOUND)
                    ? AEKey.fromTagGeneric(entry.getCompound("pattern")) : null;
            var attempt = new ProbabilityAttempt(pattern, maxWaitingTicks);
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
        private final AEKey pattern;
        private long maxWaitingTicks;
        private long elapsedTicks;
        private boolean timeout;

        private ProbabilityAttempt(AEKey pattern, long maxWaitingTicks) {
            this.pattern = pattern;
            this.maxWaitingTicks = maxWaitingTicks;
        }

        /**
         * Returns true only on the transition to timeout.
         */
        private boolean tick(long elapsedGameTicks) {
            if (timeout) {
                return false;
            }
            if (elapsedTicks < maxWaitingTicks) {
                elapsedTicks += Math.min(elapsedGameTicks, maxWaitingTicks - elapsedTicks);
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
            if (pattern != null) {
                tag.put("pattern", pattern.toTagGeneric());
            }
            tag.putLong("maxWaitingTicks", maxWaitingTicks);
            tag.putLong("elapsedTicks", elapsedTicks);
            tag.putBoolean("timeout", timeout);
            tag.put("remaining", remaining.writeToNBT());
            return tag;
        }
    }
}
