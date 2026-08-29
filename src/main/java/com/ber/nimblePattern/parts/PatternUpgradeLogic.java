package com.ber.nimblePattern.parts;

import appeng.api.inventories.InternalInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.ber.nimblePattern.helpers.IPatternUpgradeLogicHost;
import net.minecraft.nbt.CompoundTag;

public class PatternUpgradeLogic implements InternalInventoryHost {
    private final IPatternUpgradeLogicHost host;

    public static final int INPUT_PATTERN_COLUMNS = 3;
    public static final int INPUT_PATTERN_VISIBLE_ROWS = 3;
    public static final int INPUT_PATTERN_TOTAL_ROWS = 27;
    public static final int INPUT_PATTERN_SLOTS = INPUT_PATTERN_COLUMNS * INPUT_PATTERN_TOTAL_ROWS;
    public static final int CONDITION_ITEM_SLOTS = 1;

    private final AppEngInternalInventory inputPatternInv = new AppEngInternalInventory(this, INPUT_PATTERN_SLOTS);
    private final AppEngInternalInventory conditionItemInv = new AppEngInternalInventory(this, CONDITION_ITEM_SLOTS);

    private boolean isLoading = false;

    public PatternUpgradeLogic(IPatternUpgradeLogicHost host) {
        this.host = host;
    }

    public AppEngInternalInventory getInputPatternInv() {
        return inputPatternInv;
    }

    public AppEngInternalInventory getConditionItemInv() {
        return conditionItemInv;
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
            inputPatternInv.readFromNBT(data, "inputPattern");
            conditionItemInv.readFromNBT(data, "conditionItem");
        } finally {
            isLoading = false;
        }
    }

    public void writeToNBT(CompoundTag data) {
        inputPatternInv.writeToNBT(data, "inputPattern");
        conditionItemInv.writeToNBT(data, "conditionItem");
    }
}
