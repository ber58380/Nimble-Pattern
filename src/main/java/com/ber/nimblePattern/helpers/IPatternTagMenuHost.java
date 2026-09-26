package com.ber.nimblePattern.helpers;

import appeng.api.util.IConfigurableObject;
import com.ber.nimblePattern.parts.PatternTagLogic;

public interface IPatternTagMenuHost extends IConfigurableObject {
    PatternTagLogic getLogic();
}
