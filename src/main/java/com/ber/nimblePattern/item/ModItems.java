package com.ber.nimblePattern.item;

import appeng.items.parts.PartItem;
import com.ber.nimblePattern.NimblePattern;
import com.ber.nimblePattern.item.storage.LoopStorageCell1kItem;
import com.ber.nimblePattern.item.storage.LoopStorageCell4kItem;
import com.ber.nimblePattern.item.storage.LoopStorageCell16kItem;
import com.ber.nimblePattern.item.storage.LoopStorageCell64kItem;
import com.ber.nimblePattern.item.storage.LoopStorageCell256kItem;
import com.ber.nimblePattern.parts.PatternTagTerminalPart;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NimblePattern.MOD_ID);

    public static final RegistryObject<Item> PATTERN_TAG_TERMINAL = ITEMS.register("pattern_tag_terminal",
            () -> new PartItem<>(
                    new Item.Properties(),
                    PatternTagTerminalPart.class,
                    PatternTagTerminalPart::new));

    public static final RegistryObject<LoopStorageCell1kItem> LOOP_STORAGE_CELL_1K =
            ITEMS.register("loop_storage_cell_1k", LoopStorageCell1kItem::new);
    public static final RegistryObject<LoopStorageCell4kItem> LOOP_STORAGE_CELL_4K =
            ITEMS.register("loop_storage_cell_4k", LoopStorageCell4kItem::new);
    public static final RegistryObject<LoopStorageCell16kItem> LOOP_STORAGE_CELL_16K =
            ITEMS.register("loop_storage_cell_16k", LoopStorageCell16kItem::new);
    public static final RegistryObject<LoopStorageCell64kItem> LOOP_STORAGE_CELL_64K =
            ITEMS.register("loop_storage_cell_64k", LoopStorageCell64kItem::new);
    public static final RegistryObject<LoopStorageCell256kItem> LOOP_STORAGE_CELL_256K =
            ITEMS.register("loop_storage_cell_256k", LoopStorageCell256kItem::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
