package com.ber.nimblePattern.crafting;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.ExecutingCraftingJob;
import com.ber.nimblePattern.mixin.ae2.ElapsedTimeTrackerAccessorMixin;
import com.ber.nimblePattern.mixin.ae2.ExecutingCraftingJobAccessorMixin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public final class ProbabilityCraftingSupplement {
    private static final Constructor<?> TASK_CONSTRUCTOR;
    private static final Field TASK_VALUE;

    // TaskProgress is package-private, so use reflection to get it.
    // Value is the number of execution that hasn't been dispatched yet.
    static {
        try {
            var type = Class.forName("appeng.crafting.execution.ExecutingCraftingJob$TaskProgress");
            TASK_CONSTRUCTOR = type.getDeclaredConstructor();
            TASK_CONSTRUCTOR.setAccessible(true);
            TASK_VALUE = type.getDeclaredField("value");
            TASK_VALUE.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private ProbabilityCraftingSupplement() {
    }

    /**
     * Based on supplement crafting plan, count the execution times of patterns and additional outputs
     */
    public static Supplement prepare(ExecutingCraftingJob job, ICraftingPlan plan, GenericStack replaced) {
        var access = (ExecutingCraftingJobAccessorMixin) job;
        // the patterns that need to be executed with the times of execution
        var updates = new HashMap<IPatternDetails, Object>();
        // the additional outputs for the supplement crafting jobs
        var work = new KeyCounter();

        try {
            for (var entry : plan.patternTimes().entrySet()) {
                long count = entry.getValue();
                if (count <= 0) {
                    continue;
                }
                var old = access.nimblePattern$getTasks().get(entry.getKey());
                long previous = old == null ? 0 : TASK_VALUE.getLong(old);
                var progress = TASK_CONSTRUCTOR.newInstance();
                TASK_VALUE.setLong(progress, previous + count);
                updates.put(entry.getKey(), progress);
                for (var output : entry.getKey().getOutputs()) {
                    work.add(output.what(), output.amount() * count);
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot supplement the current AE crafting job", e);
        }

        // The additional crafting is to execute again to gain the target blocked output,
        // not to increase the crafting jobs.
        // For example, previous crafting job is blocked by waiting 3A. The additional job
        // will craft 3A, but the final waiting output is still 3A, not 6A.
        long replacement = Math.min(work.get(replaced.what()), replaced.amount());
        if (replacement > 0) {
            work.remove(replaced.what(), replacement);
        }
        return new Supplement(updates, work);
    }


    public record Supplement(Map<IPatternDetails, Object> tasks, KeyCounter additionalWork) {
        public void apply(ExecutingCraftingJob job) {
            var access = (ExecutingCraftingJobAccessorMixin) job;
            access.nimblePattern$getTasks().putAll(tasks);
            var tracker = (ElapsedTimeTrackerAccessorMixin) access.nimblePattern$getElapsedTimeTracker();
            for (var entry : additionalWork) {
                if (entry.getLongValue() > 0) {
                    tracker.nimblePattern$addMaxItems(entry.getLongValue() * entry.getKey().getAmountPerUnit(),
                            entry.getKey().getType());
                }
            }
        }
    }

}
