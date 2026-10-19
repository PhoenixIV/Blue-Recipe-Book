package dev.bluerecipebook.client.index;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

/** The flat list of synced recipes, without special and incomplete ones. */
public final class RecipeIndex {
    private static List<RecipeHolder<?>> recipes = List.of();
    private static int generation;

    private RecipeIndex() {
    }

    public static void rebuild(RecipeManager manager) {
        List<RecipeHolder<?>> next = new ArrayList<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (recipe.isSpecial() || recipe.isIncomplete()) {
                continue;
            }
            next.add(holder);
        }
        recipes = List.copyOf(next);
        generation++;
    }

    public static void rebuildFromLevel() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            rebuild(minecraft.level.getRecipeManager());
        }
    }

    public static List<RecipeHolder<?>> recipes() {
        return recipes;
    }

    /**
     * Returns whether the client's recipe manager has {@code id}, special and incomplete recipes included.
     * {@link Presence#UNKNOWN} when that list has not arrived, so a caller does not treat every id as missing.
     */
    public static Presence contains(ResourceLocation id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return Presence.UNKNOWN;
        }
        RecipeManager manager = minecraft.level.getRecipeManager();
        if (manager.getRecipes().isEmpty()) {
            return Presence.UNKNOWN;
        }
        return manager.byKey(id).isPresent() ? Presence.PRESENT : Presence.ABSENT;
    }

    /** Returns a number that changes each time {@link #recipes()} is rebuilt. */
    public static int generation() {
        return generation;
    }

    /** Whether {@link #contains} found the id. Unknown means the recipe list has not arrived. */
    public enum Presence {
        UNKNOWN,
        PRESENT,
        ABSENT
    }
}
