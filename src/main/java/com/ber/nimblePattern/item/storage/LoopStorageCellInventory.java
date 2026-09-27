package com.ber.nimblePattern.item.storage;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import appeng.blockentity.storage.IOPortBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mixed item/fluid inventory used by every loop storage cell tier.
 */
public final class LoopStorageCellInventory implements StorageCell {
    private final ItemStack stack;
    private final LoopStorageCellItem item;
    private final @Nullable ISaveProvider host;
    private final Map<AEKey, Long> contents;
    private boolean dirty;

    LoopStorageCellInventory(ItemStack stack, LoopStorageCellItem item, @Nullable ISaveProvider host) {
        this.stack = stack;
        this.item = item;
        this.host = host;
        this.contents = new LinkedHashMap<>(LoopStorageCellItem.readContents(stack));
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(what, amount, mode, source);
        if (amount == 0 || !LoopStorageCellItem.isSupportedKey(what) || !isIoPortTarget(source)) {
            return 0;
        }

        var targetAmount = item.getConfiguredAmount(stack, what);
        var currentAmount = contents.getOrDefault(what, 0L);
        if (targetAmount <= currentAmount) {
            return 0;
        }

        var capacityForKey = getCapacityForKey(what, currentAmount);
        var accepted = Math.min(amount, Math.min(targetAmount - currentAmount, capacityForKey - currentAmount));
        if (accepted <= 0) {
            return 0;
        }

        if (mode == Actionable.MODULATE) {
            contents.put(what, currentAmount + accepted);
            dirty = true;
            persist();
            if (host != null) {
                host.saveChanges();
            }
        }
        return accepted;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(what, amount, mode, source);
        return 0;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        contents.forEach(out::add);
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
        return false;
    }

    @Override
    public CellState getStatus() {
        if (contents.isEmpty()) {
            return CellState.EMPTY;
        }
        return canAcceptConfiguredContent() ? CellState.NOT_EMPTY : CellState.FULL;
    }

    @Override
    public double getIdleDrain() {
        return item.getIdleDrain();
    }

    @Override
    public boolean canFitInsideCell() {
        return false;
    }

    @Override
    public Component getDescription() {
        return stack.getHoverName();
    }

    @Override
    public void persist() {
        if (!dirty) {
            return;
        }
        LoopStorageCellItem.writeContents(stack, contents);
        dirty = false;
    }

    private boolean isIoPortTarget(IActionSource source) {
        // The I/O port also uses its own source while inserting into the network. Requiring a host-less
        // inventory distinguishes the cell physically inside the port from a loop cell mounted in a drive.
        return host == null && source.machine().filter(IOPortBlockEntity.class::isInstance).isPresent();
    }

    private boolean canAcceptConfiguredContent() {
        for (var entry : item.getConfiguredAmounts(stack).entrySet()) {
            var current = contents.getOrDefault(entry.getKey(), 0L);
            if (current < entry.getValue() && current < getCapacityForKey(entry.getKey(), current)) {
                return true;
            }
        }
        return false;
    }

    private long getCapacityForKey(AEKey key, long currentAmount) {
        var bytesForCurrentKey = bytesUsed(key, currentAmount);
        var usedWithoutCurrentKey = Math.max(0, getUsedBytes() - bytesForCurrentKey);
        var bytesAvailableForKey = Math.max(0, item.getCapacityBytes() - usedWithoutCurrentKey);
        return saturatedMultiply(bytesAvailableForKey, key.getAmountPerByte());
    }

    private long getUsedBytes() {
        long used = 0;
        for (var entry : contents.entrySet()) {
            used = saturatedAdd(used, bytesUsed(entry.getKey(), entry.getValue()));
        }
        return used;
    }

    private static long bytesUsed(AEKey key, long amount) {
        if (amount <= 0) {
            return 0;
        }
        var perByte = key.getAmountPerByte();
        return 1 + (amount - 1) / perByte;
    }

    private static long saturatedAdd(long a, long b) {
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }

    private static long saturatedMultiply(long a, long b) {
        return a > 0 && b > Long.MAX_VALUE / a ? Long.MAX_VALUE : a * b;
    }
}
