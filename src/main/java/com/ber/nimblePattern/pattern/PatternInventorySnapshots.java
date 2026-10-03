package com.ber.nimblePattern.pattern;

import appeng.api.inventories.InternalInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.util.Map;
import java.util.WeakHashMap;

/** Server-thread shared change detection, at most once per inventory per five ticks. */
public final class PatternInventorySnapshots {
    private static final Map<InternalInventory, Snapshot> SNAPSHOTS = new WeakHashMap<>();
    public static final class Snapshot {
        private ItemStack[] stacks = new ItemStack[0];
        private long checked = Long.MIN_VALUE;
        private long revision;
        public long revision() { return revision; }
        public int size() { return stacks.length; }
        public ItemStack stack(int slot) { return stacks[slot]; }
    }
    public static Snapshot get(InternalInventory inventory) {
        return get(inventory, false);
    }
    public static Snapshot get(InternalInventory inventory, boolean force) {
        var snapshot = SNAPSHOTS.computeIfAbsent(inventory, k -> new Snapshot());
        var server = ServerLifecycleHooks.getCurrentServer();
        long now = server == null ? 0 : server.getTickCount();
        if (!force && snapshot.checked != Long.MIN_VALUE && now >= snapshot.checked && now - snapshot.checked < 5) return snapshot;
        snapshot.checked = now;
        if (snapshot.stacks.length != inventory.size()) {
            snapshot.stacks = new ItemStack[inventory.size()];
            java.util.Arrays.fill(snapshot.stacks, ItemStack.EMPTY);
            snapshot.revision++;
        }
        boolean changed = false;
        for (int i = 0; i < snapshot.stacks.length; i++) {
            var stack = inventory.getStackInSlot(i);
            if (!ItemStack.matches(stack, snapshot.stacks[i])) {
                snapshot.stacks[i] = stack.copy();
                changed = true;
            }
        }
        if (changed) snapshot.revision++;
        return snapshot;
    }
    public static void clear() { SNAPSHOTS.clear(); }
    private PatternInventorySnapshots() {}
}
