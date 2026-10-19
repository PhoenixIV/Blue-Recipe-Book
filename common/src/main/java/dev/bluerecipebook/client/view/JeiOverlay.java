package dev.bluerecipebook.client.view;

import dev.bluerecipebook.client.DevBooks;
import dev.bluerecipebook.client.screen.BookHost;
import net.minecraft.client.gui.screens.Screen;

/** The rule for hiding JEI's item list on the blue book screens. That list is what takes Ctrl+F. */
public final class JeiOverlay {
    private JeiOverlay() {
    }

    public static boolean hidden(Screen screen) {
        return DevBooks.showBlue() && screen instanceof BookHost host && !host.skipBook();
    }
}
