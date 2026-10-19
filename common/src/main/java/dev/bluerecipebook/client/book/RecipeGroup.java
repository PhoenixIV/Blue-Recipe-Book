package dev.bluerecipebook.client.book;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** One page slot, holding the recipes that share a {@link #key(RecipeHolder, boolean)}. */
public final class RecipeGroup {
    private final List<RecipeHolder<?>> recipes = new ArrayList<>();

    /**
     * Returns the slot key of a recipe: its group string when {@code grouped} is on and it has one, otherwise its result item.
     * A recipe with no result keeps its own slot.
     * Vanilla's recipe-book tabs play no part, so Search can join recipes that vanilla keeps on different tabs.
     */
    public static String key(RecipeHolder<?> holder, boolean grouped) {
        if (grouped) {
            String group = holder.value().getGroup();
            if (group != null && !group.isEmpty()) {
                return "group\0" + group;
            }
        }
        ResourceLocation result = resultItem(holder);
        return result == null ? holder.id().toString() : "result\0" + result;
    }

    /** Returns the result stack, or empty when the client has no level or the recipe cannot name it. */
    public static ItemStack result(RecipeHolder<?> holder) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return ItemStack.EMPTY;
        }
        try {
            return holder.value().getResultItem(minecraft.level.registryAccess());
        } catch (RuntimeException ignored) {
            // A mod recipe can fail to name its result.
            return ItemStack.EMPTY;
        }
    }

    /** Returns the item this recipe makes, or null when the result is empty. */
    private static ResourceLocation resultItem(RecipeHolder<?> holder) {
        ItemStack stack = result(holder);
        return stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    public void add(RecipeHolder<?> holder) {
        this.recipes.add(holder);
    }

    public List<RecipeHolder<?>> recipes() {
        return this.recipes;
    }

    public int size() {
        return this.recipes.size();
    }
}
