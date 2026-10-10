package com.ber.nimblePattern.pattern.loop;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;

public final class LoopPatternParser {
    private LoopPatternParser() {
    }

    private static Map<AEKey, Long> aggregateInputs(AEProcessingPattern pattern) throws ParseException {
        var result = new LinkedHashMap<AEKey, Long>();
        for (var input : pattern.getInputs()) {
            var possible = input.getPossibleInputs();
            if (possible.length == 0) {
                throw new ParseException("missing_input");
            }
            var primaryInput = possible[0];
            long amount;
            try {
                amount = Math.multiplyExact(primaryInput.amount(), input.getMultiplier());
            } catch (ArithmeticException e) {
                throw new ParseException("amount_overflow");
            }
            result.merge(primaryInput.what(), amount, Math::addExact);
        }
        return result;
    }

    public static LoopChain parse(Level level, List<ItemStack> patterns, GenericStack entry, GenericStack exit, UUID storageCellId) throws ParseException {
        if (entry == null || exit == null) {
            throw new ParseException("missing_endpoints");
        }

        var remaining = new ArrayList<LoopNode>();
        for (int index = 0; index < patterns.size(); index++) {
            var pattern = patterns.get(index);
            var details = PatternDetailsHelper.decodePattern(pattern, level);
            if (!(details instanceof AEProcessingPattern aep)) {
                throw new ParseException("processing_only");
            }
            var outputs = aep.getOutputs();
            if (outputs.length == 0) {
                throw new ParseException("missing_primary_output");
            }
            var output = outputs[0];
            remaining.add(new LoopNode(index, pattern.copy(), aggregateInputs(aep), output));
        }

        // The loop chain should have at least 2 patterns. If the loop only has 1 pattern,
        // it should use tool pattern instead.
        if (remaining.size() < 2) {
            throw new ParseException("too_few_patterns");
        }
        if (remaining.stream().noneMatch(node -> node.output.what().equals(entry.what())) ||
                remaining.stream().noneMatch(node -> node.output.what().equals(exit.what()))
        ) {
            throw new ParseException("endpoints_not_outputs");
        }

        var ordered = new ArrayList<LoopNode>();
        var external = new LinkedHashMap<AEKey, Long>();
        AEKey current = entry.what();
        long previousOutputAmount = -1;
        long seedAmount = -1;
        while (!remaining.isEmpty()) {
            LoopNode next = null;
            for (var node : remaining) {
                if (node.inputs.containsKey(current)) {
                    if (next != null) {
                        throw new ParseException("ambiguous_path");
                    }
                    next = node;
                }
            }
            if (next == null) {
                throw new ParseException("broken_path");
            }
            long chainAmount = next.inputs.get(current);
            if (ordered.isEmpty()) {
                seedAmount = chainAmount;
            } else if (previousOutputAmount != chainAmount) {
                throw new ParseException("unbalanced_intermediate");
            }
            for (var input : next.inputs.entrySet()) {
                if (!input.getKey().equals(current)) {
                    external.merge(input.getKey(), input.getValue(), Math::addExact);
                }
            }
            ordered.add(next);
            remaining.remove(next);
            current = next.output.what();
            previousOutputAmount = next.output.amount();
            if (current.equals(entry.what())) {
                break;
            }
        }

        if (!remaining.isEmpty()) {
            throw new ParseException("unused_patterns");
        }
        if (!current.equals(entry.what())) {
            throw new ParseException("not_closed");
        }
        if (!ordered.get(ordered.size() - 2).output.what().equals(exit.what())) {
            throw new ParseException("wrong_exit");
        }
        if (previousOutputAmount <= seedAmount) {
            throw new ParseException("no_net_output");
        }

        var externalInputs = external.entrySet().stream().map(entryAmount -> new GenericStack(entryAmount.getKey(), entryAmount.getValue())).toList();
        var outputs = ordered.stream().map(node -> node.output).toList();
        var loopId = UUID.randomUUID();
        var loopData = new ArrayList<LoopPatternData>(ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            loopData.add(new LoopPatternData(
                    loopId,
                    storageCellId,
                    i,
                    ordered.size(),
                    new GenericStack(entry.what(), seedAmount),
                    exit,
                    seedAmount,
                    previousOutputAmount - seedAmount,
                    externalInputs,
                    outputs
            ));
        }
        return new LoopChain(
                ordered.stream().map(node -> node.stack).toList(),
                ordered.stream().mapToInt(node -> node.index).boxed().toList(),
                loopData,
                entry.what(),
                seedAmount
        );
    }


    private record LoopNode(int index, ItemStack stack, Map<AEKey, Long> inputs, GenericStack output) {
    }

    public record LoopChain(List<ItemStack> patterns, List<Integer> indices, List<LoopPatternData> metadata,
                            AEKey entry, long seedAmount) {
    }

    public static final class ParseException extends Exception {
        private final String reason;

        public ParseException(String reason) {
            super(reason);
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }
}
