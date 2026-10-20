package com.ber.nimblePattern.crafting;

import com.ber.nimblePattern.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

public final class MachineExecutionMonitor {
    // Total base recipe operations should be taken for the delivered inputs
    private long requiredOperations;
    // Operations that have successfully started, including those already finished
    private long startedOperations;
    // Operations that have successfully finished
    private long finishedOperations;
    // unique execution ID -> number of operations in this execution (some machines have parallel ability)
    private final Map<Long, Long> runningExecutions = new HashMap<>();
    // true means now is at the phase of collection outputs from machines
    private boolean collecting;
    private long collectionRemainingTicks;
    // true means could not track machines now, e.g. block has been unloaded
    private boolean suspended;
    // true means previous record could not be used anymore, e.g. machine is replaced
    private boolean invalid;
    private long lastGameTick = -1;

    public MachineExecutionMonitor() {
    }

    // region getters
    public boolean isInvalid() {
        return invalid;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public boolean isCollecting() {
        return collecting;
    }

    public long getRequiredOperations() {
        return requiredOperations;
    }

    public long getStartedOperations() {
        return startedOperations;
    }

    public long getFinishedOperations() {
        return finishedOperations;
    }

    public Map<Long, Long> getRunningExecutions() {
        return Map.copyOf(runningExecutions);
    }
    // endregion

    /**
     * Record new operations for the base pattern.
     */
    public void append(long operations) {
        if (operations <= 0) {
            throw new IllegalArgumentException("Operations must be positive");
        }
        if (invalid) {
            throw new IllegalStateException("Machine tracking is invalid");
        }
        if (collecting && collectionRemainingTicks == 0) {
            throw new IllegalStateException("Completed batches must not receive new work");
        }
        requiredOperations += operations;
        collecting = false;
        collectionRemainingTicks = 0;
        lastGameTick = -1;
    }

    // GT machine successfully start the execution
    public boolean start(long executionId, long operations) {
        if (invalid || suspended || operations <= 0) {
            return false;
        }
        if (runningExecutions.containsKey(executionId)) {
            return false;
        }
        if (operations > requiredOperations - startedOperations) {
            return false;
        }
        runningExecutions.put(executionId, operations);
        startedOperations += operations;
        return true;
    }

    // GT machine finishes an execution
    public boolean finish(long executionId, long gameTick) {
        if (invalid || suspended) {
            return false;
        }
        Long operations = runningExecutions.remove(executionId);
        if (operations == null) {
            return false;
        }
        // when all required operations are finished, start collection phase
        finishedOperations += operations;
        if (requiredOperations > 0 && finishedOperations == requiredOperations && runningExecutions.isEmpty()) {
            collecting = true;
            collectionRemainingTicks = Config.probabilityCollectionGraceTicks();
            lastGameTick = gameTick;
        }
        return true;
    }

    private void advanceCollectionClock(long gameTick) {
        if (lastGameTick < 0) {
            lastGameTick = gameTick;
            return;
        }
        long elapsed = Math.max(0, gameTick - lastGameTick);
        lastGameTick = gameTick;
        collectionRemainingTicks -= Math.min(elapsed, collectionRemainingTicks);
    }

    /**
     * Advance the collection phase, return true when the collection phase is ended.
     */
    public boolean tick(long gameTick) {
        if (invalid || suspended || !collecting) {
            return false;
        }
        advanceCollectionClock(gameTick);
        return collectionRemainingTicks == 0;
    }

    /**
     * Pause observation, for example when the tracked machine unloads.
     */
    public void suspend(long gameTick) {
        if (suspended || invalid) {
            return;
        }
        if (collecting) {
            advanceCollectionClock(gameTick);
        }
        suspended = true;
        lastGameTick = -1;
    }

    /**
     * Resume the observation of machine from suspended.
     */
    public void resume(long gameTick) {
        if (invalid) {
            throw new IllegalStateException("Machine tracking is invalid");
        }
        if (!suspended) {
            return;
        }
        suspended = false;
        lastGameTick = gameTick;
    }

    /**
     * Set the tracking of machine to invalid, and the tracking could not be used anymore.
     */
    public void invalidate() {
        invalid = true;
        lastGameTick = -1;
    }

    public CompoundTag save(long gameTick) {
        if (collecting && !suspended && !invalid) {
            advanceCollectionClock(gameTick);
        }
        var tag = new CompoundTag();
        tag.putLong("requiredOperations", requiredOperations);
        tag.putLong("startedOperations", startedOperations);
        tag.putLong("finishedOperations", finishedOperations);
        tag.putBoolean("collecting", collecting);
        tag.putLong("collectionRemainingTicks", collectionRemainingTicks);
        tag.putBoolean("invalid", invalid);
        var executions = new ListTag();
        for (var entry : runningExecutions.entrySet()) {
            var execution = new CompoundTag();
            execution.putLong("executionId", entry.getKey());
            execution.putLong("operations", entry.getValue());
            executions.add(execution);
        }
        tag.put("runningExecutions", executions);
        return tag;
    }

    public static MachineExecutionMonitor load(CompoundTag tag) {
        var monitor = new MachineExecutionMonitor();
        monitor.requiredOperations = tag.getLong("requiredOperations");
        monitor.startedOperations = tag.getLong("startedOperations");
        monitor.finishedOperations = tag.getLong("finishedOperations");
        monitor.collecting = tag.getBoolean("collecting");
        monitor.collectionRemainingTicks = tag.getLong("collectionRemainingTicks");
        monitor.invalid = tag.getBoolean("invalid");
        for (var value : tag.getList("runningExecutions", Tag.TAG_COMPOUND)) {
            var execution = (CompoundTag) value;
            monitor.runningExecutions.put(execution.getLong("executionId"), execution.getLong("operations"));
        }
        monitor.suspended = true;
        monitor.lastGameTick = -1;
        return monitor;
    }
}
