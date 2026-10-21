package com.ber.nimblePattern.helpers;

import appeng.api.util.IConfigurableObject;
import com.ber.nimblePattern.parts.PatternTagLogic;
import net.minecraft.world.level.Level;

public interface IPatternTagLogicHost extends IConfigurableObject {
    PatternTagLogic getLogic();

    Level getLevel();

    void markForSave();
}
