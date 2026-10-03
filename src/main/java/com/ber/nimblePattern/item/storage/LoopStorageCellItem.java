package com.ber.nimblePattern.item.storage;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.core.AEConfig;
import appeng.core.localization.Tooltips;
import appeng.items.storage.StorageCellTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared item-side implementation inherited by every loop storage cell capacity tier.
 */
public class LoopStorageCellItem extends Item implements ILoopStorageCellItem {
    static final String CONFIG_TAG = "LoopStorageConfig";
    static final String CONTENTS_TAG = "LoopStorageContents";
    static final String KEY_TAG = "Key";
    static final String AMOUNT_TAG = "Amount";
    static final String CELL_ID_TAG = "LoopStorageCellId";

    private final long capacityBytes;
    private final double idleDrain;

    public LoopStorageCellItem(LoopStorageTier tier) {
        super(new Properties().stacksTo(1));
        this.capacityBytes = tier.bytes();
        this.idleDrain = tier.idleDrain();
    }

    @Override
    public final long getCapacityBytes() {
        return capacityBytes;
    }

    public final double getIdleDrain() {
        return idleDrain;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines,
            TooltipFlag tooltipFlag) {
        var contents = readContents(stack);
        lines.add(Tooltips.bytesUsed(bytesUsed(contents), capacityBytes));
        lines.add(Tooltips.typesUsed(contents.size(), capacityBytes));
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        var content = new ArrayList<GenericStack>();
        boolean hasMoreContent = false;
        if (AEConfig.instance().isTooltipShowCellContent()) {
            readContents(stack).forEach((key, amount) ->
                    content.add(new GenericStack(key, amount)));
            content.sort(Comparator.comparingLong(GenericStack::amount).reversed());

            int maximum = AEConfig.instance().getTooltipMaxCellContentShown();
            hasMoreContent = content.size() > maximum;
            if (hasMoreContent) {
                content.subList(maximum, content.size()).clear();
            }
        }
        return Optional.of(new StorageCellTooltipComponent(List.of(), content, hasMoreContent, true));
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
    public final boolean canAddConfiguredAmount(ItemStack stack, AEKey key, long amount) {
        if (stack.getItem() != this || !isSupportedKey(key) || amount <= 0) {
            return false;
        }
        var configured = new LinkedHashMap<>(getConfiguredAmounts(stack));
        configured.merge(key, amount, LoopStorageCellItem::saturatedAdd);
        // Configuration is cumulative, but an already-filled cell may temporarily contain more than an edited
        // target. Capacity validation must account for whichever value actually occupies more bytes.
        readContents(stack).forEach((storedKey, storedAmount) ->
                configured.merge(storedKey, storedAmount, Math::max));
        return bytesUsed(configured) <= capacityBytes;
    }

    @Override
    public final UUID getOrCreateCellId(ItemStack stack) {
        if (stack.getItem() != this) {
            throw new IllegalArgumentException("The stack is not this loop storage cell item");
        }
        var root = stack.getOrCreateTag();
        if (!root.hasUUID(CELL_ID_TAG)) {
            root.putUUID(CELL_ID_TAG, UUID.randomUUID());
        }
        return root.getUUID(CELL_ID_TAG);
    }

    public final UUID getCellId(ItemStack stack) {
        if (stack.getItem() != this || !stack.hasTag() || !stack.getOrCreateTag().hasUUID(CELL_ID_TAG)) {
            return null;
        }
        return stack.getOrCreateTag().getUUID(CELL_ID_TAG);
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

    static long bytesUsed(Map<AEKey, Long> amounts) {
        long used = 0;
        for (var entry : amounts.entrySet()) {
            long perByte = entry.getKey().getAmountPerByte();
            long bytes = 1 + (entry.getValue() - 1) / perByte;
            used = saturatedAdd(used, bytes);
        }
        return used;
    }
}
