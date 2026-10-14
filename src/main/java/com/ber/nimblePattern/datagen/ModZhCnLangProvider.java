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
        add("gui.nimble_pattern.pattern_tag_terminal.tool.success", "工具样板标记成功");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.failure", "工具样板标记失败");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.missing_tools", "请在工具槽中放入要标记的物品。");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.invalid_pattern", "请放入有效样板，且样板中的数量必须有效。");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.crafting_only", "仅支持工作台合成样板。");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.missing_input", "所选工具必须是每张样板的输入材料，且物品数据必须完全一致。");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.invalid_tool", "工具必须无法损坏并在合成后原样返还，或产出同种物品且数量多于投入（如模板复制）。");
        add("gui.nimble_pattern.pattern_tag_terminal.tool.loop_conflict", "请先在循环页签清除循环标记，再标记工具样板。");
        add("itemGroup.nimble_pattern_tab", "灵巧样板");

        add("item.nimble_pattern.pattern_tag_terminal", "样板标记终端");
        add("gui.nimble_pattern.pattern_tag_terminal.search_tooltip_condition", "用 % 按更新条件搜索(%UHV)");
        add("gui.nimble_pattern.pattern_tag_terminal.search_tooltip_status", "用 ~ 按更新状态搜索(~UPGRADE)");
        add("gui.nimble_pattern.pattern_tag_terminal.clear", "清除");
        add("gui.nimble_pattern.pattern_tag_terminal.apply", "应用");
        add("gui.nimble_pattern.pattern_tag_terminal.tab.upgrade", "更新样板");
        add("gui.nimble_pattern.pattern_tag_terminal.tab.loop", "循环样板");
        add("gui.nimble_pattern.pattern_tag_terminal.tab.tool", "工具样板");

        add("tooltip.nimble_pattern.fake_pattern", "假样板");
        add("tooltip.nimble_pattern.condition", "更新条件：%s");
        add("tooltip.nimble_pattern.state.UNTRACKED", "更新状态：未追踪");
        add("tooltip.nimble_pattern.state.LATEST", "更新状态：最新");
        add("tooltip.nimble_pattern.state.UPGRADE", "更新状态：可更新");
        add("toast.nimble_pattern.pattern_upgrade_title", "样板可更新");
        add("toast.nimble_pattern.pattern_ugprade_content", "%s已获得，%d个相关样板可更新");

        add("tooltip.nimble_pattern.loop_pattern", "循环样板");
        add("item.nimble_pattern.loop_storage_cell_1k", "1k 循环存储元件");
        add("item.nimble_pattern.loop_storage_cell_4k", "4k 循环存储元件");
        add("item.nimble_pattern.loop_storage_cell_16k", "16k 循环存储元件");
        add("item.nimble_pattern.loop_storage_cell_64k", "64k 循环存储元件");
        add("item.nimble_pattern.loop_storage_cell_256k", "256k 循环存储元件");
        // 标记循环样板的错误信息
        add("gui.nimble_pattern.tag_loop_pattern_failure", "循环样板标记失败");
        add("gui.nimble_pattern.missing_loop_storage_cell", "缺少循环存储元件");
        add("gui.nimble_pattern.loop_storage_cell_not_enough_capacity", "循环存储元件容量不足");
        add("gui.nimble_pattern.loop_amount_overflow", "循环样板数值计算溢出");
        add("gui.nimble_pattern.missing_network", "终端未连接到可用的网络");
        add("gui.nimble_pattern.return_seed_failure", "无法将循环种子放回网络");
        add("gui.nimble_pattern.wrong_loop_storage_cell", "错误的循环存储元件");
        add("gui.nimble_pattern.invalid_loop_data", "循环数据无效或同组样板记录不一致");
        add("gui.nimble_pattern.no_loop_patterns", "没有循环样板");
        add("gui.nimble_pattern.incomplete_loop", "请放入同一循环的全部样板后再清理");
        add("gui.nimble_pattern.missing_input", "存在没有可用主输入的样板");
        add("gui.nimble_pattern.missing_endpoints", "请同时标记循环的入口和出口");
        add("gui.nimble_pattern.processing_only", "仅支持处理样板");
        add("gui.nimble_pattern.missing_primary_output", "存在没有主产物的样板");
        add("gui.nimble_pattern.too_few_patterns", "循环产线至少需要两个样板");
        add("gui.nimble_pattern.endpoints_not_outputs", "标记的入口和出口必须是某个样板的主产物");
        add("gui.nimble_pattern.ambiguous_path", "无法确定循环路径");
        add("gui.nimble_pattern.broken_path", "所选样板无法组成连续的循环路径");
        add("gui.nimble_pattern.unbalanced_intermediate", "相邻样板的中间产物数量不匹配");
        add("gui.nimble_pattern.unused_patterns", "存在多余的未使用样板");
        add("gui.nimble_pattern.not_closed", "所选样板无法回到循环入口");
        add("gui.nimble_pattern.wrong_exit", "标记的出口不是闭环前的主产物");
        add("gui.nimble_pattern.no_net_output", "该循环产线没有净产出");
        // 标记循环样板成功的信息
        add("gui.nimble_pattern.tag_loop_pattern_success", "循环样板标记成功");
        // 循环样板的合成信息
        add("toast.nimble_pattern.loop_seed_return_failed_title", "循环样板启动材料无法放回");
        add("toast.nimble_pattern.loop_seed_return_failed_content", "%s无法放回，请检查相应的循环存储元件。");
        add("toast.nimble_pattern.loop_seed_lost_title", "循环样板启动材料丢失");
        add("toast.nimble_pattern.loop_seed_lost_content", "%s丢失，请及时补充循环存储元件。");

        add("tooltip.nimble_pattern.tool_pattern", "工具样板");
        add("gui.nimble_pattern.tag_tool_failure", "工具样板标记失败");
        add("gui.nimble_pattern.no_tool", "工具未选择");
        add("gui.nimble_pattern.crafting_only", "仅支持合成样板");
        add("gui.nimble_pattern.tool_changed", "工具合成后会有变化");
        add("gui.nimble_pattern.tool_is_not_input", "选定工具不是样板的输入");
        add("gui.nimble_pattern.tool_is_damageable", "工具会损坏");
    }
}
