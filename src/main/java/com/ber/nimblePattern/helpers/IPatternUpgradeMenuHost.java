package com.ber.nimblePattern.helpers;

import appeng.api.util.IConfigurableObject;
import com.ber.nimblePattern.parts.PatternUpgradeLogic;

public interface IPatternUpgradeMenuHost extends IConfigurableObject {
    PatternUpgradeLogic getLogic();
}
