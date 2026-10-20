package com.ber.nimblePattern.crafting;

import appeng.api.config.Actionable;
import appeng.api.features.IPlayerRegistry;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftingPlan;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import com.ber.nimblePattern.mixin.ae2.ExecutingCraftingJobAccessorMixin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.concurrent.Future;

/**
 * Supplements the existing CPU job. Never submits a second job or allocates a CPU.
 */
public final class ProbabilityRetryController {
    private final CraftingCPUCluster cluster;
    // How much outputs of supplement crafting is already recorded in original crafting plan,
    // and should not add new waiting.
    private final ListCraftingInventory replacementCredits = new ListCraftingInventory(key -> {
    });
    private Future<ICraftingPlan> calculation;
    // the original crafting job
    private ExecutingCraftingJob calculationJob;
    // the target crafting output of this retry crafting
    private GenericStack requested;
    private boolean stopped;

    public ProbabilityRetryController(CraftingCPUCluster cluster) {
        this.cluster = cluster;
    }

    /**
     * Determine the CPU of original crafting job is not linked to the network.
     */
    private boolean isCancelled(ExecutingCraftingJob job) {
        if (job == null) {
            return true;
        }
        var link = ((ExecutingCraftingJobAccessorMixin) job).nimblePattern$getLink();
        return cluster.craftingLogic.getLastLink() != link || link.isCanceled();
    }

    /**
     * Return the material to the network
     */
    private void returnMaterial(ListCraftingInventory inventory, AEKey key, long amount) {
        var grid = cluster.getGrid();
        long returned = grid == null ? 0 : grid.getStorageService().getInventory()
                .insert(key, amount, Actionable.MODULATE, cluster.getSrc());
        if (returned < amount) {
            inventory.insert(key, amount - returned, Actionable.MODULATE);
        }
    }

    /**
     * Collect the target material from the network storage for supplement crafting
     */
    private void collectExisting(ExecutingCraftingJob job, ListCraftingInventory inventory, AEKey key, long amount) {
        if (amount <= 0 || isCancelled(job)) {
            return;
        }
        var grid = cluster.getGrid();
        if (grid == null) {
            return;
        }
        var storage = grid.getStorageService().getInventory();
        long extracted = storage.extract(key, amount, Actionable.MODULATE, cluster.getSrc());
        if (extracted <= 0) {
            return;
        }
        long accepted = cluster.craftingLogic.insert(key, extracted, Actionable.MODULATE);
        if (accepted < extracted) {
            returnMaterial(inventory, key, extracted - accepted);
        }
        cluster.markDirty();
    }

    private void cancelCalculation() {
        if (calculation != null) {
            calculation.cancel(true);
        }
        calculation = null;
        calculationJob = null;
        requested = null;
    }

    private void fail(ExecutingCraftingJob job) {
        cancelCalculation();
        if (stopped || isCancelled(job)) {
            return;
        }
        stopped = true;
        cluster.markDirty();
        var playerId = ((ExecutingCraftingJobAccessorMixin) job).nimblePattern$getPlayerId();
        var server = cluster.getLevel().getServer();
        var player = playerId == null || server == null ? null : IPlayerRegistry.getConnected(server, playerId);
        if (player != null) {
            // TODO:更改消息提示
            player.sendSystemMessage(Component.translatable("message.nimble_pattern.probability_blocked"));
        }
    }

    /**
     * Tools already reserved/borrowed by this job must not be acquired a second time.
     */
    private static ICraftingPlan reuseReservedTools(ICraftingPlan plan, ToolCraftingController tools) {
        if (tools == null) {
            return plan;
        }
        var used = new KeyCounter();
        used.addAll(plan.usedItems());
        var missing = new KeyCounter();
        missing.addAll(plan.missingItems());
        for (var tool : ToolReservation.getToolReservation(plan.patternTimes())) {
            var key = tool.getKey();
            long held = Math.min(tool.getLongValue(), tools.getHeldAmount(key));
            used.remove(key, Math.min(held, used.get(key)));
            missing.remove(key, Math.min(held, missing.get(key)));
        }
        used.removeZeros();
        missing.removeZeros();
        // Preserve a simulation failure with no reported missing items (e.g. an invalid recipe).
        boolean simulation = !missing.isEmpty() || plan.simulation() && plan.missingItems().isEmpty();
        return new CraftingPlan(plan.finalOutput(), plan.bytes(), simulation, plan.multiplePaths(),
                used, plan.emittedItems(), missing, plan.patternTimes());
    }

