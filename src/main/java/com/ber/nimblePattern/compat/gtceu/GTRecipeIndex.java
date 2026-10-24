package com.ber.nimblePattern.compat.gtceu;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import com.ber.nimblePattern.pattern.upgrade.UpgradeRules;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Server-thread only. Rebuilt after datapack reload, released when the server stops.
 */
public final class GTRecipeIndex {
    private static final Map<RecipeManager, GTRecipeIndex> CACHE = new IdentityHashMap<>();
    final Map<ResourceLocation, GTRecipe> byRecipeId = new HashMap<>();
    final Map<AEKey, List<GTRecipe>> byOutput = new HashMap<>();
    private final Map<GTRecipeType, Map<AEKey, List<GTRecipe>>> byTypeAndOutput = new IdentityHashMap<>();
    private final Map<GTRecipe, Map<AEKey, Long>> outputAmounts = new IdentityHashMap<>();
    private final Map<GTRecipe, Long> inputCosts = new IdentityHashMap<>();
    // matching history of recent 100 patterns
    private final Map<Signature, Optional<GTRecipe>> cachedMatches = new LinkedHashMap<>(128, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Signature, Optional<GTRecipe>> entry) {
            return size() > 100;
        }
    };

    public GTRecipeIndex(Collection<GTRecipe> recipes) {
        for (var recipe : recipes) {
            byRecipeId.put(recipe.id, recipe);
            inputCosts.put(recipe, GTRecipeUpgradeFinder.getInputCost(recipe));
            var keys = new HashSet<AEKey>();
            for (var content : GTRecipeUpgradeFinder.getContents(recipe, false)) {
                for (var stack : GTRecipeUpgradeFinder.getAlternatives(content)) {
                    keys.add(stack.what());
                }
            }
            var yields = new HashMap<AEKey, Long>();
            outputAmounts.put(recipe, yields);
            for (var key : keys) {
                yields.put(key, GTRecipeUpgradeFinder.getOutputAmount(recipe, key));
                byOutput.computeIfAbsent(key, k -> new ArrayList<>()).add(recipe);
                byTypeAndOutput.computeIfAbsent(recipe.recipeType, t -> new HashMap<>())
                        .computeIfAbsent(key, k -> new ArrayList<>()).add(recipe);
            }
        }
        byOutput.forEach((key, list) -> list.sort(recipeOrder(key)));
        byTypeAndOutput.values().forEach(map -> map.forEach((key, list) -> list.sort(recipeOrder(key))));
    }

    private Comparator<GTRecipe> recipeOrder(AEKey output) {
        return Comparator.comparingLong(this::getInputCost)
                .thenComparing(Comparator.<GTRecipe>comparingLong(r -> getOutputAmount(r, output)).reversed())
                .thenComparing(r -> r.id.toString());
    }

    /**
     * Add and find pattern in matching history.
     */
    public GTRecipe findLegacy(AEProcessingPattern pattern) {
        var inputs = Arrays.stream(pattern.getSparseInputs()).filter(Objects::nonNull)
                .map(GenericStack::what).collect(Collectors.toUnmodifiableSet());
        var outputs = Arrays.stream(pattern.getSparseOutputs()).filter(Objects::nonNull).toList();
        var signature = new Signature(inputs, outputs.stream().map(GenericStack::what)
                .collect(Collectors.toUnmodifiableSet()), pattern.getPrimaryOutput().what());
        return cachedMatches.computeIfAbsent(signature, key -> Optional.ofNullable(
                        UpgradeRules.unique(byOutput.getOrDefault(key.primaryOutput(), List.of()), r -> GTRecipeUpgradeFinder.matches(pattern, r))))
                .orElse(null);
    }

    /**
     * Get or create all registered GT recipes.
     */
    public static GTRecipeIndex getOrCreate(RecipeManager manager) {
        return CACHE.computeIfAbsent(manager, m -> new GTRecipeIndex(m.getRecipes().stream()
                .filter(GTRecipe.class::isInstance).map(GTRecipe.class::cast).toList()));
    }

    public static void clear() {
        CACHE.clear();
    }

    public long getInputCost(GTRecipe recipe) {
        return inputCosts.get(recipe);
    }

    public long getOutputAmount(GTRecipe recipe, AEKey output) {
        return outputAmounts.getOrDefault(recipe, Map.of()).getOrDefault(output, 0L);
    }

    /**
     * Get recipes for the target output, same machine first, then other machines.
     * Both groups are ordered by input cost and yield; the second group is traversed only as fallback.
     */
    public Iterable<GTRecipe> getRecipes(GTRecipe oldRecipe, AEKey output) {
        return () -> Stream.concat(
                byTypeAndOutput.getOrDefault(oldRecipe.recipeType, Map.of()).getOrDefault(output, List.of()).stream(),
                byOutput.getOrDefault(output, List.of()).stream().filter(r -> r.recipeType != oldRecipe.recipeType)
        ).iterator();
    }

    private record Signature(Set<AEKey> inputs, Set<AEKey> outputs, AEKey primaryOutput) {
    }
}
