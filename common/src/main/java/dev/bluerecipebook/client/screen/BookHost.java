package dev.bluerecipebook.client.screen;

import dev.bluerecipebook.client.index.CraftingStation;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface BookHost {
    BlueBookAttachment attachment();

    RecipeBookComponent vanillaBook();

    CraftingStation station();

    BookAnchor anchor();

    AbstractContainerMenu menu();

    boolean narrow();

    boolean skipBook();

    AbstractContainerScreen<?> screen();

    int imageWidth();

    void setLeftPos(int left);

    <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderable(T widget);

    <T extends GuiEventListener & NarratableEntry> T addEventWidget(T widget);

    default void relayout() {
        this.attachment().layout(this.screen().width, this.screen().height, this.imageWidth(), this.narrow(), this::setLeftPos);
    }
}
