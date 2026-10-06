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
import java.util.UUID;

public class LoopStorageCellInventory implements StorageCell {
    private final ItemStack stack;
    private final LoopStorageCellItem item;
    private final @Nullable ISaveProvider host;
    private final Map<AEKey, Long> contents;
    private final Map<AEKey, Long> configured;
    private boolean dirty;

    public LoopStorageCellInventory(ItemStack stack, LoopStorageCellItem item, ISaveProvider host) {
        this.stack = stack;
        this.item = item;
        this.host = host;
        this.contents = new LinkedHashMap<>(LoopStorageCellItem.readContents(stack));
        this.configured = item.getConfiguredAmounts(stack);
    }

    // region implement functions in StorageCell
    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(what, amount, mode, source);
        if (amount == 0 || !LoopStorageCellItem.isValidKey(what) || !isIOPortTarget(source)) {
            return 0;
        }
        var targetAmount = configured.getOrDefault(what, 0L);
        var currentAmount = contents.getOrDefault(what, 0L);
        if (targetAmount <= currentAmount) {
            return 0;
        }
        long accepted = Math.min(amount, targetAmount - currentAmount);
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
        if (!isIOPortTarget(source)) {
            return 0;
        }
        return extractStored(what, amount, mode);
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
        return LoopStorageCellItem.byteUsed(contents) >= item.getCapacityBytes() ? CellState.FULL : CellState.NOT_EMPTY;
    }

    @Override
    public double getIdleDrain() {
        return item.getIdleDrain();
    }

    @Override
    public boolean canFitInsideCell() {
        return true;
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
    // endregion

    public UUID getCellId() {
        return item.getCellId(stack);
    }

    private boolean isIOPortTarget(IActionSource source) {
        return host == null && source.machine().filter(IOPortBlockEntity.class::isInstance).isPresent();
    }

    private void saveChanges() {
        dirty = true;
        persist();
        if (host != null) {
            host.saveChanges();
        }
    }

    private long extractStored(AEKey what, long amount, Actionable mode) {
        long stored = contents.getOrDefault(what, 0L);
        long extracted = Math.min(amount, stored);
        if (extracted <= 0) {
            return 0;
        }
        if (mode == Actionable.MODULATE) {
            long remaining = stored - extracted;
            if (remaining == 0) {
                contents.remove(what);
            } else {
                contents.put(what, remaining);
            }
            saveChanges();
        }
        return extracted;
    }

    public long extractForLoop(AEKey what, long amount, Actionable mode) {
        if (amount <= 0 || configured.getOrDefault(what, 0L) <= 0) {
            return 0;
        }
        return extractStored(what, amount, mode);
    }

    public long insertForLoop(AEKey what, long amount, Actionable mode) {
        if (amount <= 0) {
            return 0;
        }
        long targetAmount = configured.getOrDefault(what, 0L);
        long currentAmount = contents.getOrDefault(what, 0L);
        long accepted = Math.min(amount, Math.max(0, targetAmount - currentAmount));
        if (accepted > 0 && mode == Actionable.MODULATE) {
            contents.put(what, currentAmount + accepted);
            saveChanges();
        }
        return accepted;
    }
}
