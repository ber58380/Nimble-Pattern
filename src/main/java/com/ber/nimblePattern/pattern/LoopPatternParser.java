package com.ber.nimblePattern.pattern;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LoopPatternParser {
    private LoopPatternParser() {
    }

    public static Result parse(Level level, List<ItemStack> patternStacks, GenericStack entry, GenericStack exit,
            UUID storageCellId) throws ParseException {
        if (entry == null || exit == null) {
            throw new ParseException("missing_endpoints");
        }

        var remaining = new ArrayList<Node>();
        for (int sourceIndex = 0; sourceIndex < patternStacks.size(); sourceIndex++) {
            var stack = patternStacks.get(sourceIndex);
            var details = PatternDetailsHelper.decodePattern(stack, level);
            if (!(details instanceof AEProcessingPattern pattern)) {
                throw new ParseException("processing_only");
            }
            var outputs = pattern.getOutputs();
            if (outputs.length == 0) {
                throw new ParseException("missing_primary_output");
            }
            remaining.add(new Node(sourceIndex, stack.copy(), aggregateInputs(pattern), outputs[0]));
        }
        if (remaining.size() < 2) {
            throw new ParseException("too_few_patterns");
        }
        if (remaining.stream().noneMatch(node -> node.primary.what().equals(entry.what()))
                || remaining.stream().noneMatch(node -> node.primary.what().equals(exit.what()))) {
            throw new ParseException("endpoints_not_outputs");
        }

        var ordered = new ArrayList<Node>(remaining.size());
        var external = new LinkedHashMap<AEKey, Long>();
        AEKey current = entry.what();
        long previousOutputAmount = -1;
        long seedAmount = -1;

        while (!remaining.isEmpty()) {
            Node next = null;
            for (var candidate : remaining) {
                if (candidate.inputs.containsKey(current)) {
                    if (next != null) {
                        throw new ParseException("ambiguous_path");
                    }
                    next = candidate;
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
                    external.merge(input.getKey(), input.getValue(), LoopPatternParser::checkedAdd);
                }
            }

            ordered.add(next);
            remaining.remove(next);
            current = next.primary.what();
            previousOutputAmount = next.primary.amount();

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
        if (!ordered.get(ordered.size() - 2).primary.what().equals(exit.what())) {
            throw new ParseException("wrong_exit");
        }
        if (previousOutputAmount <= seedAmount) {
            throw new ParseException("no_net_output");
        }

        var externalInputs = external.entrySet().stream()
                .map(entryAmount -> new GenericStack(entryAmount.getKey(), entryAmount.getValue()))
                .toList();
        var mainOutputs = ordered.stream().map(node -> node.primary).toList();
        var groupId = UUID.randomUUID();
        var data = new ArrayList<LoopPatternData>(ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            data.add(new LoopPatternData(
                    groupId,
                    storageCellId,
                    i,
                    ordered.size(),
                    new GenericStack(entry.what(), seedAmount),
                    exit,
                    seedAmount,
                    previousOutputAmount - seedAmount,
                    externalInputs,
                    mainOutputs));
        }

        return new Result(
                ordered.stream().map(node -> node.stack).toList(),
                ordered.stream().mapToInt(node -> node.sourceIndex).boxed().toList(),
                data,
                entry.what(),
                seedAmount);
    }

    private static Map<AEKey, Long> aggregateInputs(AEProcessingPattern pattern) throws ParseException {
        var result = new LinkedHashMap<AEKey, Long>();
        for (var input : pattern.getInputs()) {
            var possible = input.getPossibleInputs();
            if (possible.length == 0) {
                throw new ParseException("missing_input");
            }
            var primary = possible[0];
            long amount;
            try {
                amount = Math.multiplyExact(primary.amount(), input.getMultiplier());
            } catch (ArithmeticException e) {
                throw new ParseException("amount_overflow");
            }
            result.merge(primary.what(), amount, LoopPatternParser::checkedAdd);
        }
        return result;
    }

    private static long checkedAdd(long left, long right) {
        return Math.addExact(left, right);
    }

    private record Node(int sourceIndex, ItemStack stack, Map<AEKey, Long> inputs,
                        GenericStack primary) {
    }

    public record Result(List<ItemStack> patterns, List<Integer> sourceIndices, List<LoopPatternData> metadata,
                         AEKey entry, long seedAmount) {
    }

    public static final class ParseException extends Exception {
        private final String reason;

        public ParseException(String reason) {
            super(reason);
            this.reason = reason;
        }

        public String reason() {
            return reason;
        }
    }
}
