package dev.bluerecipebook.client.book;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * One lightbox panel. A group lightbox has one cell per item, and a cell with several recipes gets the
 * many-recipe frame. An alternative recipes lightbox has one cell per recipe, drawn as vanilla's small crafting grid.
 */
public final class Lightbox {
    public static final Component CHOOSE_TYPE = Component.translatable("gui.bluerecipebook.group.choose").withStyle(ChatFormatting.GRAY);
    private static final ResourceLocation PANEL = ResourceLocation.withDefaultNamespace("recipe_book/overlay_recipe");
    private static final ResourceLocation SLOT_CRAFTABLE = ResourceLocation.withDefaultNamespace("recipe_book/slot_craftable");
    private static final ResourceLocation SLOT_UNCRAFTABLE = ResourceLocation.withDefaultNamespace("recipe_book/slot_uncraftable");
    private static final ResourceLocation SLOT_MANY_CRAFTABLE = ResourceLocation.withDefaultNamespace("recipe_book/slot_many_craftable");
    private static final ResourceLocation SLOT_MANY_UNCRAFTABLE = ResourceLocation.withDefaultNamespace("recipe_book/slot_many_uncraftable");
    private static final ResourceLocation CRAFTING = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay");
    private static final ResourceLocation CRAFTING_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay_highlighted");
    private static final ResourceLocation CRAFTING_DISABLED = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay_disabled");
    private static final ResourceLocation CRAFTING_DISABLED_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay_disabled_highlighted");
    private static final ResourceLocation FURNACE = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay");
    private static final ResourceLocation FURNACE_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay_highlighted");
    private static final ResourceLocation FURNACE_DISABLED = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay_disabled");
    private static final ResourceLocation FURNACE_DISABLED_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay_disabled_highlighted");
    private static final int CELL = 25;
    /** Vanilla's recipe button is 24 pixels. The 25 pixel step leaves a one pixel gap. */
    private static final int PREVIEW = 24;
    private static final int PAD = 4;
    /** Pixels between ingredient centres in the 3x3 grid. */
    private static final int STEP = 7;
    private static final int GRID_ORIGIN = 3;
    /** Centre of a lone furnace ingredient. */
    private static final int FURNACE_ORIGIN = 10;
    /** Vanilla draws each ingredient at 0.375 scale, so 16 pixels become 6. */
    private static final float ICON_SCALE = 0.375f;
    /** First pixel under the slot. The slot icon above stays clear. */
    private static final int BELOW_SLOT = CELL;
    private static final int SHADE = 0x60000000;

    private final boolean alternativeRecipes;
    private List<List<RecipeHolder<?>>> cells = List.of();
    /** Cell that opened the lightbox above this one, or -1. That cell stays undimmed. */
    private int openCell = -1;
    /** One layout per cell. Empty for a group lightbox. */
    private final List<Layout> layouts = new ArrayList<>();
    private int x;
    private int y;
    private int columns;
    private int width;
    private int height;

    public Lightbox(boolean alternativeRecipes) {
        this.alternativeRecipes = alternativeRecipes;
    }

    public boolean isOpen() {
        return !this.cells.isEmpty();
    }

    /**
     * Opens the panel below the slot at the anchor, centered on it and slid to stay between the bounds.
     * A panel wider than that span is centered on the span.
     * Vertically only the screen limits it, so it may extend past the book.
     *
     * @param boundLeft left edge of the inner page, inside the book frame
     * @param boundRight right edge of the inner page
     */
    public void open(List<List<RecipeHolder<?>>> cells, int anchorX, int anchorY, int boundLeft, int boundRight, int screenHeight) {
        this.cells = List.copyOf(cells);
        this.layouts.clear();
        if (this.alternativeRecipes) {
            for (List<RecipeHolder<?>> cell : this.cells) {
                this.layouts.add(layout(cell.getFirst()));
            }
        }
        int count = this.cells.size();
        this.columns = count > 16 ? 5 : 4;
        int across = Math.min(count, this.columns);
        int rows = Mth.ceil(count / (float) this.columns);
        this.width = across * CELL + PAD * 2;
        this.height = rows * CELL + PAD * 2;
        this.x = centeredOn(anchorX, boundLeft, boundRight);
        int top = anchorY + BELOW_SLOT;
        if (top + this.height > screenHeight) {
            top = screenHeight - this.height;
        }
        if (top < 0) {
            top = 0;
        }
        this.y = top;
    }

