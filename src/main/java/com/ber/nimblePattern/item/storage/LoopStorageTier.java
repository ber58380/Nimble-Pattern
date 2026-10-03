package com.ber.nimblePattern.item.storage;

import appeng.items.storage.StorageTier;

/** Capacity data only; registry names and existing cell NBT remain unchanged. */
public enum LoopStorageTier {
    SIZE_1K(StorageTier.SIZE_1K),
    SIZE_4K(StorageTier.SIZE_4K),
    SIZE_16K(StorageTier.SIZE_16K),
    SIZE_64K(StorageTier.SIZE_64K),
    SIZE_256K(StorageTier.SIZE_256K);

    private final StorageTier tier;

    LoopStorageTier(StorageTier tier) { this.tier = tier; }
    public long bytes() { return tier.bytes(); }
    public double idleDrain() { return tier.idleDrain(); }
    public String registryName() { return "loop_storage_cell_" + tier.namePrefix(); }
}
