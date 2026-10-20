package com.ber.nimblePattern.compat.gtceu;

import appeng.api.stacks.GenericStack;
import com.ber.nimblePattern.crafting.MachineExecutionMonitor;
import com.ber.nimblePattern.crafting.ProbabilityDispatchContext;
import com.ber.nimblePattern.crafting.ProbabilityTrackingController;
import com.ber.nimblePattern.pattern.wrapper.NimbleProcessingPattern;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

public final class GTMachineTracking {
    private static final Map<RecipeLogic, Queue> QUEUES = new IdentityHashMap<>();
    private static long sequence;

    private GTMachineTracking() {
    }

    public static long sequence() {
        return sequence;
    }

    public static boolean register(ServerLevel level, ProbabilityTrackingController tracking,
                                   NimbleProcessingPattern pattern, long operations,
                                   ProbabilityDispatchContext capture, long beforePush) {
        if (!capture.hasCompleteCapture() || beforePush != sequence) {
            return false;
        }
        var deliveries = capture.getDeliveries();
        var first = deliveries.get(0);
        // Ordinary provider pushes should have exactly one receiving block/face.
        if (deliveries.stream().anyMatch(d -> !d.target().equals(first.target())
                || d.inputSide() != first.inputSide())) {
            return false;
        }
        var resolved = GTMachineResolver.resolve(level, first).orElse(null);
        if (resolved == null || resolved.recipeLogic().getClass() != RecipeLogic.class) {
            return false;
        }
        var inputs = deliveries.stream().flatMap(d -> d.inputs().stream()).toList();
        var id = tracking.dispatchedCandidate(pattern.getDefinition(), pattern.getPrimaryOutput(),
                operations, pattern.getMaxWaitingTicks());
        var queue = QUEUES.computeIfAbsent(resolved.recipeLogic(), ignored -> new Queue());
        queue.batches.add(new Batch(level, tracking, id, pattern, inputs, first, resolved));
        return true;
    }

    /**
     * Called only after setupRecipe actually consumed inputs.
     */
    public static void start(RecipeLogic logic, GTRecipe executed) {
        long executionId = ++sequence;
        var queue = QUEUES.get(logic);
        if (queue == null) {
            return;
        }
        if (!queue.running.isEmpty()) {
            invalidate(logic); // A new start without completion/reset means continuity was lost.
            return;
        }
        var level = queue.batches.get(0).level;
        var raw = level.getRecipeManager().byKey(executed.id).orElse(null);
        if (!(raw instanceof GTRecipe base)) {
            invalidate(logic);
            return;
        }

        long unassigned = -1;
        long executionOperations = -1;
        for (var batch : queue.batches) {
            if (!batch.tracking.isTracking(batch.id) || !batch.valid()) {
                batch.invalidate();
                continue;
            }
            if (!GTRecipeMatcher.matches(batch.pattern, batch.inputs, base)) {
                continue; // Another recipe may be running on the same machine.
            }
            if (batch.recipeId != null && !batch.recipeId.equals(base.id)) {
                batch.invalidate();
                continue;
            }
            var total = GTRecipeOperations.calculate(base, batch.inputs);
            var count = GTRecipeOperations.getExecutedOperations(base, executed, batch.inputs);
            if (total.isEmpty() || count.isEmpty()) {
                batch.invalidate();
                continue;
            }
            if (executionOperations < 0) {
                executionOperations = count.getAsLong();
                unassigned = executionOperations;
            } else if (executionOperations != count.getAsLong()) {
                invalidate(logic);
                return;
            }
            if (unassigned == 0) {
                continue;
            }
            if (batch.monitor == null) {
                batch.monitor = batch.tracking.activateMachine(batch.id, total.getAsLong());
                batch.recipeId = base.id;
            }
            if (batch.monitor == null) {
                continue;
            }
            long remaining = batch.monitor.getRequiredOperations() - batch.monitor.getStartedOperations();
            long allocated = Math.min(unassigned, remaining);
            if (allocated > 0 && batch.monitor.start(executionId, allocated)) {
                queue.running.add(new Running(batch, executionId));
                unassigned -= allocated;
            }
        }
        prune(logic, queue);
    }

