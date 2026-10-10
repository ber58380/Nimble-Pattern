package com.ber.nimblePattern.crafting;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import com.ber.nimblePattern.item.storage.LoopStorageCellAccess;
import com.ber.nimblePattern.item.storage.LoopStorageCellInventory;
import com.ber.nimblePattern.pattern.loop.LoopPatternData;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import com.ber.nimblePattern.pattern.PatternMapping;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import com.ber.nimblePattern.pattern.wrapper.NimbleProcessingPattern;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class LoopCraftingController {
    private final CraftingCPUCluster cluster;
    private final ListCraftingInventory working = new ListCraftingInventory(key -> {
    });
    private boolean active;
    private List<NimbleProcessingPattern> steps = List.of();
    private LoopPatternData data;
    // Seeds belong to the CPU job, not to a single dispatched pattern operation.
    private final Map<UUID, ReservedSeed> reservedSeeds = new LinkedHashMap<>();
    private ItemStack definition = ItemStack.EMPTY;
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

    public boolean start(NimbleProcessingPattern composite, KeyCounter[] externalInputs, long operations) {
        if (active || operations <= 0 || !composite.getLoopMode() || cluster.getGrid() == null) {
            return false;
        }
        var loopData = composite.getLoopData();
        long totalOutput;
        try {
            totalOutput = Math.multiplyExact(composite.getPrimaryOutput().amount(), operations);
            for (var output : loopData.outputs()) {
                Math.multiplyExact(output.amount(), operations);
            }
        } catch (ArithmeticException overflow) {
            return false;
        }
        List<NimbleProcessingPattern> resolved;
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
            var reserved = reservedSeeds.get(loopData.loopId());
            if (reserved != null) {
                if (!reserved.cellId().equals(loopData.storageCellId())
                        || !reserved.seed().what().equals(loopData.entry().what())
                        || reserved.seed().amount() != loopData.seedAmount()) return false;
                reservedSeeds.remove(loopData.loopId());
            } else {
                loopCell = LoopStorageCellAccess.find(
                        cluster.getGrid().getStorageService().getInventory(), loopData.storageCellId(),
                        loopData.entry().what(), loopData.seedAmount());
                if (loopCell == null || loopCell.extractForLoop(loopData.entry().what(), loopData.seedAmount(),
                        Actionable.MODULATE) != loopData.seedAmount()) return false;
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
        this.definition = composite.getDefinition().toStack();
        this.compositeOperation = composite.isLoopComposite();
        this.advertisedOutput = new GenericStack(composite.getPrimaryOutput().what(), totalOutput);
        this.remainingOperations = operations;
        this.batchOperations = 0;
        this.stepIndex = 0;
        this.expectedKey = null;
        this.expectedRemaining = 0;
        this.active = true;
        cluster.markDirty();
        return true;
    }

    public int tick(CraftingService craftingService, IEnergyService energyService, Level level) {
        if (!active || expectedRemaining > 0 || stepIndex >= data.size()) {
            return 0;
        }
        if (steps.isEmpty()) {
            var resolved = resolveSteps(craftingService, data);
            if (resolved == null) return 0; // Providers may not yet be loaded after restarting.
            steps = resolved;
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
            if (provider.pushPattern(PatternMapping.getOriginalPattern(provider, wrapper), holders)) {
                energyService.extractAEPower(power, Actionable.MODULATE, PowerMultiplier.CONFIG);
                cluster.markDirty();
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
        cluster.markDirty();
        expectedRemaining -= accepted;
        if (expectedRemaining > 0) {
            return new Acceptance(accepted, null);
        }

        expectedKey = null;
        stepIndex++;
        if (stepIndex < data.size()) {
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
            long seed = working.extract(data.entry().what(), data.seedAmount(), Actionable.MODULATE);
            if (seed > 0) {
                reservedSeeds.put(data.loopId(), new ReservedSeed(data.storageCellId(),
                        new GenericStack(data.entry().what(), seed)));
            }
        }

        long output = working.extract(advertisedOutput.what(), advertisedOutput.amount(), Actionable.MODULATE);
        if (output != advertisedOutput.amount()) {
            return new Acceptance(accepted, null);
        }

        active = false;
        var finalOutput = new GenericStack(advertisedOutput.what(), output);
        var remainder = new ArrayList<GenericStack>();
        for (var entry : working.list) {
            remainder.add(new GenericStack(entry.getKey(), entry.getLongValue()));
        }
        resetFinishedState();
        return new Acceptance(accepted, finalOutput, List.copyOf(remainder));
    }

    /** Release job-owned seeds once, at completion or cancellation, using the current network mounts. */
    public List<SeedFailure> releaseSeeds(ListCraftingInventory cpuInventory) {
        var failures = new ArrayList<SeedFailure>();
        if (active && compositeOperation) {
            long held = working.extract(data.entry().what(), data.seedAmount(), Actionable.MODULATE);
            if (held > 0) {
                reservedSeeds.put(data.loopId(), new ReservedSeed(data.storageCellId(),
                        new GenericStack(data.entry().what(), held)));
            }
            if (held < data.seedAmount()) failures.add(new SeedFailure(data.entry().what(), false));
        }
        var grid = cluster.getGrid();
        for (var reserved : reservedSeeds.values()) {
            var seed = reserved.seed();
            var cell = grid == null ? null : LoopStorageCellAccess.findMounted(
                    grid.getStorageService().getInventory(), reserved.cellId());
            long restored = 0;
            if (cell != null && cell.insertForLoop(seed.what(), seed.amount(), Actionable.SIMULATE) == seed.amount()) {
                restored = cell.insertForLoop(seed.what(), seed.amount(), Actionable.MODULATE);
            }
            if (restored < seed.amount()) {
                cpuInventory.insert(seed.what(), seed.amount() - restored, Actionable.MODULATE);
                failures.add(new SeedFailure(seed.what(), true));
            }
        }
        for (var entry : working.list) {
            cpuInventory.insert(entry.getKey(), entry.getLongValue(), Actionable.MODULATE);
        }
        reservedSeeds.clear();
        resetFinishedState();
        cluster.markDirty();
        return List.copyOf(failures);
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        var seeds = new ListTag();
        reservedSeeds.forEach((loopId, reserved) -> {
            var seed = new CompoundTag();
            seed.putUUID("loopId", loopId);
            seed.putUUID("cellId", reserved.cellId());
            seed.put("seed", GenericStack.writeTag(reserved.seed()));
            seeds.add(seed);
        });
        tag.put("reservedSeeds", seeds);
        tag.putBoolean("active", active);
        if (active) {
            tag.put("definition", definition.save(new CompoundTag()));
            tag.put("working", working.writeToNBT());
            tag.putBoolean("composite", compositeOperation);
            tag.put("advertisedOutput", GenericStack.writeTag(advertisedOutput));
            tag.putInt("stepIndex", stepIndex);
            tag.putLong("remainingOperations", remainingOperations);
            tag.putLong("batchOperations", batchOperations);
            if (expectedKey != null && expectedRemaining > 0) {
                tag.put("expected", GenericStack.writeTag(new GenericStack(expectedKey, expectedRemaining)));
            }
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        resetFinishedState();
        reservedSeeds.clear();
        for (var entry : tag.getList("reservedSeeds", Tag.TAG_COMPOUND)) {
            var seed = (CompoundTag) entry;
            var stack = GenericStack.readTag(seed.getCompound("seed"));
            if (stack == null || stack.amount() <= 0) throw new IllegalArgumentException("Invalid reserved loop seed");
            reservedSeeds.put(seed.getUUID("loopId"), new ReservedSeed(seed.getUUID("cellId"), stack));
        }
        if (!tag.getBoolean("active")) return;
        definition = ItemStack.of(tag.getCompound("definition"));
        data = NimblePatternTag.getLoop(definition);
        advertisedOutput = GenericStack.readTag(tag.getCompound("advertisedOutput"));
        stepIndex = tag.getInt("stepIndex");
        remainingOperations = tag.getLong("remainingOperations");
        batchOperations = tag.getLong("batchOperations");
        if (data == null || advertisedOutput == null || stepIndex < 0 || stepIndex >= data.size()
                || remainingOperations <= 0 || batchOperations < 0 || batchOperations > remainingOperations) {
            throw new IllegalArgumentException("Invalid saved loop execution");
        }
        working.readFromNBT(tag.getList("working", Tag.TAG_COMPOUND));
        compositeOperation = tag.getBoolean("composite");
        var expected = GenericStack.readTag(tag.getCompound("expected"));
        if (expected != null && expected.amount() > 0) {
            expectedKey = expected.what();
            expectedRemaining = expected.amount();
        }
        // Resolve live providers lazily; an in-flight return can be accepted immediately.
        active = true;
    }

    private record ReservedSeed(UUID cellId, GenericStack seed) {}
    public record SeedFailure(AEKey key, boolean returnFailed) {}

    private List<NimbleProcessingPattern> resolveSteps(CraftingService service, LoopPatternData loopData) {
        var result = new ArrayList<NimbleProcessingPattern>(loopData.size());
        for (int index = 0; index < loopData.size(); index++) {
            NimbleProcessingPattern found = null;
            var output = loopData.outputs().get(index).what();
            for (var candidate : service.getCraftingFor(output)) {
                if (candidate instanceof NimbleProcessingPattern nimble && nimble.getLoopMode()) {
                    var candidateData = nimble.getLoopData();
                    if (candidateData.loopId().equals(loopData.loopId()) && candidateData.index() == index) {
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
        definition = ItemStack.EMPTY;
        compositeOperation = false;
        advertisedOutput = null;
        stepIndex = 0;
        expectedKey = null;
        expectedRemaining = 0;
        remainingOperations = 0;
        batchOperations = 0;
        working.clear();
    }

    /**
     * Scales input extraction only; providers receive their registered raw pattern and the multiplied holders.
     */
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

    public record Acceptance(long accepted, GenericStack completedOutput,
                             List<GenericStack> remainder) {
        public Acceptance(long accepted, GenericStack completedOutput) {
            this(accepted, completedOutput, List.of());
        }

        public static final Acceptance NOT_HANDLED = new Acceptance(0, null);

        public boolean handled() {
            return accepted > 0;
        }
    }
}
