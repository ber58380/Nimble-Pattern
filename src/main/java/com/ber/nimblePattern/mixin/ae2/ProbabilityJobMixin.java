package com.ber.nimblePattern.mixin.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.inv.ListCraftingInventory;
import com.ber.nimblePattern.probability.ProbabilityJob;
import org.spongepowered.asm.mixin.*;
import java.util.Map;

@Mixin(value = ExecutingCraftingJob.class, remap = false)
public class ProbabilityJobMixin implements ProbabilityJob {
    @Shadow @Final private Map<IPatternDetails, Object> tasks;
    @Shadow @Final private ListCraftingInventory waitingFor;
    @Shadow @Final private appeng.crafting.execution.ElapsedTimeTracker timeTracker;
    @Override public ListCraftingInventory nimble$waiting() { return waitingFor; }
    @Override public void nimble$appendPlan(ICraftingPlan plan, appeng.api.stacks.GenericStack replaced) {
        // AE's nested TaskProgress is package-private. These AE member names are not Minecraft-obfuscated.
        try {
            var type = Class.forName("appeng.crafting.execution.ExecutingCraftingJob$TaskProgress");
            var constructor = type.getDeclaredConstructor();
            var value = type.getDeclaredField("value");
            constructor.setAccessible(true); value.setAccessible(true);
            var addWork = appeng.crafting.execution.ElapsedTimeTracker.class.getDeclaredMethod("addMaxItems",
                    long.class, appeng.api.stacks.AEKeyType.class);
            addWork.setAccessible(true);
            var updates = new java.util.HashMap<IPatternDetails, Object>();
            var work = new appeng.api.stacks.KeyCounter();
            for (var entry : plan.patternTimes().entrySet()) {
                var progress = constructor.newInstance();
                var old = tasks.get(entry.getKey());
                value.setLong(progress, Math.addExact(old == null ? 0 : value.getLong(old), entry.getValue()));
                updates.put(entry.getKey(), progress);
                for (var output : entry.getKey().getOutputs()) {
                    long amount = Math.multiplyExact(output.amount(), entry.getValue());
                    work.set(output.what(), Math.addExact(work.get(output.what()), amount));
                }
            }
            work.remove(replaced.what(), Math.min(work.get(replaced.what()), replaced.amount()));
            for (var entry : work) addWork.invoke(timeTracker,
                    Math.multiplyExact(entry.getLongValue(), entry.getKey().getAmountPerUnit()), entry.getKey().getType());
            tasks.putAll(updates);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unsupported AE crafting task layout", e);
        }
    }
}
