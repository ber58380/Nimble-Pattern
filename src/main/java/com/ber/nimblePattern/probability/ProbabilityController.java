package com.ber.nimblePattern.probability;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.*;
import appeng.api.stacks.*;
import appeng.crafting.execution.*;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.ber.nimblePattern.Config;
import com.ber.nimblePattern.network.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;
import java.util.concurrent.Future;

/** Server-thread ledger. Failed rolls never synthesize output or discard already received physical items. */
public final class ProbabilityController {
    private final List<Attempt> attempts = new ArrayList<>();
    private final Map<AEKey, List<Attempt>> byOutput = new HashMap<>();
    private static final Set<CraftingCPUCluster> ACTIVE = Collections.newSetFromMap(new WeakHashMap<>());
    private CraftingCPUCluster owner;
    public static boolean anyActive() { return !ACTIVE.isEmpty(); }
    /** Small active set intersection: ordinary networks do not scan every CPU looking for priority work. */
    public static long insertPriority(Set<CraftingCPUCluster> networkCpus, AEKey key, long amount, Actionable mode) {
        if (ACTIVE.isEmpty()) return -1;
        // insert can finish a job and remove its CPU from ACTIVE.
        var selected = new ArrayList<CraftingCPUCluster>();
        for (var cpu : ACTIVE) if (networkCpus.contains(cpu)) selected.add(cpu);
        if (selected.isEmpty()) return -1;
        long accepted = 0;
        for (var cpu : selected) {
            if (accepted == amount) break;
            accepted += cpu.craftingLogic.insert(key, amount - accepted, mode);
        }
        return accepted;
    }
    public static void resetTracking() { ACTIVE.clear(); }
    public void bind(CraftingCPUCluster cluster) {
        if (owner != null && owner != cluster) ACTIVE.remove(owner);
        owner = cluster;
        if (enabled) ACTIVE.add(cluster);
    }
    private final ListCraftingInventory supplies = new ListCraftingInventory(k -> {});
    private final ListCraftingInventory credits = new ListCraftingInventory(k -> {});
    private final ListCraftingInventory tools = new ListCraftingInventory(k -> {});
    private Future<ICraftingPlan> calculation;
    private final Map<Attempt, Long> retryDebts = new LinkedHashMap<>();
    private GenericStack requested;
    private int poll;
    private boolean warned;
    private boolean enabled;
    private AEKey failureOutput;
    private boolean progressDirty;

    private final class Attempt implements ProbabilityMachines.Ticket {
        final ListCraftingInventory remaining = new ListCraftingInventory(k -> {});
        ResourceLocation recipe;
        BlockPos machine;
        Object logic;
        long cycles, elapsed, grace;
        boolean failed;
        @Override public ResourceLocation recipe() { return recipe; }
        @Override public boolean finished() { return cycles == 0; }
        @Override public long complete(long operations) {
            long accepted = Math.min(cycles, operations);
            cycles -= accepted;
            progressDirty |= accepted > 0;
            return accepted;
        }
    }

    public boolean active() { return enabled; }

    public void dispatched(IPatternDetails pattern, Object provider, CraftingCPUCluster cluster) {
        dispatched(pattern, provider, cluster, 1);
    }
    public void dispatched(IPatternDetails pattern, Object provider, CraftingCPUCluster cluster, long operations) {
        var metadata = pattern instanceof com.ber.nimblePattern.pattern.NimbleEncodedPattern n ? n.metadata()
                : com.ber.nimblePattern.pattern.PatternMetadata.read(pattern.getDefinition().toStack());
        if (!metadata.probability() || operations <= 0) return;
        enabled = true;
        bind(cluster);
        var attempt = new Attempt();
        attempt.recipe = metadata.recipe();
        attempt.cycles = Math.multiplyExact(metadata.cycles(), operations);
        for (var output : pattern.getOutputs()) attempt.remaining.insert(output.what(), Math.multiplyExact(output.amount(), operations), Actionable.MODULATE);
        var machine = attempt.cycles > 0 ? ProbabilityMachines.resolveProvider(provider, cluster.getLevel()) : null;
        if (machine != null && attempt.recipe != null) {
            attempt.machine = machine.pos(); attempt.logic = machine.logic();
            ProbabilityMachines.watch(machine.logic(), attempt);
        }
        attempts.add(attempt);
        index(attempt);
    }

