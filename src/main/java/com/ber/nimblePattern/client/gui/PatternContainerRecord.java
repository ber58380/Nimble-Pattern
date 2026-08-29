package com.ber.nimblePattern.client.gui;

import appeng.util.inv.AppEngInternalInventory;

public class PatternContainerRecord {
    private final long serverId;
    private final AppEngInternalInventory inventory;

    public PatternContainerRecord(long serverId, int slots) {
        this.inventory = new AppEngInternalInventory(slots);
        this.serverId = serverId;
    }

    public long getServerId() {
        return serverId;
    }

    public AppEngInternalInventory getInventory() {
        return inventory;
    }

}
