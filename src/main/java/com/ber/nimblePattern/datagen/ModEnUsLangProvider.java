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
        add("item.nimble_pattern.pattern_upgrade_terminal", "Pattern Upgrade Terminal");
        add("gui.nimble_pattern.pattern_upgrade_terminal.search_tooltip_condition", "Use % to search by conditions (%UHV)");
        add("gui.nimble_pattern.pattern_upgrade_terminal.search_tooltip_status", "Use ~ to search by status (~UPGRADE)");
        add("gui.nimble_pattern.pattern_upgrade_terminal.conditions", "Upgrade Conditions");
        add("gui.nimble_pattern.pattern_upgrade_terminal.clear", "Clear");
        add("gui.nimble_pattern.pattern_upgrade_terminal.apply", "Apply");
        add("tooltip.nimble_pattern.condition", "Update condition: %s");
        add("tooltip.nimble_pattern.state.UNTRACKED", "Update state: Untracked");
        add("tooltip.nimble_pattern.state.LATEST", "Update state: Latest");
        add("tooltip.nimble_pattern.state.UPDATE", "Update state: Update available");
        add("toast.nimble_pattern.pattern_upgrade_title", "Upgrade of patterns are available");
        add("toast.nimble_pattern.pattern_ugprade_content", "%s obtained, %d related patterns are available for upgrade");
    }
}
