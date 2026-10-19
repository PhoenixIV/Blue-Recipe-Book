package dev.bluerecipebook.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * Development controls, which exist only while the client was started with
 * {@code -Dbluerecipebook.devToggle=true}. A built jar does not set that property.
 */
public final class DevBooks {
    public static final String PROPERTY = "bluerecipebook.devToggle";

    private static boolean blue = true;

    private DevBooks() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean(PROPERTY);
    }

    /** Returns whether the blue book shows. Always true outside dev mode. */
    public static boolean showBlue() {
        return !enabled() || blue;
    }

    public static void toggle() {
        if (enabled()) {
            blue = !blue;
        }
    }

    public static String label() {
        return showBlue() ? "Blue" : "Vanilla";
    }

    public static boolean onCraftingScreen() {
        Screen screen = Minecraft.getInstance().screen;
        return screen instanceof InventoryScreen || screen instanceof CraftingScreen;
    }
}
