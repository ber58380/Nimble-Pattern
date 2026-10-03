package com.ber.nimblePattern.crafting;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftingPlan;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.menu.AutoCraftingMenu;
import com.ber.nimblePattern.pattern.NimbleAssemblerPattern;
import com.ber.nimblePattern.pattern.NimbleEncodedPattern;
import com.ber.nimblePattern.pattern.ToolPatternData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@GameTestHolder("nimble_pattern")
@PrefixGameTestTemplate(false)
public final class ToolPatternGameTests {
    private static final AEItemKey TEMPLATE = AEItemKey.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
    private static final AEItemKey DIAMOND = AEItemKey.of(Items.DIAMOND);
    private static final AEItemKey NETHERRACK = AEItemKey.of(Items.NETHERRACK);

    @GameTest(template = "empty")
    public static void unbreakableToolWaitsForRemainder(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var original = new ArrayList<>(manager.getRecipes());
        var recipe = new net.minecraft.world.item.crafting.ShapelessRecipe(
                ResourceLocation.fromNamespaceAndPath("nimble_pattern", "test_reusable_tool"), "",
                net.minecraft.world.item.crafting.CraftingBookCategory.MISC, new ItemStack(Items.OAK_PLANKS),
                net.minecraft.core.NonNullList.of(net.minecraft.world.item.crafting.Ingredient.EMPTY,
                        net.minecraft.world.item.crafting.Ingredient.of(Items.SHEARS),
                        net.minecraft.world.item.crafting.Ingredient.of(Items.STICK))) {
            @Override
            public net.minecraft.core.NonNullList<ItemStack> getRemainingItems(net.minecraft.world.inventory.CraftingContainer grid) {
                var result = net.minecraft.core.NonNullList.withSize(grid.getContainerSize(), ItemStack.EMPTY);
                for (int slot = 0; slot < grid.getContainerSize(); slot++) {
                    if (grid.getItem(slot).is(Items.SHEARS)) result.set(slot, grid.getItem(slot).copy());
                }
                return result;
            }
        };
        var recipes = new ArrayList<>(original);
        recipes.add(recipe);
        manager.replaceRecipes(recipes);
        try {
            var shears = new ItemStack(Items.SHEARS);
            shears.getOrCreateTag().putBoolean("Unbreakable", true);
            var tool = AEItemKey.of(shears);
            var inputs = new ItemStack[9];
            java.util.Arrays.fill(inputs, ItemStack.EMPTY);
            inputs[0] = shears;
            inputs[1] = new ItemStack(Items.STICK);
            var stack = PatternDetailsHelper.encodeCraftingPattern(recipe, inputs, new ItemStack(Items.OAK_PLANKS), false, false);
            ToolPatternData.write(stack, ToolPatternData.validate(PatternDetailsHelper.decodePattern(stack, helper.getLevel()), List.of(tool)));
            var pattern = (NimbleAssemblerPattern) NimbleEncodedPattern.wrap(PatternDetailsHelper.decodePattern(stack, helper.getLevel()), false);
            var cpu = new ListCraftingInventory(key -> {});
            cpu.insert(tool, 1, Actionable.MODULATE);
            cpu.insert(AEItemKey.of(Items.STICK), 10, Actionable.MODULATE);
            var controller = new ToolCraftingController();
            controller.reserve(ToolCraftingPlan.reservations(Map.of(pattern, 10L)), cpu);
            var products = new ArrayList<GenericStack>();
            var provider = new ICraftingProvider() {
                public List<IPatternDetails> getAvailablePatterns() { return List.of(pattern.getPattern()); }
                public boolean isBusy() { return false; }
                public boolean pushPattern(IPatternDetails details, KeyCounter[] holders) {
                    var raw = (AECraftingPattern) details;
                    var grid = new TransientCraftingContainer(new AutoCraftingMenu(), 3, 3);
                    raw.fillCraftingGrid(holders, grid::setItem);
                    products.add(GenericStack.fromItemStack(raw.assemble(grid, helper.getLevel())));
                    for (var remainder : raw.getRemainingItems(grid)) {
                        if (!remainder.isEmpty()) products.add(GenericStack.fromItemStack(remainder));
                    }
                    return true;
                }
            };
            long delivered = 0;
            for (int i = 0; i < 10; i++) {
                var holders = CraftingCpuHelper.extractPatternInputs(pattern, cpu, helper.getLevel(), new KeyCounter(), new KeyCounter());
                helper.assertTrue(controller.push(pattern, provider, holders, helper.getLevel()), "Reusable tool dispatch");
                var output = products.get(0);
                controller.accept(output.what(), output.amount(), Actionable.MODULATE);
                helper.assertTrue(controller.finishStep().isEmpty(), "Do not finish before the tool comes back");
                var remainder = products.get(1);
                helper.assertTrue(remainder.what().equals(tool), "Recipe returns the exact tool NBT");
                controller.accept(remainder.what(), remainder.amount(), Actionable.MODULATE);
                delivered += controller.finishStep().stream().mapToLong(GenericStack::amount).sum();
                products.clear();
            }
            controller.release(cpu);
            helper.assertTrue(delivered == 10 && cpu.list.get(tool) == 1, "Ten products and the same single unbreakable tool");
            helper.succeed();
        } finally {
            manager.replaceRecipes(original);
        }
    }

