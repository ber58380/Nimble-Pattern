package com.ber.nimblePattern.pattern.upgrade;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEKey;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.hooks.ticking.TickHandler;
import com.ber.nimblePattern.network.PatternUpgradeNotificationPacket;
import com.ber.nimblePattern.network.WirelessNotification;
import com.ber.nimblePattern.pattern.NimblePatternTag;

import java.util.*;

import static com.ber.nimblePattern.pattern.upgrade.UpgradeState.UPGRADE;

public final class PatternUpgradeTracker {
    private static final PatternUpgradeTracker INSTANCE = new PatternUpgradeTracker();
    private final LinkedHashSet<AEKey> pendingConditions = new LinkedHashSet<>();
    private final Map<AEKey, List<Location>> byCondition = new HashMap<>();
    private final Map<InternalInventory, InventoryIndex> inventories = new IdentityHashMap<>();
    private int refreshTicks;

    private PatternUpgradeTracker() {
    }

    public static PatternUpgradeTracker instance() {
        return INSTANCE;
    }

    public synchronized void enqueueCondition(AEKey what) {
        if (what != null) {
            pendingConditions.add(what.dropSecondary());
        }
    }

    private void removeIndex(InventoryIndex index) {
        index.conditions.forEach((condition, location) -> {
            var all = byCondition.get(condition);
            if (all != null) {
                all.removeAll(location);
                if (all.isEmpty()) {
                    byCondition.remove(condition);
                }
            }
        });
    }

    private void rebuildIndex() {
        var seen = Collections.newSetFromMap(new IdentityHashMap<InternalInventory, Boolean>());
        for (var grid : TickHandler.instance().getGridList()) {
            for (var machine : grid.getMachineClasses()) {
                if (PatternContainer.class.isAssignableFrom(machine)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends PatternContainer> cls = (Class<? extends PatternContainer>) machine;
                    for (PatternContainer container : grid.getActiveMachines(cls)) {
                        var inv = container.getTerminalPatternInventory();
                        if (!seen.add(inv)) {
                            continue;
                        }
                        var snapshot = PatternInventorySnapshots.getSnapshot(inv, true);
                        var old = inventories.get(inv);
                        if (old != null && old.revision == snapshot.getRevision() && old.grid() == grid) {
                            continue;
                        }
                        if (old != null) {
                            removeIndex(old);
                        }
                        var conditions = new HashMap<AEKey, List<Location>>();
                        for (int i = 0; i < snapshot.size(); i++) {
                            var pattern = snapshot.getStack(i);
                            if (pattern.isEmpty()) {
                                continue;
                            }
                            AEKey condition = NimblePatternTag.getCondition(pattern);
                            if (condition != null) {
                                conditions.computeIfAbsent(condition, k -> new ArrayList<>()).add(new Location(inv, i));
                            }
                        }
                        inventories.put(inv, new InventoryIndex(snapshot.getRevision(), grid, conditions));
                        conditions.forEach((condition, locations) ->
                                byCondition.computeIfAbsent(condition, k -> new ArrayList<>()).addAll(locations));
                    }
                }
            }
        }
        var iterator = inventories.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!seen.contains(entry.getKey())) {
                removeIndex(entry.getValue());
                iterator.remove();
            }
        }
    }

    public synchronized void updateStatus() {
        if (++refreshTicks < 20) {
            return;
        }
        refreshTicks = 0;
        rebuildIndex();
        if (pendingConditions.isEmpty()) {
            return;
        }
        Set<AEKey> toProcess = new LinkedHashSet<>(pendingConditions);
        pendingConditions.clear();
        Map<IGrid, Map<AEKey, Integer>> upgradeCounter = new IdentityHashMap<>();
        for (var condition : toProcess) {
            for (var location : byCondition.getOrDefault(condition, List.of())) {
                var inv = location.inventory();
                var index = inventories.get(inv);
                if (index == null || location.slot() >= inv.size()) {
                    continue;
                }
                var original = inv.getStackInSlot(location.slot());
                if (!condition.equals(NimblePatternTag.getCondition(original)) || NimblePatternTag.getStatus(original) == UPGRADE) {
                    continue;
                }
                var pattern = original.copy();
                NimblePatternTag.tagStatus(pattern);
                inv.setItemDirect(location.slot(), pattern);
                upgradeCounter.computeIfAbsent(index.grid, ignored -> new HashMap<>())
                        .merge(condition, 1, Integer::sum);
            }
        }
        upgradeCounter.forEach((grid, counts) ->
                counts.forEach((condition, count) ->
                        WirelessNotification.broadcast(grid, new PatternUpgradeNotificationPacket(condition, count))));
    }

    public synchronized void clear() {
        pendingConditions.clear();
        byCondition.clear();
        inventories.clear();
        refreshTicks = 0;
        PatternInventorySnapshots.clear();
    }

    private record Location(InternalInventory inventory, int slot) {
    }

    private record InventoryIndex(long revision, IGrid grid, Map<AEKey, List<Location>> conditions) {
    }
}
