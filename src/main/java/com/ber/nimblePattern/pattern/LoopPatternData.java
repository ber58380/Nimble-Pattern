package com.ber.nimblePattern.pattern;

import appeng.api.stacks.GenericStack;

import java.util.List;
import java.util.UUID;

public record LoopPatternData(
        UUID groupId,
        UUID storageCellId,
        int index,
        int size,
        GenericStack entry,
        GenericStack exit,
        long seedAmount,
        long netOutputAmount,
        List<GenericStack> externalInputs,
        List<GenericStack> mainOutputs) {

    public LoopPatternData {
        externalInputs = List.copyOf(externalInputs);
        mainOutputs = List.copyOf(mainOutputs);
    }

    public boolean isClosingPattern() {
        return index == size - 1;
    }
}
