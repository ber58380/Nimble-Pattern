package com.ber.nimblePattern.network;

import appeng.api.client.AEKeyRendering;
import appeng.api.stacks.AEKey;
import com.ber.nimblePattern.client.gui.widgets.NimbleToast;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

public record LoopSeedLostNotificationPacket(AEKey what, boolean returnFailed) {
    public static void encode(LoopSeedLostNotificationPacket msg, FriendlyByteBuf buf) {
        AEKey.writeKey(buf, msg.what);
        buf.writeBoolean(msg.returnFailed);
    }

    public static LoopSeedLostNotificationPacket decode(FriendlyByteBuf buf) {
        return new LoopSeedLostNotificationPacket(Objects.requireNonNull(AEKey.readKey(buf)), buf.readBoolean());
    }

    public static void handle(LoopSeedLostNotificationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> ClientHandler.show(msg)));
        ctx.get().setPacketHandled(true);
    }

    private static final class ClientHandler {
        private static void show(LoopSeedLostNotificationPacket msg) {
            Component title;
            Component content;
            if (msg.returnFailed) {
                title = Component.translatable("toast.nimble_pattern.loop_seed_return_failed_title");
                content = Component.translatable("toast.nimble_pattern.loop_seed_return_failed_content", AEKeyRendering.getDisplayName(msg.what));
            } else {
                title = Component.translatable("toast.nimble_pattern.loop_seed_lost_title");
                content = Component.translatable("toast.nimble_pattern.loop_seed_lost_content", AEKeyRendering.getDisplayName(msg.what));
            }
            Minecraft.getInstance().getToasts().addToast(new NimbleToast(msg.what(), title, content));
        }
    }
}