    private void index(Attempt attempt) {
        for (var e : attempt.remaining.list) if (e.getLongValue() > 0)
            byOutput.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(attempt);
    }

    private void rebuildOutputIndex() {
        byOutput.clear();
        attempts.forEach(this::index);
    }

    public void received(AEKey key, long amount) {
        var candidates = byOutput.get(key);
        if (candidates == null || amount <= 0) return;
        var iterator = candidates.iterator();
        while (iterator.hasNext()) {
            var attempt = iterator.next();
            amount -= attempt.remaining.extract(key, amount, Actionable.MODULATE);
            if (attempt.remaining.list.get(key) == 0) iterator.remove();
            if (amount == 0) break;
        }
        if (candidates.isEmpty()) byOutput.remove(key);
    }

    /** Expected output of a replacement task replaces an existing debt, rather than adding a second debt. */
    public long consumeCredit(AEKey key, long amount) {
        return credits.extract(key, amount, Actionable.MODULATE);
    }

    public long collect(AEKey key, long amount, Actionable mode, ListCraftingInventory inventory) {
        long accepted = supplies.extract(key, amount, mode);
        if (mode == Actionable.MODULATE && accepted > 0) inventory.insert(key, accepted, mode);
        return accepted;
    }
    public long waiting(AEKey key) { return supplies.extract(key, Long.MAX_VALUE, Actionable.SIMULATE); }
    public void addWaiting(Set<AEKey> keys) { for (var e : supplies.list) keys.add(e.getKey()); }

