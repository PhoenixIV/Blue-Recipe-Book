package dev.bluerecipebook.client.book;

import dev.bluerecipebook.client.screen.mixin.RecipeBookComponentAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Ghost for a recipe the player's book does not contain.
 * The server ignores the place packet in that case. {@code setupGhostRecipe} is what the ghost packet would have run.
 */
public final class LockedGhost {
    private LockedGhost() {
    }

    /** Returns whether the player's recipe book lacks the recipe. False when there is no player. */
    public static boolean missing(RecipeHolder<?> holder) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && !minecraft.player.getRecipeBook().contains(holder);
    }

    /**
     * Returns the grid to the inventory, then draws the ghost.
     * Reports false when those items do not fit, and leaves the grid and the ghost alone.
     * {@code setupGhostRecipe} only adds ingredients, so a ghost that was already showing is cleared first.
     */
    public static boolean show(RecipeBookComponent book, AbstractContainerMenu menu, RecipeHolder<?> holder) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !GridReturn.takeAll(menu, minecraft.player)) {
            return false;
        }
        ((RecipeBookComponentAccess) (Object) book).ghost().clear();
        book.setupGhostRecipe(holder, menu.slots);
        return true;
    }
}
