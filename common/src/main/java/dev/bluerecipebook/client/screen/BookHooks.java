package dev.bluerecipebook.client.screen;

import dev.bluerecipebook.client.DevBooks;
import dev.bluerecipebook.client.book.BlueBook;
import dev.bluerecipebook.client.index.RecipeIndex;
import net.minecraft.client.gui.GuiGraphics;

public final class BookHooks {
    private BookHooks() {
    }

    public static void init(BookHost host) {
        if (host.skipBook()) {
            return;
        }
        RecipeIndex.rebuildFromLevel();
        host.attachment().prepare(host);
        host.relayout();
    }

    public static boolean narrowOpen(BookHost host) {
        return !host.skipBook() && DevBooks.showBlue() && host.narrow() && host.attachment().book.isOpen();
    }

    public static void renderBook(BookHost host, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (host.skipBook() || !DevBooks.showBlue() || narrowOpen(host)) {
            return;
        }
        host.attachment().book.render(graphics, mouseX, mouseY, partialTick);
    }

    public static void renderTooltip(BookHost host, GuiGraphics graphics, int mouseX, int mouseY) {
        if (host.skipBook() || !DevBooks.showBlue()) {
            return;
        }
        host.attachment().book.renderTooltip(graphics, mouseX, mouseY);
    }

    public static void tick(BookHost host) {
        if (!host.skipBook()) {
            host.attachment().book.tick();
        }
    }

    public static boolean keys(BookHost host, int key, int scan, int modifiers) {
        if (host.skipBook()) {
            return false;
        }
        return host.attachment().keyPressed(key, scan, modifiers, host::relayout, host.screen()::setFocused);
    }

    public static boolean interceptClick(BookHost host, double mouseX, double mouseY, int button) {
        if (host.skipBook() || !DevBooks.showBlue()) {
            return false;
        }
        BlueBook book = host.attachment().book;
        if (book.interceptClick(mouseX, mouseY, button)) {
            return true;
        }
        // A click outside every lightbox closes them all. The click still carries on to the page.
        book.dismissLightboxes();
        return narrowOpen(host) && !book.isMouseOver(mouseX, mouseY);
    }

    public static boolean hideSlotHover(BookHost host, double mouseX, double mouseY) {
        if (host.skipBook() || !DevBooks.showBlue()) {
            return false;
        }
        return host.attachment().book.lightboxCovers(mouseX, mouseY) || narrowOpen(host);
    }

    public static void recipesUpdated(BookHost host) {
        if (host.skipBook()) {
            return;
        }
        host.attachment().book.knownRecipesChanged();
    }
}