    public void tick(CraftingCPUCluster cluster, ExecutingCraftingJob job, ListCraftingInventory inventory) {
        if (!enabled) return;
        if (++poll < 10) return;
        poll = 0;
        var grid = cluster.getGrid();
        if (grid == null || job == null) return;
        boolean changed = progressDirty;
        progressDirty = false;
        for (var e : new ArrayList<>(supplies.list.keySet())) {
            long amount = supplies.extract(e, Long.MAX_VALUE, Actionable.SIMULATE);
            long extracted = grid.getStorageService().getInventory().extract(e, amount, Actionable.MODULATE, cluster.getSrc());
            collect(e, extracted, Actionable.MODULATE, inventory);
            changed |= extracted > 0;
        }
        ((ProbabilityTools) cluster.craftingLogic).nimble$reserveTools(tools.list);
        if (!supplies.list.isEmpty() && !warned && failureOutput != null) {
            notifyMissing(failureOutput); warned = true;
        }
        var machines = new HashMap<BlockPos, ProbabilityMachines.Machine>();
        for (var attempt : attempts) {
            if (attempt.failed) continue;
            if (attempt.machine == null) {
                changed = true;
                attempt.elapsed += 10;
                attempt.failed = attempt.elapsed >= Config.PROBABILITY_TIMEOUT_SECONDS.get() * 20L;
            } else {
                // No forced chunk loads and no timeout for supported but unloaded/paused machines.
                if (!cluster.getLevel().hasChunkAt(attempt.machine)) continue;
                if (!machines.containsKey(attempt.machine)) machines.put(attempt.machine,
                        ProbabilityMachines.resolveAt(cluster.getLevel(), attempt.machine));
                var current = machines.get(attempt.machine);
                if (current == null) {
                    ProbabilityMachines.unwatch(attempt.logic, attempt);
                    changed = true;
                    attempt.machine = null; attempt.elapsed = 0; continue;
                }
                if (attempt.logic != current.logic()) {
                    ProbabilityMachines.unwatch(attempt.logic, attempt);
                    attempt.logic = current.logic(); ProbabilityMachines.watch(current.logic(), attempt);
                }
                // Allow outputs to leave the machine and reach storage before checking the deficit.
                if (attempt.cycles == 0) { attempt.grace += 10; changed = true; }
                attempt.failed = attempt.grace >= 20;
            }
        }
        boolean removed = attempts.removeIf(a -> {
            if (!a.remaining.list.isEmpty() || retryDebts.containsKey(a) || a.machine != null && a.cycles != 0 && !a.failed) return false;
            ProbabilityMachines.unwatch(a.logic, a);
            return true;
        });
        if (removed) { rebuildOutputIndex(); changed = true; }
        if (changed) cluster.markDirty();
        if (calculation != null) {
            if (!calculation.isDone()) return;
            try {
                var plan = calculation.get();
                long stillNeeded = 0;
                for (var debt : retryDebts.entrySet()) stillNeeded += debt.getKey().remaining
                        .extract(requested.what(), debt.getValue(), Actionable.SIMULATE);
                if (plan.patternTimes().isEmpty()) {
                    // Provider removed or no remaining recipe: keep the original output debt for manual delivery.
                    for (var a : retryDebts.keySet()) {
                        ProbabilityMachines.unwatch(a.logic, a);
                        a.failed = false; a.machine = null; a.elapsed = 0;
                    }
                    if (!warned) { notifyMissing(requested.what()); warned = true; }
                } else if (stillNeeded == requested.amount()) {
                    ((ProbabilityJob) job).nimble$appendPlan(plan, requested);
                    credits.insert(requested.what(), requested.amount(), Actionable.MODULATE);
                    for (var debt : retryDebts.entrySet()) debt.getKey().remaining
                            .extract(requested.what(), debt.getValue(), Actionable.MODULATE);
                    rebuildOutputIndex();
                    for (var e : plan.usedItems()) supplies.insert(e.getKey(), e.getLongValue(), Actionable.MODULATE);
                    for (var e : plan.missingItems()) supplies.insert(e.getKey(), e.getLongValue(), Actionable.MODULATE);
                    for (var e : plan.emittedItems()) supplies.insert(e.getKey(), e.getLongValue(), Actionable.MODULATE);
                    var neededTools = com.ber.nimblePattern.crafting.ToolCraftingPlan.reservations(plan.patternTimes());
                    for (var tool : neededTools) {
                        supplies.extract(tool.getKey(), Math.min(tool.getLongValue(), plan.missingItems().get(tool.getKey()))
                                + Math.min(tool.getLongValue(), ((ProbabilityTools) cluster.craftingLogic).nimble$toolsHeld(tool.getKey())), Actionable.MODULATE);
                        tools.list.set(tool.getKey(), Math.max(tools.list.get(tool.getKey()), tool.getLongValue()));
                    }
                    failureOutput = requested.what();
                    if (plan.simulation() && !warned) { notifyMissing(requested.what()); warned = true; }
                }
            } catch (Exception e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                if (!warned) org.slf4j.LoggerFactory.getLogger(ProbabilityController.class).warn("Probability retry calculation could not be appended", e);
                if (!warned) { notifyMissing(requested.what()); warned = true; }
                poll = -190;
            } finally { calculation = null; requested = null; retryDebts.clear(); cluster.markDirty(); }
        }
        if (supplies.list.isEmpty()) {
            var activeOutputs = new HashSet<AEKey>();
            for (var a : attempts) if (!a.failed) activeOutputs.addAll(a.remaining.list.keySet());
            var pending = new HashMap<AEKey, Long>();
            for (var attempt : attempts) {
                if (!attempt.failed) continue;
                for (var e : attempt.remaining.list) {
                    long missing = Math.min(e.getLongValue(), ((ProbabilityJob) job).nimble$waiting()
                            .extract(e.getKey(), Long.MAX_VALUE, Actionable.SIMULATE));
                    if (missing <= 0) continue;
                    // A previously appended retry can also supply byproducts of this failed batch.
                    if (activeOutputs.contains(e.getKey())
                            || pending.computeIfAbsent(e.getKey(), cluster.craftingLogic::getPendingOutputs) > 0) continue;
                    // Coalesce this key's failed batches into one calculation, keeping each original debt owner.
                    long budget = ((ProbabilityJob) job).nimble$waiting().extract(e.getKey(), Long.MAX_VALUE, Actionable.SIMULATE);
                    long total = 0;
                    for (var candidate : byOutput.getOrDefault(e.getKey(), List.of())) {
                        if (!candidate.failed || total == budget) continue;
                        long part = Math.min(candidate.remaining.list.get(e.getKey()), budget - total);
                        if (part > 0) { retryDebts.put(candidate, part); total += part; }
                    }
                    requested = new GenericStack(e.getKey(), total);
                    calculation = grid.getCraftingService().beginCraftingCalculation(cluster.getLevel(), cluster::getSrc,
                            e.getKey(), total, CalculationStrategy.REPORT_MISSING_ITEMS);
                    cluster.markDirty(); return;
                }
            }
        }
    }

