package com.ber.nimblePattern.mixin.ae2;

import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingCalculation;
import appeng.crafting.inv.NetworkCraftingSimulationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = CraftingCalculation.class, remap = false)
public interface CraftingCalculationMixin {
    @Accessor("networkInv")
    NetworkCraftingSimulationState getNetworkInv();

    @Accessor("simRequester")
    ICraftingSimulationRequester getSimRequester();

    @Invoker("addMissing")
    void invokeAddMissing(AEKey what, long amount);
}
