package com.ber.nimblePattern.probability;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;

public interface ProbabilityTools {
    long nimble$toolsHeld(AEKey key);
    void nimble$reserveTools(KeyCounter tools);
    boolean nimble$specialOutputPending(AEKey key);
}
