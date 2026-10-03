package com.ber.nimblePattern.probability;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import java.util.*;

/** Small, recipe-specific completion counters. A paused or unloaded machine is not a failed roll. */
public final class ProbabilityMachines {
    private ProbabilityMachines() {}
    public interface Target { BlockPos nimble$lastProbabilityTarget(); }
    public interface Tracked {}
    public interface Ticket {
        ResourceLocation recipe();
        long complete(long operations);
        default boolean finished() { return false; }
    }
    private static final Map<Object, List<java.lang.ref.WeakReference<Ticket>>> TICKETS = new WeakHashMap<>();
    private static final ClassValue<Boolean> KNOWN_LOGIC = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("onRecipeFinish").getDeclaringClass().getName()
                        .equals("com.gregtechceu.gtceu.api.machine.trait.RecipeLogic");
            } catch (NoSuchMethodException ignored) { return false; }
        }
    };
    public static boolean hasTickets(Object logic) { return TICKETS.containsKey(logic); }
    public static void clear() { TICKETS.clear(); }

    public static void completed(Object logic, ResourceLocation recipe, long operations) {
        if (recipe != null && operations > 0) {
            var queue = TICKETS.get(logic);
            if (queue == null) return;
            var iterator = queue.iterator();
            while (iterator.hasNext()) {
                var ticket = iterator.next().get();
                if (ticket == null) { iterator.remove(); continue; }
                if (recipe.equals(ticket.recipe()) && operations > 0) operations -= ticket.complete(operations);
                if (ticket.finished()) iterator.remove();
            }
            if (queue.isEmpty()) TICKETS.remove(logic);
        }
    }
    public static void watch(Object logic, Ticket ticket) {
        TICKETS.computeIfAbsent(logic, k -> new ArrayList<>()).add(new java.lang.ref.WeakReference<>(ticket));
    }
    public static void unwatch(Object logic, Ticket ticket) {
        var queue = TICKETS.get(logic);
        if (queue == null) return;
        queue.removeIf(ref -> ref.get() == null || ref.get() == ticket);
        if (queue.isEmpty()) TICKETS.remove(logic);
    }
    public record Machine(BlockPos pos, Object logic) {}

    public static Machine resolveProvider(Object provider, Level level) {
        try {
            Machine resolved;
            if (provider instanceof Target target) {
                var pos = target.nimble$lastProbabilityTarget();
                resolved = pos == null ? null : resolveAt(level, pos);
            } else resolved = resolveMachine(provider);
            // Existing work may belong to another provider/job. Do not attribute its next completion to us.
            return resolved != null && Boolean.TRUE.equals(ProbabilityRecipes.call(resolved.logic(), "isIdle")) ? resolved : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) { return null; }
    }
    public static Machine resolveAt(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        var block = level.getBlockEntity(pos);
        if (block == null) return null;
        try { return resolveMachine(ProbabilityRecipes.call(block, "getMetaMachine")); }
        catch (ReflectiveOperationException | RuntimeException ignored) { return null; }
    }
    private static Machine resolveMachine(Object machine) throws ReflectiveOperationException {
        if (machine == null) return null;
        try {
            Object logic = ProbabilityRecipes.call(machine, "getRecipeLogic");
            if (logic instanceof Tracked && KNOWN_LOGIC.get(logic.getClass()))
                return new Machine((BlockPos) ProbabilityRecipes.call(machine, "getPos"), logic);
        } catch (NoSuchMethodException ignored) { }
        var controllers = (List<?>) ProbabilityRecipes.call(machine, "getControllers");
        // Shared hatches / proxies serving several controllers are ambiguous; use the timeout fallback.
        return controllers.size() == 1 ? resolveMachine(controllers.get(0)) : null;
    }
}
