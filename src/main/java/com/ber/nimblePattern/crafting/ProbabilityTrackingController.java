package com.ber.nimblePattern.crafting;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.inv.ListCraftingInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;

/**
 * The probability controller to track and update states of all probability patterns.
 * It uses either an elapsed-time limit or a machine execution monitor to determine the failure of probability crafting.
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
     * Use the timeout tracking mechanism.
     */
    public void dispatchedTimeout(AEKey pattern, GenericStack[] outputs, long operations, long maxWaitingTicks) {
        if (operations <= 0 || maxWaitingTicks <= 0) {
            return;
        }
        ProbabilityAttempt attempt = null;
        for (var existing : probabilityAttempts) {
            if (existing.machineMonitor == null && existing.retryReason == RetryReason.NONE
                    && pattern.equals(existing.pattern)) {
                attempt = existing;
                break;
            }
        }
        boolean newAttempt = attempt == null;
        if (newAttempt) {
            attempt = new ProbabilityAttempt(UUID.randomUUID(), pattern);
        }
        attempt.maxWaitingTicks += maxWaitingTicks * operations;
        attempt.registerOutputs(outputs, operations);
        if (newAttempt && !attempt.remaining.list.isEmpty()) {
            probabilityAttempts.add(attempt);
        }
    }

    private ProbabilityAttempt getAttempt(UUID batchId) {
        for (var attempt : probabilityAttempts) {
            if (attempt.batchId.equals(batchId)) {
                return attempt;
            }
        }
        return null;
    }

    /** A candidate machine batch keeps its own timeout until its first matching execution. */
    public UUID dispatchedCandidate(AEKey pattern, GenericStack output, long operations, long waitingTicks) {
        var attempt = new ProbabilityAttempt(UUID.randomUUID(), pattern);
        attempt.maxWaitingTicks = waitingTicks * operations;
        attempt.registerOutputs(new GenericStack[]{output}, operations);
        probabilityAttempts.add(attempt);
        return attempt.batchId;
    }

    public boolean isTracking(UUID batchId) {
        var attempt = getAttempt(batchId);
        return attempt != null && attempt.retryReason == RetryReason.NONE
                && (attempt.machineMonitor == null || !attempt.machineMonitor.isInvalid());
    }

    public MachineExecutionMonitor activateMachine(UUID batchId, long operations) {
        var attempt = getAttempt(batchId);
        if (attempt == null || attempt.retryReason != RetryReason.NONE || operations <= 0) {
            return null;
        }
        if (attempt.machineMonitor == null) {
            attempt.machineMonitor = new MachineExecutionMonitor();
            attempt.machineMonitor.append(operations);
        }
        return attempt.machineMonitor;
    }

    /**
     * When a probability crafting task is dispatched successfully, register it to track.
     * Use the machine tracking mechanism.
     */
    public void dispatchedMachine(UUID batchId, AEKey pattern, GenericStack[] outputs, long patternOperations, long machineOperations, long maxWaitingTicks, long gameTick) {
        if (patternOperations <= 0 || machineOperations <= 0 || maxWaitingTicks <= 0) {
            throw new IllegalArgumentException("Pattern operations, machine operations and waiting ticks must be positive");
        }
        ProbabilityAttempt attempt = getAttempt(batchId);
        boolean newAttempt = attempt == null;
        if (newAttempt) {
            attempt = new ProbabilityAttempt(batchId, pattern);
            attempt.machineMonitor = new MachineExecutionMonitor();
        } else {
            if (attempt.machineMonitor == null || !pattern.equals(attempt.pattern)) {
                throw new IllegalStateException("Batch ID belongs to different tracking work");
            }
            if (attempt.retryReason != RetryReason.NONE) {
                throw new IllegalStateException("A retrying batch must not receive new work");
            }
            if (attempt.machineMonitor.isInvalid() || attempt.machineMonitor.isSuspended()) {
                throw new IllegalStateException("Machine tracking must be valid and active");
            }
            if (attempt.machineMonitor.tick(gameTick)) {
                throw new IllegalStateException("Collection has finished and need to create a new batch");
            }
        }

        attempt.machineMonitor.append(machineOperations);
        attempt.maxWaitingTicks += maxWaitingTicks * patternOperations;
        attempt.registerOutputs(outputs, patternOperations);
        if (newAttempt && !attempt.remaining.list.isEmpty()) {
            probabilityAttempts.add(attempt);
        }
    }

    public Map<UUID, MachineExecutionMonitor> getMachineMonitors() {
        var monitors = new HashMap<UUID, MachineExecutionMonitor>();
        for (var attempt : probabilityAttempts) {
            if (attempt.machineMonitor != null) {
                monitors.put(attempt.batchId, attempt.machineMonitor);
            }
        }
        return Map.copyOf(monitors);
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
    public List<List<GenericStack>> tick(long elapsedGameTicks, long gameTick) {
        var expired = new ArrayList<List<GenericStack>>();
        for (var attempt : probabilityAttempts) {
            // When the machine tracking is invalid, rolling back to timeout mechanism
            if (attempt.machineMonitor != null && attempt.machineMonitor.isInvalid()) {
                attempt.machineMonitor = null;
                attempt.retryReason = RetryReason.NONE;
                attempt.elapsedTicks = 0;
                continue;
            }

            if (attempt.retryReason != RetryReason.NONE) {
                continue;
            }
            if (attempt.machineMonitor != null) {
                if (attempt.machineMonitor.tick(gameTick)) {
                    attempt.retryReason = RetryReason.MACHINE_FINISHED;
                    expired.add(attempt.snapshot());
                }
                continue;
            }
            attempt.elapsedTicks += Math.min(elapsedGameTicks, attempt.maxWaitingTicks - attempt.elapsedTicks);
            if (attempt.elapsedTicks >= attempt.maxWaitingTicks) {
                attempt.retryReason = RetryReason.TIMEOUT;
                expired.add(attempt.snapshot());
            }
        }
        return expired;
    }

    public List<GenericStack> getRetryOutputs() {
        var missing = new KeyCounter();
        for (var attempt : probabilityAttempts) {
            if (attempt.canRetry()) {
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

    public long getRetryAmount(AEKey key) {
        long amount = 0;
        for (var attempt : probabilityAttempts) {
            if (attempt.canRetry()) {
                amount += attempt.remaining.list.get(key);
            }
        }
        return amount;
    }

    /**
     * When a supplement crafting plan is started to craft retry outputs,
     * remove the tracking for it.
     */
    public void receiveSupplementPlan(AEKey key, long amount) {
        var iterator = probabilityAttempts.iterator();
        while (iterator.hasNext() && amount > 0) {
            var attempt = iterator.next();
            if (!attempt.canRetry()) {
                continue;
            }
            amount -= attempt.remaining.extract(key, amount, Actionable.MODULATE);
            if (attempt.remaining.list.isEmpty()) {
                iterator.remove();
            }
        }
    }

    public CompoundTag save(long gameTick) {
        var tag = new CompoundTag();
        var entries = new ListTag();
        for (var attempt : probabilityAttempts) {
            entries.add(attempt.save(gameTick));
        }
        tag.put("attempts", entries);
        return tag;
    }

    public void load(CompoundTag tag) {
        release();
        for (var value : tag.getList("attempts", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) value;
            UUID batchId = entry.getUUID("batchId");
            AEKey pattern = AEKey.fromTagGeneric(entry.getCompound("pattern"));
            var attempt = new ProbabilityAttempt(batchId, pattern);
            attempt.maxWaitingTicks = entry.getLong("maxWaitingTicks");
            if (entry.contains("machineMonitor", Tag.TAG_COMPOUND)) {
                attempt.machineMonitor = MachineExecutionMonitor.load(entry.getCompound("machineMonitor"));
                // Runtime machine identities and execution events do not survive a reload.
                // Re-enter timeout instead of leaving a suspended monitor waiting forever.
                attempt.machineMonitor.invalidate();
            } else if (attempt.maxWaitingTicks <= 0) {
                continue;
            }
            attempt.elapsedTicks = Math.max(0, Math.min(attempt.maxWaitingTicks, entry.getLong("elapsedTicks")));
            attempt.retryReason = RetryReason.valueOf(entry.getString("retryReason"));
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
        // A pattern might deliver to different machines. Identify one attributed machine batch.
        private final UUID batchId;
        private final AEKey pattern;
        private final ListCraftingInventory remaining = new ListCraftingInventory(key -> {
        });
        private long maxWaitingTicks = 0;
        private long elapsedTicks = 0;
        private MachineExecutionMonitor machineMonitor;
        private RetryReason retryReason = RetryReason.NONE;

        private ProbabilityAttempt(UUID batchId, AEKey pattern) {
            this.batchId = batchId;
            this.pattern = pattern;
        }

        private void registerOutputs(GenericStack[] outputs, long operations) {
            for (var output : outputs) {
                long amount = output.amount() * operations;
                if (amount > 0) {
                    remaining.insert(output.what(), amount, Actionable.MODULATE);
                }
            }
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

        /**
         * Determine this attempt could retry or not.
         * If it's the machine tracking mechanism, the machine must be valid and working.
         */
        private boolean canRetry() {
            if (retryReason == RetryReason.NONE) {
                return false;
            }
            return machineMonitor == null || (!machineMonitor.isInvalid()) && (!machineMonitor.isSuspended());
        }

        private CompoundTag save(long gameTick) {
            var tag = new CompoundTag();
            tag.putUUID("batchId", batchId);
            if (pattern != null) {
                tag.put("pattern", pattern.toTagGeneric());
            }
            tag.putLong("maxWaitingTicks", maxWaitingTicks);
            tag.putLong("elapsedTicks", elapsedTicks);
            tag.putString("retryReason", retryReason.name());
            tag.put("remaining", remaining.writeToNBT());
            if (machineMonitor != null) {
                tag.put("machineMonitor", machineMonitor.save(gameTick));
            }
            return tag;
        }
    }

    private enum RetryReason {
        NONE,
        TIMEOUT,
        MACHINE_FINISHED
    }
}
