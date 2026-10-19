package dev.bluerecipebook.client.screen;

import dev.bluerecipebook.client.BookSettings;
import dev.bluerecipebook.client.BookSettingsScreen;
import dev.bluerecipebook.client.DevBooks;
import dev.bluerecipebook.client.book.BlueBook;
import dev.bluerecipebook.client.dev.DevFeedScreen;
import dev.bluerecipebook.client.discover.DiscoveryOrder;
import dev.bluerecipebook.client.strict.StrictDiscovery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

/** Corner buttons registered only when dev mode is enabled ({@code -Dbluerecipebook.devToggle=true} is set). */
public final class DevCorner {
    private static final int MARGIN = 2;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 2;
    private static final int SWAP_WIDTH = 64;
    private static final int FILTER_WIDTH = 90;
    private static final int UNLOCK_WIDTH = 60;
    private static final int FEED_WIDTH = 44;
    private static final int RESET_WIDTH = 52;
    private static final int OPTIONS_WIDTH = 60;

    private Button swap;
    private Button filter;
    private Button unlock;
    private Button reset;
    private Button options;
    private Button feed;

    public void prepare(BookHost host, BlueBook book) {
        if (!DevBooks.enabled()) {
            return;
        }
        this.swap = Button.builder(Component.literal(DevBooks.label()), button -> {
            DevBooks.toggle();
            host.relayout();
        }).bounds(0, MARGIN, SWAP_WIDTH, BUTTON_HEIGHT).build();
        host.addRenderable(this.swap);
        this.filter = Button.builder(filterLabel(), button -> {
            toggleRevealAllRecipes();
            book.markDirty();
            host.relayout();
        }).bounds(0, MARGIN, FILTER_WIDTH, BUTTON_HEIGHT).build();
        host.addRenderable(this.filter);
        this.unlock = Button.builder(Component.literal("Unlock"), button -> unlockAll()).bounds(0, MARGIN, UNLOCK_WIDTH, BUTTON_HEIGHT).build();
        host.addRenderable(this.unlock);
        this.feed = Button.builder(Component.literal("Feed"), button -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new DevFeedScreen(minecraft.screen));
        }).bounds(0, MARGIN, FEED_WIDTH, BUTTON_HEIGHT).build();
        host.addRenderable(this.feed);
        this.reset = Button.builder(Component.literal("Reset"), button -> resetAll()).bounds(0, MARGIN, RESET_WIDTH, BUTTON_HEIGHT).build();
        host.addRenderable(this.reset);
        this.options = Button.builder(Component.translatable("gui.bluerecipebook.options"), button -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(BookSettingsScreen.open(minecraft.screen));
        }).bounds(0, MARGIN, OPTIONS_WIDTH, BUTTON_HEIGHT).build();
        host.addRenderable(this.options);
    }

    public void layout() {
        int x = MARGIN;
        if (this.swap != null) {
            this.swap.setMessage(Component.literal(DevBooks.label()));
            x = place(this.swap, x, SWAP_WIDTH);
        }
        if (this.filter != null) {
            this.filter.setMessage(filterLabel());
            x = place(this.filter, x, FILTER_WIDTH);
        }
        if (this.unlock != null) {
            x = place(this.unlock, x, UNLOCK_WIDTH);
        }
        if (this.feed != null) {
            x = place(this.feed, x, FEED_WIDTH);
        }
        if (this.reset != null) {
            x = place(this.reset, x, RESET_WIDTH);
        }
        if (this.options != null) {
            place(this.options, x, OPTIONS_WIDTH);
        }
    }

    private static int place(Button button, int x, int width) {
        button.setPosition(x, MARGIN);
        return x + width + BUTTON_GAP;
    }

    private static void unlockAll() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return;
        }
        connection.sendCommand("recipe give @s *");
        connection.sendCommand("advancement grant @s from minecraft:recipes/root");
    }

    private static void resetAll() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.sendCommand("recipe take @s *");
            connection.sendCommand("advancement revoke @s from minecraft:recipes/root");
        }
        // Empty the files now. The remove packets arrive later, and a leftover id still sorts as discovered.
        DiscoveryOrder.clear();
        StrictDiscovery.clear();
    }

    private static void toggleRevealAllRecipes() {
        BookSettings settings = BookSettings.get();
        settings.revealAllRecipes = !settings.revealAllRecipes;
        BookSettings.save();
    }

    private static Component filterLabel() {
        return Component.translatable(BookSettings.get().revealAllRecipes
                ? "gui.bluerecipebook.filter.all"
                : "gui.bluerecipebook.filter.discovered");
    }
}
