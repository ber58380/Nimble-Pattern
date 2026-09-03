package com.ber.nimblePattern;

import appeng.api.ids.AECreativeTabIds;
import appeng.api.parts.PartModels;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.items.parts.PartModelsHelper;
import com.ber.nimblePattern.compat.extendedae.ExtendedAECompat;
import com.ber.nimblePattern.item.ModItems;
import com.ber.nimblePattern.menu.PatternUpgradeTermMenu;
import com.ber.nimblePattern.network.NimblePatternNetwork;
import com.ber.nimblePattern.parts.PatternUpgradeTerminalPart;
import com.ber.nimblePattern.pattern.PatternUpgradeTracker;
import com.glodblock.github.extendedae.common.EPPItemAndBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(NimblePattern.MOD_ID)
public class NimblePattern {
    public static final String MOD_ID = "nimble_pattern";

    public NimblePattern(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);

        ModItems.register(modEventBus);

        // trigger initialization to wait in ae2 registration queue
        PatternUpgradeTermMenu.TYPE.toString();

        NimblePatternNetwork.init();
        // register part models in ae2
        PartModels.registerModels(PartModelsHelper.createModels(PatternUpgradeTerminalPart.class));

        modEventBus.addListener(this::addCreative);

        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        String patternProviderGroup = "gui.ae2.CraftingInterface";
        Upgrades.add(AEItems.FUZZY_CARD, AEBlocks.PATTERN_PROVIDER, 1, patternProviderGroup);
        Upgrades.add(AEItems.FUZZY_CARD, AEParts.PATTERN_PROVIDER, 1, patternProviderGroup);
        if (ExtendedAECompat.LOADED) {
            Upgrades.add(AEItems.FUZZY_CARD, EPPItemAndBlock.EX_PATTERN_PROVIDER, 1, patternProviderGroup);
            Upgrades.add(AEItems.FUZZY_CARD, EPPItemAndBlock.EX_PATTERN_PROVIDER_PART, 1, patternProviderGroup);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            PatternUpgradeTracker.instance().updateStatus();
        }
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // 暂时先添加到ae2创造物品栏，后续再创建模组的物品栏
        if (event.getTabKey() == AECreativeTabIds.MAIN) {
            event.accept(ModItems.PATTERN_UPGRADE_TERMINAL);
        }
    }
}
