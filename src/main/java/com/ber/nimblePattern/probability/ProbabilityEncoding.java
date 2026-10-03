package com.ber.nimblePattern.probability;

import appeng.api.stacks.*;
import appeng.util.ConfigInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import java.math.BigInteger;
import java.util.*;

public final class ProbabilityEncoding {
    private ProbabilityEncoding() {}
    public interface Menu {
        void nimble$selectProbabilityRecipe(ResourceLocation id);
    }

    public static boolean marked(ItemStack stack) {
        var root = stack.getTagElement("nimble_pattern");
        return root != null && root.contains("probability", 10);
    }

    public static boolean reencodingSamePattern(ItemStack previous, ItemStack encoded) {
        if (!marked(previous)) return false;
        try {
            var before = new appeng.crafting.pattern.AEProcessingPattern(AEItemKey.of(previous));
            var after = new appeng.crafting.pattern.AEProcessingPattern(AEItemKey.of(encoded));
            return Arrays.equals(before.getSparseInputs(), after.getSparseInputs())
                    && Arrays.equals(before.getSparseOutputs(), after.getSparseOutputs());
        } catch (RuntimeException ignored) { return false; }
    }

    public static ResourceLocation recipe(ItemStack stack) {
        var root = stack.getTagElement("nimble_pattern");
        return root == null ? null : ResourceLocation.tryParse(root.getCompound("probability").getString("recipe"));
    }

    public static long cycles(ItemStack stack) {
        var root = stack.getTagElement("nimble_pattern");
        return root == null ? 1 : Math.max(0, root.getCompound("probability").getLong("cycles"));
    }

    public static void mark(ItemStack stack, ResourceLocation recipe, long cycles) {
        var data = new CompoundTag();
        data.putString("recipe", recipe.toString());
        data.putLong("cycles", cycles);
        stack.getOrCreateTagElement("nimble_pattern").put("probability", data);
    }

    public record Normalized(List<GenericStack> inputs, List<GenericStack> outputs, long cycles) {}

    /** Quantities and externally supplied inputs do not change an output's probability. */
    public static boolean hasProbabilityOutput(List<GenericStack> displayed, List<ProbabilityRecipes.Output> recipeOutputs) {
        var known = new HashSet<AEKey>();
        var probabilistic = new HashSet<AEKey>();
        for (var output : recipeOutputs) {
            if (output.stack() == null || output.chance().numerator().signum() <= 0) continue;
            known.add(output.stack().what());
            if (output.chance().numerator().compareTo(output.chance().denominator()) < 0)
                probabilistic.add(output.stack().what());
        }
        return !displayed.isEmpty()
                && displayed.stream().allMatch(s -> s.amount() > 0 && known.contains(s.what()))
                && displayed.stream().anyMatch(s -> probabilistic.contains(s.what()));
    }

    /** Count executions from retained inputs, never from edited outputs. Zero selects timeout tracking. */
    public static long editedCycles(Normalized original, List<GenericStack> inputs) {
        var perBatch = new HashMap<AEKey, Long>();
        for (var input : original.inputs()) perBatch.merge(input.what(), input.amount(), Math::addExact);
        var actual = new HashMap<AEKey, Long>();
        for (var input : inputs) actual.merge(input.what(), input.amount(), Math::addExact);
        Long cycles = null;
        for (var input : actual.entrySet()) {
            Long batch = perBatch.get(input.getKey());
            if (batch == null) continue;
            var scaled = BigInteger.valueOf(input.getValue()).multiply(BigInteger.valueOf(original.cycles()))
                    .divideAndRemainder(BigInteger.valueOf(batch));
            if (scaled[1].signum() != 0 || scaled[0].signum() <= 0) return 0;
            long count = scaled[0].longValueExact();
            if (cycles != null && cycles != count) return 0;
            cycles = count;
        }
        return cycles == null ? 0 : cycles;
    }

    public static Normalized normalize(List<GenericStack> inputs, List<GenericStack> displayed,
                                       List<ProbabilityRecipes.Output> outputs) {
        var raw = new HashMap<AEKey, Long>();
        var expected = new HashMap<AEKey, ExpectedAmount>();
        boolean probabilistic = false;
        for (var output : outputs) {
            if (output.stack() == null || output.stack().amount() <= 0) return null;
            var chance = output.chance();
            if (chance.numerator().compareTo(chance.denominator()) > 0) return null;
            probabilistic |= chance.numerator().compareTo(chance.denominator()) < 0;
            raw.merge(output.stack().what(), output.stack().amount(), Math::addExact);
            expected.merge(output.stack().what(), chance.multiply(output.stack().amount()), ExpectedAmount::add);
        }
        if (!probabilistic || inputs.isEmpty() || displayed.isEmpty()) return null;
        // JEI may omit byproducts (e.g. sequenced assembly). Only encode the displayed outputs.
        var selected = new LinkedHashMap<AEKey, Long>();
        for (var stack : displayed) selected.merge(stack.what(), stack.amount(), Math::addExact);
        BigInteger scale = BigInteger.ONE;
        probabilistic = false;
        for (var entry : selected.entrySet()) {
            if (!Objects.equals(raw.get(entry.getKey()), entry.getValue())) return null;
            var amount = expected.get(entry.getKey());
            if (amount.numerator().signum() == 0) return null;
            probabilistic |= amount.numerator().compareTo(BigInteger.valueOf(entry.getValue()).multiply(amount.denominator())) < 0;
            scale = scale.divide(scale.gcd(amount.denominator())).multiply(amount.denominator());
        }
        if (!probabilistic) return null;
        long factor = scale.longValueExact();
        var normalizedInputs = inputs.stream().map(s -> new GenericStack(s.what(), Math.multiplyExact(s.amount(), factor))).toList();
        var normalizedOutputs = selected.keySet().stream().map(k -> new GenericStack(k, expected.get(k).scaled(factor))).toList();
        // AE's pattern editor stores finite positive quantities; reject instead of silently truncating.
        if (normalizedInputs.stream().anyMatch(s -> s.amount() > Integer.MAX_VALUE)
                || normalizedOutputs.stream().anyMatch(s -> s.amount() > Integer.MAX_VALUE)) return null;
        return new Normalized(normalizedInputs, normalizedOutputs, factor);
    }

    public static List<GenericStack> snapshot(ConfigInventory inv) {
        var result = new ArrayList<GenericStack>();
        for (int i = 0; i < inv.size(); i++) if (inv.getStack(i) != null) result.add(inv.getStack(i));
        return List.copyOf(result);
    }
}
