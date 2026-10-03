package com.ber.nimblePattern.probability;

import appeng.api.crafting.*;
import appeng.api.stacks.*;
import appeng.helpers.patternprovider.PatternContainer;
import com.ber.nimblePattern.pattern.NimbleEncodedPattern;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Retains metadata when a third-party provider rebuilds a physical processing pattern. */
public final class ProbabilityPattern extends NimbleEncodedPattern {
    private ProbabilityPattern(IPatternDetails raw, ItemStack physical) {
        super(raw instanceof NimbleEncodedPattern n ? n.getPattern() : raw,
                raw instanceof NimbleEncodedPattern n && n.getFuzzyMode(), null, definition(raw, physical));
    }
    private static AEItemKey definition(IPatternDetails raw, ItemStack physical) {
        var stack = raw.getDefinition().toStack();
        ProbabilityEncoding.mark(stack, ProbabilityEncoding.recipe(physical), ProbabilityEncoding.cycles(physical));
        return AEItemKey.of(stack);
    }

    public static IPatternDetails recover(Object provider, IPatternDetails pattern) {
        return new Index(provider).recover(pattern);
    }

    /** One immutable inventory snapshot per provider rebuild, not one decoding pass per exposed pattern. */
    public static final class Index {
        private record Candidate(ItemStack stack, Map<AEKey, Long> inputs, Map<AEKey, Long> outputs) {}
        private final Map<GenericStack, List<Candidate>> byOutput = new HashMap<>();

        public Index(Object provider) {
            if (!(provider instanceof PatternContainer container)) return;
            var inv = container.getTerminalPatternInventory();
            var stacks = new ArrayList<ItemStack>();
            boolean hasProbability = false;
            for (int i = 0; i < inv.size(); i++) {
                var stack = inv.getStackInSlot(i);
                if (!PatternDetailsHelper.isEncodedPattern(stack)) continue;
                stacks.add(stack);
                hasProbability |= ProbabilityEncoding.marked(stack);
            }
            if (!hasProbability) return;
            // Include ordinary patterns too: an ordinary duplicate makes recovery ambiguous.
            for (var stack : stacks) {
                try {
                    var physical = new appeng.crafting.pattern.AEProcessingPattern(AEItemKey.of(stack));
                    byOutput.computeIfAbsent(physical.getPrimaryOutput(), k -> new ArrayList<>())
                            .add(new Candidate(stack.copy(), inputs(physical), outputs(physical)));
                } catch (RuntimeException ignored) { }
            }
        }
        public IPatternDetails recover(IPatternDetails pattern) {
            if (byOutput.isEmpty() || (pattern instanceof NimbleEncodedPattern n ? n.metadata().probability()
                    : ProbabilityEncoding.marked(pattern.getDefinition().toStack()))) return pattern;
            var candidates = byOutput.get(pattern.getPrimaryOutput());
            if (candidates == null) return pattern;
            var inputs = inputs(pattern);
            var outputs = outputs(pattern);
            ItemStack match = null;
            for (var candidate : candidates) {
                if (!contained(outputs, candidate.outputs) || !contained(inputs, candidate.inputs)) continue;
                if (!ProbabilityEncoding.marked(candidate.stack) || ProbabilityEncoding.recipe(candidate.stack) == null
                        || match != null) return pattern;
                match = candidate.stack;
            }
            return match == null ? pattern : new ProbabilityPattern(pattern, match);
        }
    }
    private static Map<AEKey, Long> outputs(IPatternDetails p) {
        var result = new HashMap<AEKey, Long>();
        for (var s : p.getOutputs()) result.merge(s.what(), s.amount(), Math::addExact);
        return result;
    }
    private static Map<AEKey, Long> inputs(IPatternDetails p) {
        var result = new HashMap<AEKey, Long>();
        for (var input : p.getInputs()) {
            var possible = input.getPossibleInputs();
            if (possible.length != 1) return Map.of();
            result.merge(possible[0].what(), Math.multiplyExact(input.getMultiplier(), possible[0].amount()), Math::addExact);
        }
        return result;
    }
    private static boolean contained(Map<AEKey, Long> exposed, Map<AEKey, Long> original) {
        return !exposed.isEmpty() && exposed.entrySet().stream().allMatch(e -> Objects.equals(e.getValue(), original.get(e.getKey())));
    }
}
