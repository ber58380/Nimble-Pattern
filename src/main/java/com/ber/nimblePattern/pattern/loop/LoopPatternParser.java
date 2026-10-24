package com.ber.nimblePattern.pattern.loop;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import net.minecraft.world.item.ItemStack;
import com.ber.nimblePattern.pattern.NimblePatternTag;
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
                throw new ParseException("loop_amount_overflow");
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
            if (NimblePatternTag.getLoop(pattern) != null) {
                throw new ParseException("loop_already_encoded");
            }
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

        LoopGraphPlanner.Plan<AEKey> plan;
        try {
            plan = LoopGraphPlanner.plan(remaining.stream()
                    .map(node -> new LoopGraphPlanner.Recipe<>(node.inputs, node.output.what(), node.output.amount()))
                    .toList(), entry.what(), exit.what());
        } catch (IllegalArgumentException e) {
            throw new ParseException(e.getMessage());
        }
        var ordered = plan.order().stream().map(remaining::get).toList();
        long seedAmount = plan.seed();
        var externalInputs = plan.external().entrySet().stream()
                .map(value -> new GenericStack(value.getKey(), value.getValue())).toList();
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
                    plan.netOutput(),
                    externalInputs,
                    outputs,
                    plan.operations()
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
