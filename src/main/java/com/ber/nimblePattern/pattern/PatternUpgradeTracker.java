package com.ber.nimblePattern.pattern;

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
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

import static com.ber.nimblePattern.pattern.UpdateState.UPDATE;

public final class PatternUpgradeTracker {
    private static final PatternUpgradeTracker INSTANCE = new PatternUpgradeTracker();

    public static PatternUpgradeTracker instance() {
        return INSTANCE;
    }

    private final Set<String> trackedConditions = new HashSet<>();
    private final LinkedHashSet<ResourceLocation> pendingIds = new LinkedHashSet<>();
    private record Location(appeng.api.inventories.InternalInventory inventory, int slot) {}
    private final Map<String, List<Location>> byCondition = new HashMap<>();
    private record InventoryIndex(long revision, Map<String, List<Location>> conditions) {}
    private final Map<appeng.api.inventories.InternalInventory, InventoryIndex> inventories = new IdentityHashMap<>();
    private int refreshTicks;

    private PatternUpgradeTracker() {
    }

    private static boolean isID(String condition) {
        if (condition == null || condition.isBlank()) {
            return false;
        }
        ResourceLocation id = ResourceLocation.tryParse(condition);
        if (id == null) return false;
        return ForgeRegistries.ITEMS.containsKey(id) || ForgeRegistries.FLUIDS.containsKey(id) || ForgeRegistries.BLOCKS.containsKey(id);
    }

    public synchronized boolean isEmpty() {
        return trackedConditions.isEmpty();
    }

    public synchronized void enqueueIfTracked(String Id) {
        var id = ResourceLocation.tryParse(Id);
        if (id != null) enqueueIfTracked(id);
    }

    public synchronized void enqueueIfTracked(ResourceLocation id) {
        // Keep arrivals until the next refresh, including newly placed tagged patterns.
        pendingIds.add(id);
    }

    public synchronized void updateStatus() {
        // Shared, bounded polling also covers third-party inventories without change callbacks.
        if (++refreshTicks < 20) return;
        refreshTicks = 0;
        rebuildIndex();
        if (pendingIds.isEmpty()) {
            return;
        }
        var toProcess = new ArrayList<>(pendingIds);
        pendingIds.clear();
        Map<String, Integer> upgradeCounter = new HashMap<>();
        for (var id : toProcess) {
            var condition = id.toString();
            for (var location : byCondition.getOrDefault(condition, List.of())) {
                var inv = location.inventory();
                if (location.slot() >= inv.size()) continue;
                var original = inv.getStackInSlot(location.slot());
                if (!condition.equals(NimblePatternTag.getCondition(original)) || NimblePatternTag.getStatus(original) == UPDATE) continue;
                var pattern = original.copy();
                NimblePatternTag.tagStatus(pattern);
                inv.setItemDirect(location.slot(), pattern);
                upgradeCounter.merge(condition, 1, Integer::sum);
            }
        }
        notifyPlayers(upgradeCounter);
    }

    private void rebuildIndex() {
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<appeng.api.inventories.InternalInventory, Boolean>());
        for (var grid : TickHandler.instance().getGridList()) {
            for (var machine : grid.getMachineClasses()) {
                if (PatternContainer.class.isAssignableFrom(machine)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends PatternContainer> cls = (Class<? extends PatternContainer>) machine;
                    for (PatternContainer container : grid.getActiveMachines(cls)) {
                        var inv = container.getTerminalPatternInventory();
                        if (!seen.add(inv)) continue;
                        var snapshot = PatternInventorySnapshots.get(inv, true);
                        var old = inventories.get(inv);
                        if (old != null && old.revision == snapshot.revision()) continue;
                        if (old != null) removeIndex(old);
                        var conditions = new HashMap<String, List<Location>>();
                        for (int i = 0; i < snapshot.size(); i++) {
                            var pattern = snapshot.stack(i);
                            if (pattern.isEmpty()) continue;
                            String condition = NimblePatternTag.getCondition(pattern);
                            if (isID(condition)) {
                                conditions.computeIfAbsent(condition, k -> new ArrayList<>()).add(new Location(inv, i));
                            }
                        }
                        inventories.put(inv, new InventoryIndex(snapshot.revision(), conditions));
                        conditions.forEach((condition, locations) ->
                                byCondition.computeIfAbsent(condition, k -> new ArrayList<>()).addAll(locations));
                    }
                }
            }
        }
        var iterator = inventories.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!seen.contains(entry.getKey())) { removeIndex(entry.getValue()); iterator.remove(); }
        }
        trackedConditions.clear();
        trackedConditions.addAll(byCondition.keySet());
    }

    private void removeIndex(InventoryIndex index) {
        index.conditions.forEach((condition, locations) -> {
            var all = byCondition.get(condition);
            if (all != null) {
                all.removeAll(locations);
                if (all.isEmpty()) byCondition.remove(condition);
            }
        });
    }

    public synchronized void clear() {
        trackedConditions.clear(); pendingIds.clear(); byCondition.clear(); inventories.clear(); refreshTicks = 0;
        PatternInventorySnapshots.clear();
    }

    private void notifyPlayers(Map<String, Integer> upgradeCounter) {
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
                if (!stack.isEmpty()
                        && stack.getItem() instanceof WirelessTerminalItem wirelessTerminal
                        // Should have some power
                        && wirelessTerminal.getAECurrentPower(stack) > 0
                        // Should be linked (we don't know if it's linked to the grid for which we get notifications)
                        && wirelessTerminal.getLinkedPosition(stack) != null) {
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

}
