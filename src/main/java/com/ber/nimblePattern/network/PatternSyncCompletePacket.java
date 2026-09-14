package com.ber.nimblePattern.network;

import com.ber.nimblePattern.client.gui.PatternUpgradeTermScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Marks the end of a full Pattern Upgrade Terminal synchronization batch.
 */
public record PatternSyncCompletePacket() {
    public static void encode(PatternSyncCompletePacket msg, FriendlyByteBuf buf) {
    }

    public static PatternSyncCompletePacket decode(FriendlyByteBuf buf) {
        return new PatternSyncCompletePacket();
    }

    public static void handle(PatternSyncCompletePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof PatternUpgradeTermScreen screen) {
                screen.finishFullUpdate();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
