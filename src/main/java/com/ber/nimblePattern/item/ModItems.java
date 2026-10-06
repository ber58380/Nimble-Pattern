package com.ber.nimblePattern.item;

import appeng.items.parts.PartItem;
import com.ber.nimblePattern.NimblePattern;
import com.ber.nimblePattern.item.storage.LoopStorageCellItem;
import com.ber.nimblePattern.item.storage.LoopStorageTier;
import com.ber.nimblePattern.parts.PatternTagTerminalPart;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NimblePattern.MOD_ID);

    public static final RegistryObject<Item> PATTERN_TAG_TERMINAL = ITEMS.register("pattern_tag_terminal",
            () -> new PartItem<>(
                    new Item.Properties(),
                    PatternTagTerminalPart.class,
                    PatternTagTerminalPart::new));

    private static RegistryObject<LoopStorageCellItem> makeLoopStorageCell(LoopStorageTier tier) {
        return ITEMS.register(tier.registryName(), () -> new LoopStorageCellItem(tier));
    }

    public static final RegistryObject<LoopStorageCellItem> LOOP_STORAGE_CELL_1K = makeLoopStorageCell(LoopStorageTier.SIZE_1K);
    public static final RegistryObject<LoopStorageCellItem> LOOP_STORAGE_CELL_4K = makeLoopStorageCell(LoopStorageTier.SIZE_4K);
    public static final RegistryObject<LoopStorageCellItem> LOOP_STORAGE_CELL_16K = makeLoopStorageCell(LoopStorageTier.SIZE_16K);
    public static final RegistryObject<LoopStorageCellItem> LOOP_STORAGE_CELL_64K = makeLoopStorageCell(LoopStorageTier.SIZE_64K);
    public static final RegistryObject<LoopStorageCellItem> LOOP_STORAGE_CELL_256K = makeLoopStorageCell(LoopStorageTier.SIZE_256K);
}