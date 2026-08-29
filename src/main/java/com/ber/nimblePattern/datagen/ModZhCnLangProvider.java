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
        add("item.nimble_pattern.pattern_upgrade_terminal", "样板更新终端");
        add("gui.nimble_pattern.pattern_upgrade_terminal.search_tooltip_condition", "用 % 按更新条件搜索(%UHV)");
        add("gui.nimble_pattern.pattern_upgrade_terminal.search_tooltip_status", "用 ~ 按更新状态搜索(~UPGRADE)");
        add("gui.nimble_pattern.pattern_upgrade_terminal.conditions", "更新条件");
        add("gui.nimble_pattern.pattern_upgrade_terminal.clear", "清除");
        add("gui.nimble_pattern.pattern_upgrade_terminal.apply", "应用");
        add("tooltip.nimble_pattern.condition", "更新条件：%s");
        add("tooltip.nimble_pattern.state.UNTRACKED", "更新状态：未追踪");
        add("tooltip.nimble_pattern.state.LATEST", "更新状态：最新");
        add("tooltip.nimble_pattern.state.UPDATE", "更新状态：可更新");
        add("toast.nimble_pattern.pattern_upgrade_title", "样板可更新");
        add("toast.nimble_pattern.pattern_ugprade_content", "%s已获得，%d个相关样板可更新");
    }
}
