package com.ber.nimblePattern.mixin.ae2;

import appeng.crafting.execution.ExecutingCraftingJob;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ExecutingCraftingJob.class, remap = false)
public interface ExecutingCraftingJobAccessorMixin {
    @Accessor("remainingAmount")
    long nimblePattern$getRemainingAmount();

    @Accessor("playerId")
    @Nullable Integer nimblePattern$getPlayerId();
}
