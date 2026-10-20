package com.ber.nimblePattern.compat.gtceu;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.OptionalLong;

public final class GTRecipeOperations {
    private GTRecipeOperations() {
    }

    private static boolean matchesMaterial(Object content, AEKey key) {
        if (key instanceof AEItemKey itemKey) {
            if (content instanceof Ingredient ingredient) {
                return ingredient.test(itemKey.toStack());
            }
            return content instanceof ItemStack stack && !stack.isEmpty() && key.equals(AEItemKey.of(stack));
        } else if (key instanceof AEFluidKey fluidKey) {
            if (content instanceof FluidIngredient ingredient) {
                return ingredient.test(FluidStack.create(fluidKey.getFluid(), 1, fluidKey.getTag()));
            }
            return content instanceof FluidStack stack && !stack.isEmpty() && key.equals(AEFluidKey.of(stack.getFluid(), stack.getTag()));
        }
        return false;
    }

    /**
     * Find the delivered input matches to which input in the GT recipe
     */
    @Nullable
    private static Content findUniqueInput(GTRecipe recipe, AEKey key) {
        List<Content> contents;
        if (key instanceof AEItemKey) {
            contents = recipe.getInputContents(ItemRecipeCapability.CAP);
        } else if (key instanceof AEFluidKey) {
            contents = recipe.getInputContents(FluidRecipeCapability.CAP);
        } else {
            return null;
        }
        Content matched = null;
        for (var content : contents) {
            if (!matchesMaterial(content.content, key)) {
                continue;
            }
            // If the delivered input matches two different inputs in GT recipe, this delivered input
            // could not be considered as a reliable basis for calculate operations.
            if (matched != null) {
                return null;
            }
            matched = content;
        }
        return matched;
    }

    private static boolean containsRandomAmount(Ingredient ingredient) {
        if (ingredient instanceof IntProviderIngredient) {
            return true;
        }
        if (ingredient instanceof SizedIngredient sized) {
            return containsRandomAmount(sized.getInner());
        }
        return false;
    }

    /**
     * Get the consumption of input when executes one GT recipe
     */
    private static long getConsumption(Object content) {
        if (content instanceof SizedIngredient ingredient) {
            if (containsRandomAmount(ingredient.getInner())) {
                return -1;
            }
            return ingredient.getAmount();
        } else if (content instanceof ItemStack stack) {
            return stack.getCount();
        } else if (content != null && content.getClass() == Ingredient.class) {
            return 1;
        } else if (content instanceof FluidIngredient ingredient) {
            return ingredient.getAmount();
        } else if (content instanceof FluidStack stack) {
            return stack.getAmount();
        }
        return -1;
    }

    /**
     * Based on GT recipe, calculate the machine needs to execute how many operations to consume all delivered inputs
     */
    public static OptionalLong calculate(GTRecipe baseRecipe, List<GenericStack> deliveredInputs) {
        // find delivered inputs that are reliable to calculate operations
        var deliveredByInput = new IdentityHashMap<Content, Long>();
        for (var input : deliveredInputs) {
            if (input.amount() <= 0) {
                continue;
            }
            var content = findUniqueInput(baseRecipe, input.what());
            if (content == null) {
                continue;
            }
            // the probability consumption inputs could not be a reliable basis of calculating operations
            if (content.maxChance <= 0 || content.chance != content.maxChance) {
                continue;
            }
            deliveredByInput.merge(content, input.amount(), Long::sum);
        }

        long operations = 0;
        for (var entry : deliveredByInput.entrySet()) {
            long perOperation = getConsumption(entry.getKey().content);
            long delivered = entry.getValue();
            if (perOperation <= 0) {
                continue;
            }
            long currentOperations = delivered / perOperation;
            if (currentOperations <= 0 || delivered % perOperation != 0) {
                return OptionalLong.empty();
            }
            if (operations == 0) {
                operations = currentOperations;
            }
            // if different materials calculate different operations, the result is not reliable
            else if (operations != currentOperations) {
                return OptionalLong.empty();
            }
        }
        return operations > 0 ? OptionalLong.of(operations) : OptionalLong.empty();
    }

    /**
     * The execution of machine this time equals to how many base operations
     */
    public static OptionalLong getExecutedOperations(GTRecipe base, GTRecipe executed, List<GenericStack> inputs) {
        long operations = 0;
        for (var input : inputs) {
            var original = findUniqueInput(base, input.what());
            var modified = findUniqueInput(executed, input.what());
            if (original == null || modified == null
                    || original.maxChance <= 0 || original.chance != original.maxChance
                    || modified.chance != modified.maxChance) {
                continue;
            }
            long unit = getConsumption(original.content);
            long amount = getConsumption(modified.content);
            if (unit <= 0 || amount <= 0) {
                continue;
            }
            if (amount % unit != 0 || (operations != 0 && operations != amount / unit)) {
                return OptionalLong.empty();
            }
            operations = amount / unit;
        }
        return operations > 0 ? OptionalLong.of(operations) : OptionalLong.empty();
    }
}
