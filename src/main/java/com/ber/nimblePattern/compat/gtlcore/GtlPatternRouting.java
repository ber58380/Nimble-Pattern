package com.ber.nimblePattern.compat.gtlcore;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import com.ber.nimblePattern.pattern.wrapper.NimbleCraftingPattern;
import com.ber.nimblePattern.pattern.wrapper.NimbleProcessingPattern;

import java.util.*;

/**
 * Choose the executor before planning: graph jobs do not run Nimble's classic CPU controllers.
 */
public final class GtlPatternRouting {
    private GtlPatternRouting() {
    }

    public static boolean requiresClassic(AEKey requested, Iterable<IPatternDetails> patterns) {
        // Include secondary outputs and every candidate, not only AE's primary-output lookup.
        // A planner can choose a different candidate when inventory or provider availability changes.
        Map<AEKey, List<IPatternDetails>> producers = new HashMap<>();
        for (var pattern : patterns) {
            for (var output : pattern.getOutputs()) {
                producers.computeIfAbsent(output.what(), ignored -> new ArrayList<>()).add(pattern);
            }
        }
        Set<AEKey> visited = new HashSet<>();
        Set<IPatternDetails> visitedPatterns = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<AEKey> pending = new ArrayDeque<>();
        pending.add(requested);
        while (!pending.isEmpty()) {
            var key = pending.removeFirst();
            if (!visited.add(key)) {
                continue;
            }
            for (var pattern : producers.getOrDefault(key, List.of())) {
                if (!visitedPatterns.add(pattern)) {
                    continue;
                }
                if (pattern instanceof NimbleProcessingPattern processing
                        && (processing.getLoopMode() || processing.getFakeMode()
                        || processing.getFuzzyMode() || processing.isProbabilityPattern())) {
                    return true;
                }
                if (pattern instanceof NimbleCraftingPattern crafting && crafting.isToolPattern()) {
                    return true;
                }
                for (var input : pattern.getInputs()) {
                    for (var candidate : input.getPossibleInputs()) {
                        pending.addLast(candidate.what());
                    }
                }
            }
        }
        return false;
    }
}
