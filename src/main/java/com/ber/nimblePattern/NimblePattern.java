package com.ber.nimblePattern;

import appeng.api.parts.PartModels;
import appeng.api.storage.StorageCells;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.items.parts.PartModelsHelper;
import com.ber.nimblePattern.compat.extendedae.ExtendedAECompat;
import com.ber.nimblePattern.item.ModCreativeModeTabs;
import com.ber.nimblePattern.item.ModItems;
import com.ber.nimblePattern.item.storage.LoopStorageCellHandler;
import com.ber.nimblePattern.menu.PatternTagTermMenu;
import com.ber.nimblePattern.network.NimblePatternNetwork;
import com.ber.nimblePattern.parts.PatternTagTerminalPart;
import com.ber.nimblePattern.pattern.PatternMapping;
import com.ber.nimblePattern.pattern.upgrade.PatternUpgradeTracker;
import com.glodblock.github.extendedae.common.EPPItemAndBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
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
        ModCreativeModeTabs.register(modEventBus);

        // trigger initialization to wait in ae2 registration queue
        PatternTagTermMenu.TYPE.toString();

        NimblePatternNetwork.init();
        // register part models in ae2
        PartModels.registerModels(PartModelsHelper.createModels(PatternTagTerminalPart.class));

        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        StorageCells.addCellHandler(LoopStorageCellHandler.INSTANCE);

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
        PatternUpgradeTracker.instance().clear();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        PatternUpgradeTracker.instance().clear();
        PatternMapping.clear();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            PatternUpgradeTracker.instance().updateStatus();
        }
    }

}
