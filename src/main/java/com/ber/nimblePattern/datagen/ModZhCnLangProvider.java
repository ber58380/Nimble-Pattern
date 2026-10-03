package com.ber.nimblePattern.datagen;

import com.ber.nimblePattern.NimblePattern;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ModZhCnLangProvider extends LanguageProvider {
    public ModZhCnLangProvider(PackOutput output) {
        super(output, NimblePattern.MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add("tooltip.nimble_pattern.probability", "概率样板");
        add("toast.nimble_pattern.probability_failed_title", "概率合成失败");
        add("toast.nimble_pattern.probability_failed_content", "%s概率合成缺失材料");
        add("gui.nimble_pattern.pattern_tag_terminal.tab.tool", "工具样板");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.success", "工具样板应用成功");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.crafting_only", "工具样板目前只支持合成样板，不支持处理样板");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.not_returned", "该配方不会原样返还工具，无法重复使用");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.missing_tools", "请至少标记一种工具");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.missing_input", "标记的工具必须是每个样板的输入材料");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.invalid_tool", "工具必须无限耐久，或在该配方中有净增产");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.invalid_pattern", "请放入有效的编码样板");
        add("item.nimble_pattern.pattern_tag_terminal", "样板标记终端");
        add("item.nimble_pattern.loop_storage_cell_1k", "1k 循环存储元件");
        add("item.nimble_pattern.loop_storage_cell_4k", "4k 循环存储元件");
        add("item.nimble_pattern.loop_storage_cell_16k", "16k 循环存储元件");
        add("item.nimble_pattern.loop_storage_cell_64k", "64k 循环存储元件");
        add("item.nimble_pattern.loop_storage_cell_256k", "256k 循环存储元件");
        add("gui.nimble_pattern.pattern_tag_terminal.search_tooltip_condition", "用 % 按更新条件搜索(%UHV)");
        add("gui.nimble_pattern.pattern_tag_terminal.search_tooltip_status", "用 ~ 按更新状态搜索(~UPGRADE)");
        add("gui.nimble_pattern.pattern_tag_terminal.conditions", "更新条件");
        add("gui.nimble_pattern.pattern_tag_terminal.clear", "清除");
        add("gui.nimble_pattern.pattern_tag_terminal.apply", "应用");
        add("gui.nimble_pattern.pattern_tag_terminal.tab.upgrade", "更新样板");
        add("gui.nimble_pattern.pattern_tag_terminal.tab.loop", "循环样板");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.success", "循环样板应用成功");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.missing_cell", "请放入循环存储元件");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.missing_endpoints", "请同时标记循环入口和出口");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.processing_only", "循环产线只能包含处理样板");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.too_few_patterns", "循环产线至少需要两个样板");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.missing_primary_output", "存在没有主产物的样板");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.endpoints_not_outputs", "标记的入口和出口必须是样板的主产物");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.missing_input", "存在没有可用主输入的样板");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.ambiguous_path", "有多个样板消耗同一种循环物品，无法确定路径");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.broken_path", "所选样板无法组成连续的循环路径");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.unbalanced_intermediate", "相邻样板的中间产物数量不匹配");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.unused_patterns", "存在不属于所标记循环的样板");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.not_closed", "所选样板无法回到循环入口");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.wrong_exit", "标记的出口不是闭环前的主产物");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.no_net_output", "该循环产线不会净增循环入口物品");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.cell_capacity", "循环存储元件无法再预留所需的入口物品");
        add("gui.nimble_pattern.pattern_tag_terminal.loop.amount_overflow", "样板中的数量过大");
        add("tooltip.nimble_pattern.condition", "更新条件：%s");
        add("tooltip.nimble_pattern.state.UNTRACKED", "更新状态：未追踪");
        add("tooltip.nimble_pattern.state.LATEST", "更新状态：最新");
        add("tooltip.nimble_pattern.state.UPDATE", "更新状态：可更新");
        add("toast.nimble_pattern.pattern_upgrade_title", "样板可更新");
        add("toast.nimble_pattern.pattern_ugprade_content", "%s已获得，%d个相关样板可更新");
        add("toast.nimble_pattern.loop_seed_lost_title", "循环样板启动材料丢失");
        add("toast.nimble_pattern.loop_seed_lost_content", "%s丢失，请及时补充循环存储元件。");
    }
}
