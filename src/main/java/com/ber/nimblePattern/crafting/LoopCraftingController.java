package com.ber.nimblePattern.crafting;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import com.ber.nimblePattern.item.storage.LoopStorageCellAccess;
import com.ber.nimblePattern.item.storage.LoopStorageCellInventory;
import com.ber.nimblePattern.pattern.LoopPatternData;
import com.ber.nimblePattern.pattern.NimbleEncodedPattern;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Executes a collapsed batch, growing the reserved seed before dispatching larger physical batches. */
public final class LoopCraftingController {
    private final CraftingCPUCluster cluster;
    private final ListCraftingInventory working = new ListCraftingInventory(key -> {
    });

    private boolean active;
    private List<NimbleEncodedPattern> steps = List.of();
    private LoopPatternData data;
    private LoopStorageCellInventory cell;
    private boolean compositeOperation;
    private GenericStack advertisedOutput;
    private int stepIndex;
    private AEKey expectedKey;
    private long expectedRemaining;
    private long remainingOperations;
    private long batchOperations;

    public LoopCraftingController(CraftingCPUCluster cluster) {
        this.cluster = cluster;
    }

    public boolean isActive() {
        return active;
    }

    public boolean start(NimbleEncodedPattern composite, KeyCounter[] externalInputs, long operations) {
        if (active || operations <= 0 || !composite.getLoopMode() || cluster.getGrid() == null) {
            return false;
        }
        var loopData = composite.getLoopData();
        long totalOutput;
        try {
            totalOutput = Math.multiplyExact(composite.getPrimaryOutput().amount(), operations);
            // The last physical output includes the working seeds, not just the net gain.
            for (var output : loopData.mainOutputs()) {
                Math.multiplyExact(output.amount(), operations);
            }
        } catch (ArithmeticException overflow) {
            return false;
        }
        List<NimbleEncodedPattern> resolved;
        LoopStorageCellInventory loopCell = null;
        if (composite.isLoopComposite()) {
            var service = (CraftingService) cluster.getGrid().getCraftingService();
            resolved = resolveSteps(service, loopData);
            if (resolved == null) {
                return false;
            }
            try {
                for (var step : resolved) {
                    for (var input : step.getPattern().getInputs()) {
                        long multiplier = Math.multiplyExact(input.getMultiplier(), operations);
                        for (var candidate : input.getPossibleInputs()) {
                            Math.multiplyExact(candidate.amount(), multiplier);
                        }
                    }
                    for (var output : step.getPattern().getOutputs()) {
                        Math.multiplyExact(output.amount(), operations);
                    }
                }
            } catch (ArithmeticException overflow) {
                return false;
            }
            loopCell = LoopStorageCellAccess.find(
                    cluster.getGrid().getStorageService().getInventory(),
                    loopData.storageCellId(),
                    loopData.entry().what(),
                    loopData.seedAmount());
            if (loopCell == null
                    || loopCell.extractForLoop(loopData.entry().what(), loopData.seedAmount(), Actionable.MODULATE)
                    != loopData.seedAmount()) {
                return false;
            }
        } else {
            resolved = List.of(composite);
        }

        working.clear();
        for (var holder : externalInputs) {
            for (var input : holder) {
                working.insert(input.getKey(), input.getLongValue(), Actionable.MODULATE);
            }
        }
        if (composite.isLoopComposite()) {
            working.insert(loopData.entry().what(), loopData.seedAmount(), Actionable.MODULATE);
        }

        this.data = loopData;
        this.steps = resolved;
        this.cell = loopCell;
        this.compositeOperation = composite.isLoopComposite();
        this.advertisedOutput = new GenericStack(composite.getPrimaryOutput().what(), totalOutput);
        this.remainingOperations = operations;
        this.batchOperations = 0;
        this.stepIndex = 0;
        this.expectedKey = null;
        this.expectedRemaining = 0;
        this.active = true;
        return true;
    }

