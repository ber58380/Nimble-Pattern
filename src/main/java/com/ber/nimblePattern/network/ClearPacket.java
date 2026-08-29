package com.ber.nimblePattern.network;

import com.ber.nimblePattern.client.gui.PatternUpgradeTermScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClearPacket() {
    public static void encode(ClearPacket msg, FriendlyByteBuf buf) {

    }

    public static ClearPacket decode(FriendlyByteBuf buf) {
        return new ClearPacket();
    }

    public static void handle(ClearPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof PatternUpgradeTermScreen screen) {
                screen.clear();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
