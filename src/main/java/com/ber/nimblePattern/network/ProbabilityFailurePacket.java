package com.ber.nimblePattern.network;

import appeng.api.stacks.AEKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.Objects;
import java.util.function.Supplier;

public record ProbabilityFailurePacket(AEKey what) {
    public static void encode(ProbabilityFailurePacket msg, FriendlyByteBuf buf) { AEKey.writeKey(buf, msg.what); }
    public static ProbabilityFailurePacket decode(FriendlyByteBuf buf) {
        return new ProbabilityFailurePacket(Objects.requireNonNull(AEKey.readKey(buf)));
    }
    public static void handle(ProbabilityFailurePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> Client.show(msg)));
        ctx.get().setPacketHandled(true);
    }
    private static final class Client {
        static void show(ProbabilityFailurePacket msg) {
            net.minecraft.client.Minecraft.getInstance().getToasts().addToast(
                    new com.ber.nimblePattern.client.gui.widgets.LoopSeedLostToast(msg.what,
                            "toast.nimble_pattern.probability_failed_title", "toast.nimble_pattern.probability_failed_content"));
        }
    }
}
