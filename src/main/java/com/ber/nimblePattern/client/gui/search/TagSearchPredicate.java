package com.ber.nimblePattern.client.gui.search;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.crafting.pattern.EncodedPatternItem;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

final class TagSearchPredicate implements Predicate<ItemStack> {
    private final String term;
    /**
     * Stores the tag keys we found for each AE key type we encountered.
     */
    private final Map<AEKeyType, List<TagKey<?>>> tagCache = new IdentityHashMap<>();

    public TagSearchPredicate(String term) {
        this.term = term.toLowerCase(Locale.ROOT);
    }

    /**
     * Finds all tags for all AE key types that match the given search pattern.
     */
    private List<TagKey<?>> getTagsMatchingTerm(AEKeyType keyType) {
        return keyType.getTagNames()
                .filter(tagKey -> {
                    // ResourceLocations require namespace and path to already be lowercase
                    var tagId = tagKey.location();
                    if (term.contains(":")) {
                        return tagId.toString().contains(term);
                    } else {
                        return tagId.getNamespace().contains(term) || tagId.getPath().contains(term);
                    }
                })
                .toList();
    }

    @Override
    public boolean test(ItemStack stack) {
        ItemStack target = stack.getItem() instanceof EncodedPatternItem iep && !iep.getOutput(stack).isEmpty() ? iep.getOutput(stack) : ItemStack.EMPTY;
        var key = AEItemKey.of(target);
        if (key == null) {
            return false;
        }

        var tags = tagCache.computeIfAbsent(key.getType(), this::getTagsMatchingTerm);

        for (var tag : tags) {
            if (key.isTagged(tag)) {
                return true;
            }
        }

        return false;
    }
}
