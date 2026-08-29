package com.ber.nimblePattern.network;

import com.ber.nimblePattern.client.gui.PatternUpgradeTermScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

public class ConditionPacket {
    private Set<String> conditions;

    public ConditionPacket(Set<String> conditions) {
        this.conditions = conditions;
    }

    public static void encode(ConditionPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.conditions.size());
        for (var condition : msg.conditions) {
            buf.writeUtf(condition);
        }
    }

    public static ConditionPacket decode(FriendlyByteBuf buf) {
        int conditionsSize = buf.readVarInt();
        Set<String> conditions = new LinkedHashSet<>(conditionsSize);
        for (int i = 0; i < conditionsSize; i++) {
            conditions.add(buf.readUtf());
        }
        return new ConditionPacket(conditions);
    }

    public static void handle(ConditionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof PatternUpgradeTermScreen screen) {
                screen.postConditionUpdate(msg.conditions);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
