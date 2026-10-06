package com.ber.nimblePattern.item;

import com.ber.nimblePattern.NimblePattern;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NimblePattern.MOD_ID);

    public static final RegistryObject<CreativeModeTab> NIMBLE_PATTERN_TAB = CREATIVE_MODE_TAB.register("nimble_pattern_tab", () -> CreativeModeTab.builder()
            .icon(() -> ModItems.PATTERN_TAG_TERMINAL.get().getDefaultInstance())
            .title(Component.translatable("itemGroup.nimble_pattern_tab")).
            displayItems((pParameters, pOutput) -> {
                pOutput.accept(ModItems.PATTERN_TAG_TERMINAL.get());
                pOutput.accept(ModItems.LOOP_STORAGE_CELL_1K.get());
                pOutput.accept(ModItems.LOOP_STORAGE_CELL_4K.get());
                pOutput.accept(ModItems.LOOP_STORAGE_CELL_16K.get());
                pOutput.accept(ModItems.LOOP_STORAGE_CELL_64K.get());
                pOutput.accept(ModItems.LOOP_STORAGE_CELL_256K.get());
            }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
