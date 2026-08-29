package com.ber.nimblePattern.item;

import appeng.items.parts.PartItem;
import com.ber.nimblePattern.NimblePattern;
import com.ber.nimblePattern.parts.PatternUpgradeTerminalPart;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NimblePattern.MOD_ID);

    public static final RegistryObject<Item> PATTERN_UPGRADE_TERMINAL = ITEMS.register("pattern_upgrade_terminal",
            () -> new PartItem<>(
                    new Item.Properties(),
                    PatternUpgradeTerminalPart.class,
                    PatternUpgradeTerminalPart::new));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}