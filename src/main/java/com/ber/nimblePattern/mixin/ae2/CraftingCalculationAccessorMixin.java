package com.ber.nimblePattern.mixin.ae2;

import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingCalculation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = CraftingCalculation.class, remap = false)
public interface CraftingCalculationAccessorMixin {
    @Accessor("networkInv")
    appeng.crafting.inv.NetworkCraftingSimulationState nimblePattern$getNetworkInventory();
    @Accessor("simRequester")
    ICraftingSimulationRequester nimblePattern$getSimulationRequester();

    @Invoker("addMissing")
    void nimblePattern$addMissing(AEKey what, long amount);
}
