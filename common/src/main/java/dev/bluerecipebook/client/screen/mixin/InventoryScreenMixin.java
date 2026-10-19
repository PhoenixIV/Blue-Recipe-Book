package dev.bluerecipebook.client.screen.mixin;

import dev.bluerecipebook.client.index.CraftingStation;
import dev.bluerecipebook.client.screen.BlueBookAttachment;
import dev.bluerecipebook.client.screen.BookAnchor;
import dev.bluerecipebook.client.screen.BookHooks;
import dev.bluerecipebook.client.screen.BookHost;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends EffectRenderingInventoryScreen<InventoryMenu> implements BookHost {
    @Shadow
    private RecipeBookComponent recipeBookComponent;
    @Shadow
    private boolean widthTooNarrow;
    @Unique
    private final BlueBookAttachment blueRecipeBook$books = new BlueBookAttachment();

    protected InventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public BlueBookAttachment attachment() {
        return this.blueRecipeBook$books;
    }

    @Override
    public RecipeBookComponent vanillaBook() {
        return this.recipeBookComponent;
    }

    @Override
    public CraftingStation station() {
        return CraftingStation.INVENTORY;
    }

    @Override
    public BookAnchor anchor() {
        return BookAnchor.INVENTORY;
    }

    @Override
    public AbstractContainerMenu menu() {
        return this.menu;
    }

    @Override
    public boolean narrow() {
        return this.widthTooNarrow;
    }

    @Override
    public boolean skipBook() {
        // JEI asks while the screen is opening, before Minecraft has set this field.
        return this.minecraft != null && this.minecraft.gameMode != null && this.minecraft.gameMode.hasInfiniteItems();
    }

    @Override
    public InventoryScreen screen() {
        return (InventoryScreen) (Object) this;
    }

    @Override
    public int imageWidth() {
        return this.imageWidth;
    }

    @Override
    public void setLeftPos(int left) {
        this.leftPos = left;
    }

    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderable(T widget) {
        return this.addRenderableWidget(widget);
    }

    @Override
    public <T extends GuiEventListener & NarratableEntry> T addEventWidget(T widget) {
        return this.addWidget(widget);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void blueRecipeBook$init(CallbackInfo callback) {
        BookHooks.init(this);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void blueRecipeBook$narrow(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        if (!BookHooks.narrowOpen(this)) {
            return;
        }
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        this.blueRecipeBook$books.book.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        this.blueRecipeBook$books.book.renderTooltip(graphics, mouseX, mouseY);
        callback.cancel();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
    private void blueRecipeBook$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        BookHooks.renderBook(this, graphics, mouseX, mouseY, partialTick);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void blueRecipeBook$tooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        BookHooks.renderTooltip(this, graphics, mouseX, mouseY);
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void blueRecipeBook$tick(CallbackInfo callback) {
        BookHooks.tick(this);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void blueRecipeBook$keys(int key, int scan, int modifiers, CallbackInfoReturnable<Boolean> callback) {
        if (BookHooks.keys(this, key, scan, modifiers)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void blueRecipeBook$click(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (BookHooks.interceptClick(this, mouseX, mouseY, button)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "isHovering(IIIIDD)Z", at = @At("HEAD"), cancellable = true)
    private void blueRecipeBook$hover(int x, int y, int width, int height, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> callback) {
        if (BookHooks.hideSlotHover(this, mouseX, mouseY)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "recipesUpdated", at = @At("TAIL"))
    private void blueRecipeBook$recipes(CallbackInfo callback) {
        BookHooks.recipesUpdated(this);
    }
}
