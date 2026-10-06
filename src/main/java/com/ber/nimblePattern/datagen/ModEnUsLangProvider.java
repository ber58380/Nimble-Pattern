package com.ber.nimblePattern.datagen;

import com.ber.nimblePattern.NimblePattern;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ModEnUsLangProvider extends LanguageProvider {
    public ModEnUsLangProvider(PackOutput output) {
        super(output, NimblePattern.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.nimble_pattern_tab", "Nimble Pattern");
        
        add("item.nimble_pattern.pattern_tag_terminal", "Pattern Tag Terminal");
        add("gui.nimble_pattern.pattern_tag_terminal.search_tooltip_condition", "Use % to search by conditions (%UHV)");
        add("gui.nimble_pattern.pattern_tag_terminal.search_tooltip_status", "Use ~ to search by status (~UPGRADE)");
        add("gui.nimble_pattern.pattern_tag_terminal.clear", "Clear");
        add("gui.nimble_pattern.pattern_tag_terminal.apply", "Apply");
        add("gui.nimble_pattern.pattern_tag_terminal.tab.upgrade", "Upgrade Patterns");
        add("gui.nimble_pattern.pattern_tag_terminal.tab.loop", "Loop Patterns");
        add("tooltip.nimble_pattern.condition", "Upgrade condition: %s");
        add("tooltip.nimble_pattern.state.UNTRACKED", "Upgrade state: Untracked");
        add("tooltip.nimble_pattern.state.LATEST", "Upgrade state: Latest");
        add("tooltip.nimble_pattern.state.UPDATE", "Upgrade state: Upgrade available");
        add("toast.nimble_pattern.pattern_upgrade_title", "Upgrade of patterns are available");
        add("toast.nimble_pattern.pattern_ugprade_content", "%s obtained, %d related patterns are available for upgrade");

        add("item.nimble_pattern.loop_storage_cell_1k", "1k Loop Storage Cell");
        add("item.nimble_pattern.loop_storage_cell_4k", "4k Loop Storage Cell");
        add("item.nimble_pattern.loop_storage_cell_16k", "16k Loop Storage Cell");
        add("item.nimble_pattern.loop_storage_cell_64k", "64k Loop Storage Cell");
        add("item.nimble_pattern.loop_storage_cell_256k", "256k Loop Storage Cell");
    }
}
