package com.ber.nimblePattern.crafting;

import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Collect actual input deliveries during one synchronous pattern push
 */
public final class ProbabilityDispatchContext implements AutoCloseable {
    private static final ThreadLocal<ProbabilityDispatchContext> CURRENT = new ThreadLocal<>();
    private final ProbabilityDispatchContext previous;
    private final List<Delivery> deliveries = new ArrayList<>();
    private boolean incomplete;
    private boolean closed;

    private ProbabilityDispatchContext() {
        previous = CURRENT.get();
        CURRENT.set(this);
    }

    public static ProbabilityDispatchContext open() {
        return new ProbabilityDispatchContext();
    }

    public static boolean isCapturing() {
        return CURRENT.get() != null;
    }

    public static void recordDelivery(ServerLevel level, BlockPos targetPos, Direction inputSide, KeyCounter acceptedInputs) {
        var context = CURRENT.get();
        if (context == null) {
            return;
        }
        var inputs = new ArrayList<GenericStack>();
        for (var entry : acceptedInputs) {
            long amount = entry.getLongValue();
            if (amount > 0) {
                inputs.add(new GenericStack(entry.getKey(), amount));
            }
        }
        if (inputs.isEmpty()) {
            return;
        }
        context.deliveries.add(new Delivery(GlobalPos.of(level.dimension(), targetPos.immutable()), inputSide, inputs));
    }

    /**
     * Mark the capture incomplete when accepted inputs can't be assigned to a known target.
     * The CPU should use timeout tracking for such a dispatch.
     */
    public static void markIncomplete() {
        var context = CURRENT.get();
        if (context != null) {
            context.incomplete = true;
        }
    }

    public boolean hasCompleteCapture() {
        return !incomplete && !deliveries.isEmpty();
    }

    public List<Delivery> getDeliveries() {
        return List.copyOf(deliveries);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (CURRENT.get() != this) {
            throw new IllegalStateException("Probability dispatch contexts must close in reverse order");
        }
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
        closed = true;
    }

    public record Delivery(GlobalPos target, Direction inputSide, List<GenericStack> inputs) {
        public Delivery {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(inputSide, "inputSide");
            inputs = List.copyOf(inputs);
        }
    }
}
