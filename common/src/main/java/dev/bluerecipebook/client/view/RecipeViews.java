package dev.bluerecipebook.client.view;

import dev.architectury.platform.Platform;
import dev.bluerecipebook.BlueRecipeBook;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional JEI access: opens a result in JEI when that mod is loaded.
 * The screen class is loaded by name, so a client without JEI never initializes it.
 */
public final class RecipeViews {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueRecipeBook.MOD_ID);

    private RecipeViews() {
    }

    public static void openOutput(ItemStack stack) {
        if (stack.isEmpty() || !Platform.isModLoaded("jei")) {
            return;
        }
        try {
            Class.forName("dev.bluerecipebook.client.view.JeiRecipeScreen")
                    .getMethod("open", ItemStack.class)
                    .invoke(null, stack);
        } catch (ReflectiveOperationException exception) {
            LOGGER.error("Could not open JEI for {}", stack.getHoverName().getString(), exception);
        }
    }
}
