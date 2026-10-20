package com.ber.nimblePattern;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = NimblePattern.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.LongValue PROBABILITY_COLLECTION_GRACE_TICKS =
            BUILDER.comment("The grace waiting ticks for probability outputs return to network from GT machines",
                            "unit: ticks (1 second = 20 ticks)")
                    .defineInRange("probability.collectionGraceTicks", 60, 0, Long.MAX_VALUE);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static long probabilityCollectionGraceTicks() {
        return PROBABILITY_COLLECTION_GRACE_TICKS.get();
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {

    }
}
