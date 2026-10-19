package dev.bluerecipebook.client.strict;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

/**
 * The check that every published ingredient of a recipe has been held, where one held item covers every slot
 * that accepts it and any one item from a tag covers that tag.
 * An empty {@code getItems()} is a blank slot in a shaped pattern and is skipped.
 * Special and incomplete recipes never publish those entries; callers leave them out of the book.
 */
final class FoundIngredients {
    private FoundIngredients() {
    }

    static boolean satisfied(Recipe<?> recipe) {
        NonNullList<Ingredient> ingredients;
        try {
            ingredients = recipe.getIngredients();
        } catch (RuntimeException exception) {
            return true;
        }
        for (Ingredient ingredient : ingredients) {
            if (ingredient == null) {
                continue;
            }
            ItemStack[] stacks;
            try {
                stacks = ingredient.getItems();
            } catch (RuntimeException exception) {
                continue;
            }
            if (stacks == null || stacks.length == 0) {
                continue;
            }
            if (!accepted(stacks)) {
                return false;
            }
        }
        return true;
    }

    private static boolean accepted(ItemStack[] stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && HeldItems.has(stack.getItem())) {
                return true;
            }
        }
        return false;
    }
}
