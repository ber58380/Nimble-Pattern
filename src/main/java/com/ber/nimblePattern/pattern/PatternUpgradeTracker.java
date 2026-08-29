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
    private final LinkedHashSet<String> pendingIds = new LinkedHashSet<>();

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

    public synchronized void updateTracked(Set<String> conditions) {
        trackedConditions.clear();
        if (conditions == null) {
            return;
        }
        for (String condition : conditions) {
            if (isID(condition)) {
                trackedConditions.add(condition);
            }
        }
    }

    public synchronized boolean isEmpty() {
        return trackedConditions.isEmpty();
    }

    public synchronized void enqueueIfTracked(String Id) {
        if (trackedConditions.contains(Id)) {
            pendingIds.add(Id);
        }
    }

    public synchronized void updateStatus() {
        if (pendingIds.isEmpty()) {
            return;
        }
        Set<String> toProcess = new LinkedHashSet<>(pendingIds);
        pendingIds.clear();
        Map<String, Integer> upgradeCounter = new HashMap<>();
        for (var grid : TickHandler.instance().getGridList()) {
            for (var machine : grid.getMachineClasses()) {
                if (PatternContainer.class.isAssignableFrom(machine)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends PatternContainer> cls = (Class<? extends PatternContainer>) machine;
                    for (PatternContainer container : grid.getActiveMachines(cls)) {
                        var inv = container.getTerminalPatternInventory();
                        for (int i = 0; i < inv.size(); i++) {
                            var pattern = inv.getStackInSlot(i).copy();
                            if (pattern.isEmpty()) {
                                continue;
                            }
                            String condition = NimblePatternTag.getCondition(pattern);
                            if (toProcess.contains(condition) && NimblePatternTag.getStatus(pattern) != UPDATE) {
                                NimblePatternTag.tagStatus(pattern);
                                inv.setItemDirect(i, pattern);
                                upgradeCounter.merge(condition, 1, Integer::sum);
                            }
                        }
                    }
                }
            }
        }
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
