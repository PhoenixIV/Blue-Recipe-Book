package dev.bluerecipebook.client.screen.mixin;

import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccess {
    /**
     * Writes the field and nothing else.
     * The vanilla {@code setVisible} would also build the search box, save the open state, and send a packet. Not what we want.
     * An accessor named {@code setVisible} would replace that method, hence the prefix.
     */
    @Accessor("visible")
    void blueRecipeBook$setVisible(boolean visible);

    @Accessor("ghostRecipe")
    GhostRecipe ghost();
}
