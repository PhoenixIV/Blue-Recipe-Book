package dev.bluerecipebook.client.index;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;

/** The open screen's crafting grid. A cooking recipe reports that it fits every size, so the crafting-recipe check comes first. */
public enum CraftingStation {
    INVENTORY(2, 2),
    TABLE(3, 3);

    private final int width;
    private final int height;

    CraftingStation(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public boolean fits(Recipe<?> recipe) {
        return recipe instanceof CraftingRecipe && recipe.canCraftInDimensions(this.width, this.height);
    }
}
