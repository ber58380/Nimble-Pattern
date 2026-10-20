package com.ber.nimblePattern.network;

import appeng.api.networking.IGrid;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.util.SearchInventoryEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class WirelessNotification {
    private WirelessNotification() {
    }

    /**
     * Determine whether the player has wireless terminal binding to the network
     */
    private static boolean hasLinkedTerminal(ServerPlayer player, IGrid grid) {
        for (var stack : SearchInventoryEvent.getItems(player)) {
            if (stack.isEmpty() || !(stack.getItem() instanceof WirelessTerminalItem terminal)) {
                continue;
            }
            if (terminal.getAECurrentPower(stack) > 0 && terminal.getLinkedGrid(stack, player.serverLevel(), null) == grid) {
                return true;
            }
        }
        return false;
    }

    /**
     * Send a notification toast to all players having wireless terminal binding to the network
     */
    public static void broadcast(IGrid grid, Object packet) {
        if (grid == null) {
            return;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (var player : server.getPlayerList().getPlayers()) {
            if (hasLinkedTerminal(player, grid)) {
                NimblePatternNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }
}
