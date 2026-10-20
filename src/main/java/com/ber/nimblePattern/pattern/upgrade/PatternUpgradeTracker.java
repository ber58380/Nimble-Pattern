package com.ber.nimblePattern.pattern.upgrade;

import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEKey;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.hooks.ticking.TickHandler;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.util.SearchInventoryEvent;
import com.ber.nimblePattern.network.NimblePatternNetwork;
import com.ber.nimblePattern.network.PatternUpgradeNotificationPacket;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

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
                        if (old != null && old.revision == snapshot.getRevision()) {
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
                        inventories.put(inv, new InventoryIndex(snapshot.getRevision(), conditions));
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

    private void notifyPlayers(Map<AEKey, Integer> upgradeCounter) {
        if (upgradeCounter.isEmpty()) {
            return;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean hasWireless = false;
            for (ItemStack stack : SearchInventoryEvent.getItems(player)) {
                if (!stack.isEmpty() && stack.getItem() instanceof WirelessTerminalItem wirelessTerminalItem
                        && wirelessTerminalItem.getAECurrentPower(stack) > 0
                        && wirelessTerminalItem.getLinkedPosition(stack) != null) {
                    hasWireless = true;
                    break;
                }
            }
            if (hasWireless) {
                for (var entry : upgradeCounter.entrySet()) {
                    NimblePatternNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PatternUpgradeNotificationPacket(entry.getKey(), entry.getValue()));
                }
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
        Map<AEKey, Integer> upgradeCounter = new HashMap<>();
        for (var condition : toProcess) {
            for (var location : byCondition.getOrDefault(condition, List.of())) {
                var inv = location.inventory();
                if (location.slot() >= inv.size()) {
                    continue;
                }
                var original = inv.getStackInSlot(location.slot());
                if (!condition.equals(NimblePatternTag.getCondition(original)) || NimblePatternTag.getStatus(original) == UPGRADE) {
                    continue;
                }
                var pattern = original.copy();
                NimblePatternTag.tagStatus(pattern);
                inv.setItemDirect(location.slot(), pattern);
                upgradeCounter.merge(condition, 1, Integer::sum);
            }
        }
        notifyPlayers(upgradeCounter);
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

    private record InventoryIndex(long revision, Map<AEKey, List<Location>> conditions) {
    }
}
