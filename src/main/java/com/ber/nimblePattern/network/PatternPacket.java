package com.ber.nimblePattern.network;

import com.ber.nimblePattern.client.gui.PatternUpgradeTermScreen;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PatternPacket {
    private boolean fullUpdate;
    private long inventoryId;
    private int inventorySize; // Only valid if fullUpdate
    private Int2ObjectMap<ItemStack> slots;

    public PatternPacket(FriendlyByteBuf buf) {
        inventoryId = buf.readVarLong();
        fullUpdate = buf.readBoolean();
        if (fullUpdate) {
            inventorySize = buf.readVarInt();
        }
        var slotsCount = buf.readVarInt();
        slots = new Int2ObjectArrayMap<>(slotsCount);
        for (int i = 0; i < slotsCount; i++) {
            var slot = buf.readVarInt();
            var item = buf.readItem();
            slots.put(slot, item);
        }
    }

    public static void encode(PatternPacket msg, FriendlyByteBuf buf) {
        buf.writeVarLong(msg.inventoryId);
        buf.writeBoolean(msg.fullUpdate);
        if (msg.fullUpdate) {
            buf.writeVarInt(msg.inventorySize);
        }
        buf.writeVarInt(msg.slots.size());
        for (var entry : msg.slots.int2ObjectEntrySet()) {
            buf.writeVarInt(entry.getIntKey());
            buf.writeItem(entry.getValue());
        }
    }

    public static PatternPacket decode(FriendlyByteBuf buf) {
        return new PatternPacket(buf);
    }

    public static void handle(PatternPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof PatternUpgradeTermScreen screen) {
                if (msg.fullUpdate) {
                    screen.postFullUpdate(msg.inventoryId, msg.inventorySize, msg.slots);
                } else {
                    screen.postIncrementalUpdate(msg.inventoryId, msg.slots);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private PatternPacket(boolean fullUpdate, long inventoryId, int inventorySize, Int2ObjectMap<ItemStack> slots) {
        this.fullUpdate = fullUpdate;
        this.inventoryId = inventoryId;
        this.inventorySize = inventorySize;
        this.slots = slots;
    }

    public static PatternPacket fullUpdate(long inventoryId, int inventorySize, Int2ObjectMap<ItemStack> slots) {
        return new PatternPacket(true, inventoryId, inventorySize, slots);
    }

    public static PatternPacket incrementalUpdate(long inventoryId, Int2ObjectMap<ItemStack> slots) {
        return new PatternPacket(false, inventoryId, 0, slots);
    }
}
