package com.ber.nimblePattern.client.gui.search;

import appeng.api.stacks.AEKey;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

public class PatternSearch {
    private String query = "";

    private Predicate<ItemStack> search = (e) -> true;
    final Map<AEKey, String> tooltipCache = new WeakHashMap<>();

    public PatternSearch() {
    }

    public String getQuery() {
        return query;
    }

    public void setSearchString(String query) {
        if (!query.equals(this.query)) {
            this.search = fromString(query);
            this.query = query;
        }
    }

    public boolean matches(ItemStack stack) {
        return search.test(stack);
    }

    private Predicate<ItemStack> fromString(String searchString) {
        var orParts = searchString.split("\\|");

        if (orParts.length == 1) {
            return AndSearchPredicate.of(getPredicates(orParts[0]));
        }
        var orPartFilters = new ArrayList<Predicate<ItemStack>>(orParts.length);

        for (String orPart : orParts) {
            orPartFilters.add(AndSearchPredicate.of(getPredicates(orPart)));
        }

        return OrSearchPredicate.of(orPartFilters);
    }


    private List<Predicate<ItemStack>> getPredicates(String query) {
        var terms = query.toLowerCase().trim().split("\\s+");
        var predicateFilters = new ArrayList<Predicate<ItemStack>>(terms.length);

        for (String part : terms) {
            if (part.startsWith("@")) {
                predicateFilters.add(new ModSearchPredicate(part.substring(1)));
            } else if (part.startsWith("#")) {
                predicateFilters.add(new TooltipsSearchPredicate(part.substring(1), tooltipCache));
            } else if (part.startsWith("$")) {
                predicateFilters.add(new TagSearchPredicate(part.substring(1)));
            } else if (part.startsWith("*")) {
                predicateFilters.add(new ItemIdSearchPredicate(part.substring(1)));
            } else if (part.startsWith("%")) {
                predicateFilters.add(new ConditionSearchPredicate(part.substring(1)));
            } else if (part.startsWith("~")) {
                predicateFilters.add(new StatusSearchPredicate(part.substring(1)));
            } else {
                predicateFilters.add(new NameSearchPredicate(part));
            }
        }

        return predicateFilters;
    }
}
