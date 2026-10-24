package com.ber.nimblePattern.pattern.loop;

import java.util.*;

/**
 * Balances primary-output recipes, then schedules them using only the selected entry as seed.
 */
public final class LoopGraphPlanner {
    private LoopGraphPlanner() {
    }

    public record Recipe<K>(Map<K, Long> inputs, K output, long amount) {
        public Recipe {
            inputs = Map.copyOf(inputs);
            if (amount <= 0 || inputs.values().stream().anyMatch(n -> n <= 0)) {
                throw new IllegalArgumentException("Invalid recipe amounts");
            }
        }
    }

    public record Plan<K>(List<Integer> order, List<Long> operations, Map<K, Long> external,
                          long seed, long netOutput) {
    }

    public static <K> Plan<K> plan(List<Recipe<K>> recipes, K entry, K exit) {
        var producers = new LinkedHashMap<K, Integer>();
        for (int i = 0; i < recipes.size(); i++) {
            if (producers.putIfAbsent(recipes.get(i).output(), i) != null) {
                fail("ambiguous_path");
            }
        }
        if (!producers.containsKey(entry) || !producers.containsKey(exit)) {
            fail("endpoints_not_outputs");
        }
        int closing = producers.get(entry);
        if (!recipes.get(closing).inputs().containsKey(exit)) {
            fail("wrong_exit");
        }

        // Every selected recipe must belong to the entry's strongly connected component.
        var forward = new HashSet<K>();
        var backward = new HashSet<K>();
        forward.add(entry);
        backward.add(entry);
        for (int pass = 0; pass < recipes.size(); pass++) {
            for (var recipe : recipes) {
                if (recipe.inputs().keySet().stream().anyMatch(forward::contains)) {
                    forward.add(recipe.output());
                }
                if (backward.contains(recipe.output())) {
                    backward.addAll(recipe.inputs().keySet());
                }
            }
        }
        if (!forward.containsAll(producers.keySet()) || !backward.containsAll(producers.keySet())) {
            fail("unused_patterns");
        }

        long[] counts = new long[recipes.size()];
        Arrays.fill(counts, 1);
        boolean balanced = false;
        // Monotone integer balance; bounded to reject non-productive cycles without hanging the server.
        for (int pass = 0; pass < 4096; pass++) {
            var consumed = new HashMap<K, Long>();
            for (int i = 0; i < recipes.size(); i++) {
                for (var input : recipes.get(i).inputs().entrySet()) {
                    consumed.merge(input.getKey(), Math.multiplyExact(input.getValue(), counts[i]), Math::addExact);
                }
            }
            boolean changed = false;
            for (var producer : producers.entrySet()) {
                int i = producer.getValue();
                long needed = consumed.getOrDefault(producer.getKey(), 0L);
                if (producer.getKey().equals(entry)) needed = Math.addExact(needed, 1);
                long amount = recipes.get(i).amount();
                long times = needed / amount + (needed % amount == 0 ? 0 : 1);
                if (times > counts[i]) {
                    counts[i] = times;
                    changed = true;
                }
            }
            if (!changed) {
                balanced = true;
                break;
            }
        }
        if (!balanced) {
            fail("no_net_output");
        }

        // Cutting the seed key must make the remaining dependencies acyclic. Do not borrow other intermediates.
        var available = new HashMap<K, Long>();
        var external = new LinkedHashMap<K, Long>();
        var order = new ArrayList<Integer>();
        var operations = new ArrayList<Long>();
        var done = new HashSet<Integer>();
        long seed = 0;
        while (order.size() < recipes.size()) {
            int next = -1;
            for (int i = 0; i < recipes.size(); i++) {
                if (done.contains(i)) continue;
                var recipe = recipes.get(i);
                boolean ready = true;
                for (var input : recipe.inputs().entrySet()) {
                    if (producers.containsKey(input.getKey()) && !input.getKey().equals(entry)
                            && available.getOrDefault(input.getKey(), 0L) < Math.multiplyExact(input.getValue(), counts[i])) {
                        ready = false;
                        break;
                    }
                }
                if (ready) {
                    next = i;
                    break;
                }
            }
            if (next < 0) fail("broken_path");
            var recipe = recipes.get(next);
            for (var input : recipe.inputs().entrySet()) {
                long amount = Math.multiplyExact(input.getValue(), counts[next]);
                K key = input.getKey();
                if (!producers.containsKey(key)) {
                    external.merge(key, amount, Math::addExact);
                } else {
                    long held = available.getOrDefault(key, 0L);
                    if (key.equals(entry) && held < amount) {
                        seed = Math.addExact(seed, amount - held);
                        held = amount;
                    }
                    available.put(key, held - amount);
                }
            }
            available.merge(recipe.output(), Math.multiplyExact(recipe.amount(), counts[next]), Math::addExact);
            done.add(next);
            order.add(next);
            operations.add(counts[next]);
        }
        long net = Math.subtractExact(available.getOrDefault(entry, 0L), seed);
        if (seed <= 0 || net <= 0) fail("no_net_output");
        // Closing metadata is attached to the last recipe, which must produce the entry.
        if (order.get(order.size() - 1) != closing) fail("wrong_exit");
        return new Plan<>(List.copyOf(order), List.copyOf(operations), Collections.unmodifiableMap(external), seed, net);
    }

    private static void fail(String reason) {
        throw new IllegalArgumentException(reason);
    }
}
