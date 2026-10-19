package dev.bluerecipebook.client.screen;

import dev.bluerecipebook.BlueRecipeBook;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;

public final class BlueBookButton extends AbstractWidget {
    private static final ResourceLocation NORMAL = ResourceLocation.fromNamespaceAndPath(
            BlueRecipeBook.MOD_ID, "textures/gui/recipe_book_button.png");
    private static final ResourceLocation HIGHLIGHTED = ResourceLocation.fromNamespaceAndPath(
            BlueRecipeBook.MOD_ID, "textures/gui/recipe_book_button_highlighted.png");

    private final Runnable onPress;

    public BlueBookButton(int x, int y, Runnable onPress) {
        super(x, y, 20, 18, CommonComponents.EMPTY);
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation texture = this.isHoveredOrFocused() ? HIGHLIGHTED : NORMAL;
        graphics.blit(texture, this.getX(), this.getY(), 0, 0, this.width, this.height, 20, 18);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.onPress.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
