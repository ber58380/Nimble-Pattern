package com.ber.nimblePattern.item.storage;

import appeng.api.stacks.AEKey;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;

/**
 * Configuration API used by the pattern tag terminal to mark what a loop storage cell may store.
 */
public interface ILoopStorageCellItem {
    long getCapacityBytes();

    Map<AEKey, Long> getConfiguredAmounts(ItemStack stack);

    long getConfiguredAmount(ItemStack stack, AEKey key);

    /**
     * Adds another marked amount to the amount already configured for this key.
     */
    void addConfiguredAmount(ItemStack stack, AEKey key, long amount);

    boolean canAddConfiguredAmount(ItemStack stack, AEKey key, long amount);

    UUID getOrCreateCellId(ItemStack stack);

    void clearConfiguredAmounts(ItemStack stack);
}
