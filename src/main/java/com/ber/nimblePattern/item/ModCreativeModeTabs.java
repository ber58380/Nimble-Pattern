package com.ber.nimblePattern.item;

import com.ber.nimblePattern.NimblePattern;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NimblePattern.MOD_ID);

    public static void register(IEventBus eventBus) {
//        CREATIVE_MODE_TAB.register(eventBus);

    }
}
