package com.ber.nimblePattern.network;

import com.ber.nimblePattern.NimblePattern;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NimblePatternNetwork {
    private static final String PROTOCOL_VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(NimblePattern.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void init() {
        CHANNEL.registerMessage(0, ClearPacket.class, ClearPacket::encode, ClearPacket::decode, ClearPacket::handle);
        CHANNEL.registerMessage(1, PatternPacket.class, PatternPacket::encode, PatternPacket::decode, PatternPacket::handle);
        CHANNEL.registerMessage(2, ConditionPacket.class, ConditionPacket::encode, ConditionPacket::decode, ConditionPacket::handle);
        CHANNEL.registerMessage(3, PatternUpgradeNotificationPacket.class, PatternUpgradeNotificationPacket::encode, PatternUpgradeNotificationPacket::decode, PatternUpgradeNotificationPacket::handle);
        CHANNEL.registerMessage(4, PatternSyncCompletePacket.class, PatternSyncCompletePacket::encode, PatternSyncCompletePacket::decode, PatternSyncCompletePacket::handle);
        CHANNEL.registerMessage(5, LoopSeedLostNotificationPacket.class, LoopSeedLostNotificationPacket::encode, LoopSeedLostNotificationPacket::decode, LoopSeedLostNotificationPacket::handle);
        CHANNEL.registerMessage(6, ProbabilityRecipePacket.class, ProbabilityRecipePacket::encode, ProbabilityRecipePacket::decode,
                ProbabilityRecipePacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(7, ProbabilityFailurePacket.class, ProbabilityFailurePacket::encode, ProbabilityFailurePacket::decode,
                ProbabilityFailurePacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
    }
}