    /**
     * Based on the crafting plan, collect materials and send to the CPU
     */
    private boolean acquireMaterials(ICraftingPlan plan, ListCraftingInventory inventory) {
        var storage = cluster.getGrid().getStorageService().getInventory();
        for (var entry : plan.usedItems()) {
            long required = entry.getLongValue();
            if (required > 0 && storage.extract(entry.getKey(), required,
                    Actionable.SIMULATE, cluster.getSrc()) != required) {
                return false;
            }
        }
        var acquired = new KeyCounter();
        boolean transferred = false;
        try {
            for (var entry : plan.usedItems()) {
                long required = entry.getLongValue();
                if (required <= 0) {
                    continue;
                }
                long amount = storage.extract(entry.getKey(), required, Actionable.MODULATE, cluster.getSrc());
                if (amount > 0) {
                    acquired.add(entry.getKey(), amount);
                }
                if (amount != required) {
                    return false;
                }
            }
            for (var entry : acquired) {
                inventory.insert(entry.getKey(), entry.getLongValue(), Actionable.MODULATE);
            }
            transferred = true;
            return true;
        } finally {
            if (!transferred) {
                for (var entry : acquired) {
                    returnMaterial(inventory, entry.getKey(), entry.getLongValue());
                }
            }
            cluster.markDirty();
        }
    }

    /**
     * Submit the supplement crafting plan and corresponding materials to the CPU
     */
    private ICraftingPlan submitCalculation(ExecutingCraftingJob job, ListCraftingInventory inventory,
                                            ProbabilityTrackingController tracking, ToolCraftingController tools) {
        if (calculationJob != job) {
            cancelCalculation();
            return null;
        }
        if (!calculation.isDone()) {
            return null;
        }
        var future = calculation;
        var target = requested;
        calculation = null;
        calculationJob = null;
        requested = null;
        try {
            var plan = future.get();
            // Use newly available stock before committing an old network snapshot.
            collectExisting(job, inventory, target.what(), tracking.getRetryAmount(target.what()));
            if (isCancelled(job) || tracking.getRetryAmount(target.what()) != target.amount()) {
                return null;
            }
            plan = reuseReservedTools(plan, tools);
            if (plan.simulation()
                    || !plan.missingItems().isEmpty()
                    || plan.patternTimes().isEmpty()
                    || !plan.emittedItems().isEmpty()
                    || !plan.finalOutput().what().equals(target.what())
                    || plan.finalOutput().amount() < target.amount()
                    || plan.bytes() > cluster.getAvailableStorage()) {
                fail(job);
                return null;
            }
            // A removed provider can invalidate the asynchronous plan even if all materials remain.
            var craftingService = (CraftingService) cluster.getGrid().getCraftingService();
            for (var pattern : plan.patternTimes().keySet()) {
                if (!craftingService.getProviders(pattern).iterator().hasNext()) {
                    fail(job);
                    return null;
                }
            }
            var supplement = ProbabilityCraftingSupplement.prepare(job, plan, target);
            if (!acquireMaterials(plan, inventory)) {
                fail(job);
                return null;
            }
            // Apply supplement crafting plan to the CPU, add extra crafting steps
            supplement.apply(job);
            replacementCredits.insert(target.what(), target.amount(), Actionable.MODULATE);
            tracking.receiveSupplementPlan(target.what(), target.amount());
            cluster.markDirty();
            return plan;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(job);
        } catch (Exception | LinkageError e) {
            fail(job);
        }
        return null;
    }

    /**
     * Return the supplement crafting plan so the CPU can reserve materials for its new tasks.
     */
    public ICraftingPlan tick(ExecutingCraftingJob job, ListCraftingInventory inventory,
                              ProbabilityTrackingController tracking, ToolCraftingController tools) {
        if (isCancelled(job)) {
            cancelCalculation();
            return null;
        }
        if (stopped || !cluster.isActive() || cluster.getGrid() == null) {
            return null;
        }
        // only calculate one supplement crafting once time
        if (calculation != null) {
            return submitCalculation(job, inventory, tracking, tools);
        }
        for (var missing : tracking.getRetryOutputs()) {
            var key = missing.what();
            long amount = missing.amount();
            if (amount <= 0) {
                continue;
            }
            collectExisting(job, inventory, key, amount);
            // Insertion may finish the job and release this controller reentrantly.
            if (isCancelled(job)) {
                return null;
            }
            amount = tracking.getRetryAmount(key);
            if (amount <= 0) {
                continue;
            }
            try {
                requested = new GenericStack(key, amount);
                calculationJob = job;
                calculation = cluster.getGrid().getCraftingService().beginCraftingCalculation(
                        cluster.getLevel(), cluster::getSrc, key, amount,
                        CalculationStrategy.REPORT_MISSING_ITEMS);
            } catch (RuntimeException e) {
                fail(job);
            }
            return null;
        }
        return null;
    }

    /**
     * Receive the outputs from supplement crafting job
     */
    public long reuseWaiting(AEKey key, long amount, Actionable mode) {
        return replacementCredits.extract(key, amount, mode);
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putBoolean("stopped", stopped);
        tag.put("replacementCredits", replacementCredits.writeToNBT());
        return tag;
    }

    public void load(CompoundTag tag) {
        release();
        stopped = tag.getBoolean("stopped");
        replacementCredits.readFromNBT(tag.getList("replacementCredits", Tag.TAG_COMPOUND));
    }


    public void release() {
        cancelCalculation();
        replacementCredits.clear();
        stopped = false;
    }
}
