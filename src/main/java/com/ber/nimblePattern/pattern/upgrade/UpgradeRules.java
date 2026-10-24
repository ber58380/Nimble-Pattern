package com.ber.nimblePattern.pattern.upgrade;

import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public final class UpgradeRules {
    private UpgradeRules() {
    }

    /**
     * The recommendation rules is: lower cost > higher output
     */
    public static boolean improves(long oldAmount, long oldInputCost, long amount, long inputCost) {
        return inputCost < oldInputCost || inputCost == oldInputCost && amount > oldAmount;
    }

    public static <T> T unique(Collection<T> values, Predicate<T> matches) {
        T result = null;
        for (T value : values) {
            if (matches.test(value)) {
                if (result != null) {
                    return null;
                }
                result = value;
            }
        }
        return result;
    }

    public static <A, B> boolean contained(Collection<A> pattern, Collection<B> recipe,
                                           BiPredicate<A, B> accepts) {
        return pattern.stream().allMatch(a -> recipe.stream().anyMatch(b -> accepts.test(a, b)));
    }
}
