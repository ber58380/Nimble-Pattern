package com.ber.nimblePattern.menu;

import appeng.menu.SlotSemantic;

import static appeng.menu.SlotSemantics.register;

public final class SlotSemantics {
    public static final SlotSemantic INPUT_PATTERN = register("INPUT_PATTERN", false);
    public static final SlotSemantic UPGRADE_CONDITION = register("UPGRADE_CONDITION", false);
    public static final SlotSemantic LOOP_INPUT = register("LOOP_INPUT", false);
    public static final SlotSemantic LOOP_OUTPUT = register("LOOP_OUTPUT", false);
    public static final SlotSemantic LOOP_STORAGE_CELL = register("LOOP_STORAGE_CELL", false);
    public static final SlotSemantic TOOL_INPUT = register("TOOL_INPUT", false);
}