    private static void notifyMissing(AEKey what) {
        NimblePatternNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new ProbabilityFailurePacket(what));
    }

    public void clear() {
        if (calculation != null) calculation.cancel(true);
        calculation = null; requested = null;
        retryDebts.clear();
        for (var a : attempts) { a.cycles = 0; ProbabilityMachines.unwatch(a.logic, a); }
        if (owner != null) ACTIVE.remove(owner);
        owner = null;
        byOutput.clear();
        progressDirty = false;
        attempts.clear(); supplies.clear(); credits.clear(); tools.clear(); warned = false; enabled = false; failureOutput = null;
    }
    public CompoundTag save() {
        var tag = new CompoundTag(); var list = new ListTag();
        for (var a : attempts) {
            var t = new CompoundTag();
            t.put("remaining", a.remaining.writeToNBT());
            if (a.recipe != null) t.putString("recipe", a.recipe.toString());
            if (a.machine != null) t.putLong("machine", a.machine.asLong());
            t.putLong("cycles", a.cycles); t.putLong("elapsed", a.elapsed); t.putLong("grace", a.grace);
            t.putBoolean("failed", a.failed); list.add(t);
        }
        tag.put("attempts", list); tag.put("supplies", supplies.writeToNBT()); tag.put("credits", credits.writeToNBT());
        tag.put("tools", tools.writeToNBT());
        if (failureOutput != null) tag.put("failureOutput", failureOutput.toTagGeneric());
        tag.putBoolean("warned", warned); tag.putBoolean("enabled", enabled); return tag;
    }
    public void load(CompoundTag tag) {
        clear(); supplies.readFromNBT(tag.getList("supplies", 10)); credits.readFromNBT(tag.getList("credits", 10));
        tools.readFromNBT(tag.getList("tools", 10));
        failureOutput = tag.contains("failureOutput", Tag.TAG_COMPOUND)
                ? AEKey.fromTagGeneric(tag.getCompound("failureOutput")) : null;
        warned = tag.getBoolean("warned");
        enabled = tag.getBoolean("enabled");
        for (var entry : tag.getList("attempts", 10)) {
            var t = (CompoundTag) entry; var a = new Attempt();
            a.remaining.readFromNBT(t.getList("remaining", 10));
            a.recipe = ResourceLocation.tryParse(t.getString("recipe"));
            a.cycles = t.getLong("cycles"); a.elapsed = t.getLong("elapsed"); a.grace = t.getLong("grace");
            // A loaded CPU has no reliable ownership history for already-running machine work. Treat this as
            // untraceable rather than waiting forever for completion events which may have happened before load.
            if (t.contains("machine")) a.elapsed = 0;
            a.failed = t.getBoolean("failed"); attempts.add(a);
        }
        rebuildOutputIndex();
    }
}
