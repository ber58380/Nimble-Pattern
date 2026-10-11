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
        add("gui.nimble_pattern.pattern_tag_terminal.loop.clear_success", "Loop markings and their seed reservations cleared");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.wrong_cell", "Every selected loop must be bound to the inserted cell");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.incomplete_loop", "Insert every pattern in each selected loop before clearing");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.invalid_loop_data", "Invalid or inconsistent loop metadata");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.no_loop_patterns", "No loop patterns to clear");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.missing_network", "The terminal is not connected to an active network");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.invalid_cell_config", "The cell has insufficient or invalid seed reservations");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.return_blocked", "The network could not accept all released materials; markings and configuration were kept");
        add("toast.nimble_pattern.loop_seed_return_failed_title", "Loop seed could not be returned");
        add("toast.nimble_pattern.loop_seed_return_failed_content", "%s could not be returned. Please check the corresponding loop storage cell.");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.success", "Loop patterns applied");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.missing_cell", "Insert a loop storage cell");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.missing_endpoints", "Mark both the loop input and output");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.processing_only", "The loop can only contain processing patterns");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.too_few_patterns", "A loop requires at least two patterns");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.missing_primary_output", "A pattern has no primary output");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.endpoints_not_outputs", "The marked endpoints must be primary pattern outputs");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.missing_input", "A pattern has no usable primary input");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.ambiguous_path", "More than one pattern consumes the same loop material");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.broken_path", "The selected patterns do not form a continuous loop");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.unbalanced_intermediate", "Adjacent patterns use different intermediate amounts");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.unused_patterns", "Some selected patterns are outside the marked loop");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.not_closed", "The selected patterns do not return to the loop input");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.wrong_exit", "The marked output is not directly before the loop closes");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.no_net_output", "The loop does not produce a net gain of its input");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.cell_capacity", "The loop storage cell cannot reserve the required input");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.amount_overflow", "A pattern amount is too large");
        add("toast.nimble_pattern.loop_seed_lost_title", "Loop Pattern Seed Lost");
        add("toast.nimble_pattern.loop_seed_lost_content", "%s was lost. Refill the loop storage cell promptly.");
        add("itemGroup.nimble_pattern_tab", "Nimble Pattern");

        add("item.nimble_pattern.pattern_tag_terminal", "Pattern Tag Terminal");
        add("gui.nimble_pattern.pattern_tag_terminal.search_tooltip_condition", "Use % to search by conditions (%UHV)");
        add("gui.nimble_pattern.pattern_tag_terminal.search_tooltip_status", "Use ~ to search by status (~UPGRADE)");
        add("gui.nimble_pattern.pattern_tag_terminal.clear", "Clear");
        add("gui.nimble_pattern.pattern_tag_terminal.apply", "Apply");
        add("gui.nimble_pattern.pattern_tag_terminal.tab.upgrade", "Upgrade Patterns");
        add("gui.nimble_pattern.pattern_tag_terminal.tab.loop", "Loop Patterns");

        add("tooltip.nimble_pattern.fake_pattern", "Fake Pattern");
        add("tooltip.nimble_pattern.condition", "Upgrade condition: %s");
        add("tooltip.nimble_pattern.state.UNTRACKED", "Upgrade state: Untracked");
        add("tooltip.nimble_pattern.state.LATEST", "Upgrade state: Latest");
        add("tooltip.nimble_pattern.state.UPGRADE", "Upgrade state: Upgrade available");
        add("toast.nimble_pattern.pattern_upgrade_title", "Upgrade of patterns are available");
        add("toast.nimble_pattern.pattern_ugprade_content", "%s obtained, %d related patterns are available for upgrade");

        add("tooltip.nimble_pattern.loop_pattern", "Loop Pattern");
        add("item.nimble_pattern.loop_storage_cell_1k", "1k Loop Storage Cell");
        add("item.nimble_pattern.loop_storage_cell_4k", "4k Loop Storage Cell");
        add("item.nimble_pattern.loop_storage_cell_16k", "16k Loop Storage Cell");
        add("item.nimble_pattern.loop_storage_cell_64k", "64k Loop Storage Cell");
        add("item.nimble_pattern.loop_storage_cell_256k", "256k Loop Storage Cell");
    }
}
