package com.ber.nimblePattern.network;

import appeng.api.stacks.AEKey;
import com.ber.nimblePattern.client.gui.PatternTagTermScreen;
import com.ber.nimblePattern.client.gui.widgets.PatternUpgradeToast;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PatternUpgradeNotificationPacket {
    private final AEKey condition;
    private final int count;

    public PatternUpgradeNotificationPacket(AEKey condition, int count) {
        this.condition = condition;
        this.count = count;
    }

    public static void encode(PatternUpgradeNotificationPacket msg, FriendlyByteBuf buf) {
        AEKey.writeKey(buf, msg.condition);
        buf.writeVarInt(msg.count);
    }

    public static PatternUpgradeNotificationPacket decode(FriendlyByteBuf buf) {
        return new PatternUpgradeNotificationPacket(AEKey.readKey(buf), buf.readVarInt());
    }

    public static void handle(PatternUpgradeNotificationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.show(msg));
        });
        ctx.get().setPacketHandled(true);
    }

    private static final class ClientHandler {
        private static void show(PatternUpgradeNotificationPacket msg) {
            var minecraft = Minecraft.getInstance();
            // since the upgrade terminal is opened, do not send toast
            if (minecraft.screen instanceof PatternTagTermScreen) {
                return;
            }
            if (minecraft.player == null || msg.condition == null) {
                return;
            }
            minecraft.getToasts().addToast(new PatternUpgradeToast(msg.condition, msg.count));
        }
    }
}
