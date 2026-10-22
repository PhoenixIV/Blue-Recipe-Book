package dev.bluerecipebook.client.book;

import dev.bluerecipebook.client.BookMemory;
import dev.bluerecipebook.client.BookSettings;
import dev.bluerecipebook.client.DevBooks;
import dev.bluerecipebook.client.category.BookCategories;
import dev.bluerecipebook.client.category.BookCategory;
import dev.bluerecipebook.client.discover.DiscoveryOrder;
import dev.bluerecipebook.client.strict.StrictDiscovery;
import dev.bluerecipebook.client.index.CraftingStation;
import dev.bluerecipebook.client.index.RecipeIndex;
import dev.bluerecipebook.client.screen.BookLayout;
import dev.bluerecipebook.client.screen.mixin.RecipeBookComponentAccess;
import dev.bluerecipebook.client.search.ResultNames;
import dev.bluerecipebook.client.view.RecipeViews;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.GhostRecipe;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The blue book panel the player sees. Unlock flags, the open state, and grid placement stay on the vanilla book. */
public final class BlueBook implements GuiEventListener, NarratableEntry {
    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/recipe_book.png");
    private static final ResourceLocation TAB = ResourceLocation.withDefaultNamespace("recipe_book/tab");
    private static final ResourceLocation TAB_SELECTED = ResourceLocation.withDefaultNamespace("recipe_book/tab_selected");
    private static final ResourceLocation SLOT_CRAFTABLE = ResourceLocation.withDefaultNamespace("recipe_book/slot_craftable");
    private static final ResourceLocation SLOT_UNCRAFTABLE = ResourceLocation.withDefaultNamespace("recipe_book/slot_uncraftable");
    private static final ResourceLocation SLOT_MANY_CRAFTABLE = ResourceLocation.withDefaultNamespace("recipe_book/slot_many_craftable");
    private static final ResourceLocation SLOT_MANY_UNCRAFTABLE = ResourceLocation.withDefaultNamespace("recipe_book/slot_many_uncraftable");
    private static final ResourceLocation PAGE_FORWARD = ResourceLocation.withDefaultNamespace("recipe_book/page_forward");
    private static final ResourceLocation PAGE_FORWARD_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("recipe_book/page_forward_highlighted");
    private static final ResourceLocation PAGE_BACKWARD = ResourceLocation.withDefaultNamespace("recipe_book/page_backward");
    private static final ResourceLocation PAGE_BACKWARD_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("recipe_book/page_backward_highlighted");
    private static final Component SEARCH_NAME = Component.translatable("gui.bluerecipebook.search");
    private static final float BOOK_Z = 100.0f;
    private static final float SHADE_Z = 250.0f;
    private static final float TOOLTIP_Z = 300.0f;
    /** Z of the first lightbox. Each one above it adds {@code LAYER_STEP}, so items and frames of the upper one stay on top. */
    private static final float LAYER_Z = 400.0f;
    private static final float LAYER_STEP = 400.0f;
    private static final int BOOK_SHADE = 0x60000000;
    /** Inner panel of recipe_book.png, inside the frame. */
    private static final int PAGE_LEFT = 8;
    private static final int PAGE_TOP = 8;
    private static final int PAGE_WIDTH = 131;
    private static final int PAGE_HEIGHT = 150;
    /** Square over the magnifying glass painted in recipe_book.png, left of the search text. */
    private static final int SEARCH_GLASS_X = 9;
    private static final int SEARCH_GLASS_Y = 12;
    private static final int SEARCH_GLASS = 16;
    private static final ItemStack SEARCH_ICON = new ItemStack(Items.COMPASS);
    /** Vanilla's top-right button sprites. Here they switch the order and do not filter. */
    private static final ResourceLocation ORDER_ON = ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled");
    private static final ResourceLocation ORDER_ON_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled_highlighted");
    private static final ResourceLocation ORDER_OFF = ResourceLocation.withDefaultNamespace("recipe_book/filter_disabled");
    private static final ResourceLocation ORDER_OFF_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("recipe_book/filter_disabled_highlighted");
    private static final int ORDER_X = 110;
    private static final int ORDER_Y = 12;
    private static final int ORDER_WIDTH = 26;
    private static final int ORDER_HEIGHT = 16;
    private static final int TAB_X = 30;
    private static final int TAB_Y = 3;
    private static final int TAB_WIDTH = 35;
    private static final int TAB_HEIGHT = 27;
    private static final int TAB_SELECTED_SHIFT = 2;
    private static final int SLOT_SIZE = 25;
    private static final int SLOT_ORIGIN_Y = 31;
    /** First pixel under the four rows of recipes, where the page number and arrows sit. */
    private static final int PAGE_FOOTER_Y = SLOT_ORIGIN_Y + SLOT_SIZE * 4;
    private static final int PAGE_BUTTON_Y = 137;
    private static final int PAGE_BACK_X = 38;
    private static final int PAGE_FORWARD_X = 93;
    private static final int PAGE_BUTTON_WIDTH = 12;
    private static final int PAGE_BUTTON_HEIGHT = 17;
    private static final Component ORDER_CRAFTABLE_FIRST = Component.translatable("gui.bluerecipebook.order.craftable_first");
    private static final Component ORDER_NEWEST_FIRST = Component.translatable("gui.bluerecipebook.order.newest_first");

    private final List<RecipeGroup> visible = new ArrayList<>();
    /** Open lightboxes, bottom first. The first is the page slot's, and each one above is an item's alternatives. */
    private final List<Lightbox> lightboxes = new ArrayList<>();
    private final Set<ResourceLocation> craftable = new HashSet<>();
    private List<BookCategory> shownCategories = List.of();
    private AbstractContainerMenu menu;
    private RecipeBookComponent vanillaBook;
    private CraftingStation station = CraftingStation.INVENTORY;
    private EditBox searchBox;
    private BookCategory category = BookCategories.byId(BookMemory.categoryId());
    private boolean searchGroup = BookMemory.searchGroup();
    /** Open flag last seen from the vanilla crafting book, so a later packet can move the screen. */
    private boolean seenOpen;
    private boolean shown = true;
    private boolean focusSearch;
    private boolean narrow;
    private int page = BookMemory.page();
    private int bookX;
    private int bookY;
    private int screenHeight;
    private int inventoryChanges = -1;
    private int indexGeneration = -1;
    private int discoveryGeneration = -1;
    private int strictRevealedGeneration = -1;
    private int strictSettingsGeneration = -1;
    /** Recipes that passed the tab, unlock and text checks for {@code matchedQuery}. A longer query searches only this list. Null when nothing is reusable. */
    private List<RecipeHolder<?>> matched;
    private String matchedQuery = "";
    /** Unlocked recipes that fit this station or no grid. Rebuilt when that set can change, so typing reads it without a rebuild. */
    private List<RecipeHolder<?>> unlockedRecipes = List.of();
    private final Map<ResourceLocation, String> resultNames = new HashMap<>();
    private int resultNamesGeneration = -1;
    private float iconTime;
    /** {@link NewArrivals#generation()} last applied to the tabs, or -1 before the first time. */
    private int armedArrival = -1;
    private boolean arrivalsOpen;
    private boolean groupRecipes;
    private boolean revealAllRecipes;
    /** Page slot that opened the first lightbox, or -1 when none is open or its group is not in the visible list. */
    private int openGroupSlot = -1;
    private String appliedQuery = BookMemory.query();
    private List<Component> tooltip = List.of();
    private Runnable afterChange = () -> {
    };
    private boolean focused;

