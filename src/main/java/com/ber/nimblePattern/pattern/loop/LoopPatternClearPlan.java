package com.ber.nimblePattern.pattern.loop;

import appeng.api.stacks.AEKey;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public record LoopPatternClearPlan(Map<Integer, ItemStack> patterns, Map<AEKey, Long> seeds) {
    public static LoopPatternClearPlan prepare(Map<Integer, ItemStack> input, UUID cellId)
            throws LoopPatternParser.ParseException {
        var groups = new LinkedHashMap<UUID, LoopPatternData>();
        var indices = new HashMap<UUID, Set<Integer>>();
        var selected = new LinkedHashMap<Integer, ItemStack>();
        for (var entry : input.entrySet()) {
            var stack = entry.getValue();
            var data = NimblePatternTag.getLoop(stack);
            if (data == null) {
                throw new LoopPatternParser.ParseException("invalid_loop_data");
            }
            if (!data.storageCellId().equals(cellId)) {
                throw new LoopPatternParser.ParseException("wrong_loop_storage_cell");
            }
            var first = groups.putIfAbsent(data.loopId(), data);
            if (first != null) {
                var normalized = new LoopPatternData(data.loopId(), data.storageCellId(), first.index(),
                        data.size(), data.entry(), data.exit(), data.seedAmount(), data.netOutputAmount(),
                        data.externalInputs(), data.outputs());
                if (!first.equals(normalized)) {
                    throw new LoopPatternParser.ParseException("invalid_loop_data");
                }
            }
            indices.computeIfAbsent(data.loopId(), key -> new HashSet<>()).add(data.index());
            selected.put(entry.getKey(), stack.copy());
        }
        if (selected.isEmpty()) {
            throw new LoopPatternParser.ParseException("no_loop_patterns");
        }
        for (var group : groups.values()) {
            if (indices.get(group.loopId()).size() != group.size()) {
                throw new LoopPatternParser.ParseException("incomplete_loop");
            }
        }
        var reservations = new LinkedHashMap<UUID, Map<AEKey, Long>>();
        for (var stack : selected.values()) {
            NimblePatternTag.removeLoopTag(stack).forEach(reservations::putIfAbsent);
        }
        var amounts = new LinkedHashMap<AEKey, Long>();
        for (var seed : reservations.values()) {
            seed.forEach((key, amount) -> amounts.merge(key, amount, Math::addExact));
        }
        return new LoopPatternClearPlan(Collections.unmodifiableMap(selected), Collections.unmodifiableMap(amounts));
    }
}