    public int tick(CraftingService craftingService, IEnergyService energyService, Level level) {
        if (!active || expectedRemaining > 0 || stepIndex >= steps.size()) {
            return 0;
        }

        var wrapper = steps.get(stepIndex);
        var raw = wrapper.getPattern();
        if (batchOperations == 0) {
            long available = working.extract(data.entry().what(), Long.MAX_VALUE, Actionable.SIMULATE);
            batchOperations = LoopBatchSizing.next(remainingOperations, available, data.seedAmount());
            if (batchOperations == 0) {
                return 0;
            }
        }
        var scaled = new ScaledPattern(raw, batchOperations);
        var expectedOutputs = new KeyCounter();
        var expectedContainers = new KeyCounter();
        var holders = CraftingCpuHelper.extractPatternInputs(scaled, working, level, expectedOutputs, expectedContainers);
        if (holders == null) {
            return 0;
        }

        var primary = raw.getPrimaryOutput();
        expectedKey = primary.what();
        expectedRemaining = Math.multiplyExact(primary.amount(), batchOperations);
        double power = CraftingCpuHelper.calculatePatternPower(holders);
        if (energyService.extractAEPower(power, Actionable.SIMULATE, PowerMultiplier.CONFIG) < power - 0.01) {
            expectedKey = null;
            expectedRemaining = 0;
            CraftingCpuHelper.reinjectPatternInputs(working, holders);
            return 0;
        }

        for (var provider : craftingService.getProviders(wrapper)) {
            if (provider.isBusy()) {
                continue;
            }
            if (provider.pushPattern(raw, holders)) {
                energyService.extractAEPower(power, Actionable.MODULATE, PowerMultiplier.CONFIG);
                return 1;
            }
        }

        expectedKey = null;
        expectedRemaining = 0;
        CraftingCpuHelper.reinjectPatternInputs(working, holders);
        return 0;
    }

    public Acceptance acceptOutput(AEKey what, long amount, Actionable mode) {
        if (!active || expectedRemaining <= 0 || !what.equals(expectedKey)) {
            return Acceptance.NOT_HANDLED;
        }

        long accepted = Math.min(amount, expectedRemaining);
        if (mode == Actionable.SIMULATE) {
            return new Acceptance(accepted, null);
        }

        working.insert(what, accepted, Actionable.MODULATE);
        expectedRemaining -= accepted;
        if (expectedRemaining > 0) {
            return new Acceptance(accepted, null);
        }

        expectedKey = null;
        stepIndex++;
        if (stepIndex < steps.size()) {
            return new Acceptance(accepted, null);
        }

        remainingOperations -= batchOperations;
        if (remainingOperations > 0) {
            // Retain the net gain as working seed. The next whole-loop batch grows as far as the accumulated seed
            // allows, capped by the operations AE actually scheduled. These bootstrap turns are part of the order.
            stepIndex = 0;
            batchOperations = 0;
            return new Acceptance(accepted, null);
        }

        if (compositeOperation) {
            long seed = working.extract(data.entry().what(), data.seedAmount(), Actionable.SIMULATE);
            if (seed != data.seedAmount()
                    || cell.insertForLoop(data.entry().what(), seed, Actionable.SIMULATE) != seed) {
                // Keep the controller active and stop the final result from being published if its reserved seed
                // cannot be restored to the exact cell that supplied it.
                return new Acceptance(accepted, null);
            }
            long restored = cell.insertForLoop(data.entry().what(), seed, Actionable.MODULATE);
            working.extract(data.entry().what(), restored, Actionable.MODULATE);
        }

        long output = working.extract(advertisedOutput.what(), advertisedOutput.amount(), Actionable.MODULATE);
        if (output != advertisedOutput.amount()) {
            return new Acceptance(accepted, null);
        }

        active = false;
        var finalOutput = new GenericStack(advertisedOutput.what(), output);
        resetFinishedState();
        return new Acceptance(accepted, finalOutput);
    }

