package com.ber.nimblePattern.item.storage;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.core.AEConfig;
import appeng.core.localization.Tooltips;
import appeng.items.storage.StorageCellTooltipComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class LoopStorageCellItem extends Item implements ILoopStorageCellItem {
    private static final String ID_TAG = "LoopStorageCellId";
    private static final String CONFIG_TAG = "LoopStorageCellConfig";
    private static final String CONTENT_TAG = "LoopStorageCellContent";
    private static final String AEKEY_TAG = "AEKey";
    private static final String AMOUNT_TAG = "Amount";

    private final long capacityBytes;
    private final double idleDrain;

    public LoopStorageCellItem(LoopStorageTier tier) {
        super(new Item.Properties().stacksTo(1));
        this.capacityBytes = tier.bytes();
        this.idleDrain = tier.idleDrain();
    }

    // region Interface methods implementation
    @Override
    public long getCapacityBytes() {
        return capacityBytes;
    }

    @Override
    public double getIdleDrain() {
        return idleDrain;
    }

    @Override
    public Map<AEKey, Long> getConfiguredAmounts(ItemStack stack) {
        if (stack.getItem() != this || !stack.hasTag()) {
            return Map.of();
        }
        return readTag(stack, CONFIG_TAG);
    }

    @Override
    public long getConfiguredAmount(ItemStack stack, AEKey key) {
        return getConfiguredAmounts(stack).getOrDefault(key, 0L);
    }

    @Override
    public void addConfiguredAmount(ItemStack stack, AEKey key, long amount) {
        if (stack.getItem() != this || !isValidKey(key) || amount <= 0) {
            return;
        }
        var configured = new LinkedHashMap<>(getConfiguredAmounts(stack));
        configured.merge(key, amount, LoopStorageCellItem::add);
        writeTag(stack, CONFIG_TAG, configured);
    }

    @Override
    public boolean canAddConfiguredAmount(ItemStack stack, AEKey key, long amount) {
        if (stack.getItem() != this || !isValidKey(key) || amount <= 0) {
            return false;
        }
        var configured = new LinkedHashMap<>(getConfiguredAmounts(stack));
        configured.merge(key, amount, LoopStorageCellItem::add);
        return byteUsed(configured) <= capacityBytes;
    }

    @Override
    public UUID getOrCreateCellId(ItemStack stack) {
        if (stack.getItem() != this) {
            return null;
        }
        var root = stack.getOrCreateTag();
        if (!root.hasUUID(ID_TAG)) {
            root.putUUID(ID_TAG, UUID.randomUUID());
        }
        return root.getUUID(ID_TAG);
    }

    @Override
    public void clearConfiguredAmounts(ItemStack stack) {
        if (stack.getItem() == this && stack.hasTag()) {
            stack.getOrCreateTag().remove(CONFIG_TAG);
        }
    }
    // endregion

    // region util functions
    public final UUID getCellId(ItemStack stack) {
        if (stack.getItem() != this || !stack.hasTag() || !stack.getOrCreateTag().hasUUID(ID_TAG)) {
            return null;
        }
        return stack.getOrCreateTag().getUUID(ID_TAG);
    }

    static boolean isValidKey(AEKey key) {
        return key != null && (key.getType() == AEKeyType.items() || key.getType() == AEKeyType.fluids());
    }

    private static long add(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    static long byteUsed(Map<AEKey, Long> amounts) {
        long used = 0;
        for (var entry : amounts.entrySet()) {
            long perByte = entry.getKey().getAmountPerByte();
            // round up
            long bytes = 1 + (entry.getValue() - 1) / perByte;
            used = add(used, bytes);
        }
        return used;
    }

    private static void writeTag(ItemStack stack, String tagName, Map<AEKey, Long> entries) {
        var list = new ListTag();
        for (var entry : entries.entrySet()) {
            AEKey key = entry.getKey();
            long amount = entry.getValue();
            if (amount <= 0 || !isValidKey(key)) {
                continue;
            }
            var tag = new CompoundTag();
            tag.put(AEKEY_TAG, key.toTagGeneric());
            tag.putLong(AMOUNT_TAG, amount);
            list.add(tag);
        }
        var root = stack.getOrCreateTag();
        if (list.isEmpty()) {
            root.remove(tagName);
        } else {
            root.put(tagName, list);
        }
    }

    static void writeContents(ItemStack stack, Map<AEKey, Long> contents) {
        writeTag(stack, CONTENT_TAG, contents);
    }

    private static Map<AEKey, Long> readTag(ItemStack stack, String tagName) {
        var result = new LinkedHashMap<AEKey, Long>();
        var entries = stack.getOrCreateTag().getList(tagName, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.getCompound(i);
            var key = AEKey.fromTagGeneric(entry.getCompound(AEKEY_TAG));
            var amount = entry.getLong(AMOUNT_TAG);
            if (isValidKey(key) && amount > 0) {
                result.merge(key, amount, LoopStorageCellItem::add);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    static Map<AEKey, Long> readContents(ItemStack stack) {
        if (!stack.hasTag()) {
            return Map.of();
        }
        return readTag(stack, CONTENT_TAG);
    }
    // endregion

    // region render functions
    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level pLevel, List<Component> lines, TooltipFlag pIsAdvanced) {
        var contents = readContents(stack);
        lines.add(Tooltips.bytesUsed(byteUsed(contents), capacityBytes));
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        var content = new ArrayList<GenericStack>();
        boolean hasMoreContent = false;
        if (AEConfig.instance().isTooltipShowCellContent()) {
            readContents(stack).forEach((key, amount) -> {
                content.add(new GenericStack(key, amount));
            });
            content.sort(Comparator.comparingLong(GenericStack::amount).reversed());
            int maximum = AEConfig.instance().getTooltipMaxCellContentShown();
            hasMoreContent = content.size() > maximum;
            if (hasMoreContent) {
                content.subList(maximum, content.size()).clear();
            }
        }
        return Optional.of(new StorageCellTooltipComponent(List.of(), content, hasMoreContent, true));
    }
    // endregion
}
