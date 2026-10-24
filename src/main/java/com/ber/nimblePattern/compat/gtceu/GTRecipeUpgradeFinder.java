package com.ber.nimblePattern.compat.gtceu;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.*;
import appeng.crafting.pattern.AEProcessingPattern;
import com.ber.nimblePattern.NimblePattern;
import com.ber.nimblePattern.pattern.NimblePatternTag;
import com.ber.nimblePattern.pattern.upgrade.RecipeUpgradeCandidate;
import com.ber.nimblePattern.pattern.upgrade.UpgradePreview;
import com.ber.nimblePattern.pattern.upgrade.UpgradeRules;
import com.ber.nimblePattern.pattern.upgrade.UpgradeState;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntCircuitIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;


public final class GTRecipeUpgradeFinder {
    private final ServerLevel level;
    private final GTRecipeIndex index;
    // the available materials in network storage
    private final List<AEKey> available;
    // some recipes might have substitute materials, this is to ensure the fixed order of materials
    private static final Comparator<AEKey> KEY_ORDER = Comparator.comparing((AEKey k) -> k.getType().getId().toString())
            .thenComparing(k -> k.getId().toString()).thenComparing(k -> k.toTagGeneric().toString());


    public GTRecipeUpgradeFinder(ServerLevel level, KeyCounter stock) {
        this(level, stock, GTRecipeIndex.getOrCreate(level.getRecipeManager()));
    }

    GTRecipeUpgradeFinder(ServerLevel level, KeyCounter stock, Collection<GTRecipe> recipes) {
        this(level, stock, new GTRecipeIndex(recipes));
    }

    private GTRecipeUpgradeFinder(ServerLevel level, KeyCounter stock, GTRecipeIndex index) {
        this.level = level;
        this.index = index;
        this.available = stock.keySet().stream().filter(k -> stock.get(k) > 0).sorted(KEY_ORDER).toList();
    }

    /**
     * If the inputs and outputs of pattern are contained in the inputs and outputs of the recipe,
     * the pattern matches the recipe.
     */
    public static boolean matches(AEProcessingPattern pattern, GTRecipe recipe) {
        var inputs = Arrays.stream(pattern.getSparseInputs()).filter(Objects::nonNull).toList();
        var outputs = Arrays.stream(pattern.getSparseOutputs()).filter(Objects::nonNull).toList();
        return !inputs.isEmpty() && !outputs.isEmpty()
                && UpgradeRules.contained(inputs, getContents(recipe, true), (p, c) -> accepts(c, p.what()))
                && UpgradeRules.contained(outputs, getContents(recipe, false), (p, c) ->
                getAlternatives(c).stream().anyMatch(a -> a.what().equals(p.what())));
    }

    /**
     * Determine whether it needs to find upgrading recommendation for the pattern.
     */
    public static boolean eligible(ItemStack pattern) {
        if (NimblePatternTag.getStatus(pattern) != UpgradeState.UPGRADE) {
            return false;
        }
        var root = pattern.getTagElement(NimblePattern.MOD_ID);
        if (root == null) {
            return false;
        }
        return Stream.of("loop", "tool", "probability", "fake", "fuzzy").noneMatch(root::contains);
    }

