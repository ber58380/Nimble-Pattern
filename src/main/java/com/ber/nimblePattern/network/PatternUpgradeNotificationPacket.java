package com.ber.nimblePattern.network;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.ber.nimblePattern.client.gui.PatternUpgradeTermScreen;
import com.ber.nimblePattern.client.gui.PatternUpgradeToast;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class PatternUpgradeNotificationPacket {
    private final String condition;
    private final int count;

    public PatternUpgradeNotificationPacket(String condition, int count) {
        this.condition = condition;
        this.count = count;
    }

    public static void encode(PatternUpgradeNotificationPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.condition);
        buf.writeVarInt(msg.count);
    }

    public static PatternUpgradeNotificationPacket decode(FriendlyByteBuf buf) {
        return new PatternUpgradeNotificationPacket(buf.readUtf(), buf.readVarInt());
    }

    public static void handle(PatternUpgradeNotificationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var minecraft = Minecraft.getInstance();
            // since the upgrade terminal is opened, do not send toast
            if (minecraft.screen instanceof PatternUpgradeTermScreen) {
                return;
            }
            if (minecraft.player == null) {
                return;
            }
            ResourceLocation id = ResourceLocation.tryParse(msg.condition);
            AEKey what = null;
            if (id != null) {
                if (ForgeRegistries.ITEMS.containsKey(id)) {
                    what = AEItemKey.of(ForgeRegistries.ITEMS.getValue(id).getDefaultInstance());
                } else if (ForgeRegistries.FLUIDS.containsKey(id)) {
                    what = AEFluidKey.of(ForgeRegistries.FLUIDS.getValue(id));
                } else if (ForgeRegistries.BLOCKS.containsKey(id)) {
                    var block = ForgeRegistries.BLOCKS.getValue(id);
                    var asItem = block.asItem();
                    what = asItem != Items.AIR ? AEItemKey.of(asItem.getDefaultInstance()) : null;
                }
            }
            minecraft.getToasts().addToast(new PatternUpgradeToast(msg.condition, msg.count, what));
        });
        ctx.get().setPacketHandled(true);
    }
}
