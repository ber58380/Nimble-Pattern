package com.ber.nimblePattern.crafting;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.*;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.util.inv.AppEngInternalInventory;
import com.ber.nimblePattern.item.ModItems;
import com.ber.nimblePattern.item.storage.LoopStorageTier;
import com.ber.nimblePattern.pattern.*;
import com.ber.nimblePattern.probability.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("nimble_pattern")
@PrefixGameTestTemplate(false)
public final class OptimizationGameTests {
    private static final AEItemKey IRON = AEItemKey.of(Items.IRON_INGOT);
    private static final AEItemKey GOLD = AEItemKey.of(Items.GOLD_INGOT);
    private static final ResourceLocation RECIPE = ResourceLocation.fromNamespaceAndPath("nimble_pattern", "optimization_test");
    private static ItemStack patternStack() {
        return PatternDetailsHelper.encodeProcessingPattern(new GenericStack[] {new GenericStack(IRON, 1)},
                new GenericStack[] {new GenericStack(GOLD, 1)});
    }
    private static AEItemKey variant() {
        var stack = new ItemStack(Items.GOLD_INGOT);
        stack.getOrCreateTag().putString("variant", "other");
        return AEItemKey.of(stack);
    }

    public static final class LimitedProvider implements ICraftingProvider {
        private final IPatternDetails nativePattern;
        LimitedProvider(IPatternDetails nativePattern) { this.nativePattern = nativePattern; }
        public List<IPatternDetails> getAvailablePatterns() { return List.of(nativePattern); }
        public boolean isBusy() { return false; }
        public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputs) { return pattern == nativePattern; }
        public long gtlcore$getMaxPatternOperations(IPatternDetails pattern, long remaining) {
            return pattern == nativePattern ? Math.min(remaining, 32) : 0;
        }
    }

    @GameTest(template = "empty")
    public static void batchLimitsUseProviderNativeIdentity(GameTestHelper h) {
        var raw = PatternDetailsHelper.decodePattern(patternStack(), h.getLevel());
        var wrapped = NimbleEncodedPattern.wrap(raw, true);
        var provider = new LimitedProvider(raw);
        ProviderPatternIndex.register(provider, List.of(raw), List.of(wrapped));
        h.assertTrue(com.ber.nimblePattern.compat.gtl.GtlBatchAdapter.maximum(provider, wrapped, 100) == 32,
                "GTL capacity queries use native pattern identity, not wrapper equality");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fakeModeIsCachedAndCannotOverrideProbability(GameTestHelper h) {
        var book = new ItemStack(Items.BOOK);
        book.setHoverName(net.minecraft.network.chat.Component.literal("virtual-output"));
        var encoded = PatternDetailsHelper.encodeProcessingPattern(new GenericStack[] {new GenericStack(IRON, 1)},
                new GenericStack[] {new GenericStack(AEItemKey.of(book), 1)});
        h.assertTrue(NimbleEncodedPattern.wrap(PatternDetailsHelper.decodePattern(encoded, h.getLevel()), true).getFakeMode(),
                "Named-book virtual completion remains compatible with fuzzy processing");
        ProbabilityEncoding.mark(encoded, RECIPE, 1);
        h.assertTrue(!NimbleEncodedPattern.wrap(PatternDetailsHelper.decodePattern(encoded, h.getLevel()), false).getFakeMode(),
                "Probability output must not be acknowledged as a virtual result");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void updateConditionsAreTrackedAcrossNetworksWithoutMenus(GameTestHelper h) {
        var firstPos = new net.minecraft.core.BlockPos(0, 1, 0);
        var secondPos = new net.minecraft.core.BlockPos(2, 1, 2);
        for (var pos : List.of(firstPos, secondPos)) {
            h.setBlock(pos.below(), appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block());
            h.setBlock(pos, appeng.core.definitions.AEBlocks.PATTERN_PROVIDER.block());
        }
        var first = ((appeng.helpers.patternprovider.PatternContainer) h.getBlockEntity(firstPos)).getTerminalPatternInventory();
        var second = ((appeng.helpers.patternprovider.PatternContainer) h.getBlockEntity(secondPos)).getTerminalPatternInventory();
        var a = patternStack(); NimblePatternTag.tagUpdate(a, "minecraft:diamond"); first.setItemDirect(0, a);
        var b = patternStack(); NimblePatternTag.tagUpdate(b, "minecraft:emerald"); second.setItemDirect(0, b);
        h.runAfterDelay(40, () -> PatternUpgradeTracker.instance().enqueueIfTracked("minecraft:diamond"));
        h.runAfterDelay(70, () -> {
            h.assertTrue(NimblePatternTag.getStatus(first.getStackInSlot(0)) == UpdateState.UPDATE, "First network tracked without opening a terminal");
            h.assertTrue(NimblePatternTag.getStatus(second.getStackInSlot(0)) == UpdateState.LATEST, "Unrelated condition unchanged");
            PatternUpgradeTracker.instance().enqueueIfTracked("minecraft:emerald");
        });
        h.runAfterDelay(100, () -> {
            h.assertTrue(NimblePatternTag.getStatus(second.getStackInSlot(0)) == UpdateState.UPDATE, "Second network's conditions were not overwritten");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void providerIndexAndMenuSnapshotsAvoidRepeatedInventoryScans(GameTestHelper h) {
        int[] reads = {0};
        var marked = patternStack(); ProbabilityEncoding.mark(marked, RECIPE, 1);
        var inv = new appeng.api.inventories.InternalInventory() {
            public int size() { return 512; }
            public ItemStack getStackInSlot(int slot) { reads[0]++; return slot == 0 ? marked : ItemStack.EMPTY; }
            public void setItemDirect(int slot, ItemStack stack) { throw new UnsupportedOperationException(); }
        };
        var provider = java.lang.reflect.Proxy.newProxyInstance(OptimizationGameTests.class.getClassLoader(),
                new Class<?>[] {appeng.helpers.patternprovider.PatternContainer.class}, (p, m, a) -> inv);
        var index = new ProbabilityPattern.Index(provider);
        var raw = PatternDetailsHelper.decodePattern(patternStack(), h.getLevel());
        int afterIndex = reads[0];
        for (int i = 0; i < 512; i++) index.recover(raw);
        h.assertTrue(afterIndex == 512 && reads[0] == afterIndex, "512 pattern lookups scan the physical inventory only once");
        var snapshot = PatternInventorySnapshots.get(inv);
        int afterSnapshot = reads[0];
        for (int i = 0; i < 20; i++) h.assertTrue(PatternInventorySnapshots.get(inv) == snapshot, "Menus share a snapshot");
        h.assertTrue(reads[0] == afterSnapshot, "20 menu readers do not rescan the inventory");
        long revision = snapshot.revision();
        marked.getOrCreateTag().putInt("edit", 1);
        h.assertTrue(PatternInventorySnapshots.get(inv, true).revision() > revision, "In-place NBT edits invalidate the snapshot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void exactReceiptsCannotLeaveStaleFuzzyDebt(GameTestHelper h) {
        var ledger = new FuzzyOutputLedger();
        var waiting = new ListCraftingInventory(k -> {});
        ledger.add(GOLD, 1); waiting.insert(GOLD, 1, Actionable.MODULATE);
        long[] received = {0};
        h.assertTrue(ledger.extract(waiting, GOLD, 1, Actionable.MODULATE, (k, n) -> received[0] += n).amount() == 1, "Exact receipt");
        var extra = ledger.extract(waiting, variant(), 1, Actionable.SIMULATE, (k, n) -> received[0] += n);
        h.assertTrue(extra.amount() == 0 && !extra.fuzzy() && received[0] == 1, "Already fulfilled fuzzy debt cannot accept another variant");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fuzzyReceiptsUseActualDebtAndSurviveReload(GameTestHelper h) {
        var ledger = new FuzzyOutputLedger(); ledger.add(GOLD, 8);
        var restored = new FuzzyOutputLedger(); restored.load(ledger.save());
        var waiting = new ListCraftingInventory(k -> {}); waiting.insert(GOLD, 2, Actionable.MODULATE);
        long[] received = {0};
        var simulated = restored.extract(waiting, variant(), 8, Actionable.SIMULATE, (k, n) -> received[0] += n);
        h.assertTrue(simulated.amount() == 2 && simulated.fuzzy() && received[0] == 0, "Simulation neither overclaims nor mutates");
        var actual = restored.extract(waiting, variant(), 8, Actionable.MODULATE, (k, n) -> received[0] += n);
        h.assertTrue(actual.amount() == 2 && received[0] == 2 && restored.save().isEmpty(), "Only actual debt is settled");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void patternIdentityIsSymmetricAndKeepsFuzzyMode(GameTestHelper h) {
        var raw = PatternDetailsHelper.decodePattern(patternStack(), h.getLevel());
        var exact = NimbleEncodedPattern.wrap(raw, false);
        var fuzzy = NimbleEncodedPattern.wrap(raw, true);
        var reloaded = NimbleEncodedPattern.wrap(PatternDetailsHelper.decodePattern(fuzzy.getDefinition(), h.getLevel()), false);
        h.assertTrue(!exact.equals(raw) && !raw.equals(exact), "No asymmetric raw/wrapper equality");
        h.assertTrue(!exact.equals(fuzzy) && !fuzzy.equals(exact), "Different execution policies have different identities");
        h.assertTrue(fuzzy.equals(reloaded) && reloaded.equals(fuzzy) && fuzzy.hashCode() == reloaded.hashCode()
                && reloaded.getFuzzyMode(), "Execution identity survives saved definition");
        var annotated = patternStack(); NimblePatternTag.tagUpdate(annotated, "minecraft:diamond");
        var updated = NimbleEncodedPattern.wrap(PatternDetailsHelper.decodePattern(annotated, h.getLevel()), false);
        h.assertTrue(exact.equals(updated) && exact.hashCode() == updated.hashCode(), "Management metadata does not strand pending work");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void providerMatchingUsesNativePatternAfterReload(GameTestHelper h) {
        var raw = PatternDetailsHelper.decodePattern(patternStack(), h.getLevel());
        var fuzzy = NimbleEncodedPattern.wrap(raw, true);
        ICraftingProvider provider = new ICraftingProvider() {
            public List<IPatternDetails> getAvailablePatterns() { return List.of(raw); }
            public boolean isBusy() { return false; }
            public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputs) { return pattern == raw; }
        };
        ProviderPatternIndex.register(provider, List.of(raw), List.of(fuzzy));
        var reloaded = NimbleEncodedPattern.wrap(PatternDetailsHelper.decodePattern(fuzzy.getDefinition(), h.getLevel()), false);
        h.assertTrue(ProviderPatternIndex.nativePattern(provider, reloaded) == raw, "Saved runtime metadata must not be sent as native provider identity");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void providerUnmountCannotDeleteReplacementIndex(GameTestHelper h) {
        var oldRaw = PatternDetailsHelper.decodePattern(patternStack(), h.getLevel());
        var newRaw = PatternDetailsHelper.decodePattern(patternStack(), h.getLevel());
        var compiled = NimbleEncodedPattern.wrap(oldRaw, true);
        var provider = new LimitedProvider(oldRaw);
        var oldRegistration = ProviderPatternIndex.register(provider, List.of(oldRaw), List.of(compiled));
        var newRegistration = ProviderPatternIndex.register(provider, List.of(newRaw), List.of(compiled));
        ProviderPatternIndex.unregister(provider, oldRegistration);
        h.assertTrue(ProviderPatternIndex.nativePattern(provider, compiled) == newRaw, "Old unmount cannot evict replacement");
        ProviderPatternIndex.unregister(provider, newRegistration);
        h.assertTrue(ProviderPatternIndex.nativePattern(provider, compiled) == oldRaw, "Unmount releases native index and uses wrapper fallback");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void enumCellsKeepCapacityIdsAndCumulativeConfiguration(GameTestHelper h) {
        long[] bytes = {1024, 4096, 16384, 65536, 262144};
        int index = 0;
        for (var tier : LoopStorageTier.values()) {
            var registration = ModItems.LOOP_STORAGE_CELLS.get(tier);
            var item = registration.get();
            h.assertTrue(registration.getId().getPath().equals(tier.registryName()) && item.getCapacityBytes() == bytes[index]
                    && item.getIdleDrain() == 0.5 * (index + 1), "Existing registry id, capacity and drain preserved");
            var stack = new ItemStack(item);
            item.addConfiguredAmount(stack, IRON, 2); item.addConfiguredAmount(stack, IRON, 1);
            item.addConfiguredAmount(stack, GOLD, 1);
            h.assertTrue(item.getConfiguredAmount(stack, IRON) == 3 && item.getConfiguredAmount(stack, GOLD) == 1, "Cumulative markings unchanged");
            index++;
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void probabilityIndexDoesNotGuessAmongOrdinaryDuplicates(GameTestHelper h) {
        var ordinary = patternStack(); var marked = ordinary.copy(); ProbabilityEncoding.mark(marked, RECIPE, 1);
        var inv = new AppEngInternalInventory(2);
        inv.setItemDirect(0, marked); inv.setItemDirect(1, ordinary);
        var provider = java.lang.reflect.Proxy.newProxyInstance(OptimizationGameTests.class.getClassLoader(),
                new Class<?>[] {appeng.helpers.patternprovider.PatternContainer.class}, (p, m, a) -> inv);
        var raw = PatternDetailsHelper.decodePattern(ordinary, h.getLevel());
        h.assertTrue(new ProbabilityPattern.Index(provider).recover(raw) == raw, "An ordinary duplicate makes provenance ambiguous");
        inv.setItemDirect(1, ItemStack.EMPTY);
        var index = new ProbabilityPattern.Index(provider);
        var recovered = (NimbleEncodedPattern) index.recover(raw);
        h.assertTrue(recovered.metadata().probability() && RECIPE.equals(recovered.metadata().recipe()), "Unique tagged candidate can recover");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void probabilityBatchCountsEveryOutputAndCleansPriority(GameTestHelper h) {
        var stack = patternStack(); ProbabilityEncoding.mark(stack, RECIPE, 0);
        var pattern = NimbleEncodedPattern.wrap(PatternDetailsHelper.decodePattern(stack, h.getLevel()), false);
        var cluster = new appeng.me.cluster.implementations.CraftingCPUCluster(net.minecraft.core.BlockPos.ZERO, net.minecraft.core.BlockPos.ZERO);
        var controller = new ProbabilityController();
        try {
            controller.dispatched(pattern, new Object(), cluster, 100);
            var saved = controller.save();
            var remaining = new ListCraftingInventory(k -> {});
            remaining.readFromNBT(saved.getList("attempts", 10).getCompound(0).getList("remaining", 10));
            h.assertTrue(remaining.list.get(GOLD) == 100 && ProbabilityController.anyActive(), "Batch output debt is multiplied once");
            controller.received(GOLD, 37);
            var restored = new ProbabilityController(); restored.load(controller.save());
            var restoredRemaining = new ListCraftingInventory(k -> {});
            restoredRemaining.readFromNBT(restored.save().getList("attempts", 10).getCompound(0).getList("remaining", 10));
            h.assertTrue(restoredRemaining.list.get(GOLD) == 63, "Batch receipt and reload preserve remaining amount");
            restored.clear();
        } finally { controller.clear(); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void completedMachineTicketsAreActivelyRemoved(GameTestHelper h) {
        var machine = new Object();
        class Ticket implements ProbabilityMachines.Ticket {
            long remaining = 10;
            public ResourceLocation recipe() { return RECIPE; }
            public long complete(long amount) { long n = Math.min(amount, remaining); remaining -= n; return n; }
            public boolean finished() { return remaining == 0; }
        }
        var ticket = new Ticket(); ProbabilityMachines.watch(machine, ticket);
        ProbabilityMachines.completed(machine, RECIPE, 10);
        h.assertTrue(!ProbabilityMachines.hasTickets(machine), "Completed tickets do not wait for GC");
        ProbabilityMachines.watch(machine, new Ticket());
        ProbabilityMachines.watch(machine, ticket); ProbabilityMachines.unwatch(machine, ticket);
        h.assertTrue(ProbabilityMachines.hasTickets(machine), "Unwatch must retain other jobs");
        h.succeed();
    }
}
