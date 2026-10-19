package dev.bluerecipebook.client.view;

import dev.bluerecipebook.BlueRecipeBook;
import java.lang.reflect.Field;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IScreenHandler;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IScreenHelper;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JeiPlugin
public class JeiRecipeScreen implements IModPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueRecipeBook.MOD_ID);
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(BlueRecipeBook.MOD_ID, "jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        hideItemList(jeiRuntime.getScreenHelper());
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    /** Shows the JEI recipes that make {@code stack}. Does nothing without a JEI runtime, or for an empty stack. */
    public static void open(ItemStack stack) {
        IJeiRuntime jei = runtime;
        if (jei == null || stack.isEmpty()) {
            return;
        }
        IFocus<ItemStack> focus = jei.getJeiHelpers().getFocusFactory()
                .createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack);
        jei.getRecipesGui().show(focus);
    }

    /**
     * Wraps JEI's handler for container screens so it returns null while the blue book is shown.
     * JEI draws the item list, and takes Ctrl+F, when the handler returns non-null, so null hides it.
     * The handler JEI registered still runs for every other screen.
     */
    @SuppressWarnings("unchecked")
    private static void hideItemList(IScreenHelper screens) {
        try {
            Field field = screens.getClass().getDeclaredField("guiScreenHandlers");
            field.setAccessible(true);
            Map<Class<?>, IScreenHandler<?>> handlers = (Map<Class<?>, IScreenHandler<?>>) field.get(screens);
            IScreenHandler<AbstractContainerScreen<?>> jei =
                    (IScreenHandler<AbstractContainerScreen<?>>) handlers.get(AbstractContainerScreen.class);
            IScreenHandler<AbstractContainerScreen<?>> wrapped = screen -> {
                if (JeiOverlay.hidden(screen)) {
                    return null;
                }
                return jei == null ? null : jei.apply(screen);
            };
            handlers.put(AbstractContainerScreen.class, wrapped);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.error("Could not hide JEI while the blue book is the recipe book", exception);
        }
    }
}
