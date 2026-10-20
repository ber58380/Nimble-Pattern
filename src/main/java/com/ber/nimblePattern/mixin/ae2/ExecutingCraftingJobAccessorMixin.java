package com.ber.nimblePattern.mixin.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import appeng.crafting.CraftingLink;
import appeng.crafting.execution.ElapsedTimeTracker;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.inv.ListCraftingInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = ExecutingCraftingJob.class, remap = false)
public interface ExecutingCraftingJobAccessorMixin {
    @Accessor("remainingAmount")
    long nimblePattern$getRemainingAmount();

    @Accessor("tasks")
    Map<IPatternDetails, Object> nimblePattern$getTasks();

    @Accessor("waitingFor")
    ListCraftingInventory nimblePattern$getWaitingFor();

    @Accessor("timeTracker")
    ElapsedTimeTracker nimblePattern$getElapsedTimeTracker();

    @Accessor("link")
    CraftingLink nimblePattern$getLink();

    @Accessor("finalOutput")
    GenericStack nimblePattern$getFinalOutput();
}
