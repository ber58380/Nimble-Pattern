package com.ber.nimblePattern.network;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import com.ber.nimblePattern.client.gui.widgets.NimbleToast;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ProbabilityBlockedNotificationPacket(AEKey what) {
    public static void encode(ProbabilityBlockedNotificationPacket msg, FriendlyByteBuf buf) {
        AEKey.writeKey(buf, msg.what);
    }

    public static ProbabilityBlockedNotificationPacket decode(FriendlyByteBuf buf) {
        return new ProbabilityBlockedNotificationPacket(AEKey.readKey(buf));
    }

    public static void handle(ProbabilityBlockedNotificationPacket msg, Supplier<NetworkEvent.Context> context) {
        var ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.show(msg)));
        ctx.setPacketHandled(true);
    }

    private static final class ClientHandler {
        private static void show(ProbabilityBlockedNotificationPacket msg) {
            var minecraft = Minecraft.getInstance();
            if (minecraft.player == null || msg.what() == null) {
                return;
            }
            minecraft.getToasts().addToast(new NimbleToast(msg.what(), Component.translatable("toast.nimble_pattern.probability_blocked_title"), Component.translatable("toast.nimble_pattern.probability_blocked_content", AEKeyRendering.getDisplayName(msg.what))));
        }
    }
}