    /**
     * Stops the internal loop immediately. If its seed is still in the controller (before the first step or between
     * steps after it has returned to the entry key), restore it to the exact cell it came from. Everything else that
     * has not yet been dispatched is returned to the CPU so AE's normal cancellation can put it back on the network.
     *
     * @return the entry key when the seed had already entered an external machine and could not be restored
     */
    public AEKey cancelAndReturnSeed(ListCraftingInventory cpuInventory) {
        if (!active) {
            return null;
        }

        AEKey lostSeed = null;
        if (compositeOperation) {
            var seedKey = data.entry().what();
            long seedAmount = data.seedAmount();
            boolean canRestore = cell != null
                    && working.extract(seedKey, seedAmount, Actionable.SIMULATE) == seedAmount
                    && cell.insertForLoop(seedKey, seedAmount, Actionable.SIMULATE) == seedAmount;
            if (canRestore) {
                long extracted = working.extract(seedKey, seedAmount, Actionable.MODULATE);
                long inserted = cell.insertForLoop(seedKey, extracted, Actionable.MODULATE);
                if (inserted != seedAmount) {
                    // A concurrent cell change invalidated the simulation. Undo the partial return where possible and
                    // let AE put the recovered portion back into ordinary network storage during cancellation.
                    if (inserted > 0) {
                        cell.extractForLoop(seedKey, inserted, Actionable.MODULATE);
                    }
                    working.insert(seedKey, extracted, Actionable.MODULATE);
                    lostSeed = seedKey;
                }
            } else {
                lostSeed = seedKey;
            }
        }

        for (var entry : working.list) {
            cpuInventory.insert(entry.getKey(), entry.getLongValue(), Actionable.MODULATE);
        }
        resetFinishedState();
        return lostSeed;
    }

    private List<NimbleEncodedPattern> resolveSteps(CraftingService service, LoopPatternData loopData) {
        var result = new ArrayList<NimbleEncodedPattern>(loopData.size());
        for (int index = 0; index < loopData.size(); index++) {
            NimbleEncodedPattern found = null;
            var output = loopData.mainOutputs().get(index).what();
            for (var candidate : service.getCraftingFor(output)) {
                if (candidate instanceof NimbleEncodedPattern nimble && nimble.getLoopMode()) {
                    var candidateData = nimble.getLoopData();
                    if (candidateData.groupId().equals(loopData.groupId()) && candidateData.index() == index) {
                        found = nimble;
                        break;
                    }
                }
            }
            if (found == null) {
                return null;
            }
            result.add(found);
        }
        return result;
    }

    private void resetFinishedState() {
        active = false;
        steps = List.of();
        data = null;
        cell = null;
        compositeOperation = false;
        advertisedOutput = null;
        stepIndex = 0;
        expectedKey = null;
        expectedRemaining = 0;
        remainingOperations = 0;
        batchOperations = 0;
        working.clear();
    }

    /** Scales input extraction only; providers receive their registered raw pattern and the multiplied holders. */
    private record ScaledPattern(IPatternDetails pattern, long operations) implements IPatternDetails {
        @Override
        public AEItemKey getDefinition() {
            return pattern.getDefinition();
        }

        @Override
        public IInput[] getInputs() {
            return java.util.Arrays.stream(pattern.getInputs()).map(input -> new IInput() {
                @Override
                public GenericStack[] getPossibleInputs() {
                    return input.getPossibleInputs();
                }

                @Override
                public long getMultiplier() {
                    return Math.multiplyExact(input.getMultiplier(), operations);
                }

                @Override
                public boolean isValid(AEKey key, Level level) {
                    return input.isValid(key, level);
                }

                @Override
                public AEKey getRemainingKey(AEKey key) {
                    return input.getRemainingKey(key);
                }
            }).toArray(IInput[]::new);
        }

        @Override
        public GenericStack[] getOutputs() {
            return java.util.Arrays.stream(pattern.getOutputs())
                    .map(output -> new GenericStack(output.what(), Math.multiplyExact(output.amount(), operations)))
                    .toArray(GenericStack[]::new);
        }
    }

    public record Acceptance(long accepted, GenericStack completedOutput) {
        public static final Acceptance NOT_HANDLED = new Acceptance(0, null);

        public boolean handled() {
            return accepted > 0;
        }
    }
}
