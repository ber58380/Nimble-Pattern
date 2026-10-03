package com.ber.nimblePattern;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = NimblePattern.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue PROBABILITY_TIMEOUT_SECONDS = BUILDER
            .comment("Fallback timeout in seconds, only for probability patterns without a supported machine tracker.")
            .defineInRange("probabilityPatternTimeoutSeconds", 120, 1, 86400);
    static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {

    }
}
