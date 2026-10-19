package dev.bluerecipebook.client.screen;

import dev.bluerecipebook.client.DevBooks;
import dev.bluerecipebook.client.book.BlueBook;
import dev.bluerecipebook.client.screen.mixin.RecipeBookComponentAccess;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.inventory.RecipeBookType;
import org.lwjgl.glfw.GLFW;

/** Book widgets for one inventory or crafting screen. */
public final class BlueBookAttachment {
    public final BlueBook book = new BlueBook();
    public BlueBookButton blueButton;
    public ImageButton vanillaButton;
    private final DevCorner devCorner = new DevCorner();
    private boolean narrow;
    private BookAnchor anchor = BookAnchor.INVENTORY;

    /**
     * Attaches the book to the host's screen.
     * Run after the screen's own {@code init}, which adds the vanilla button this looks up.
     */
    public void prepare(BookHost host) {
        AbstractContainerScreen<?> screen = host.screen();
        this.anchor = host.anchor();
        this.vanillaButton = findVanillaButton(screen);
        this.blueButton = new BlueBookButton(0, 0, () -> {
            this.book.toggleOpen();
            host.relayout();
        });
        host.addRenderable(this.blueButton);
        this.devCorner.prepare(host, this.book);
        host.addEventWidget(this.book);
        this.book.setAfterChange(host::relayout);
        this.book.bind(host.menu(), host.vanillaBook(), host.station());
    }

    public void layout(int screenWidth, int screenHeight, int imageWidth, boolean narrow, IntConsumer setLeft) {
        this.narrow = narrow;
        boolean showBlue = DevBooks.showBlue();
        RecipeBookComponent vanilla = this.book.vanillaBook();
        RecipeBookComponentAccess access = (RecipeBookComponentAccess) (Object) vanilla;
        // The accessor writes the field. setVisible saves the open state and sends a packet.
        int left;
        if (showBlue) {
            access.blueRecipeBook$setVisible(false);
            left = BookLayout.screenLeft(screenWidth, imageWidth, this.book.isOpen(), narrow);
        } else {
            RecipeBook saved = Minecraft.getInstance().player.getRecipeBook();
            access.blueRecipeBook$setVisible(saved.isOpen(RecipeBookType.CRAFTING));
            left = vanilla.updateScreenPosition(screenWidth, imageWidth);
        }
        setLeft.accept(left);
        int x = this.anchor.buttonX(left);
        int y = this.anchor.buttonY(screenHeight);
        if (this.vanillaButton != null) {
            this.vanillaButton.visible = !showBlue;
            this.vanillaButton.setPosition(x, y);
        }
        if (this.blueButton != null) {
            this.blueButton.visible = showBlue;
            this.blueButton.setPosition(x, y);
        }
        this.devCorner.layout();
        this.book.setShown(showBlue);
        this.book.layout(screenWidth, screenHeight, narrow);
    }

    public boolean keyPressed(int key, int scan, int modifiers, Runnable relayout, Consumer<GuiEventListener> focus) {
        boolean find = key == GLFW.GLFW_KEY_F && Screen.hasControlDown() && !Screen.hasAltDown();
        if (find) {
            if (DevBooks.enabled() && !DevBooks.showBlue()) {
                DevBooks.toggle();
            }
            // Ctrl+F goes to search, so the open lightboxes close.
            this.book.dismissLightboxes();
            // layout() focuses the box after the screen focuses the book.
            this.book.openAndFocusSearch();
            focus.accept(this.book);
            relayout.run();
            return true;
        }
        // The screen closes on Escape before a focused widget sees the key.
        if (DevBooks.showBlue() && this.book.isOpen() && key == GLFW.GLFW_KEY_ESCAPE) {
            if (this.book.closeLightbox()) {
                return true;
            }
            if (this.narrow) {
                this.book.toggleOpen();
                relayout.run();
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the vanilla recipe-book button, or null when no child matches.
     * The screen does not expose it, so this finds the 20 by 18 image button.
     */
    private static ImageButton findVanillaButton(AbstractContainerScreen<?> screen) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof ImageButton button && button.getWidth() == 20 && button.getHeight() == 18) {
                return button;
            }
        }
        return null;
    }
}
