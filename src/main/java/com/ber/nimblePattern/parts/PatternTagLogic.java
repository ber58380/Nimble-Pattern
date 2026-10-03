package com.ber.nimblePattern.parts;

import appeng.api.inventories.InternalInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.ber.nimblePattern.helpers.IPatternTagLogicHost;
import net.minecraft.nbt.CompoundTag;

public class PatternTagLogic implements InternalInventoryHost {
    private final IPatternTagLogicHost host;

    public static final int INPUT_PATTERN_COLUMNS = 3;
    public static final int INPUT_PATTERN_VISIBLE_ROWS = 3;
    public static final int INPUT_PATTERN_TOTAL_ROWS = 27;
    public static final int INPUT_PATTERN_SLOTS = INPUT_PATTERN_COLUMNS * INPUT_PATTERN_TOTAL_ROWS;
    public static final int CONDITION_ITEM_SLOTS = 1;
    public static final int LOOP_ENDPOINT_SLOTS = 2;
    public static final int LOOP_STORAGE_CELL_SLOTS = 1;

    private final AppEngInternalInventory inputPatternInv = new AppEngInternalInventory(this, INPUT_PATTERN_SLOTS);
    private final AppEngInternalInventory conditionItemInv = new AppEngInternalInventory(this, CONDITION_ITEM_SLOTS);
    private final AppEngInternalInventory loopEndpointInv = new AppEngInternalInventory(this, LOOP_ENDPOINT_SLOTS);
    private final AppEngInternalInventory loopStorageCellInv = new AppEngInternalInventory(this, LOOP_STORAGE_CELL_SLOTS);

    private TagMode mode = TagMode.UPGRADE;
    private final AppEngInternalInventory toolInv = new AppEngInternalInventory(this, 4);

    public AppEngInternalInventory getToolInv() {
        return toolInv;
    }
    private boolean isLoading = false;

    public PatternTagLogic(IPatternTagLogicHost host) {
        this.host = host;
    }

    public AppEngInternalInventory getInputPatternInv() {
        return inputPatternInv;
    }

    public AppEngInternalInventory getConditionItemInv() {
        return conditionItemInv;
    }

    public AppEngInternalInventory getLoopEndpointInv() {
        return loopEndpointInv;
    }

    public AppEngInternalInventory getLoopStorageCellInv() {
        return loopStorageCellInv;
    }

    public TagMode getMode() {
        return mode;
    }

    public void setMode(TagMode mode) {
        if (mode != null && this.mode != mode) {
            this.mode = mode;
            saveChanges();
        }
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        saveChanges();
    }

    @Override
    public void saveChanges() {
        if (!isLoading) {
            host.markForSave();
        }
    }

    @Override
    public boolean isClientSide() {
        return host.getLevel().isClientSide();
    }

    public void readFromNBT(CompoundTag data) {
        isLoading = true;
        try {
            try {
                mode = TagMode.valueOf(data.getString("mode"));
            } catch (IllegalArgumentException ignored) {
                mode = TagMode.UPGRADE;
            }
            inputPatternInv.readFromNBT(data, "inputPattern");
            conditionItemInv.readFromNBT(data, "conditionItem");
            loopEndpointInv.readFromNBT(data, "loopEndpoints");
            toolInv.readFromNBT(data, "tools");
            loopStorageCellInv.readFromNBT(data, "loopStorageCell");
        } finally {
            isLoading = false;
        }
    }

    public void writeToNBT(CompoundTag data) {
        data.putString("mode", mode.name());
        inputPatternInv.writeToNBT(data, "inputPattern");
        conditionItemInv.writeToNBT(data, "conditionItem");
        loopEndpointInv.writeToNBT(data, "loopEndpoints");
        toolInv.writeToNBT(data, "tools");
        loopStorageCellInv.writeToNBT(data, "loopStorageCell");
    }
}
