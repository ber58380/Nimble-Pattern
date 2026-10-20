package com.ber.nimblePattern.gametest;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.pattern.AEProcessingPattern;
import com.ber.nimblePattern.Config;
import com.ber.nimblePattern.compat.gtceu.GTMachineTracking;
import com.ber.nimblePattern.compat.gtceu.GTRecipeMatcher;
import com.ber.nimblePattern.compat.gtceu.GTRecipeOperations;
import com.ber.nimblePattern.crafting.ProbabilityDispatchContext;
import com.ber.nimblePattern.crafting.ProbabilityTrackingController;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import com.ber.nimblePattern.pattern.wrapper.NimbleProcessingPattern;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTMachines;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder("nimble_pattern")
@PrefixGameTestTemplate(false)
public final class ProbabilityMachineGameTests {
    private static NimbleProcessingPattern pattern() {
        var stack = PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[]{new GenericStack(AEItemKey.of(Items.GRAVEL), 1)},
                new GenericStack[]{new GenericStack(AEItemKey.of(Items.FLINT), 1)});
        NimblePatternTag.tagProbability(stack, 80);
        var key = AEItemKey.of(stack);
        return new NimbleProcessingPattern(new AEProcessingPattern(key), key);
    }

    @GameTest(template = "forge:empty", timeoutTicks = 100)
    public static void quantitiesAndFallback(GameTestHelper helper) {
        var base = GTRecipeBuilder.of(new ResourceLocation("nimble_pattern", "test_ratio"),
                        GTRecipeTypes.MACERATOR_RECIPES)
                .inputItems(Items.GRAVEL, 2).outputItems(Items.FLINT).duration(20).buildRawRecipe();
        var inputs = List.of(new GenericStack(AEItemKey.of(Items.GRAVEL), 20));
        helper.assertTrue(GTRecipeMatcher.matches(pattern(), inputs, base), "Primary output must match");
        helper.assertTrue(GTRecipeOperations.calculate(base, inputs).orElse(-1) == 10, "20/2 operations");
        helper.assertTrue(GTRecipeOperations.getExecutedOperations(base,
                base.copy(ContentModifier.multiplier(4)), inputs).orElse(-1) == 4, "Parallel ratio");
        helper.assertTrue(GTRecipeOperations.calculate(base,
                List.of(new GenericStack(AEItemKey.of(Items.GRAVEL), 7))).isEmpty(), "Reject incomplete ratio");

        var tracking = new ProbabilityTrackingController();
        var pattern = pattern();
        var id = tracking.dispatchedCandidate(pattern.getDefinition(), pattern.getPrimaryOutput(), 10, 80);
        var monitor = tracking.activateMachine(id, 10);
        monitor.start(1, 10);
        tracking.tick(10000, 10000);
        helper.assertTrue(tracking.getRetryOutputs().isEmpty(), "Valid monitor must suppress timeout");
        monitor.invalidate();
        tracking.tick(10000, 20000);
        tracking.tick(799, 20799);
        helper.assertTrue(tracking.getRetryOutputs().isEmpty(), "Fallback starts a fresh scaled timeout");
        tracking.tick(1, 20800);
        helper.assertTrue(tracking.getRetryAmount(AEItemKey.of(Items.FLINT)) == 10, "Fallback deficit");

        var saved = new ProbabilityTrackingController();
        var savedId = saved.dispatchedCandidate(pattern.getDefinition(), pattern.getPrimaryOutput(), 1, 80);
        saved.activateMachine(savedId, 1);
        var restored = new ProbabilityTrackingController();
        restored.load(saved.save(0));
        restored.tick(0, 0);
        restored.tick(80, 80);
        helper.assertTrue(restored.getRetryAmount(AEItemKey.of(Items.FLINT)) == 1, "Reload must not stay suspended");
        helper.succeed();
    }

    @GameTest(template = "forge:empty", timeoutTicks = 100)
    public static void realMachineEventsAndSharedCpu(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(pos, GTMachines.MACERATOR[1].getBlock().defaultBlockState());
        var machine = MetaMachine.getMachine(level, pos);
        var logic = ((IRecipeLogicMachine) machine).getRecipeLogic();
        var inventory = machine.getItemTransferCap(Direction.UP, false);
        helper.assertTrue(inventory != null, "GT input capability");
        var base = GTRecipeBuilder.of(new ResourceLocation("nimble_pattern", "test_events"),
                        GTRecipeTypes.MACERATOR_RECIPES)
                .inputItems(Items.GRAVEL).outputItems(Items.FLINT).duration(20).buildRawRecipe();
        var recipes = new ArrayList<>(level.getRecipeManager().getRecipes());
        var withTest = new ArrayList<>(recipes);
        withTest.add(base);
        level.getRecipeManager().replaceRecipes(withTest);
        var first = new ProbabilityTrackingController();
        var second = new ProbabilityTrackingController();
        try {
            var pattern = pattern();
            for (var tracker : List.of(first, second)) {
                try (var capture = ProbabilityDispatchContext.open()) {
                    var inputs = new KeyCounter();
                    inputs.add(AEItemKey.of(Items.GRAVEL), 3);
                    ProbabilityDispatchContext.recordDelivery(level, pos, Direction.UP, inputs);
                    helper.assertTrue(GTMachineTracking.register(level, tracker, pattern, 3,
                            capture, GTMachineTracking.sequence()), "Register direct machine");
                }
            }
            // Exercise the actual injected setupRecipe, not a direct call to the manager.
            inventory.setStackInSlot(0, new ItemStack(Items.GRAVEL, 4));
            logic.setupRecipe(base.copy(ContentModifier.multiplier(4)));
            helper.assertTrue(first.getMachineMonitors().values().iterator().next().getStartedOperations() == 3,
                    "First CPU receives three operations");
            helper.assertTrue(second.getMachineMonitors().values().iterator().next().getStartedOperations() == 1,
                    "Second CPU receives only the remaining operation");
            logic.onRecipeFinish();
            long now = level.getGameTime();
            first.tick(10000, now + Config.probabilityCollectionGraceTicks());
            second.tick(10000, now + Config.probabilityCollectionGraceTicks());
            helper.assertTrue(first.getRetryAmount(AEItemKey.of(Items.FLINT)) == 3, "Completed batch retries after grace");
            helper.assertTrue(second.getRetryOutputs().isEmpty(), "Incomplete batch cannot retry on timeout");

            // The remainder finishes in another parallel execution.
            inventory.setStackInSlot(0, new ItemStack(Items.GRAVEL, 2));
            logic.setupRecipe(base.copy(ContentModifier.multiplier(2)));
            logic.onRecipeFinish();
            second.received(AEItemKey.of(Items.FLINT), 2);
            second.tick(0, now + Config.probabilityCollectionGraceTicks());
            helper.assertTrue(second.getRetryAmount(AEItemKey.of(Items.FLINT)) == 1, "Only actual missing output retries");
            GTMachineTracking.tick();

            // A failed setup must not count an execution.
            var failed = new ProbabilityTrackingController();
            try (var capture = ProbabilityDispatchContext.open()) {
                var inputs = new KeyCounter();
                inputs.add(AEItemKey.of(Items.GRAVEL), 1);
                ProbabilityDispatchContext.recordDelivery(level, pos, Direction.UP, inputs);
                GTMachineTracking.register(level, failed, pattern, 1, capture, GTMachineTracking.sequence());
            }
            inventory.setStackInSlot(0, ItemStack.EMPTY);
            logic.setupRecipe(base.copy());
            helper.assertTrue(failed.getMachineMonitors().isEmpty(), "Rejected input must not start monitoring");
            GTMachineTracking.release(failed);
        } finally {
            GTMachineTracking.release(first);
            GTMachineTracking.release(second);
            level.getRecipeManager().replaceRecipes(recipes);
        }
        helper.succeed();
    }
}