    public void close() {
        this.cells = List.of();
        this.layouts.clear();
        this.openCell = -1;
    }

    public void setOpenCell(int cell) {
        this.openCell = cell;
    }

    /** Darkens this panel. The cell that opened the alternative recipes lightbox above stays clear. */
    public void renderShade(GuiGraphics graphics, float z) {
        if (!this.isOpen()) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, z);
        int left = this.x;
        int top = this.y;
        int right = this.x + this.width;
        int bottom = this.y + this.height;
        if (this.openCell < 0) {
            graphics.fill(left, top, right, bottom, SHADE);
            graphics.pose().popPose();
            return;
        }
        int holeLeft = this.cellX(this.openCell);
        int holeTop = this.cellY(this.openCell);
        int holeRight = holeLeft + CELL;
        int holeBottom = holeTop + CELL;
        graphics.fill(left, top, right, holeTop, SHADE);
        graphics.fill(left, holeBottom, right, bottom, SHADE);
        graphics.fill(left, holeTop, holeLeft, holeBottom, SHADE);
        graphics.fill(holeRight, holeTop, right, holeBottom, SHADE);
        graphics.pose().popPose();
    }

    public int cellCount() {
        return this.cells.size();
    }

    /** Returns the recipes in a cell: several in a group lightbox, one in an alternative recipes lightbox. */
    public List<RecipeHolder<?>> cell(int index) {
        return this.cells.get(index);
    }

    public int cellX(int index) {
        return this.x + PAD + CELL * (index % this.columns);
    }

    public int cellY(int index) {
        return this.y + PAD + CELL * (index / this.columns);
    }

    public boolean contains(double mouseX, double mouseY) {
        return this.isOpen() && mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
    }

    /** Returns the index of the cell under the cursor, or -1. */
    public int indexAt(double mouseX, double mouseY) {
        for (int index = 0; index < this.cells.size(); index++) {
            int cellX = this.cellX(index);
            int cellY = this.cellY(index);
            if (mouseX >= cellX && mouseX < cellX + CELL && mouseY >= cellY && mouseY < cellY + CELL) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Draws the panel at {@code z}. A cell under the cursor is outlined and its tooltip returned,
     * unless {@code covered} says a lightbox above has the cursor.
     */
    public List<Component> render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float z,
            float time,
            Function<RecipeHolder<?>, ItemStack> result,
            Set<ResourceLocation> craftable,
            boolean covered) {
        if (!this.isOpen()) {
            return List.of();
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, z);
        graphics.blitSprite(PANEL, this.x, this.y, this.width, this.height);
        List<Component> tooltip = List.of();
        int hovered = covered ? -1 : this.indexAt(mouseX, mouseY);
        for (int index = 0; index < this.cells.size(); index++) {
            List<RecipeHolder<?>> cell = this.cells.get(index);
            boolean hover = index == hovered;
            int cellX = this.cellX(index);
            int cellY = this.cellY(index);
            if (this.alternativeRecipes) {
                this.renderPreview(graphics, index, cell.getFirst(), craftable, hover, time);
                if (hover) {
                    graphics.renderOutline(cellX, cellY, CELL, CELL, 0xFFFFFFFF);
                }
            } else {
                ResourceLocation item = RecipeGroup.resultId(cell.getFirst());
                float pop = item == null ? 0.0F : NewArrivals.itemTime(item);
                int drawIndex = index;
                NewArrivals.animateSlot(graphics, cellX, cellY, CELL, pop, () -> {
                    this.renderIcon(graphics, drawIndex, cell, result, craftable, hover);
                    if (hover) {
                        graphics.renderOutline(cellX, cellY, CELL, CELL, 0xFFFFFFFF);
                    }
                });
            }
            if (hover) {
                ItemStack stack = result.apply(cell.getFirst());
                List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
                if (cell.size() > 1) {
                    lines.add(CHOOSE_TYPE);
                }
                tooltip = lines;
            }
        }
        graphics.pose().popPose();
        return tooltip;
    }

    private void renderIcon(
            GuiGraphics graphics,
            int index,
            List<RecipeHolder<?>> cell,
            Function<RecipeHolder<?>, ItemStack> result,
            Set<ResourceLocation> craftable,
            boolean hover) {
        boolean bright = false;
        for (RecipeHolder<?> holder : cell) {
            bright |= craftable.contains(holder.id());
        }
        boolean many = cell.size() > 1;
        ResourceLocation sprite;
        if (many) {
            sprite = bright ? SLOT_MANY_CRAFTABLE : SLOT_MANY_UNCRAFTABLE;
        } else {
            sprite = bright ? SLOT_CRAFTABLE : SLOT_UNCRAFTABLE;
        }
        int cellX = this.cellX(index);
        int cellY = this.cellY(index);
        graphics.blitSprite(sprite, cellX, cellY, CELL, CELL);
        ItemStack stack = result.apply(cell.getFirst());
        if (!stack.isEmpty()) {
            graphics.renderFakeItem(stack, cellX + 4, cellY + 4);
        }
    }

    private void renderPreview(GuiGraphics graphics, int index, RecipeHolder<?> holder, Set<ResourceLocation> craftable, boolean hover, float time) {
        Layout layout = this.layouts.get(index);
        boolean bright = craftable.contains(holder.id());
        ResourceLocation sprite;
        if (layout.furnace()) {
            sprite = bright ? (hover ? FURNACE_HIGHLIGHTED : FURNACE) : (hover ? FURNACE_DISABLED_HIGHLIGHTED : FURNACE_DISABLED);
        } else {
            sprite = bright ? (hover ? CRAFTING_HIGHLIGHTED : CRAFTING) : (hover ? CRAFTING_DISABLED_HIGHLIGHTED : CRAFTING_DISABLED);
        }
        int cellX = this.cellX(index);
        int cellY = this.cellY(index);
        graphics.blitSprite(sprite, cellX, cellY, PREVIEW, PREVIEW);
        graphics.pose().pushPose();
        // The ingredients draw above the frame.
        graphics.pose().translate(cellX + 2.0, cellY + 2.0, 150.0);
        for (Spot spot : layout.spots()) {
            if (spot.stacks().length == 0) {
                continue;
            }
            graphics.pose().pushPose();
            graphics.pose().translate(spot.x(), spot.y(), 0.0);
            graphics.pose().scale(ICON_SCALE, ICON_SCALE, 1.0f);
            graphics.pose().translate(-8.0, -8.0, 0.0);
            ItemStack[] stacks = spot.stacks();
            graphics.renderItem(stacks[Mth.floor(time / 30.0f) % stacks.length], 0, 0);
            graphics.pose().popPose();
        }
        graphics.pose().popPose();
    }

    /** Returns where each ingredient sits in the 3x3 grid, as vanilla's placement does it. */
    private static Layout layout(RecipeHolder<?> holder) {
        Recipe<?> recipe = holder.value();
        List<Ingredient> ingredients = recipe.getIngredients();
        if (!(recipe instanceof CraftingRecipe) && ingredients.size() == 1) {
            return new Layout(true, List.of(spot(FURNACE_ORIGIN, FURNACE_ORIGIN, ingredients.getFirst())));
        }
        List<Spot> spots = new ArrayList<>();
        if (recipe instanceof ShapedRecipe shaped) {
            int width = shaped.getWidth();
            int height = shaped.getHeight();
            int left = (3 - width) / 2;
            int top = (3 - height) / 2;
            for (int index = 0; index < ingredients.size(); index++) {
                int column = left + index % width;
                int row = top + index / width;
                addSpot(spots, column, row, ingredients.get(index));
            }
        } else {
            for (int index = 0; index < ingredients.size(); index++) {
                addSpot(spots, index % 3, index / 3, ingredients.get(index));
            }
        }
        return new Layout(false, spots);
    }

    private static void addSpot(List<Spot> spots, int column, int row, Ingredient ingredient) {
        Spot spot = spot(GRID_ORIGIN + column * STEP, GRID_ORIGIN + row * STEP, ingredient);
        if (spot.stacks().length > 0) {
            spots.add(spot);
        }
    }

    private static Spot spot(int x, int y, Ingredient ingredient) {
        return new Spot(x, y, ingredient.getItems());
    }

    private int centeredOn(int anchorX, int boundLeft, int boundRight) {
        int left = anchorX + CELL / 2 - this.width / 2;
        int room = boundRight - boundLeft;
        if (this.width > room) {
            return boundLeft - (this.width - room) / 2;
        }
        return Mth.clamp(left, boundLeft, boundRight - this.width);
    }

    private record Spot(int x, int y, ItemStack[] stacks) {
    }

    private record Layout(boolean furnace, List<Spot> spots) {
    }
}
