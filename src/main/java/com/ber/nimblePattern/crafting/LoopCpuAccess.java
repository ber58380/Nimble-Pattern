package com.ber.nimblePattern.crafting;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.me.service.CraftingService;
import com.ber.nimblePattern.pattern.wrapper.NimbleProcessingPattern;
import net.minecraft.nbt.CompoundTag;

/**
 * Bridge used by the optional GTL graph CPU integration. The loop controller normally lives beside
 * AE's {@code CraftingCpuLogic}; GTL executes graph jobs outside of AE's classic task loop and therefore
 * needs a narrow way to start, tick and feed that controller without depending on Nimble classes.
 */
public interface LoopCpuAccess {
    boolean nimblePattern$startLoop(NimbleProcessingPattern composite, KeyCounter[] externalInputs, long operations);

    boolean nimblePattern$hasActiveLoop();

    void nimblePattern$tickLoop(CraftingService service, IEnergyService energy);

    LoopCraftingController.Acceptance nimblePattern$acceptLoopOutput(AEKey key, long amount, Actionable mode);

    void nimblePattern$deliverLoopRemainder(AEKey key, long amount);

    void nimblePattern$loadGraphLoop(CompoundTag tag);

    void nimblePattern$finishGraphLoop();
}
