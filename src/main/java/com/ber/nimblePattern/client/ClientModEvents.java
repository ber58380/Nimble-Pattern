package com.ber.nimblePattern.client;

import appeng.api.util.AEColor;
import appeng.client.gui.style.StyleManager;
import appeng.client.render.StaticItemColor;
import com.ber.nimblePattern.NimblePattern;
import com.ber.nimblePattern.client.gui.PatternUpgradeTermScreen;
import com.ber.nimblePattern.item.ModItems;
import com.ber.nimblePattern.menu.PatternUpgradeTermMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = NimblePattern.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.<PatternUpgradeTermMenu, PatternUpgradeTermScreen<PatternUpgradeTermMenu>>register(
                    PatternUpgradeTermMenu.TYPE,
                    (menu, inv, title) -> new PatternUpgradeTermScreen<>(
                            menu, inv, title, StyleManager.loadStyleDoc("/screens/terminals/pattern_upgrade_terminal.json"))
            );
        });
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // register color for pattern upgrade terminal in player's inventory
        event.getItemColors().register(
                new StaticItemColor(AEColor.TRANSPARENT),
                ModItems.PATTERN_UPGRADE_TERMINAL.get()
        );
    }
}
