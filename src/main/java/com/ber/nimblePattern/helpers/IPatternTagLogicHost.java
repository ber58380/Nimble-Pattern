package com.ber.nimblePattern.helpers;

import com.ber.nimblePattern.parts.PatternTagLogic;
import net.minecraft.world.level.Level;

public interface IPatternTagLogicHost {
    PatternTagLogic getLogic();

    Level getLevel();

    void markForSave();
}