    public void bind(AbstractContainerMenu menu, RecipeBookComponent vanillaBook, CraftingStation station) {
        this.menu = menu;
        this.vanillaBook = vanillaBook;
        this.station = station;
        this.seenOpen = this.isOpen();
        this.searchGroup = BookMemory.searchGroup();
        BookCategory selected = BookCategories.byId(BookMemory.categoryId());
        this.category = selected == null ? BookCategories.all().getFirst() : selected;
        this.page = BookMemory.page();
        this.appliedQuery = BookMemory.query();
        this.groupRecipes = groupSetting();
        this.revealAllRecipes = revealAllRecipesSetting();
        this.dismissLightboxes();
        this.resetQuery();
        this.markDirty();
    }

    public RecipeBookComponent vanillaBook() {
        return this.vanillaBook;
    }

    public void setShown(boolean shown) {
        this.shown = shown;
        if (!shown) {
            this.dismissLightboxes();
        }
    }

    /** Returns the vanilla crafting book's saved open state. The server stores that flag. */
    public boolean isOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.getRecipeBook().isOpen(RecipeBookType.CRAFTING);
    }

    public void toggleOpen() {
        boolean open = !this.isOpen();
        this.publishOpen(open);
        if (open) {
            this.resetQuery();
        } else {
            this.dismissLightboxes();
            this.unfocusSearch();
        }
    }

    public void openAndFocusSearch() {
        if (!this.isOpen()) {
            this.resetQuery();
        }
        this.publishOpen(true);
        this.focusSearch = true;
    }

    /**
     * Saves {@code open} on the vanilla crafting book and sends it to the server.
     * The vanilla widgets stay hidden while the blue book is showing. The saved flag stays.
     */
    private void publishOpen(boolean open) {
        if (this.isOpen() == open) {
            this.seenOpen = open;
            return;
        }
        if (this.vanillaBook != null) {
            RecipeBookComponentAccess access = (RecipeBookComponentAccess) (Object) this.vanillaBook;
            access.blueRecipeBook$applyOpen(open);
            if (DevBooks.showBlue()) {
                access.blueRecipeBook$setVisible(false);
            }
        }
        this.seenOpen = this.isOpen();
    }

    /** Moves the screen when a recipe packet replaces the saved open flag. */
    private void followOpen() {
        boolean open = this.isOpen();
        if (open == this.seenOpen) {
            return;
        }
        this.seenOpen = open;
        if (!open) {
            this.dismissLightboxes();
            this.unfocusSearch();
        } else if (this.shown) {
            this.resetQuery();
        }
        this.afterChange.run();
    }

    public void unfocusSearch() {
        if (this.searchBox != null) {
            this.searchBox.setFocused(false);
        }
    }

    private void selectSearch() {
        this.searchBox.setFocused(true);
        this.searchBox.moveCursorToEnd(false);
        this.searchBox.setHighlightPos(0);
    }

    private boolean overSearchGlass(double mouseX, double mouseY) {
        return inside(mouseX, mouseY, this.bookX + SEARCH_GLASS_X, this.bookY + SEARCH_GLASS_Y, SEARCH_GLASS, SEARCH_GLASS);
    }

    /**
     * Rebuilds the tabs, the unlocked snapshot and the page, and closes the lightboxes.
     * Typing skips this and reuses the unlocked snapshot.
     */
    public void markDirty() {
        this.matched = null;
        this.rebuildUnlocked();
        this.refreshList();
        this.captureGenerations();
    }

    /**
     * Rebuilds the unlocked snapshot after the known-set packet and leaves the lightboxes open.
     * That packet is how an unlock arrives, and {@link #markDirty} would close them.
     */
    public void knownRecipesChanged() {
        this.matched = null;
        this.rebuildUnlocked();
        this.refreshList(false);
        // The order append runs after this returns, so this counter is still the previous one and the next tick sorts the new ids.
        this.captureGenerations();
    }

    private void captureGenerations() {
        this.indexGeneration = RecipeIndex.generation();
        this.discoveryGeneration = DiscoveryOrder.generation();
        this.strictRevealedGeneration = StrictDiscovery.revealedGeneration();
        this.strictSettingsGeneration = StrictDiscovery.settingsGeneration();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            this.inventoryChanges = minecraft.player.getInventory().getTimesChanged();
        }
    }

    private void rebuildUnlocked() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            this.unlockedRecipes = List.of();
            this.shownCategories = List.of();
            return;
        }
        boolean revealAllRecipes = revealAllRecipesSetting();
        this.revealAllRecipes = revealAllRecipes;
        // Held items decide strict visibility, so they are read before the snapshot is taken.
        StrictDiscovery.prepare();
        this.unlockedRecipes = this.unlocked(minecraft.player.getRecipeBook(), revealAllRecipes);
        this.refreshShownCategories(this.unlockedRecipes);
    }

    public void setAfterChange(Runnable afterChange) {
        this.afterChange = afterChange;
    }

    public void layout(int screenWidth, int screenHeight, boolean narrow) {
        this.screenHeight = screenHeight;
        this.narrow = narrow;
        this.bookX = BookLayout.bookX(screenWidth, narrow);
        this.bookY = BookLayout.bookY(screenHeight);
        this.ensureSearchBox();
        this.searchBox.setPosition(this.bookX + 25, this.bookY + 13);
        // Ctrl+F asks for focus here, after the screen has focused the book and the box has a position.
        if (this.focusSearch) {
            this.selectSearch();
            this.focusSearch = false;
        }
    }

    public void tick() {
        // An unfocused box still paints its selection, so a dropped focus looks ready and the next character does nothing.
        if (!this.focused) {
            this.unfocusSearch();
        }
        this.followOpen();
        if (!this.shown || this.menu == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        int generation = RecipeIndex.generation();
        int discovered = DiscoveryOrder.generation();
        int revealed = StrictDiscovery.revealedGeneration();
        int strictSettings = StrictDiscovery.settingsGeneration();
        int changes = minecraft.player.getInventory().getTimesChanged();
        boolean group = groupSetting();
        boolean revealAllRecipes = revealAllRecipesSetting();
        // A recipe reload, or turning strict discovery, grouping, or Reveal all recipes on or off, closes the lightboxes.
        // An unlock keeps them open either way. The page follows the open slot.
        if (generation != this.indexGeneration || strictSettings != this.strictSettingsGeneration
                || group != this.groupRecipes || revealAllRecipes != this.revealAllRecipes) {
            this.groupRecipes = group;
            this.revealAllRecipes = revealAllRecipes;
            this.markDirty();
        } else if (discovered != this.discoveryGeneration || revealed != this.strictRevealedGeneration) {
            this.discoveryGeneration = discovered;
            this.strictRevealedGeneration = revealed;
            this.inventoryChanges = changes;
            this.matched = null;
            this.rebuildUnlocked();
            this.refreshList(false, true);
        } else if (changes != this.inventoryChanges) {
            this.inventoryChanges = changes;
            // Placing a recipe changes the inventory. The lightbox stays; only craftable flags and page order update.
            this.refreshList(false);
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.tooltip = List.of();
        if (!this.isOpen()) {
            this.arrivalsOpen = false;
            return;
        }
        if (!this.shown) {
            return;
        }
        // The world tick stops while this screen is open, so the icon advances from render time.
        if (!Screen.hasControlDown()) {
            this.iconTime += partialTick;
        }
        this.armArrivals();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, BOOK_Z);
        graphics.blit(BOOK_TEXTURE, this.bookX, this.bookY, 1, 1, BookLayout.IMAGE_WIDTH, BookLayout.IMAGE_HEIGHT);
        if (this.searchBox != null) {
            this.searchBox.render(graphics, mouseX, mouseY, partialTick);
        }
        this.renderTabs(graphics, mouseX, mouseY, partialTick);
        this.renderPage(graphics, mouseX, mouseY, partialTick);
        this.renderOrderButton(graphics, mouseX, mouseY);
        if (!this.lightboxes.isEmpty()) {
            this.renderBookShade(graphics);
            List<Component> picked = List.of();
            for (int layer = 0; layer < this.lightboxes.size(); layer++) {
                float z = LAYER_Z + LAYER_STEP * layer;
                boolean covered = this.coveredAbove(layer, mouseX, mouseY);
                List<Component> lines = this.lightboxes.get(layer).render(
                        graphics, mouseX, mouseY, z, this.iconTime, RecipeGroup::result, this.craftable, covered);
                if (!lines.isEmpty()) {
                    picked = lines;
                }
                if (layer + 1 < this.lightboxes.size()) {
                    this.lightboxes.get(layer).renderShade(graphics, z + SHADE_Z);
                }
            }
            // renderPage already named the slot under the cursor. Keep that only for the undimmed group.
            if (!picked.isEmpty() || !this.openGroupContains(mouseX, mouseY) || this.anyLightboxContains(mouseX, mouseY)) {
                this.tooltip = picked;
            }
        }
        NewArrivals.advanceItems(partialTick);
        graphics.pose().popPose();
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.shown || !this.isOpen() || this.tooltip.isEmpty()) {
            return;
        }
        // A tooltip is drawn above the topmost lightbox. Each lightbox sits LAYER_STEP above the one below it.
        if (!this.lightboxes.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0f, 0.0f, TOOLTIP_Z + LAYER_STEP * this.lightboxes.size());
            graphics.renderComponentTooltip(Minecraft.getInstance().font, this.tooltip, mouseX, mouseY);
            graphics.pose().popPose();
            return;
        }
        graphics.renderComponentTooltip(Minecraft.getInstance().font, this.tooltip, mouseX, mouseY);
    }

    /**
     * Returns whether a lightbox, or the page slot that opened the first one, took the click.
     * A click anywhere else returns false and leaves the lightboxes open, so the caller dismisses them.
     */
    public boolean interceptClick(double mouseX, double mouseY, int button) {
        if (!this.shown || this.lightboxes.isEmpty()) {
            return false;
        }
        for (int index = this.lightboxes.size() - 1; index >= 0; index--) {
            Lightbox box = this.lightboxes.get(index);
            if (!box.contains(mouseX, mouseY)) {
                continue;
            }
            int cell = box.indexAt(mouseX, mouseY);
            if (button == 0 && cell >= 0) {
                this.closeAbove(index);
                this.pressCell(index, cell);
            }
            return true;
        }
        if (this.openGroupContains(mouseX, mouseY)) {
            this.closeAbove(0);
            this.clickRecipe(this.openGroupSlot, button);
            return true;
        }
        return false;
    }

    /**
     * Handles a click on a cell. A cell with several recipes opens an alternative recipes lightbox on top, after placing one when it can.
     * Alt or Ctrl opens the viewer on the result and places nothing.
     */
    private void pressCell(int layer, int cell) {
        Lightbox box = this.lightboxes.get(layer);
        List<RecipeHolder<?>> recipes = box.cell(cell);
        if (altOrControl()) {
            RecipeViews.openOutput(RecipeGroup.result(GroupOrder.cellRecipe(recipes, this.craftable, this.groupRecipes)));
            return;
        }
        if (recipes.size() == 1) {
            this.activate(recipes.getFirst());
        } else {
            RecipeHolder<?> chosen = GroupOrder.cellRecipe(recipes, this.craftable, this.groupRecipes);
            this.placeIfPossible(chosen);
            if (this.isOpen()) {
                box.setOpenCell(cell);
                this.pushLightbox(true, singles(recipes), box.cellX(cell), box.cellY(cell));
            }
        }
        // A narrow screen closes the book when a recipe is placed. The lightboxes close with it.
        if (!this.isOpen()) {
            this.dismissLightboxes();
        }
    }

    /** Closes the top lightbox. Returns false when none is open. */
    public boolean closeLightbox() {
        if (this.lightboxes.isEmpty()) {
            return false;
        }
        this.lightboxes.removeLast();
        if (this.lightboxes.isEmpty()) {
            this.openGroupSlot = -1;
        }
        return true;
    }

    public void dismissLightboxes() {
        this.lightboxes.clear();
        this.openGroupSlot = -1;
    }

    /** Closes the lightboxes above {@code layer} and clears the cell of {@code layer} that opened them. */
    private void closeAbove(int layer) {
        while (this.lightboxes.size() > layer + 1) {
            this.lightboxes.removeLast();
        }
        if (layer >= 0 && layer < this.lightboxes.size()) {
            this.lightboxes.get(layer).setOpenCell(-1);
        }
    }

    private void pushLightbox(boolean alternativeRecipes, List<List<RecipeHolder<?>>> cells, int anchorX, int anchorY) {
        Lightbox box = new Lightbox(alternativeRecipes);
        box.open(cells, anchorX, anchorY, this.bookX + PAGE_LEFT, this.bookX + PAGE_LEFT + PAGE_WIDTH, this.screenHeight);
        this.lightboxes.add(box);
        this.unfocusSearch();
    }

    public boolean lightboxCovers(double mouseX, double mouseY) {
        return this.shown && this.anyLightboxContains(mouseX, mouseY);
    }

    private boolean anyLightboxContains(double mouseX, double mouseY) {
        for (Lightbox box : this.lightboxes) {
            if (box.contains(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    private boolean coveredAbove(int layer, double mouseX, double mouseY) {
        for (int above = layer + 1; above < this.lightboxes.size(); above++) {
            if (this.lightboxes.get(above).contains(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.shown || !this.isOpen()) {
            return false;
        }
        if (this.interceptClick(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && this.searchBox != null && this.overSearchGlass(mouseX, mouseY)) {
            this.selectSearch();
            return true;
        }
        if (this.searchBox != null && this.searchBox.mouseClicked(mouseX, mouseY, button)) {
            // mouseClicked places the cursor and does not focus the box.
            this.searchBox.setFocused(true);
            return true;
        }
        this.unfocusSearch();
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (button != 0 && button != 1) {
            return true;
        }
        if (button == 0 && this.clickOrderButton(mouseX, mouseY)) {
            return true;
        }
        if (this.clickTab(mouseX, mouseY)) {
            return true;
        }
        if (this.clickPage(mouseX, mouseY)) {
            return true;
        }
        int slot = this.slotAt(mouseX, mouseY);
        if (slot >= 0) {
            this.clickRecipe(slot, button);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // The vanilla book reports that the mouse is never over it, so the wheel is handled here.
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (scrollY == 0) {
            return true;
        }
        if (this.overPageFooter(mouseX, mouseY) || !scrollMovesItems()) {
            this.turnPage(scrollY > 0 ? -1 : 1);
            this.dismissLightboxes();
            return true;
        }
        RecipeHolder<?> holder = this.recipeUnder(mouseX, mouseY);
        if (holder != null) {
            this.scrollRecipe(holder, scrollY > 0);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (!this.shown || !this.isOpen() || this.searchBox == null || !this.searchBox.isFocused()) {
            return false;
        }
        this.searchBox.keyPressed(key, scan, modifiers);
        // A key the box ignores is still handled, so the inventory key, drop, hotbar, and Tab do not run.
        return true;
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (!this.shown || !this.isOpen() || this.searchBox == null || !this.searchBox.isFocused()) {
            return false;
        }
        return this.searchBox.charTyped(character, modifiers);
    }

    @Override
    public void setFocused(boolean focused) {
        // The screen blurs the current widget, then focuses it again. Clearing the search box here undoes Ctrl+F and a click on the field.
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.shown || !this.isOpen()) {
            return false;
        }
        if (this.anyLightboxContains(mouseX, mouseY)) {
            return true;
        }
        return inside(mouseX, mouseY, this.bookX - 32, this.bookY, BookLayout.IMAGE_WIDTH + 32, BookLayout.IMAGE_HEIGHT);
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
    }

    private void armArrivals() {
        int arrival = NewArrivals.generation();
        if (!this.arrivalsOpen || arrival != this.armedArrival) {
            NewArrivals.arm(this.station);
            this.armedArrival = arrival;
        }
        this.arrivalsOpen = true;
    }

    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int index = 0;
        boolean searchSelected = this.viewingSearch();
        if (this.renderTab(graphics, mouseX, mouseY, index, searchSelected, List.of(SEARCH_ICON))) {
            this.tooltip = List.of(SEARCH_NAME);
        }
        index++;
        for (BookCategory group : this.shownCategories) {
            boolean selected = !searchSelected && group == this.category;
            float time = NewArrivals.tabTime(group.craftingCategory());
            if (this.renderTab(graphics, mouseX, mouseY, index, selected, group.icons(), time, partialTick,
                    delta -> NewArrivals.advanceTab(group.craftingCategory(), delta))) {
                this.tooltip = List.of(group.name());
            }
            index++;
        }
    }

    private boolean renderTab(GuiGraphics graphics, int mouseX, int mouseY, int index, boolean selected, List<ItemStack> icons) {
        int x = this.bookX - TAB_X;
        int y = this.bookY + TAB_Y + TAB_HEIGHT * index;
        int drawX = selected ? x - TAB_SELECTED_SHIFT : x;
        this.drawTab(graphics, x, drawX, y, selected, icons);
        return inside(mouseX, mouseY, drawX, y, TAB_WIDTH, TAB_HEIGHT);
    }

    private boolean renderTab(GuiGraphics graphics, int mouseX, int mouseY, int index, boolean selected, List<ItemStack> icons,
            float time, float partialTick, Consumer<Float> advance) {
        int x = this.bookX - TAB_X;
        int y = this.bookY + TAB_Y + TAB_HEIGHT * index;
        int drawX = selected ? x - TAB_SELECTED_SHIFT : x;
        NewArrivals.animateTab(graphics, x, y, TAB_HEIGHT, time, () -> this.drawTab(graphics, x, drawX, y, selected, icons));
        if (time > 0.0F) {
            advance.accept(partialTick);
        }
        return inside(mouseX, mouseY, drawX, y, TAB_WIDTH, TAB_HEIGHT);
    }

    private void drawTab(GuiGraphics graphics, int x, int drawX, int y, boolean selected, List<ItemStack> icons) {
        graphics.blitSprite(selected ? TAB_SELECTED : TAB, drawX, y, TAB_WIDTH, TAB_HEIGHT);
        int shift = selected ? -TAB_SELECTED_SHIFT : 0;
        if (icons.size() >= 2) {
            graphics.renderFakeItem(icons.get(0), x + 3 + shift, y + 5);
            graphics.renderFakeItem(icons.get(1), x + 14 + shift, y + 5);
        } else if (!icons.isEmpty()) {
            graphics.renderFakeItem(icons.get(0), x + 9 + shift, y + 5);
        }
    }

    /**
     * Darkens the inner page, leaving the frame and the tabs clear. With one lightbox the open group stays clear too.
     * With a second lightbox the whole page is dimmed, and each lightbox below the top one dims itself.
     */
    private void renderBookShade(GuiGraphics graphics) {
        int left = this.bookX + PAGE_LEFT;
        int top = this.bookY + PAGE_TOP;
        int right = left + PAGE_WIDTH;
        int bottom = top + PAGE_HEIGHT;
        graphics.pose().pushPose();
        // Page items are at z 150 and their counts at z 200. The first lightbox is at z 400.
        graphics.pose().translate(0.0f, 0.0f, SHADE_Z);
        if (this.openGroupSlot < 0 || this.lightboxes.size() > 1) {
            graphics.fill(left, top, right, bottom, BOOK_SHADE);
            graphics.pose().popPose();
            return;
        }
        int slotLeft = this.slotX(this.openGroupSlot);
        int slotTop = this.slotY(this.openGroupSlot);
        int slotRight = slotLeft + SLOT_SIZE;
        int slotBottom = slotTop + SLOT_SIZE;
        graphics.fill(left, top, right, slotTop, BOOK_SHADE);
        graphics.fill(left, slotBottom, right, bottom, BOOK_SHADE);
        graphics.fill(left, slotTop, slotLeft, slotBottom, BOOK_SHADE);
        graphics.fill(slotRight, slotTop, right, slotBottom, BOOK_SHADE);
        graphics.pose().popPose();
    }

    private boolean openGroupContains(double mouseX, double mouseY) {
        if (this.openGroupSlot < 0) {
            return false;
        }
        return inside(mouseX, mouseY, this.slotX(this.openGroupSlot), this.slotY(this.openGroupSlot), SLOT_SIZE, SLOT_SIZE);
    }

    private void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int pages = this.pageCount();
        if (pages > 1) {
            Component label = Component.translatable("gui.recipebook.page", this.page + 1, pages);
            int textWidth = Minecraft.getInstance().font.width(label);
            graphics.drawString(Minecraft.getInstance().font, label, this.bookX - textWidth / 2 + 73, this.bookY + 141, -1, false);
        }
        for (int slot = 0; slot < BookLayout.RECIPES_PER_PAGE; slot++) {
            GroupOrder order = this.groupAt(slot);
            if (order == null) {
                break;
            }
            int x = this.slotX(slot);
            int y = this.slotY(slot);
            this.renderSlot(graphics, order, x, y, partialTick);
            if (inside(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE)) {
                this.tooltip = this.slotTooltip(order);
            }
        }
        if (pages > 1) {
            this.renderPageButton(graphics, mouseX, mouseY, this.bookX + PAGE_BACK_X, true);
            this.renderPageButton(graphics, mouseX, mouseY, this.bookX + PAGE_FORWARD_X, false);
        }
    }

    private void renderOrderButton(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean craftableFirst = craftableFirstSetting();
        int x = this.bookX + ORDER_X;
        int y = this.bookY + ORDER_Y;
        boolean hovered = inside(mouseX, mouseY, x, y, ORDER_WIDTH, ORDER_HEIGHT);
        ResourceLocation sprite;
        if (craftableFirst) {
            sprite = hovered ? ORDER_ON_HIGHLIGHTED : ORDER_ON;
        } else {
            sprite = hovered ? ORDER_OFF_HIGHLIGHTED : ORDER_OFF;
        }
        graphics.blitSprite(sprite, x, y, ORDER_WIDTH, ORDER_HEIGHT);
        if (hovered) {
            this.tooltip = List.of(craftableFirst ? ORDER_CRAFTABLE_FIRST : ORDER_NEWEST_FIRST);
        }
    }

    /** Returns whether the click hit the order button. A hit flips the order and saves the setting, so it survives a restart. */
    private boolean clickOrderButton(double mouseX, double mouseY) {
        if (!inside(mouseX, mouseY, this.bookX + ORDER_X, this.bookY + ORDER_Y, ORDER_WIDTH, ORDER_HEIGHT)) {
            return false;
        }
        BookSettings settings = BookSettings.get();
        settings.craftableFirst = !settings.craftableFirst;
        BookSettings.save();
        this.refreshList();
        this.setPage(0);
        return true;
    }

    private void renderPageButton(GuiGraphics graphics, int mouseX, int mouseY, int x, boolean backward) {
        int y = this.bookY + PAGE_BUTTON_Y;
        boolean hovered = inside(mouseX, mouseY, x, y, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT);
        ResourceLocation sprite;
        if (backward) {
            sprite = hovered ? PAGE_BACKWARD_HIGHLIGHTED : PAGE_BACKWARD;
        } else {
            sprite = hovered ? PAGE_FORWARD_HIGHLIGHTED : PAGE_FORWARD;
        }
        graphics.blitSprite(sprite, x, y, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT);
    }

    private void renderSlot(GuiGraphics graphics, GroupOrder order, int x, int y, float partialTick) {
        List<RecipeHolder<?>> members = order.members();
        if (members.isEmpty()) {
            return;
        }
        String key = RecipeGroup.key(members.getFirst(), this.groupRecipes);
        float time = NewArrivals.claim(key, members);
        NewArrivals.animateSlot(graphics, x, y, SLOT_SIZE, time, () -> this.drawSlot(graphics, order, x, y));
        if (time > 0.0F) {
            NewArrivals.advanceGroup(key, partialTick);
        }
    }

    private void drawSlot(GuiGraphics graphics, GroupOrder order, int x, int y) {
        List<RecipeHolder<?>> members = order.members();
        // One recipe per item. The icon walks items, never the several recipes of one item.
        List<RecipeHolder<?>> reps = order.reps();
        boolean many = members.size() > 1;
        boolean bright = false;
        for (RecipeHolder<?> holder : members) {
            if (this.craftable.contains(holder.id())) {
                bright = true;
                break;
            }
        }
        ResourceLocation sprite;
        if (many) {
            sprite = bright ? SLOT_MANY_CRAFTABLE : SLOT_MANY_UNCRAFTABLE;
        } else {
            sprite = bright ? SLOT_CRAFTABLE : SLOT_UNCRAFTABLE;
        }
        graphics.blitSprite(sprite, x, y, SLOT_SIZE, SLOT_SIZE);
        RecipeHolder<?> front = order.front();
        ItemStack stack = RecipeGroup.result(front);
        if (stack.isEmpty()) {
            return;
        }
        int drawX = x + 4;
        int drawY = y + 4;
        if (reps.size() > 1) {
            RecipeHolder<?> rear = order.rear();
            ItemStack next = rear == null ? ItemStack.EMPTY : RecipeGroup.result(rear);
            if (!next.isEmpty()) {
                // Drawn lower so a block model does not cover the front icon.
                graphics.pose().pushPose();
                graphics.pose().translate(0.0f, 0.0f, -20.0f);
                graphics.renderFakeItem(next, drawX + 1, drawY + 1);
                graphics.pose().popPose();
                drawX--;
                drawY--;
            }
        }
        graphics.renderFakeItem(stack, drawX, drawY);
    }

    private boolean clickTab(double mouseX, double mouseY) {
        int tab = this.tabAt(mouseX, mouseY);
        if (tab < 0) {
            return false;
        }
        if (tab == 0) {
            this.searchGroup = true;
            BookMemory.setSearchGroup(true);
        } else {
            BookCategory selected = this.shownCategories.get(tab - 1);
            this.searchGroup = false;
            this.category = selected;
            BookMemory.setCategoryId(selected.id());
        }
        this.setPage(0);
        this.markDirty();
        return true;
    }

    private boolean clickPage(double mouseX, double mouseY) {
        if (this.pageCount() <= 1) {
            return false;
        }
        int y = this.bookY + PAGE_BUTTON_Y;
        if (inside(mouseX, mouseY, this.bookX + PAGE_BACK_X, y, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT)) {
            this.turnPage(-1);
            return true;
        }
        if (inside(mouseX, mouseY, this.bookX + PAGE_FORWARD_X, y, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT)) {
            this.turnPage(1);
            return true;
        }
        return false;
    }

    private void clickRecipe(int slot, int button) {
        if (button != 0) {
            return;
        }
        // Read before place. The place updates the pin, and the lightbox keeps the order from this read.
        GroupOrder order = this.groupAt(slot);
        if (order == null) {
            return;
        }
        List<RecipeHolder<?>> members = order.members();
        if (members.isEmpty()) {
            return;
        }
        if (altOrControl()) {
            RecipeViews.openOutput(RecipeGroup.result(order.front()));
            return;
        }
        if (members.size() > 1) {
            this.placeIfPossible(order.front());
            if (this.isOpen()) {
                this.openPage(slot, order);
            }
            return;
        }
        this.activate(members.getFirst());
    }

    /**
     * Opens the page slot's lightbox. One item opens an alternative recipes lightbox. Several items open a group lightbox,
     * and an item with several recipes opens an alternative recipes lightbox later, from its cell.
     */
    private void openPage(int slot, GroupOrder order) {
        this.dismissLightboxes();
        this.openGroupSlot = slot;
        this.pushSlot(order);
    }

    private void pushSlot(GroupOrder order) {
        List<List<RecipeHolder<?>>> cells = order.cells();
        boolean alternativeRecipes = cells.size() == 1;
        this.pushLightbox(
                alternativeRecipes,
                alternativeRecipes ? singles(order.members()) : cells,
                this.slotX(this.openGroupSlot),
                this.slotY(this.openGroupSlot));
    }

    private void activate(RecipeHolder<?> holder) {
        if (altOrControl() || !this.station.fits(holder.value())) {
            RecipeViews.openOutput(RecipeGroup.result(holder));
            return;
        }
        this.place(holder, Screen.hasShiftDown(), true);
    }

    private void scrollRecipe(RecipeHolder<?> holder, boolean add) {
        if (!this.station.fits(holder.value())) {
            return;
        }
        if (add) {
            this.place(holder, false, false);
            return;
        }
        this.takeCraft(holder);
    }

    private void takeCraft(RecipeHolder<?> holder) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || this.menu == null || this.vanillaBook == null) {
            return;
        }
        GhostRecipe ghost = ((RecipeBookComponentAccess) (Object) this.vanillaBook).ghost();
        if (this.gridHolds(holder)) {
            if (!GridReturn.takeOne(this.menu, minecraft.player)) {
                return;
            }
            ghost.clear();
            LastGroupUse.remember(RecipeGroup.key(holder, this.groupRecipes), holder.id());
            minecraft.player.removeRecipeHighlight(holder);
            return;
        }
        RecipeHolder<?> shown = ghost.getRecipe();
        if (shown != null && shown.id().equals(holder.id())) {
            ghost.clear();
        }
    }

    private static boolean sameGhost(GhostRecipe ghost, RecipeHolder<?> holder) {
        RecipeHolder<?> shown = ghost.getRecipe();
        return shown != null && shown.id().equals(holder.id());
    }

    @SuppressWarnings("unchecked")
    private boolean gridHolds(RecipeHolder<?> holder) {
        if (!(this.menu instanceof RecipeBookMenu book)) {
            return false;
        }
        return book.recipeMatches(holder);
    }

    private RecipeHolder<?> recipeUnder(double mouseX, double mouseY) {
        for (int layer = this.lightboxes.size() - 1; layer >= 0; layer--) {
            Lightbox box = this.lightboxes.get(layer);
            if (box.contains(mouseX, mouseY)) {
                int cell = box.indexAt(mouseX, mouseY);
                return cell < 0 ? null : GroupOrder.cellRecipe(box.cell(cell), this.craftable, this.groupRecipes);
            }
        }
        if (!this.lightboxes.isEmpty() && !this.openGroupContains(mouseX, mouseY)) {
            return null;
        }
        int slot = this.slotAt(mouseX, mouseY);
        if (slot < 0) {
            return null;
        }
        GroupOrder order = this.groupAt(slot);
        if (order == null || order.members().isEmpty()) {
            return null;
        }
        return order.front();
    }

    private void place(RecipeHolder<?> holder, boolean shift, boolean closeNarrow) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null || minecraft.player == null || this.vanillaBook == null || this.menu == null) {
            return;
        }
        GhostRecipe ghost = ((RecipeBookComponentAccess) (Object) this.vanillaBook).ghost();
        // Another place clears the ghost until the server sends it back, so a ghost that is already this recipe stays.
        if (sameGhost(ghost, holder) && (LockedGhost.missing(holder) || !this.craftable.contains(holder.id()))) {
            return;
        }
        if (LockedGhost.missing(holder)) {
            if (!LockedGhost.show(this.vanillaBook, this.menu, holder)) {
                return;
            }
        } else {
            // The hidden vanilla book still draws its ghost. Clear that ghost, then place.
            ghost.clear();
            minecraft.gameMode.handlePlaceRecipe(minecraft.player.containerMenu.containerId, holder, shift);
            if (!this.craftable.contains(holder.id())) {
                // The packet fills the ingredients later. The id is set now so the next scroll does not clear this and send again.
                ghost.setRecipe(holder);
            }
        }
        LastGroupUse.remember(RecipeGroup.key(holder, this.groupRecipes), holder.id());
        minecraft.player.removeRecipeHighlight(holder);
        if (closeNarrow && this.narrow) {
            this.publishOpen(false);
            this.unfocusSearch();
            this.afterChange.run();
        }
    }

    private int tabAt(double mouseX, double mouseY) {
        int count = 1 + this.shownCategories.size();
        boolean searchSelected = this.viewingSearch();
        for (int index = 0; index < count; index++) {
            int x = this.bookX - TAB_X;
            int y = this.bookY + TAB_Y + TAB_HEIGHT * index;
            boolean selected = index == 0 ? searchSelected : !searchSelected && this.shownCategories.get(index - 1) == this.category;
            int drawX = selected ? x - TAB_SELECTED_SHIFT : x;
            if (inside(mouseX, mouseY, drawX, y, TAB_WIDTH, TAB_HEIGHT)) {
                return index;
            }
        }
        return -1;
    }

    private int slotAt(double mouseX, double mouseY) {
        for (int slot = 0; slot < BookLayout.RECIPES_PER_PAGE; slot++) {
            int entry = this.page * BookLayout.RECIPES_PER_PAGE + slot;
            if (entry >= this.visible.size()) {
                break;
            }
            if (inside(mouseX, mouseY, this.slotX(slot), this.slotY(slot), SLOT_SIZE, SLOT_SIZE)) {
                return slot;
            }
        }
        return -1;
    }

    private void setPage(int page) {
        int pages = this.pageCount();
        this.page = Mth.clamp(page, 0, Math.max(0, pages - 1));
        BookMemory.setPage(this.page);
    }

    /** Steps the open page. Past either end the other end opens. */
    private void turnPage(int step) {
        this.setPage(Math.floorMod(this.page + step, this.pageCount()));
    }

    private int pageCount() {
        return Math.max(1, (this.visible.size() + BookLayout.RECIPES_PER_PAGE - 1) / BookLayout.RECIPES_PER_PAGE);
    }

    private void ensureSearchBox() {
        if (this.searchBox != null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        this.searchBox = new EditBox(
                minecraft.font,
                this.bookX + 25,
                this.bookY + 13,
                81,
                minecraft.font.lineHeight + 5,
                Component.translatable("itemGroup.search"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setTextColor(0xFFFFFF);
        this.searchBox.setValue(BookMemory.query());
        this.searchBox.setHint(Component.translatable("gui.recipebook.search_hint").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
        // setValue notifies the responder. Attach it only after appliedQuery matches the box.
        this.appliedQuery = this.searchBox.getValue();
        this.searchBox.setResponder(value -> this.onSearchChanged());
    }

    private void resetQuery() {
        // Opening keeps a search when that option is on. The tab and the page come from BookMemory.
        if (BookSettings.get().openWhereYouLeftOff) {
            return;
        }
        if (this.searchBox != null && !this.searchBox.getValue().isEmpty()) {
            // setValue notifies the responder, which refreshes the page and leaves the unlocked snapshot.
            this.searchBox.setValue("");
            return;
        }
        if (this.appliedQuery.isEmpty()) {
            return;
        }
        this.appliedQuery = "";
        BookMemory.setQuery("");
        this.setPage(0);
        this.markDirty();
    }

    private void onSearchChanged() {
        if (this.searchBox == null) {
            return;
        }
        String query = this.searchBox.getValue();
        if (query.equals(this.appliedQuery)) {
            return;
        }
        this.appliedQuery = query;
        BookMemory.setQuery(query);
        this.setPage(0);
        this.refreshList();
    }

    private void refreshList() {
        this.refreshList(true, false);
    }

    private void refreshList(boolean closeLightboxes) {
        this.refreshList(closeLightboxes, false);
    }

    /**
     * Rebuilds the visible groups and the craftable flags for the current query.
     *
     * @param closeLightboxes false keeps the lightboxes open and turns to the page where that group now sits
     * @param orderChanged true also opens the popup again when its recipe order changed
     */
    private void refreshList(boolean closeLightboxes, boolean orderChanged) {
        String openKey = null;
        if (!closeLightboxes && this.openGroupSlot >= 0) {
            int entry = this.page * BookLayout.RECIPES_PER_PAGE + this.openGroupSlot;
            if (entry >= 0 && entry < this.visible.size() && !this.visible.get(entry).recipes().isEmpty()) {
                openKey = RecipeGroup.key(this.visible.get(entry).recipes().getFirst(), this.groupRecipes);
            }
        }
        if (closeLightboxes) {
            this.dismissLightboxes();
        }
        this.visible.clear();
        String query = (this.searchBox == null ? this.appliedQuery : this.searchBox.getValue()).toLowerCase(Locale.ROOT);
        List<RecipeHolder<?>> source;
        if (this.matched != null && query.startsWith(this.matchedQuery)) {
            // A longer query matches a subset of the last one, so that list is the candidate set.
            source = this.matched;
        } else {
            source = this.unlockedRecipes.stream().filter(holder -> this.onTab(holder.value())).toList();
        }
        List<RecipeHolder<?>> found = this.matchingText(source, query);
        this.matched = found;
        this.matchedQuery = query;
        Map<String, RecipeGroup> groups = new LinkedHashMap<>();
        for (RecipeHolder<?> holder : found) {
            String key = RecipeGroup.key(holder, this.groupRecipes);
            RecipeGroup group = groups.get(key);
            if (group == null) {
                group = new RecipeGroup();
                groups.put(key, group);
            }
            group.add(holder);
        }
        this.refreshCraft(found);
        this.visible.addAll(groups.values());
        this.visible.sort(this.pageOrder());
        if (this.page >= this.pageCount()) {
            // setPage clamps a page that is now past the end.
            this.setPage(this.page);
        }
        if (openKey != null) {
            int entry = this.entryOf(openKey);
            if (entry < 0) {
                this.openGroupSlot = -1;
            } else {
                int page = entry / BookLayout.RECIPES_PER_PAGE;
                if (page != this.page) {
                    this.setPage(page);
                }
                this.openGroupSlot = entry % BookLayout.RECIPES_PER_PAGE;
            }
        }
        if (!closeLightboxes) {
            this.refreshOpenLightboxes(orderChanged);
        }
    }

    /**
     * Opens the open slot's lightbox again when its recipes changed.
     * Craftable flags alone do not, so a place leaves the panel where the player is using it.
     * An alternative recipes popup on a cell is opened again on that same item.
     */
    private void refreshOpenLightboxes(boolean orderChanged) {
        if (this.lightboxes.isEmpty() || this.openGroupSlot < 0) {
            return;
        }
        GroupOrder order = this.groupAt(this.openGroupSlot);
        if (order == null) {
            return;
        }
        List<RecipeHolder<?>> members = order.members();
        Lightbox open = this.lightboxes.getFirst();
        boolean idsDiffer = !sameIds(open, members);
        boolean orderDiffers = orderChanged && !sameOrder(open, members);
        if (!idsDiffer && !orderDiffers) {
            return;
        }
        ItemStack upper = this.upperResult();
        this.lightboxes.clear();
        this.pushSlot(order);
        List<List<RecipeHolder<?>>> cells = order.cells();
        if (upper.isEmpty() || cells.size() == 1) {
            return;
        }
        int cell = cellOf(cells, upper);
        if (cell < 0) {
            return;
        }
        Lightbox box = this.lightboxes.getFirst();
        box.setOpenCell(cell);
        this.pushLightbox(true, singles(cells.get(cell)), box.cellX(cell), box.cellY(cell));
    }

    private ItemStack upperResult() {
        if (this.lightboxes.size() < 2 || this.lightboxes.get(1).cellCount() == 0) {
            return ItemStack.EMPTY;
        }
        return RecipeGroup.result(this.lightboxes.get(1).cell(0).getFirst()).copy();
    }

    private static boolean sameIds(Lightbox box, List<RecipeHolder<?>> recipes) {
        Set<ResourceLocation> shown = new HashSet<>();
        for (int index = 0; index < box.cellCount(); index++) {
            for (RecipeHolder<?> holder : box.cell(index)) {
                shown.add(holder.id());
            }
        }
        if (shown.size() != recipes.size()) {
            return false;
        }
        for (RecipeHolder<?> holder : recipes) {
            if (!shown.contains(holder.id())) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameOrder(Lightbox box, List<RecipeHolder<?>> recipes) {
        int index = 0;
        for (int cell = 0; cell < box.cellCount(); cell++) {
            for (RecipeHolder<?> holder : box.cell(cell)) {
                if (index >= recipes.size() || !holder.id().equals(recipes.get(index).id())) {
                    return false;
                }
                index++;
            }
        }
        return index == recipes.size();
    }

    private static int cellOf(List<List<RecipeHolder<?>>> cells, ItemStack result) {
        for (int index = 0; index < cells.size(); index++) {
            if (ItemStack.isSameItemSameComponents(RecipeGroup.result(cells.get(index).getFirst()), result)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Places {@code holder} when it fits this station and is craftable, or when it is shown as a locked ghost.
     */
    private void placeIfPossible(RecipeHolder<?> holder) {
        if (this.station.fits(holder.value()) && (this.craftable.contains(holder.id()) || LockedGhost.missing(holder))) {
            this.place(holder, Screen.hasShiftDown(), true);
        }
    }

    private static boolean altOrControl() {
        return Screen.hasAltDown() || Screen.hasControlDown();
    }

    /**
     * Returns whether this wheel event places or takes a craft.
     * Ctrl or Alt uses the other of the two wheel actions.
     */
    private static boolean scrollMovesItems() {
        return BookSettings.get().scrollToMoveItems != altOrControl();
    }

    /**
     * Returns whether the cursor is on the strip under the recipe grid.
     * A lightbox covering that strip stays with the popup.
     */
    private boolean overPageFooter(double mouseX, double mouseY) {
        if (this.anyLightboxContains(mouseX, mouseY)) {
            return false;
        }
        return inside(mouseX, mouseY, this.bookX, this.bookY + PAGE_FOOTER_Y,
                BookLayout.IMAGE_WIDTH, BookLayout.IMAGE_HEIGHT - PAGE_FOOTER_Y);
    }

    /** Returns the group in this page slot, or null when that slot is past the end of the list. */
    private GroupOrder groupAt(int slot) {
        int entry = this.page * BookLayout.RECIPES_PER_PAGE + slot;
        if (entry < 0 || entry >= this.visible.size()) {
            return null;
        }
        return GroupOrder.of(this.visible.get(entry), this.craftable, this.groupRecipes, this.iconTime);
    }

    /** Returns the index of the group with this key in the visible list, or -1 when it is absent. */
    private int entryOf(String key) {
        for (int entry = 0; entry < this.visible.size(); entry++) {
            List<RecipeHolder<?>> recipes = this.visible.get(entry).recipes();
            if (!recipes.isEmpty() && RecipeGroup.key(recipes.getFirst(), this.groupRecipes).equals(key)) {
                return entry;
            }
        }
        return -1;
    }

    /** Returns newest first, and a group with a craftable member before one without, when that order is on. */
    private Comparator<RecipeGroup> pageOrder() {
        Comparator<RecipeGroup> newestFirst = Comparator.comparingInt(this::groupRank).reversed();
        if (!craftableFirstSetting()) {
            return newestFirst;
        }
        return Comparator.comparingInt((RecipeGroup group) -> this.hasCraftable(group) ? 0 : 1).thenComparing(newestFirst);
    }

    private boolean hasCraftable(RecipeGroup group) {
        for (RecipeHolder<?> holder : group.recipes()) {
            if (this.craftable.contains(holder.id())) {
                return true;
            }
        }
        return false;
    }

    /** Fills {@code craftable} from the matched recipes, not the grouped slots, so the sort can read it before the groups are ordered. */
    private void refreshCraft(List<RecipeHolder<?>> candidates) {
        this.craftable.clear();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || this.menu == null) {
            return;
        }
        StackedContents contents = new StackedContents();
        minecraft.player.getInventory().fillStackedContents(contents);
        if (this.menu instanceof RecipeBookMenu<?, ?> bookMenu) {
            bookMenu.fillCraftSlotsStackedContents(contents);
        }
        for (RecipeHolder<?> holder : candidates) {
            if (!this.station.fits(holder.value())) {
                continue;
            }
            try {
                if (contents.canCraft(holder.value(), new IntArrayList())) {
                    this.craftable.add(holder.id());
                }
            } catch (RuntimeException ignored) {
                // A mod recipe can fail the stack match. The slot stays ghosted.
            }
        }
    }

    /**
     * Sets the category tabs to those with an unlocked recipe for this station. The search tab is always there.
     * The remembered category is left alone when it is hidden, so the crafting table can show it again.
     */
    private void refreshShownCategories(List<RecipeHolder<?>> unlocked) {
        Set<CraftingBookCategory> filled = EnumSet.noneOf(CraftingBookCategory.class);
        for (RecipeHolder<?> holder : unlocked) {
            if (this.station.fits(holder.value()) && holder.value() instanceof CraftingRecipe crafting) {
                filled.add(crafting.category());
            }
        }
        List<BookCategory> shown = new ArrayList<>();
        for (BookCategory category : BookCategories.all()) {
            if (filled.contains(category.craftingCategory())) {
                shown.add(category);
            }
        }
        this.shownCategories = List.copyOf(shown);
    }

    /**
     * Returns whether the recipe is listed. With Reveal all recipes on, every recipe is listed.
     * Otherwise it must be unlocked and, with strict discovery on, every ingredient must have been held.
     */
    private boolean listed(RecipeBook book, RecipeHolder<?> holder, boolean revealAllRecipes) {
        if (revealAllRecipes) {
            return true;
        }
        return book.contains(holder) && StrictDiscovery.visible(holder);
    }

    /** Returns the recipes that {@code listed} accepts and that fit this station. */
    private List<RecipeHolder<?>> unlocked(RecipeBook book, boolean revealAllRecipes) {
        List<RecipeHolder<?>> unlocked = new ArrayList<>();
        for (RecipeHolder<?> holder : RecipeIndex.recipes()) {
            if (this.station.fits(holder.value()) && this.listed(book, holder, revealAllRecipes)) {
                unlocked.add(holder);
            }
        }
        return unlocked;
    }

    /**
     * Keeps the recipes whose result name contains the query. A search drops members that miss it, so one remaining result is a single slot.
     *
     * @param query lowercase search text; an empty query returns {@code source} unchanged
     */
    private List<RecipeHolder<?>> matchingText(List<RecipeHolder<?>> source, String query) {
        if (query.isEmpty()) {
            return source;
        }
        List<RecipeHolder<?>> found = new ArrayList<>();
        for (RecipeHolder<?> holder : source) {
            if (ResultNames.matches(this.resultName(holder), query)) {
                found.add(holder);
            }
        }
        return found;
    }

    /** Returns the lowercase result name, cached per recipe until the index is rebuilt. */
    private String resultName(RecipeHolder<?> holder) {
        if (this.resultNamesGeneration != RecipeIndex.generation()) {
            this.resultNames.clear();
            this.resultNamesGeneration = RecipeIndex.generation();
        }
        return this.resultNames.computeIfAbsent(holder.id(), id -> ResultNames.lowerName(RecipeGroup.result(holder)));
    }

    private boolean viewingSearch() {
        return this.searchGroup || !this.shownCategories.contains(this.category);
    }

    private boolean onTab(Recipe<?> recipe) {
        if (!this.station.fits(recipe)) {
            return false;
        }
        if (this.viewingSearch()) {
            return true;
        }
        return recipe instanceof CraftingRecipe crafting
                && crafting.category() == this.category.craftingCategory();
    }

    private int slotX(int slot) {
        return this.bookX + 11 + SLOT_SIZE * (slot % 5);
    }

    private int slotY(int slot) {
        return this.bookY + SLOT_ORIGIN_Y + SLOT_SIZE * (slot / 5);
    }

    private List<Component> slotTooltip(GroupOrder order) {
        List<RecipeHolder<?>> members = order.members();
        if (members.isEmpty()) {
            return List.of();
        }
        ItemStack stack = RecipeGroup.result(order.front());
        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
        if (members.size() > 1) {
            lines.add(Lightbox.CHOOSE_TYPE);
        }
        return lines;
    }

    private static List<List<RecipeHolder<?>>> singles(List<RecipeHolder<?>> recipes) {
        List<List<RecipeHolder<?>>> cells = new ArrayList<>();
        for (RecipeHolder<?> recipe : recipes) {
            cells.add(List.of(recipe));
        }
        return cells;
    }

    /** Returns the rank of the newest member. A recipe this client has not seen unlocked ranks -1, so it does not pull the group forward. */
    private int groupRank(RecipeGroup group) {
        int rank = -1;
        for (RecipeHolder<?> holder : group.recipes()) {
            rank = Math.max(rank, GroupOrder.rank(holder.id()));
        }
        return rank;
    }

    private static boolean groupSetting() {
        return BookSettings.get().groupRecipes;
    }

    private static boolean revealAllRecipesSetting() {
        return BookSettings.get().revealAllRecipes;
    }

    private static boolean craftableFirstSetting() {
        return BookSettings.get().craftableFirst;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
