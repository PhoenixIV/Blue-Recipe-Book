package dev.bluerecipebook.client.screen.mixin;

import dev.bluerecipebook.client.strict.StrictDiscovery;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Delay of the unlock toast until strict discovery can show the recipe.
 * The unlock packet has already granted the recipe, so only the toast waits.
 */
@Mixin(RecipeToast.class)
public class RecipeToastMixin {
    @Inject(method = "addOrUpdate", at = @At("HEAD"), cancellable = true)
    private static void blueRecipeBook$delay(ToastComponent toasts, RecipeHolder<?> holder, CallbackInfo callback) {
        if (StrictDiscovery.delayToast(holder)) {
            callback.cancel();
        }
    }
}
