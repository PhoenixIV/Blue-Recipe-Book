package dev.bluerecipebook.client.screen.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccess {
    /**
     * Writes the field and nothing else.
     * An accessor named {@code setVisible} would replace that method, hence the prefix.
     */
    @Accessor("visible")
    void blueRecipeBook$setVisible(boolean visible);

    /**
     * Runs vanilla {@code setVisible}.
     * That saves the open state and sends it to the server. Callers that keep the blue book up then write the field false.
     */
    @Invoker("setVisible")
    void blueRecipeBook$applyOpen(boolean open);

    /** Null until {@code initVisuals}. The book crashes if it renders while this is null. */
    @Accessor("searchBox")
    EditBox searchBox();

    @Accessor("ghostRecipe")
    GhostRecipe ghost();
}
