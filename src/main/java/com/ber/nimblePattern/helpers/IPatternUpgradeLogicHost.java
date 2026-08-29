package com.ber.nimblePattern.helpers;

import com.ber.nimblePattern.parts.PatternUpgradeLogic;
import net.minecraft.world.level.Level;

public interface IPatternUpgradeLogicHost {
    PatternUpgradeLogic getLogic();

    Level getLevel();

    void markForSave();
}
