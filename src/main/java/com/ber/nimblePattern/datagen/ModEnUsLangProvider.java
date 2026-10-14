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
        add("gui.nimble_pattern.pattern_tag_terminal.tool.success", "Tool patterns marked successfully");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.failure", "Failed to mark tool patterns");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.missing_tools", "Place the item to mark in the tool slot.");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.invalid_pattern", "Insert valid patterns with valid ingredient amounts.");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.crafting_only", "Only crafting patterns are supported.");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.missing_input", "The selected tool must be an input of every pattern with identical item data.");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.invalid_tool", "The tool must be non-damageable and returned unchanged, or the recipe must produce more of the exact item than it consumes (such as template duplication).");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.loop_conflict", "Clear the loop marking in the Loop tab before marking tools.");
        add("itemGroup.nimble_pattern_tab", "Nimble Pattern");

        add("item.nimble_pattern.pattern_tag_terminal", "Pattern Tag Terminal");
        add("gui.nimble_pattern.pattern_tag_terminal.search_tooltip_condition", "Use % to search by conditions (%UHV)");
        add("gui.nimble_pattern.pattern_tag_terminal.search_tooltip_status", "Use ~ to search by status (~UPGRADE)");
        add("gui.nimble_pattern.pattern_tag_terminal.clear", "Clear");
        add("gui.nimble_pattern.pattern_tag_terminal.apply", "Apply");
        add("gui.nimble_pattern.pattern_tag_terminal.tab.upgrade", "Upgrade Patterns");
        add("gui.nimble_pattern.pattern_tag_terminal.tab.loop", "Loop Patterns");
        add("gui.nimble_pattern.pattern_tag_terminal.tab.tool", "Tool Patterns");

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
        // Failure information of tagging loop patterns
        add("gui.nimble_pattern.tag_loop_pattern_failure", "Fail to tag loop patterns");
        add("gui.nimble_pattern.missing_loop_storage_cell", "Miss a loop storage cell");
        add("gui.nimble_pattern.loop_storage_cell_not_enough_capacity", "The loop storage cell doesn't have enough capacity");
        add("gui.nimble_pattern.loop_amount_overflow", "The calculation of loop patterns is overflow");
        add("gui.nimble_pattern.missing_network", "The terminal is not connected to an active network");
        add("gui.nimble_pattern.return_seed_failure", "Fail to return loop seeds to the network");
        add("gui.nimble_pattern.wrong_loop_storage_cell", "Wrong loop storage cell");
        add("gui.nimble_pattern.invalid_loop_data", "Invalid or inconsistent loop metadata");
        add("gui.nimble_pattern.no_loop_patterns", "No loop patterns");
        add("gui.nimble_pattern.incomplete_loop", "Insert every pattern in the selected loop before clearing");
        add("gui.nimble_pattern.missing_input", "A pattern has no usable primary input");
        add("gui.nimble_pattern.missing_endpoints", "Mark both the loop input and output");
        add("gui.nimble_pattern.processing_only", "Only support processing patterns");
        add("gui.nimble_pattern.missing_primary_output", "A pattern has no primary output");
        add("gui.nimble_pattern.too_few_patterns", "A loop requires at least two patterns");
        add("gui.nimble_pattern.endpoints_not_outputs", "The marked endpoints must be primary pattern outputs");
        add("gui.nimble_pattern.ambiguous_path", "Fail to find the loop path of patterns");
        add("gui.nimble_pattern.broken_path", "The selected patterns do not form a continuous loop");
        add("gui.nimble_pattern.unbalanced_intermediate", "Adjacent patterns use different intermediate amounts");
        add("gui.nimble_pattern.unused_patterns", "There are extra patterns not be used");
        add("gui.nimble_pattern.not_closed", "The selected patterns do not return to the loop input");
        add("gui.nimble_pattern.wrong_exit", "The marked output is not directly before the loop closes");
        add("gui.nimble_pattern.no_net_output", "The loop does not have net outputs");
        // Successful information of tagging loop patterns
        add("gui.nimble_pattern.tag_loop_pattern_success", "Success to tag loop patterns");
        // Crafting information of loop patterns
        add("toast.nimble_pattern.loop_seed_return_failed_title", "Loop seed could not be returned");
        add("toast.nimble_pattern.loop_seed_return_failed_content", "%s could not be returned. Please check the corresponding loop storage cell.");
        add("toast.nimble_pattern.loop_seed_lost_title", "Loop Pattern Seed Lost");
        add("toast.nimble_pattern.loop_seed_lost_content", "%s was lost. Refill the loop storage cell promptly.");

        add("tooltip.nimble_pattern.tool_pattern", "Tool Pattern");
        add("gui.nimble_pattern.tag_tool_failure", "Fail to tag tool patterns");
        add("gui.nimble_pattern.no_tool", "Tool not selected");
        add("gui.nimble_pattern.crafting_only", "Only support crafting patterns");
        add("gui.nimble_pattern.tool_changed", "The tool would have changes after crafting");
        add("gui.nimble_pattern.tool_is_not_input", "Selected tool is not the input of the pattern");
        add("gui.nimble_pattern.tool_is_damageable", "Tool is damageable");
    }
}
