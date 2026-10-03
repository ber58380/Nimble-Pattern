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
        add("tooltip.nimble_pattern.probability", "Probability Pattern");
        add("toast.nimble_pattern.probability_failed_title", "Probability Crafting Failed");
        add("toast.nimble_pattern.probability_failed_content", "Missing ingredients for probability crafting: %s");
        add("gui.nimble_pattern.pattern_tag_terminal.tab.tool", "Tool Patterns");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.success", "Tool patterns applied");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.crafting_only", "Tool patterns currently support crafting patterns only, not processing patterns");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.not_returned", "This recipe does not return the unchanged tool for reuse");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.missing_tools", "Mark at least one tool");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.missing_input", "Each marked tool must be an input of every pattern");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.invalid_tool", "Tools must be unbreakable or have a net gain in this recipe");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.invalid_pattern", "Insert valid encoded patterns");
        add("item.nimble_pattern.pattern_tag_terminal", "Pattern Tag Terminal");
        add("item.nimble_pattern.loop_storage_cell_1k", "1k Loop Storage Cell");
        add("item.nimble_pattern.loop_storage_cell_4k", "4k Loop Storage Cell");
        add("item.nimble_pattern.loop_storage_cell_16k", "16k Loop Storage Cell");
        add("item.nimble_pattern.loop_storage_cell_64k", "64k Loop Storage Cell");
        add("item.nimble_pattern.loop_storage_cell_256k", "256k Loop Storage Cell");
        add("gui.nimble_pattern.pattern_tag_terminal.search_tooltip_condition", "Use % to search by conditions (%UHV)");
        add("gui.nimble_pattern.pattern_tag_terminal.search_tooltip_status", "Use ~ to search by status (~UPGRADE)");
        add("gui.nimble_pattern.pattern_tag_terminal.conditions", "Upgrade Conditions");
        add("gui.nimble_pattern.pattern_tag_terminal.clear", "Clear");
        add("gui.nimble_pattern.pattern_tag_terminal.apply", "Apply");
        add("gui.nimble_pattern.pattern_tag_terminal.tab.upgrade", "Upgrade Patterns");
        add("gui.nimble_pattern.pattern_tag_terminal.tab.loop", "Loop Patterns");
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
        add("tooltip.nimble_pattern.condition", "Upgrade condition: %s");
        add("tooltip.nimble_pattern.state.UNTRACKED", "Upgrade state: Untracked");
        add("tooltip.nimble_pattern.state.LATEST", "Upgrade state: Latest");
        add("tooltip.nimble_pattern.state.UPDATE", "Upgrade state: Upgrade available");
        add("toast.nimble_pattern.pattern_upgrade_title", "Upgrade of patterns are available");
        add("toast.nimble_pattern.pattern_ugprade_content", "%s obtained, %d related patterns are available for upgrade");
        add("toast.nimble_pattern.loop_seed_lost_title", "Loop Pattern Seed Lost");
        add("toast.nimble_pattern.loop_seed_lost_content", "%s was lost. Refill the loop storage cell promptly.");
    }
}
