package com.ber.nimblePattern.mixin.ae2;

import appeng.api.stacks.KeyCounter;
import appeng.crafting.inv.NetworkCraftingSimulationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = NetworkCraftingSimulationState.class, remap = false)
public interface NetworkCraftingSnapshotAccessor {
    @Accessor("list")
    KeyCounter nimblePattern$getSnapshot();
}
