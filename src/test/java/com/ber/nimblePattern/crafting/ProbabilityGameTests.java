package com.ber.nimblePattern.crafting;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.*;
import appeng.crafting.inv.ListCraftingInventory;
import com.ber.nimblePattern.probability.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("nimble_pattern")
@PrefixGameTestTemplate(false)
public final class ProbabilityGameTests {
    private static final AEItemKey INPUT = AEItemKey.of(Items.IRON_INGOT);
    private static final AEItemKey OUTPUT = AEItemKey.of(Items.GOLD_INGOT);
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("nimble_pattern", "chance_test");
    private static GenericStack input(long amount) { return new GenericStack(INPUT, amount); }
    private static GenericStack output(long amount) { return new GenericStack(OUTPUT, amount); }

    @GameTest(template = "empty")
    public static void editedProbabilityOutputsKeepTheirIdentity(GameTestHelper h) {
        var guaranteed = new GenericStack(AEItemKey.of(Items.DIAMOND), 1);
        var recipe = List.of(new ProbabilityRecipes.Output(output(1), ExpectedAmount.of(1, 10)),
                new ProbabilityRecipes.Output(guaranteed, ExpectedAmount.of(1, 1)));
        h.assertTrue(ProbabilityEncoding.hasProbabilityOutput(List.of(output(3)), recipe), "Custom output amount must retain probability identity");
        h.assertTrue(ProbabilityEncoding.hasProbabilityOutput(List.of(output(1), guaranteed), recipe), "Guaranteed byproducts are allowed");
        h.assertTrue(!ProbabilityEncoding.hasProbabilityOutput(List.of(guaranteed), recipe), "Guaranteed output alone is not probabilistic");
        h.assertTrue(!ProbabilityEncoding.hasProbabilityOutput(List.of(input(1)), recipe), "Unrelated replacement output must not inherit metadata");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void deletingExternalCatalystPreservesExecutionCount(GameTestHelper h) {
        var catalyst = new GenericStack(AEItemKey.of(Items.DIAMOND), 10);
        var normalized = new ProbabilityEncoding.Normalized(List.of(input(10), catalyst), List.of(output(1)), 10);
        h.assertTrue(ProbabilityEncoding.editedCycles(normalized, List.of(input(10))) == 10, "Omitted catalyst does not invalidate probability or execution count");
        h.assertTrue(ProbabilityEncoding.editedCycles(normalized, List.of(input(20))) == 20, "Execution count follows retained input quantity");
        h.assertTrue(ProbabilityEncoding.editedCycles(normalized, List.of(input(10),
                new GenericStack(catalyst.what(), 20))) == 0, "Inconsistent input scaling uses timeout, not an incorrect machine count");
        var encoded = PatternDetailsHelper.encodeProcessingPattern(new GenericStack[] {input(10)}, new GenericStack[] {output(3)});
        ProbabilityEncoding.mark(encoded, ID, ProbabilityEncoding.editedCycles(normalized, List.of(input(10))));
        var decoded = PatternDetailsHelper.decodePattern(encoded, h.getLevel());
        h.assertTrue(ProbabilityEncoding.marked(encoded) && decoded.getInputs().length == 1
                && decoded.getPrimaryOutput().amount() == 3 && ProbabilityEncoding.cycles(encoded) == 10,
                "Encoding keeps both omitted catalyst and manually adjusted output");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void expectedOutputAndFractionalScaling(GameTestHelper h) {
        var half = ProbabilityEncoding.normalize(List.of(input(1)), List.of(output(2)),
                List.of(new ProbabilityRecipes.Output(output(2), ExpectedAmount.of(1, 2))));
        h.assertTrue(half != null && half.cycles() == 1 && half.outputs().get(0).amount() == 1, "50% of two = one");
        var tenth = ProbabilityEncoding.normalize(List.of(input(1)), List.of(output(1)),
                List.of(new ProbabilityRecipes.Output(output(1), ExpectedAmount.of(1, 10))));
        h.assertTrue(tenth != null && tenth.cycles() == 10 && tenth.inputs().get(0).amount() == 10
                && tenth.outputs().get(0).amount() == 1, "Fractional output scales the entire recipe");
        var eighty = ProbabilityEncoding.normalize(List.of(input(1)), List.of(output(1)),
                List.of(new ProbabilityRecipes.Output(output(1), ExpectedAmount.decimal(0.8f))));
        h.assertTrue(eighty != null && eighty.cycles() == 5 && eighty.outputs().get(0).amount() == 4, "Float 0.8 retains decimal meaning");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void rejectUnknownOrEditedRecipe(GameTestHelper h) {
        h.assertTrue(ProbabilityEncoding.normalize(List.of(input(1)), List.of(output(3)),
                List.of(new ProbabilityRecipes.Output(output(2), ExpectedAmount.of(1, 2)))) == null, "Mismatched output must not be tagged");
        h.assertTrue(ProbabilityEncoding.normalize(List.of(input(1)), List.of(output(1)), List.of()) == null, "Unknown is not 100%");
        h.assertTrue(ProbabilityEncoding.normalize(List.of(input(1)), List.of(output(1)),
                List.of(new ProbabilityRecipes.Output(output(1), ExpectedAmount.of(0, 1)))) == null, "Zero probability cannot be requested");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void multipleOutputsUseCommonDenominator(GameTestHelper h) {
        var extra = new GenericStack(AEItemKey.of(Items.DIAMOND), 1);
        var normalized = ProbabilityEncoding.normalize(List.of(input(2)), List.of(output(1), extra), List.of(
                new ProbabilityRecipes.Output(output(1), ExpectedAmount.of(1, 2)),
                new ProbabilityRecipes.Output(extra, ExpectedAmount.of(1, 3))));
        h.assertTrue(normalized != null && normalized.cycles() == 6 && normalized.inputs().get(0).amount() == 12
                && normalized.outputs().get(0).amount() == 3 && normalized.outputs().get(1).amount() == 2, "LCM 2,3 = 6");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void completionTicketsDoNotDoubleCount(GameTestHelper h) {
        Object machine = new Object();
        class Ticket implements ProbabilityMachines.Ticket {
            long left = 10;
            public ResourceLocation recipe() { return ID; }
            public long complete(long amount) { long used = Math.min(left, amount); left -= used; return used; }
        }
        var first = new Ticket(); var second = new Ticket();
        ProbabilityMachines.watch(machine, first); ProbabilityMachines.watch(machine, second);
        ProbabilityMachines.completed(machine, ID, 12);
        h.assertTrue(first.left == 0 && second.left == 8, "12 completions cannot satisfy 20 operations");
        ProbabilityMachines.completed(machine, ResourceLocation.fromNamespaceAndPath("test", "other"), 8);
        h.assertTrue(second.left == 8, "Other recipes must not count");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void tagRemovalKeepsExpectedQuantities(GameTestHelper h) {
        var stack = PatternDetailsHelper.encodeProcessingPattern(new GenericStack[] { input(10) }, new GenericStack[] { output(1) });
        ProbabilityEncoding.mark(stack, ID, 10);
        h.assertTrue(ProbabilityEncoding.marked(stack), "Tag round trip");
        var ordinary = PatternDetailsHelper.encodeProcessingPattern(new GenericStack[] { input(10) }, new GenericStack[] { output(1) });
        h.assertTrue(ProbabilityEncoding.reencodingSamePattern(stack, ordinary), "Same recipe toggles its tag");
        var different = PatternDetailsHelper.encodeProcessingPattern(new GenericStack[] { input(20) }, new GenericStack[] { output(1) });
        h.assertTrue(!ProbabilityEncoding.reencodingSamePattern(stack, different), "Importing a different recipe is not a toggle of the old recipe");
        var decoded = PatternDetailsHelper.decodePattern(ordinary, h.getLevel());
        h.assertTrue(!ProbabilityEncoding.marked(ordinary) && decoded.getPrimaryOutput().amount() == 1
                && decoded.getInputs()[0].getMultiplier() == 10, "Re-encode only removes the probability metadata");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void retryLedgerPersistsAndCancelClears(GameTestHelper h) {
        var needs = new ListCraftingInventory(k -> {}); needs.insert(INPUT, 4, Actionable.MODULATE);
        var credit = new ListCraftingInventory(k -> {}); credit.insert(OUTPUT, 3, Actionable.MODULATE);
        var nbt = new CompoundTag(); nbt.put("supplies", needs.writeToNBT()); nbt.put("credits", credit.writeToNBT()); nbt.putBoolean("enabled", true);
        var ledger = new ProbabilityController(); ledger.load(nbt);
        var cpu = new ListCraftingInventory(k -> {});
        h.assertTrue(ledger.collect(INPUT, 8, Actionable.SIMULATE, cpu) == 4 && cpu.list.isEmpty(), "Simulation must not consume");
        h.assertTrue(ledger.collect(INPUT, 8, Actionable.MODULATE, cpu) == 4 && cpu.list.get(INPUT) == 4, "Only missing quantity is reserved");
        var restored = new ProbabilityController(); restored.load(ledger.save());
        h.assertTrue(restored.consumeCredit(OUTPUT, 2) == 2 && restored.consumeCredit(OUTPUT, 8) == 1, "Replacement outputs counted once");
        restored.clear(); h.assertTrue(!restored.active() && restored.waiting(INPUT) == 0, "Cancel clears requests");
        h.assertTrue(cpu.list.get(INPUT) == 4, "Cancel never deletes physical CPU inventory");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void probabilityMixinsLoad(GameTestHelper h) throws Exception {
        Class.forName("appeng.menu.me.items.PatternEncodingTermMenu");
        Class.forName("appeng.helpers.patternprovider.PatternProviderLogic");
        Class.forName("appeng.crafting.execution.ExecutingCraftingJob");
        Class.forName("appeng.me.service.CraftingService");
        var cpu = new appeng.crafting.execution.CraftingCpuLogic(new appeng.me.cluster.implementations.CraftingCPUCluster(
                net.minecraft.core.BlockPos.ZERO, net.minecraft.core.BlockPos.ZERO));
        h.assertTrue(cpu instanceof ProbabilityCpu, "CPU mixin installed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void failedRollWaitsForMaterialsThenCompletesOnSameCpu(GameTestHelper h) throws Exception {
        retryScenario(h, 1);
    }

    @GameTest(template = "empty")
    public static void failedBatchesCoalesceIntoOneRetry(GameTestHelper h) throws Exception {
        retryScenario(h, 10);
    }

    private static void retryScenario(GameTestHelper h, int count) throws Exception {
        var encoded = PatternDetailsHelper.encodeProcessingPattern(new GenericStack[] { input(1) }, new GenericStack[] { output(1) });
        ProbabilityEncoding.mark(encoded, ID, 1);
        var pattern = PatternDetailsHelper.decodePattern(encoded, h.getLevel());
        var stored = new KeyCounter(); stored.add(INPUT, count);
        appeng.api.storage.MEStorage storage = new appeng.api.storage.MEStorage() {
            public net.minecraft.network.chat.Component getDescription() { return net.minecraft.network.chat.Component.literal("Test storage"); }
            public long insert(AEKey key, long amount, Actionable mode, appeng.api.networking.security.IActionSource source) {
                if (mode == Actionable.MODULATE) stored.add(key, amount); return amount;
            }
            public long extract(AEKey key, long amount, Actionable mode, appeng.api.networking.security.IActionSource source) {
                long got = Math.min(stored.get(key), amount); if (mode == Actionable.MODULATE) stored.remove(key, got); return got;
            }
        };
        var storageService = proxy(appeng.api.networking.storage.IStorageService.class, (p, m, a) ->
                m.getName().equals("getInventory") ? storage : null);
        var energy = proxy(appeng.api.networking.energy.IEnergyService.class, (p, m, a) ->
                m.getName().equals("extractAEPower") ? ((Number) a[0]).doubleValue() : null);
        var serviceBox = new appeng.me.service.CraftingService[1];
        var grid = proxy(appeng.api.networking.IGrid.class, (p, m, a) -> switch (m.getName()) {
            case "getStorageService" -> storageService;
            case "getCraftingService" -> serviceBox[0];
            default -> null;
        });
        int[] pushes = {0};
        var provider = new appeng.api.networking.crafting.ICraftingProvider() {
            public List<appeng.api.crafting.IPatternDetails> getAvailablePatterns() { return List.of(pattern); }
            public boolean isBusy() { return false; }
            public boolean pushPattern(appeng.api.crafting.IPatternDetails p, KeyCounter[] holders) { pushes[0]++; return true; }
        };
        serviceBox[0] = new appeng.me.service.CraftingService(grid, storageService, energy) {
            @Override public Iterable<appeng.api.networking.crafting.ICraftingProvider> getProviders(appeng.api.crafting.IPatternDetails p) {
                return List.of(provider);
            }
            @Override public java.util.concurrent.Future<appeng.api.networking.crafting.ICraftingPlan> beginCraftingCalculation(
                    net.minecraft.world.level.Level level, appeng.api.networking.crafting.ICraftingSimulationRequester requester,
                    AEKey key, long amount, appeng.api.networking.crafting.CalculationStrategy strategy) {
                h.assertTrue(key.equals(OUTPUT) && amount == count, "Retry all failed batches in one calculation");
                var missing = new KeyCounter(); missing.add(INPUT, count);
                return java.util.concurrent.CompletableFuture.completedFuture(new appeng.crafting.CraftingPlan(
                        output(count), 1, true, false, new KeyCounter(), new KeyCounter(), missing, Map.of(pattern, (long) count)));
            }
        };
        var node = proxy(appeng.api.networking.IGridNode.class, (p, m, a) -> switch (m.getName()) {
            case "getGrid" -> grid;
            case "isActive" -> true;
            default -> null;
        });
        var core = new appeng.blockentity.crafting.CraftingBlockEntity(appeng.core.definitions.AEBlockEntities.CRAFTING_STORAGE,
                net.minecraft.core.BlockPos.ZERO, appeng.core.definitions.AEBlocks.CRAFTING_STORAGE_1K.block().defaultBlockState()) {
            @Override public appeng.api.networking.IGridNode getActionableNode() { return node; }
            @Override public void saveChanges() {}
        };
        core.setLevel(h.getLevel());
        var cluster = new appeng.me.cluster.implementations.CraftingCPUCluster(net.minecraft.core.BlockPos.ZERO, net.minecraft.core.BlockPos.ZERO);
        var sourceField = cluster.getClass().getDeclaredField("machineSrc"); sourceField.setAccessible(true);
        sourceField.set(cluster, new appeng.me.helpers.MachineSource(core));
        var bytesField = cluster.getClass().getDeclaredField("storage"); bytesField.setAccessible(true); bytesField.setLong(cluster, 1024);
        var initial = new KeyCounter(); initial.add(INPUT, count);
        var cpu = cluster.craftingLogic;
        var result = cpu.trySubmitJob(grid, new appeng.crafting.CraftingPlan(output(count), 1, false, false,
                initial, new KeyCounter(), new KeyCounter(), Map.of(pattern, (long) count)), cluster.getSrc(), null);
        h.assertTrue(result.successful(), "Initial job submitted");
        for (int i = 0; i < count; i++) h.assertTrue(cpu.executeCrafting(1, serviceBox[0], energy, h.getLevel()) == 1, "Initial dispatch");
        var jobField = appeng.crafting.execution.CraftingCpuLogic.class.getDeclaredField("job"); jobField.setAccessible(true);
        var job = (appeng.crafting.execution.ExecutingCraftingJob) jobField.get(cpu);
        var controller = ((ProbabilityCpu) cpu).nimble$probability();
        int old = com.ber.nimblePattern.Config.PROBABILITY_TIMEOUT_SECONDS.get();
        try {
            com.ber.nimblePattern.Config.PROBABILITY_TIMEOUT_SECONDS.set(1);
            for (int i = 0; i < 30; i++) controller.tick(cluster, job, cpu.getInventory());
            h.assertTrue(cpu.getPendingOutputs(OUTPUT) == count && cpu.getWaitingFor(OUTPUT) == count, "Retry remains on original CPU");
            h.assertTrue(cpu.executeCrafting(1, serviceBox[0], energy, h.getLevel()) == 0, "Missing ingredients pause retry");
            h.assertTrue(cpu.insert(INPUT, count, Actionable.MODULATE) == count, "Player-supplied input is collected");
            for (int i = 0; i < count; i++) h.assertTrue(cpu.executeCrafting(1, serviceBox[0], energy, h.getLevel()) == 1, "Retry resumes after supply");
            h.assertTrue(cpu.getWaitingFor(OUTPUT) == count, "Retry does not double the outstanding output");
            h.assertTrue(cpu.insert(OUTPUT, count, Actionable.MODULATE) == count, "Physical output is owned once");
            h.assertTrue(!cpu.hasJob() && stored.get(OUTPUT) == count && pushes[0] == count * 2, "Failed batches and retries complete normally");
        } finally { com.ber.nimblePattern.Config.PROBABILITY_TIMEOUT_SECONDS.set(old); }
        h.succeed();
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }

    @GameTest(template = "empty")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void gregtechAdapterReadsActualContentApi(GameTestHelper h) throws Exception {
        for (var recipe : h.getLevel().getRecipeManager().getRecipes()) {
            if (!recipe.getClass().getName().equals("com.gregtechceu.gtceu.api.recipe.GTRecipe")) continue;
            var original = (Map<?, ?>) recipe.getClass().getField("outputs").get(recipe);
            var itemCapability = original.keySet().stream().filter(k -> k.getClass().getSimpleName().equals("ItemRecipeCapability")).findFirst();
            if (itemCapability.isEmpty()) continue;
            var copy = ProbabilityRecipes.call(recipe, "copy");
            var outputs = (Map) copy.getClass().getField("outputs").get(copy);
            outputs.clear();
            var content = Class.forName("com.gregtechceu.gtceu.api.recipe.content.Content").getConstructor(
                    Object.class, int.class, int.class, int.class, String.class, String.class).newInstance(
                    net.minecraft.world.item.crafting.Ingredient.of(Items.GOLD_INGOT), 1000, 10000, 5000, null, null);
            outputs.put(itemCapability.get(), List.of(content));
            ((Map<?, ?>) copy.getClass().getField("tickOutputs").get(copy)).clear();
            var logic = Class.forName("com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic").getField("OR").get(null);
            ((Map) copy.getClass().getField("outputChanceLogics").get(copy)).put(itemCapability.get(), logic);
            var parsed = ProbabilityRecipes.outputs((net.minecraft.world.item.crafting.Recipe<?>) copy);
            h.assertTrue(parsed.size() == 1 && parsed.get(0).stack().what().equals(OUTPUT)
                    && parsed.get(0).chance().equals(ExpectedAmount.of(1, 10)), "GT base chance must remain 10% despite a 50% tier boost");
            h.succeed(); return;
        }
        h.fail("No GT item recipe available for API integration test");
    }

    @GameTest(template = "empty")
    public static void reconstructedProviderPatternSurvivesJobReload(GameTestHelper h) {
        var rawStack = PatternDetailsHelper.encodeProcessingPattern(new GenericStack[] {input(10)}, new GenericStack[] {output(1)});
        var physical = rawStack.copy(); ProbabilityEncoding.mark(physical, ID, 10);
        var raw = PatternDetailsHelper.decodePattern(rawStack, h.getLevel());
        var inv = new appeng.api.inventories.InternalInventory() {
            public int size() { return 1; }
            public net.minecraft.world.item.ItemStack getStackInSlot(int slot) { return physical; }
            public void setItemDirect(int slot, net.minecraft.world.item.ItemStack stack) { throw new UnsupportedOperationException(); }
        };
        var provider = proxy(appeng.helpers.patternprovider.PatternContainer.class, (p, m, a) -> inv);
        var recovered = ProbabilityPattern.recover(provider, raw);
        h.assertTrue(ProbabilityEncoding.marked(recovered.getDefinition().toStack()), "Recover physical metadata");
        var reloaded = com.ber.nimblePattern.pattern.NimbleEncodedPattern.wrap(
                PatternDetailsHelper.decodePattern(recovered.getDefinition().toStack(), h.getLevel()), false);
        h.assertTrue(reloaded.equals(recovered) && recovered.equals(reloaded) && reloaded.hashCode() == recovered.hashCode(),
                "Provider lookup must still work after the CPU is loaded from NBT");
        h.succeed();
    }
}
