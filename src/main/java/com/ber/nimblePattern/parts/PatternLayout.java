package com.ber.nimblePattern.parts;

import appeng.api.config.Setting;

public enum PatternLayout {
    GRID, GROUPED;

    public static final Setting<PatternLayout> SETTING = new Setting<>("pattern_tag_terminal_layout", PatternLayout.class);
}