    /**
     * Runs after GTL's output-space gate, before output callbacks can complete the CPU job.
     */
    public static void finished(RecipeLogic logic) {
        var queue = QUEUES.get(logic);
        if (queue == null) {
            return;
        }
        for (var run : queue.running) {
            var batch = run.batch;
            if (batch.valid() && batch.tracking.isTracking(batch.id)) {
                batch.monitor.finish(run.executionId, batch.level.getGameTime());
            } else {
                batch.invalidate();
            }
        }
        queue.running.clear();
        prune(logic, queue);
    }

    public static void invalidate(RecipeLogic logic) {
        var queue = QUEUES.remove(logic);
        if (queue != null) {
            queue.batches.forEach(Batch::invalidate);
        }
    }

    /**
     * Also runs for inactive CPUs, so unloaded/replaced machines cannot retain runtime references.
     */
    public static void tick() {
        for (var entry : new ArrayList<>(QUEUES.entrySet())) {
            for (var batch : entry.getValue().batches) {
                if (!batch.valid()) {
                    batch.invalidate();
                }
            }
            prune(entry.getKey(), entry.getValue());
        }
    }

    public static void release(ProbabilityTrackingController tracking) {
        for (var entry : new ArrayList<>(QUEUES.entrySet())) {
            entry.getValue().batches.removeIf(b -> b.tracking == tracking);
            entry.getValue().running.removeIf(r -> r.batch.tracking == tracking);
            if (entry.getValue().batches.isEmpty()) {
                QUEUES.remove(entry.getKey());
            }
        }
    }

    public static void clear() {
        QUEUES.values().forEach(q -> q.batches.forEach(Batch::invalidate));
        QUEUES.clear();
        sequence = 0;
    }

    private static void prune(RecipeLogic logic, Queue queue) {
        queue.batches.removeIf(b -> b.invalid || !b.tracking.isTracking(b.id));
        queue.running.removeIf(r -> !queue.batches.contains(r.batch));
        if (queue.batches.isEmpty()) {
            QUEUES.remove(logic);
        }
    }

    private static final class Queue {
        // All the batches of the machine that have not been finished yet
        final List<Batch> batches = new ArrayList<>();
        // All the running executions with the monitors
        final List<Running> running = new ArrayList<>();
    }

    private record Running(Batch batch, long executionId) {
    }

    /**
     * Every pattern push will generate a Batch
     */
    private static final class Batch {
        final ServerLevel level;
        final ProbabilityTrackingController tracking;
        final UUID id;
        final NimbleProcessingPattern pattern;
        final List<GenericStack> inputs;
        final ProbabilityDispatchContext.Delivery delivery;
        final GTMachineResolver.ResolvedMachine machine;
        MachineExecutionMonitor monitor;
        ResourceLocation recipeId;
        boolean invalid;

        Batch(ServerLevel level, ProbabilityTrackingController tracking, UUID id,
              NimbleProcessingPattern pattern, List<GenericStack> inputs,
              ProbabilityDispatchContext.Delivery delivery, GTMachineResolver.ResolvedMachine machine) {
            this.level = level;
            this.tracking = tracking;
            this.id = id;
            this.pattern = pattern;
            this.inputs = inputs;
            this.delivery = delivery;
            this.machine = machine;
        }

        boolean valid() {
            if (invalid || !level.hasChunkAt(machine.executor().getPos())) {
                return false;
            }
            var current = GTMachineResolver.resolve(level, delivery).orElse(null);
            return current != null && current.receiver() == machine.receiver()
                    && current.executor() == machine.executor()
                    && current.recipeLogic() == machine.recipeLogic();
        }

        void invalidate() {
            invalid = true;
            if (monitor != null) {
                monitor.invalidate();
            }
        }
    }
}
