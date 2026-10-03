package com.ber.nimblePattern.network;

import appeng.api.stacks.AEKey;
import com.ber.nimblePattern.client.gui.widgets.LoopSeedLostToast;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

public record LoopSeedLostNotificationPacket(AEKey what) {
    public static void encode(LoopSeedLostNotificationPacket msg, FriendlyByteBuf buf) {
        AEKey.writeKey(buf, msg.what);
    }

    public static LoopSeedLostNotificationPacket decode(FriendlyByteBuf buf) {
        return new LoopSeedLostNotificationPacket(Objects.requireNonNull(AEKey.readKey(buf)));
    }

    public static void handle(LoopSeedLostNotificationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> ClientHandler.show(msg)));
        ctx.get().setPacketHandled(true);
    }

    private static final class ClientHandler {
        private static void show(LoopSeedLostNotificationPacket msg) {
            Minecraft.getInstance().getToasts().addToast(new LoopSeedLostToast(msg.what));
        }
    }
}
