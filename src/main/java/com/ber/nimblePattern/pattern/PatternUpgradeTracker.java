package com.ber.nimblePattern.pattern;

import appeng.api.inventories.InternalInventory;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.hooks.ticking.TickHandler;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.util.SearchInventoryEvent;
import com.ber.nimblePattern.network.NimblePatternNetwork;
import com.ber.nimblePattern.network.PatternUpgradeNotificationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

import static com.ber.nimblePattern.pattern.UpgradeState.UPGRADE;

public final class PatternUpgradeTracker {
    private static final PatternUpgradeTracker INSTANCE = new PatternUpgradeTracker();
    private final Set<ResourceLocation> trackedConditions = new HashSet<>();
    private final LinkedHashSet<ResourceLocation> pendingIds = new LinkedHashSet<>();
    private final Map<ResourceLocation, List<Location>> byCondition = new HashMap<>();
    private final Map<InternalInventory, InventoryIndex> inventories = new IdentityHashMap<>();
    private int refreshTicks;

    private PatternUpgradeTracker() {
    }

    public static PatternUpgradeTracker instance() {
        return INSTANCE;
    }

    public synchronized boolean isEmpty() {
        return trackedConditions.isEmpty();
    }

    public synchronized void enqueueIfTracked(ResourceLocation id) {
        if (id != null) {
            pendingIds.add(id);
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
                        var conditions = new HashMap<ResourceLocation, List<Location>>();
                        for (int i = 0; i < snapshot.size(); i++) {
                            var pattern = snapshot.getStack(i);
                            if (pattern.isEmpty()) {
                                continue;
                            }
                            ResourceLocation condition = NimblePatternTag.getCondition(pattern);
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
        trackedConditions.clear();
        trackedConditions.addAll(byCondition.keySet());
    }

    private void notifyPlayers(Map<ResourceLocation, Integer> upgradeConuter) {
        if (upgradeConuter.isEmpty()) {
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
                for (var entry : upgradeConuter.entrySet()) {
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
        if (pendingIds.isEmpty()) {
            return;
        }
        Set<ResourceLocation> toProcess = new LinkedHashSet<>(pendingIds);
        pendingIds.clear();
        Map<ResourceLocation, Integer> upgradeCounter = new HashMap<>();
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
        trackedConditions.clear();
        pendingIds.clear();
        byCondition.clear();
        inventories.clear();
        refreshTicks = 0;
        PatternInventorySnapshots.clear();
    }

    private record Location(InternalInventory inventory, int slot) {
    }

    private record InventoryIndex(long revision, Map<ResourceLocation, List<Location>> conditions) {
    }
}