    private static ItemStack encoded(GameTestHelper helper) {
        var recipe = (CraftingRecipe) helper.getLevel().getRecipeManager().byKey(
                ResourceLocation.fromNamespaceAndPath("minecraft", "netherite_upgrade_smithing_template")).orElseThrow();
        ItemStack[] grid = {
                new ItemStack(Items.DIAMOND), TEMPLATE.toStack(), new ItemStack(Items.DIAMOND),
                new ItemStack(Items.DIAMOND), new ItemStack(Items.NETHERRACK), new ItemStack(Items.DIAMOND),
                new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND)
        };
        return PatternDetailsHelper.encodeCraftingPattern(recipe, grid, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 2), false, false);
    }

    private static NimbleAssemblerPattern marked(GameTestHelper helper) {
        var stack = encoded(helper);
        var raw = PatternDetailsHelper.decodePattern(stack, helper.getLevel());
        ToolPatternData.write(stack, ToolPatternData.validate(raw, List.of(TEMPLATE)));
        return (NimbleAssemblerPattern) NimbleEncodedPattern.wrap(
                PatternDetailsHelper.decodePattern(stack, helper.getLevel()), false);
    }

    @GameTest(template = "empty")
    public static void templatePlanReservesOneTool(GameTestHelper helper) {
        var pattern = marked(helper);
        helper.assertTrue(pattern.getPrimaryOutput().amount() == 1, "Only the net template is advertised");
        helper.assertTrue(pattern.getInputs().length == 2, "Template is not a per-craft consumable");
        var used = new KeyCounter();
        used.add(DIAMOND, 700);
        used.add(NETHERRACK, 100);
        var rawPlan = new CraftingPlan(new GenericStack(TEMPLATE, 100), 100, false, false,
                used, new KeyCounter(), new KeyCounter(), Map.of(pattern, 100L));
        var snapshot = new KeyCounter();
        snapshot.addAll(used);
        snapshot.add(TEMPLATE, 1);
        var plan = ToolCraftingPlan.reserve(rawPlan, snapshot);
        helper.assertTrue(!plan.simulation() && plan.usedItems().get(TEMPLATE) == 1, "100 crafts reserve exactly one template");
        snapshot.remove(TEMPLATE);
        var missing = ToolCraftingPlan.reserve(rawPlan, snapshot);
        helper.assertTrue(missing.simulation() && missing.missingItems().get(TEMPLATE) == 1, "Missing seed is reported once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void templateHundredCraftsAndReload(GameTestHelper helper) {
        var pattern = marked(helper);
        var cpu = new ListCraftingInventory(key -> {});
        cpu.insert(TEMPLATE, 1, Actionable.MODULATE);
        cpu.insert(DIAMOND, 700, Actionable.MODULATE);
        cpu.insert(NETHERRACK, 100, Actionable.MODULATE);
        var controller = new ToolCraftingController();
        controller.reserve(ToolCraftingPlan.reservations(Map.of(pattern, 100L)), cpu);
        var produced = new ArrayList<GenericStack>();
        ICraftingProvider assembler = new ICraftingProvider() {
            public List<IPatternDetails> getAvailablePatterns() { return List.of(pattern.getPattern()); }
            public boolean isBusy() { return false; }
            public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) {
                var raw = (AECraftingPattern) details;
                var grid = new TransientCraftingContainer(new AutoCraftingMenu(), 3, 3);
                raw.fillCraftingGrid(inputs, grid::setItem);
                produced.add(GenericStack.fromItemStack(raw.assemble(grid, helper.getLevel())));
                for (var remainder : raw.getRemainingItems(grid)) {
                    if (!remainder.isEmpty()) produced.add(GenericStack.fromItemStack(remainder));
                }
                return true;
            }
        };
        long delivered = 0;
        for (int operation = 0; operation < 100; operation++) {
            var holders = CraftingCpuHelper.extractPatternInputs(pattern, cpu, helper.getLevel(), new KeyCounter(), new KeyCounter());
            helper.assertTrue(holders != null && controller.push(pattern, assembler, holders, helper.getLevel()), "Push operation " + operation);
            if (operation == 50) {
                var saved = controller.save();
                controller = new ToolCraftingController();
                controller.load(saved);
            }
            for (var stack : produced) {
                helper.assertTrue(controller.accept(stack.what(), stack.amount(), Actionable.SIMULATE) == stack.amount(), "Output simulation");
                helper.assertTrue(controller.accept(stack.what(), stack.amount(), Actionable.MODULATE) == stack.amount(), "Physical output capture");
            }
            produced.clear();
            for (var result : controller.finishStep()) {
                helper.assertTrue(result.what().equals(TEMPLATE), "Only template net gain");
                delivered += result.amount();
            }
        }
        controller.release(cpu);
        helper.assertTrue(delivered == 100 && cpu.list.get(TEMPLATE) == 1, "100 delivered plus the original tool returned");
        helper.assertTrue(cpu.list.get(DIAMOND) == 0 && cpu.list.get(NETHERRACK) == 0, "Exact consumable cost");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rejectsConsumedMaterialsAndProcessing(GameTestHelper helper) {
        var raw = PatternDetailsHelper.decodePattern(encoded(helper), helper.getLevel());
        try {
            ToolPatternData.validate(raw, List.of(DIAMOND));
            helper.fail("Consumed diamond was accepted as a tool");
        } catch (IllegalArgumentException expected) {
            helper.assertTrue("invalid_tool".equals(expected.getMessage()), "Validation reason");
        }
        var processing = PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[] {new GenericStack(TEMPLATE, 1)}, new GenericStack[] {new GenericStack(TEMPLATE, 2)});
        try {
            ToolPatternData.validate(PatternDetailsHelper.decodePattern(processing, helper.getLevel()), List.of(TEMPLATE));
            helper.fail("Processing pattern was accepted");
        } catch (IllegalArgumentException expected) {
            helper.assertTrue("crafting_only".equals(expected.getMessage()), "Processing rejection reason");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cpuMixinAndJobReloadLoad(GameTestHelper helper) throws ClassNotFoundException {
        var cluster = new appeng.me.cluster.implementations.CraftingCPUCluster(
                net.minecraft.core.BlockPos.ZERO, net.minecraft.core.BlockPos.ZERO);
        Class.forName("appeng.crafting.execution.ExecutingCraftingJob");
        Class.forName("appeng.crafting.CraftingCalculation");
        Class.forName("appeng.crafting.inv.NetworkCraftingSimulationState");
        var tag = new net.minecraft.nbt.CompoundTag();
        cluster.craftingLogic.writeToNBT(tag);
        helper.assertTrue(tag.contains("nimble_fuzzy") && !tag.contains("nimble_tools") && !tag.contains("nimble_probability"),
                "CPU persistence is active without allocating unused feature controllers");
        cluster.craftingLogic.readFromNBT(tag);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void toolCancellationAndRejectedDispatch(GameTestHelper helper) {
        var pattern = marked(helper);
        var cpu = new ListCraftingInventory(key -> {});
        cpu.insert(TEMPLATE, 1, Actionable.MODULATE);
        cpu.insert(DIAMOND, 7, Actionable.MODULATE);
        cpu.insert(NETHERRACK, 1, Actionable.MODULATE);
        var controller = new ToolCraftingController();
        controller.reserve(ToolCraftingPlan.reservations(Map.of(pattern, 1L)), cpu);
        var holder = CraftingCpuHelper.extractPatternInputs(pattern, cpu, helper.getLevel(), new KeyCounter(), new KeyCounter());
        var reject = new ICraftingProvider() {
            public List<IPatternDetails> getAvailablePatterns() { return List.of(pattern.getPattern()); }
            public boolean isBusy() { return false; }
            public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) { return false; }
        };
        helper.assertTrue(!controller.push(pattern, reject, holder, helper.getLevel()), "Busy provider leaves tools reusable");
        var accept = new ICraftingProvider() {
            public List<IPatternDetails> getAvailablePatterns() { return List.of(pattern.getPattern()); }
            public boolean isBusy() { return false; }
            public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) {
                for (var input : inputs) input.clear();
                return true;
            }
        };
        helper.assertTrue(controller.push(pattern, accept, holder, helper.getLevel()), "Retry after rejected push");
        controller.accept(TEMPLATE, 1, Actionable.MODULATE);
        helper.assertTrue(controller.finishStep().isEmpty(), "Partial receipt must not complete a craft");
        controller.release(cpu);
        controller.release(cpu);
        helper.assertTrue(cpu.list.get(TEMPLATE) == 1, "Cancel releases each received physical item exactly once");
        helper.assertTrue(controller.accept(TEMPLATE, 1, Actionable.MODULATE) == 0, "Late output returns to network after cancel");
        controller.reserve(ToolCraftingPlan.reservations(Map.of(pattern, 1L)), cpu);
        helper.assertTrue(!controller.isActive(), "Cancellation clears stale execution state");
        controller.release(cpu);
        helper.assertTrue(cpu.list.get(TEMPLATE) == 1, "Starting another job preserves the real tool");
        helper.succeed();
    }
}
