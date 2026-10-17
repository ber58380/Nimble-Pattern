package com.ber.nimblePattern.mixin.ae2;

import appeng.api.stacks.AEKeyType;
import appeng.crafting.execution.ElapsedTimeTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ElapsedTimeTracker.class, remap = false)
public interface ElapsedTimeTrackerAccessorMixin {
    @Invoker("addMaxItems")
    void nimblePattern$addMaxItems(long amount, AEKeyType type);
}