    /**
     * Recommend an upgrading recipe for the old pattern.
     */
    @Nullable
    public RecipeUpgradeCandidate recommend(ItemStack stack) {
        if (!eligible(stack)) {
            return null;
        }
        var decoded = PatternDetailsHelper.decodePattern(stack, level);
        if (!(decoded instanceof AEProcessingPattern pattern)) {
            return null;
        }
        var inputs = Arrays.stream(pattern.getSparseInputs()).filter(Objects::nonNull).toList();
        var outputs = Arrays.stream(pattern.getSparseOutputs()).filter(Objects::nonNull).toList();
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return null;
        }
        GTRecipe oldRecipe = null;
        var recipeId = NimblePatternTag.getRecipeId(stack);
        if (recipeId != null) {
            var recipe = level.getRecipeManager().byKey(recipeId).orElse(null);
            if (recipe instanceof GTRecipe gt && matches(pattern, gt)) {
                oldRecipe = gt;
            }
        }
        if (oldRecipe == null) {
            oldRecipe = index.findLegacy(pattern);
            if (oldRecipe == null) {
                return null;
            }
        }
        var targetOutput = pattern.getPrimaryOutput().what();
        long oldOutputAmount = index.getOutputAmount(oldRecipe, targetOutput);
        if (oldOutputAmount <= 0 || oldRecipe.duration <= 0) {
            return null;
        }
        for (var candidate : index.getRecipes(oldRecipe, targetOutput)) {
            if (candidate.equals(oldRecipe) || candidate.duration <= 0 || !UpgradeRules.improves(
                    oldOutputAmount, index.getInputCost(oldRecipe), index.getOutputAmount(candidate, targetOutput), index.getInputCost(candidate))) {
                continue;
            }
            var result = encode(stack, inputs, targetOutput, oldRecipe, oldOutputAmount, candidate);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Transform GT recipe to ae2 pattern and generate an RecipeUpgradeCandidate object for recommendation.
     */
    @Nullable
    private RecipeUpgradeCandidate encode(ItemStack original, List<GenericStack> oldInputs, AEKey target,
                                          GTRecipe old, long oldAmount, GTRecipe recipe) {
        var encodedInputs = new LinkedHashMap<AEKey, Long>();
        var encodedOutputs = new LinkedHashMap<AEKey, Long>();
        try {
            // parse inputs of new recipe
            for (var content : getContents(recipe, true)) {
                var alternatives = getAlternatives(content);
                if (alternatives.isEmpty()) {
                    return null;
                }
                AEKey selected = oldInputs.stream().map(GenericStack::what).filter(k -> accepts(content, k))
                        .findFirst().orElseGet(() -> available.stream().filter(k -> accepts(content, k)).findFirst()
                                .orElse(alternatives.stream().map(GenericStack::what).min(KEY_ORDER).orElseThrow()));
                long amount = alternatives.get(0).amount();
                if (amount <= 0) {
                    return null;
                }
                // probability inputs and non-consumable inputs only preview in the dialog, but would not be
                // written in the new pattern
                boolean previewOnly = content.chance < content.maxChance || isPreset(content, selected);
                if (!previewOnly) {
                    encodedInputs.merge(selected, amount, Math::addExact);
                }
            }

            // parse outputs of new recipe
            for (var content : getContents(recipe, false)) {
                // Probabilistic outputs require a probability pattern, do not encode automatically
                if (content.chance != content.maxChance) {
                    return null;
                }
                var alternatives = getAlternatives(content);
                if (alternatives.size() != 1) {
                    return null;
                }
                var output = alternatives.get(0);
                if (output.amount() <= 0) {
                    return null;
                }
                encodedOutputs.merge(output.what(), output.amount(), Math::addExact);
            }
            if (encodedInputs.isEmpty() || encodedInputs.size() > AEProcessingPattern.MAX_INPUT_SLOTS
                    || encodedOutputs.size() > AEProcessingPattern.MAX_OUTPUT_SLOTS || !encodedOutputs.containsKey(target)) {
                return null;
            }

            // encode new pattern
            var outputStacks = new ArrayList<GenericStack>();
            var outputAmount = encodedOutputs.remove(target);
            outputStacks.add(new GenericStack(target, outputAmount));
            encodedOutputs.forEach((key, amount) -> outputStacks.add(new GenericStack(key, amount)));
            var inputStacks = encodedInputs.entrySet().stream().map(e -> new GenericStack(e.getKey(), e.getValue()))
                    .toArray(GenericStack[]::new);
            ItemStack replacement = PatternDetailsHelper.encodeProcessingPattern(inputStacks, outputStacks.toArray(GenericStack[]::new));
            NimblePatternTag.tagRecipeId(replacement, recipe.id);
            var preview = new UpgradePreview(0, 0, 0, recipe.recipeType.registryName,
                    recipe.id, outputStacks.get(0), oldAmount, List.of(inputStacks));
            return new RecipeUpgradeCandidate(old, recipe, replacement, preview);
        } catch (IllegalArgumentException | ArithmeticException ex) {
            return null;
        }
    }

    /**
     * Determine a material is not consumed.
     */
    private static boolean isPreset(Content content, AEKey key) {
        Object ingredient = content.content;
        if (ingredient instanceof SizedIngredient sized) {
            ingredient = sized.getInner();
        }
        if (ingredient instanceof IntCircuitIngredient) {
            return true;
        }
        String path = key.getId().getPath();
        return key.getId().getNamespace().equals("gtceu")
                && (path.startsWith("shape_mold") || path.startsWith("shape_extruder") || path.equals("programmed_circuit"));
    }

    /**
     * Get the contents of inputs or outputs. When input is true, get inputs; false means outputs.
     */
    static List<Content> getContents(GTRecipe recipe, boolean input) {
        var result = new ArrayList<Content>();
        var map = input ? recipe.inputs : recipe.outputs;
        result.addAll(map.getOrDefault(ItemRecipeCapability.CAP, List.of()));
        result.addAll(map.getOrDefault(FluidRecipeCapability.CAP, List.of()));
        return result;
    }

    static List<GenericStack> getAlternatives(Content content) {
        var result = new ArrayList<GenericStack>();
        if (content.content instanceof Ingredient ingredient) {
            long amount = ingredient instanceof SizedIngredient sized ? sized.getAmount() : 1;
            for (var item : ingredient.getItems()) {
                var key = AEItemKey.of(item);
                if (key != null) result.add(new GenericStack(key, amount));
            }
        } else if (content.content instanceof FluidIngredient ingredient) {
            for (var fluid : ingredient.getStacks()) {
                result.add(new GenericStack(AEFluidKey.of(fluid.getFluid(), fluid.getTag()), ingredient.getAmount()));
            }
        }
        return result.stream().distinct().toList();
    }

    /**
     * Determine a material could be accepted as input or not.
     */
    private static boolean accepts(Content content, AEKey key) {
        if (content.content instanceof Ingredient ingredient && key instanceof AEItemKey item) {
            if (ingredient instanceof SizedIngredient sized) ingredient = sized.getInner();
            return ingredient.test(item.toStack());
        }
        if (content.content instanceof FluidIngredient ingredient && key instanceof AEFluidKey fluid) {
            return ingredient.test(FluidStack.create(fluid.getFluid(), ingredient.getAmount(), fluid.getTag()));
        }
        return false;
    }

    /**
     * Simplified input cost per recipe execution: item count + fluid amount in mB.
     * Only count inputs written into the processing pattern, once per ingredient.
     */
    static long getInputCost(GTRecipe recipe) {
        long cost = 0;
        for (var content : getContents(recipe, true)) {
            if (content.chance < content.maxChance) {
                continue;
            }
            var alternatives = getAlternatives(content);
            if (alternatives.isEmpty()) {
                continue;
            }
            var input = alternatives.get(0);
            if (!isPreset(content, input.what())) {
                cost += input.amount();
            }
        }
        return cost;
    }

    static long getOutputAmount(GTRecipe recipe, AEKey target) {
        long amount = 0;
        try {
            for (var content : getContents(recipe, false)) {
                for (var output : getAlternatives(content)) {
                    if (output.what().equals(target)) {
                        amount = Math.addExact(amount, output.amount());
                        break;
                    }
                }
            }
        } catch (ArithmeticException ex) {
            return 0;
        }
        return amount;
    }
}
