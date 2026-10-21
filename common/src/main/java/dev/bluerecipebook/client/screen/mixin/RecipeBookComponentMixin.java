package dev.bluerecipebook.client.screen.mixin;

import dev.bluerecipebook.client.DevBooks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookComponent.class)
public class RecipeBookComponentMixin {
    @Shadow
    private void renderGhostRecipeTooltip(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void blueRecipeBook$keepHidden(CallbackInfo callback) {
        if (DevBooks.showBlue() && DevBooks.onCraftingScreen()) {
            // tick() writes the saved open state back into visible, which would show the vanilla book again.
            callback.cancel();
        }
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"))
    private void blueRecipeBook$ghostName(GuiGraphics graphics, int left, int top, int mouseX, int mouseY, CallbackInfo callback) {
        if (((RecipeBookComponent) (Object) this).isVisible()) {
            return;
        }
        if (!DevBooks.showBlue() || !DevBooks.onCraftingScreen()) {
            return;
        }
        // The vanilla tooltip returns while the book is hidden, which also skips the name on a missing ingredient.
        // Ghost rendering is a separate call and stays. Cancelling it would remove the preview from the grid.
        this.renderGhostRecipeTooltip(graphics, left, top, mouseX, mouseY);
    }
}
