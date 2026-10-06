package com.ber.nimblePattern.item.storage;

import appeng.api.stacks.AEKey;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;

public interface ILoopStorageCellItem {
    long getCapacityBytes();

    double getIdleDrain();

    Map<AEKey, Long> getConfiguredAmounts(ItemStack stack);

    long getConfiguredAmount(ItemStack stack, AEKey key);

    void addConfiguredAmount(ItemStack stack, AEKey key, long amount);

    boolean canAddConfiguredAmount(ItemStack stack, AEKey key, long amount);

    UUID getOrCreateCellId(ItemStack stack);

    void clearConfiguredAmounts(ItemStack stack);
}
