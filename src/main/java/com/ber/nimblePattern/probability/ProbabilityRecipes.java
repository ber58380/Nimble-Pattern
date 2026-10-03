package com.ber.nimblePattern.probability;

import appeng.api.stacks.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import java.util.*;

/** Explicit optional adapters, never infer chance from translated JEI tooltip text. */
public final class ProbabilityRecipes {
    private static final ClassValue<java.util.concurrent.ConcurrentMap<String, java.lang.reflect.Method>> METHODS = new ClassValue<>() {
        @Override protected java.util.concurrent.ConcurrentMap<String, java.lang.reflect.Method> computeValue(Class<?> type) {
            return new java.util.concurrent.ConcurrentHashMap<>();
        }
    };
    private ProbabilityRecipes() {}

    public record Output(GenericStack stack, ExpectedAmount chance) {}

    public static Recipe<?> unwrap(Object display) {
        if (display instanceof Recipe<?> recipe) return recipe;
        if (display != null && display.getClass().getName().equals(
                "com.gregtechceu.gtceu.integration.jei.recipe.GTRecipeWrapper")) {
            try { return (Recipe<?>) field(display, "recipe"); }
            catch (ReflectiveOperationException ignored) {}
        }
        return null;
    }

    public static List<Output> outputs(Recipe<?> recipe) {
        try {
            String name = recipe.getClass().getName();
            if (name.equals("com.gregtechceu.gtceu.api.recipe.GTRecipe")) return gregtech(recipe);
            if (isA(recipe, "com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe")) {
                var pool = (List<?>) field(recipe, "resultPool");
                var sum = ExpectedAmount.of(0, 1);
                for (var entry : pool) sum = sum.add(ExpectedAmount.decimal((Number) call(entry, "getChance")));
                var result = new ArrayList<Output>();
                for (var entry : pool) result.add(new Output(stack(call(entry, "getStack")),
                        ExpectedAmount.decimal((Number) call(entry, "getChance")).divide(sum)));
                return result;
            }
            if (isA(recipe, "com.simibubi.create.content.processing.recipe.ProcessingRecipe")) {
                var result = new ArrayList<Output>();
                for (var entry : (List<?>) call(recipe, "getRollableResults")) {
                    result.add(new Output(stack(call(entry, "getStack")),
                            ExpectedAmount.decimal((Number) call(entry, "getChance"))));
                }
                for (var fluid : (List<?>) call(recipe, "getFluidResults"))
                    result.add(new Output(stack(fluid), ExpectedAmount.of(1, 1)));
                return result;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Unknown API version or chance semantics: leave the ordinary recipe untouched.
        }
        return List.of();
    }

    private static List<Output> gregtech(Object recipe) throws ReflectiveOperationException {
        if (!((Map<?, ?>) field(recipe, "tickOutputs")).isEmpty()) return List.of();
        // OR = independent rolls. AND/XOR/first-success need different probability calculations.
        var or = Class.forName("com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic")
                .getField("OR").get(null);
        Object longOr = or;
        try { longOr = Class.forName("org.gtlcore.gtlcore.api.recipe.chance.LongChanceLogic").getField("OR").get(null); }
        catch (ClassNotFoundException ignored) { }
        var io = Class.forName("com.gregtechceu.gtceu.api.capability.recipe.IO");
        var getLogic = recipe.getClass().getMethod("getChanceLogicForCapability",
                Class.forName("com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability"), io, boolean.class);
        var result = new ArrayList<Output>();
        for (var entry : ((Map<?, ?>) field(recipe, "outputs")).entrySet()) {
            var logic = getLogic.invoke(recipe, entry.getKey(), io.getField("OUT").get(null), false);
            if (logic != or && logic != longOr) return List.of();
            for (var content : (List<?>) entry.getValue()) {
                int chance = ((Number) field(content, "chance")).intValue();
                int max = ((Number) field(content, "maxChance")).intValue();
                // Read the original server recipe only. Voltage/overclock boosts and JEI display-tier overrides
                // intentionally do not contribute to the encoded expectation.
                if (max <= 0) return List.of();
                result.add(new Output(stack(field(content, "content")), ExpectedAmount.of(Math.min(max, Math.max(0, chance)), max)));
            }
        }
        return result;
    }

    private static GenericStack stack(Object value) throws ReflectiveOperationException {
        if (value instanceof ItemStack item) return GenericStack.fromItemStack(item);
        if (value instanceof Ingredient ingredient) {
            var items = ingredient.getItems();
            if (items.length != 1) throw new IllegalArgumentException("Ambiguous output ingredient");
            return GenericStack.fromItemStack(items[0]);
        }
        if (value instanceof FluidStack fluid) return new GenericStack(AEFluidKey.of(fluid), fluid.getAmount());
        if (value.getClass().getName().equals("com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient")) {
            Object[] fluids = (Object[]) call(value, "getStacks");
            if (fluids.length != 1) throw new IllegalArgumentException("Ambiguous fluid output");
            return new GenericStack(AEFluidKey.of((Fluid) call(fluids[0], "getFluid"),
                    (CompoundTag) call(value, "getNbt")), ((Number) call(value, "getAmount")).longValue());
        }
        throw new IllegalArgumentException("Unsupported output content");
    }

    public static Object call(Object object, String name) throws ReflectiveOperationException {
        var methods = METHODS.get(object.getClass());
        var method = methods.get(name);
        if (method == null) {
            method = object.getClass().getMethod(name);
            methods.putIfAbsent(name, method);
        }
        return method.invoke(object);
    }
    private static Object field(Object object, String name) throws ReflectiveOperationException {
        return object.getClass().getField(name).get(object);
    }
    private static boolean isA(Object object, String name) {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass())
            if (type.getName().equals(name)) return true;
        return false;
    }
}
