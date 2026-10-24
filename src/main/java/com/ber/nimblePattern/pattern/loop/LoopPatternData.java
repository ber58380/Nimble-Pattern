package com.ber.nimblePattern.pattern.loop;

import appeng.api.stacks.GenericStack;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record LoopPatternData(
        UUID loopId,
        UUID storageCellId,
        int index,
        int size,
        GenericStack entry,
        GenericStack exit,
        long seedAmount,
        long netOutputAmount,
        List<GenericStack> externalInputs,
        List<GenericStack> outputs,
        List<Long> operations
) {
    public LoopPatternData(UUID loopId, UUID storageCellId, int index, int size,
                           GenericStack entry, GenericStack exit, long seedAmount, long netOutputAmount,
                           List<GenericStack> externalInputs, List<GenericStack> outputs) {
        this(loopId, storageCellId, index, size, entry, exit, seedAmount, netOutputAmount,
                externalInputs, outputs, Collections.nCopies(size, 1L));
    }

    public LoopPatternData {
        Objects.requireNonNull(loopId);
        Objects.requireNonNull(storageCellId);
        Objects.requireNonNull(entry);
        Objects.requireNonNull(exit);
        externalInputs = List.copyOf(externalInputs);
        outputs = List.copyOf(outputs);
        operations = List.copyOf(operations);
        if (operations.size() != size || operations.stream().anyMatch(n -> n <= 0)
                || size < 2 || index < 0 || index >= size || outputs.size() != size
                || seedAmount <= 0 || netOutputAmount <= 0
                || entry.amount() <= 0 || exit.amount() <= 0
                || externalInputs.stream().anyMatch(stack -> stack.amount() <= 0)
                || outputs.stream().anyMatch(stack -> stack.amount() <= 0)) {
            throw new IllegalArgumentException("Invalid loop pattern data");
        }
    }

    public boolean isClosingPattern() {
        return index == size - 1;
    }
}
