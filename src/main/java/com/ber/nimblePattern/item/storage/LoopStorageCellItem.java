package com.ber.nimblePattern.item.storage;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared item-side implementation inherited by every loop storage cell capacity tier.
 */
public class LoopStorageCellItem extends Item implements ILoopStorageCellItem {
    static final String CONFIG_TAG = "LoopStorageConfig";
    static final String CONTENTS_TAG = "LoopStorageContents";
    static final String KEY_TAG = "Key";
    static final String AMOUNT_TAG = "Amount";

    private final long capacityBytes;
    private final double idleDrain;

    protected LoopStorageCellItem(long capacityBytes, double idleDrain) {
        super(new Properties().stacksTo(1));
        this.capacityBytes = capacityBytes;
        this.idleDrain = idleDrain;
    }

    @Override
    public final long getCapacityBytes() {
        return capacityBytes;
    }

    public final double getIdleDrain() {
        return idleDrain;
    }

    @Override
    public final Map<AEKey, Long> getConfiguredAmounts(ItemStack stack) {
        if (stack.getItem() != this || !stack.hasTag()) {
            return Map.of();
        }

        var result = new LinkedHashMap<AEKey, Long>();
        var entries = stack.getOrCreateTag().getList(CONFIG_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.getCompound(i);
            var key = AEKey.fromTagGeneric(entry.getCompound(KEY_TAG));
            var amount = entry.getLong(AMOUNT_TAG);
            if (key != null && isSupportedKey(key) && amount > 0) {
                result.merge(key, amount, LoopStorageCellItem::saturatedAdd);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public final long getConfiguredAmount(ItemStack stack, AEKey key) {
        return getConfiguredAmounts(stack).getOrDefault(key, 0L);
    }

    @Override
    public final void addConfiguredAmount(ItemStack stack, AEKey key, long amount) {
        if (stack.getItem() != this) {
            throw new IllegalArgumentException("The stack is not this loop storage cell item");
        }
        if (!isSupportedKey(key)) {
            throw new IllegalArgumentException("Loop storage cells only support item and fluid keys");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("The configured amount cannot be negative");
        }
        if (amount == 0) {
            return;
        }

        var configured = new LinkedHashMap<>(getConfiguredAmounts(stack));
        configured.merge(key, amount, LoopStorageCellItem::saturatedAdd);
        writeEntries(stack, CONFIG_TAG, configured);
    }

    @Override
    public final void clearConfiguredAmounts(ItemStack stack) {
        if (stack.getItem() == this && stack.hasTag()) {
            stack.getOrCreateTag().remove(CONFIG_TAG);
        }
    }

    static boolean isSupportedKey(AEKey key) {
        return key != null && (key.getType() == AEKeyType.items() || key.getType() == AEKeyType.fluids());
    }

    static Map<AEKey, Long> readContents(ItemStack stack) {
        if (!stack.hasTag()) {
            return Map.of();
        }

        var result = new LinkedHashMap<AEKey, Long>();
        var entries = stack.getOrCreateTag().getList(CONTENTS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.getCompound(i);
            var key = AEKey.fromTagGeneric(entry.getCompound(KEY_TAG));
            var amount = entry.getLong(AMOUNT_TAG);
            if (key != null && isSupportedKey(key) && amount > 0) {
                result.merge(key, amount, LoopStorageCellItem::saturatedAdd);
            }
        }
        return result;
    }

    static void writeContents(ItemStack stack, Map<AEKey, Long> contents) {
        writeEntries(stack, CONTENTS_TAG, contents);
    }

    private static void writeEntries(ItemStack stack, String tagName, Map<AEKey, Long> entries) {
        var list = new ListTag();
        for (var entry : entries.entrySet()) {
            if (entry.getValue() <= 0 || !isSupportedKey(entry.getKey())) {
                continue;
            }
            var tag = new CompoundTag();
            tag.put(KEY_TAG, entry.getKey().toTagGeneric());
            tag.putLong(AMOUNT_TAG, entry.getValue());
            list.add(tag);
        }

        var root = stack.getOrCreateTag();
        if (list.isEmpty()) {
            root.remove(tagName);
        } else {
            root.put(tagName, list);
        }
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }
}
