package dev.bluerecipebook.client.book;

import dev.bluerecipebook.client.discover.DiscoveryFiles;
import dev.bluerecipebook.client.discover.DiscoveryOrder;
import dev.bluerecipebook.client.index.CraftingStation;
import dev.bluerecipebook.client.index.RecipeIndex;
import dev.bluerecipebook.client.strict.StrictDiscovery;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Item ids waiting for their arrival pop, for this client session.
 * Nothing here is written to disk. Leaving the world drops it.
 */
public final class NewArrivals {
    private static final float DURATION = 15.0F;

    private static final Set<ResourceLocation> seenRecipes = new HashSet<>();
    private static final Map<ResourceLocation, ResourceLocation> seenItem = new HashMap<>();
    private static final Map<ResourceLocation, Integer> seenItemCounts = new HashMap<>();
    private static final Set<ResourceLocation> pendingItems = new HashSet<>();
    private static final Map<String, Float> groupTime = new HashMap<>();
    private static final Map<ResourceLocation, Float> itemTime = new HashMap<>();
    private static final Map<CraftingBookCategory, Float> tabTime = new EnumMap<>(CraftingBookCategory.class);
    private static int place = -1;
    private static boolean syncSeen;
    private static boolean baselined;
    private static int generation;
    private static int watchedDiscovery = -1;
    private static int watchedRevealed = -1;
    private static int watchedIndex = -1;
    private static int watchedSettings = -1;

    private NewArrivals() {
    }

    /**
     * Marks the recipe book's initial sync.
     * The next pass absorbs every recipe already visible, so joining a world does not pop it.
     */
    public static void onInitialBook() {
        syncSeen = true;
        baselined = false;
        pendingItems.clear();
        groupTime.clear();
        itemTime.clear();
        tabTime.clear();
    }

    /** Notices recipes that newly qualify, including while the book is closed. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!DiscoveryFiles.ready() || minecraft.player == null) {
            if (baselined || syncSeen) {
                clearSession();
                syncSeen = false;
                baselined = false;
                place = -1;
            }
            return;
        }
        int now = DiscoveryFiles.generation();
        if (now != place) {
            clearSession();
            baselined = false;
            place = now;
        }
        // The index can arrive before the known set. Wait for the initial sync.
        if (!syncSeen || RecipeIndex.generation() == 0) {
            return;
        }
        StrictDiscovery.prepare();
        int discovered = DiscoveryOrder.generation();
        int revealed = StrictDiscovery.revealedGeneration();
        int indexed = RecipeIndex.generation();
        int settings = StrictDiscovery.settingsGeneration();
        if (baselined && discovered == watchedDiscovery && revealed == watchedRevealed
                && indexed == watchedIndex && settings == watchedSettings) {
            return;
        }
        List<RecipeHolder<?>> visible = visible(minecraft.player.getRecipeBook());
        if (!baselined) {
            absorb(visible);
        } else if (settings != watchedSettings) {
            // Turning the filter off shows recipes that were already unlocked.
            adopt(visible);
        } else {
            if (diff(visible)) {
                generation++;
            }
        }
        watch(discovered, revealed, indexed, settings);
    }

    /** Returns a counter that changes when a new item starts waiting. */
    public static int generation() {
        return generation;
    }

    /** Starts the tab pops for categories that still have a pending item on {@code station}. */
    public static void arm(CraftingStation station) {
        if (pendingItems.isEmpty()) {
            return;
        }
        Set<CraftingBookCategory> categories = new HashSet<>();
        for (RecipeHolder<?> holder : RecipeIndex.recipes()) {
            if (!station.fits(holder.value())) {
                continue;
            }
            ResourceLocation item = RecipeGroup.resultId(holder);
            if (item == null || !pendingItems.contains(item)) {
                continue;
            }
            if (holder.value() instanceof CraftingRecipe crafting) {
                categories.add(crafting.category());
            }
        }
        for (CraftingBookCategory category : categories) {
            tabTime.put(category, DURATION);
        }
    }

    /**
     * Drops pending items this group just drew.
     * Restarts that group's pop when one of them was waiting, and starts each item's pop.
     *
     * @return the pop time left, or 0 when this group is still
     */
    public static float claim(String groupKey, List<RecipeHolder<?>> members) {
        boolean arrived = false;
        for (RecipeHolder<?> holder : members) {
            ResourceLocation item = RecipeGroup.resultId(holder);
            if (item != null && pendingItems.remove(item)) {
                arrived = true;
                itemTime.put(item, DURATION);
            }
        }
        if (arrived) {
            groupTime.put(groupKey, DURATION);
        }
        return groupTime.getOrDefault(groupKey, 0.0F);
    }

    public static void advanceGroup(String groupKey, float delta) {
        Float time = groupTime.get(groupKey);
        if (time == null) {
            return;
        }
        time -= delta;
        if (time <= 0.0F) {
            groupTime.remove(groupKey);
        } else {
            groupTime.put(groupKey, time);
        }
    }

    public static float itemTime(ResourceLocation item) {
        return itemTime.getOrDefault(item, 0.0F);
    }

    /** Advances every item pop. Draw first, then call once per frame while the book is open. */
    public static void advanceItems(float delta) {
        itemTime.entrySet().removeIf(entry -> {
            float left = entry.getValue() - delta;
            if (left <= 0.0F) {
                return true;
            }
            entry.setValue(left);
            return false;
        });
    }

    public static float tabTime(CraftingBookCategory category) {
        return tabTime.getOrDefault(category, 0.0F);
    }

    public static void advanceTab(CraftingBookCategory category, float delta) {
        Float time = tabTime.get(category);
        if (time == null) {
            return;
        }
        time -= delta;
        if (time <= 0.0F) {
            tabTime.remove(category);
        } else {
            tabTime.put(category, time);
        }
    }

    /** Draws {@code draw} with the slot pop, both axes, around the middle of {@code size}. */
    public static void animateSlot(GuiGraphics graphics, int x, int y, int size, float time, Runnable draw) {
        animate(graphics, x, y, size, size, time, false, draw);
    }

    /** Draws {@code draw} with the tab pop, vertical only, around the middle of {@code height}. */
    public static void animateTab(GuiGraphics graphics, int x, int y, int height, float time, Runnable draw) {
        animate(graphics, x, y, 0, height, time, true, draw);
    }

    private static void animate(GuiGraphics graphics, int x, int y, int width, int height, float time,
            boolean verticalOnly, Runnable draw) {
        if (time <= 0.0F) {
            draw.run();
            return;
        }
        float pivotX = x + width / 2.0F;
        float pivotY = y + height / 2.0F;
        float scale = 1.0F + 0.1F * (float) Math.sin(time / DURATION * (float) Math.PI);
        graphics.pose().pushPose();
        graphics.pose().translate(pivotX, pivotY, 0.0F);
        graphics.pose().scale(verticalOnly ? 1.0F : scale, scale, 1.0F);
        graphics.pose().translate(-pivotX, -pivotY, 0.0F);
        draw.run();
        graphics.pose().popPose();
    }

    private static List<RecipeHolder<?>> visible(RecipeBook book) {
        List<RecipeHolder<?>> visible = new ArrayList<>();
        for (RecipeHolder<?> holder : RecipeIndex.recipes()) {
            Recipe<?> recipe = holder.value();
            if (!CraftingStation.INVENTORY.fits(recipe) && !CraftingStation.TABLE.fits(recipe)) {
                continue;
            }
            if (book.contains(holder) && StrictDiscovery.visible(holder)) {
                visible.add(holder);
            }
        }
        return visible;
    }

    private static void absorb(List<RecipeHolder<?>> visible) {
        clearSession();
        for (RecipeHolder<?> holder : visible) {
            remember(holder, false);
        }
        baselined = true;
    }

    private static void adopt(List<RecipeHolder<?>> visible) {
        Set<ResourceLocation> keep = new HashSet<>();
        seenRecipes.clear();
        seenItem.clear();
        seenItemCounts.clear();
        for (RecipeHolder<?> holder : visible) {
            remember(holder, false);
            ResourceLocation item = RecipeGroup.resultId(holder);
            if (item != null) {
                keep.add(item);
            }
        }
        pendingItems.retainAll(keep);
    }

    /** Returns whether a new item started waiting. */
    private static boolean diff(List<RecipeHolder<?>> visible) {
        Set<ResourceLocation> now = new HashSet<>();
        boolean gained = false;
        for (RecipeHolder<?> holder : visible) {
            now.add(holder.id());
            gained |= remember(holder, true);
        }
        List<ResourceLocation> gone = new ArrayList<>();
        for (ResourceLocation id : seenRecipes) {
            if (!now.contains(id)) {
                gone.add(id);
            }
        }
        for (ResourceLocation id : gone) {
            forget(id);
        }
        return gained;
    }

    /** Returns whether {@code holder}'s item started waiting. An item already waiting stays listed once. */
    private static boolean remember(RecipeHolder<?> holder, boolean arrive) {
        if (!seenRecipes.add(holder.id())) {
            return false;
        }
        ResourceLocation item = RecipeGroup.resultId(holder);
        if (item == null) {
            return false;
        }
        seenItem.put(holder.id(), item);
        seenItemCounts.merge(item, 1, Integer::sum);
        return arrive && pendingItems.add(item);
    }

    private static void forget(ResourceLocation recipeId) {
        if (!seenRecipes.remove(recipeId)) {
            return;
        }
        ResourceLocation item = seenItem.remove(recipeId);
        if (item == null) {
            return;
        }
        seenItemCounts.compute(item, (id, count) -> {
            int left = (count == null ? 0 : count) - 1;
            return left <= 0 ? null : left;
        });
        if (!seenItemCounts.containsKey(item)) {
            pendingItems.remove(item);
        }
    }

    private static void watch(int discovered, int revealed, int indexed, int settings) {
        watchedDiscovery = discovered;
        watchedRevealed = revealed;
        watchedIndex = indexed;
        watchedSettings = settings;
    }

    private static void clearSession() {
        seenRecipes.clear();
        seenItem.clear();
        seenItemCounts.clear();
        pendingItems.clear();
        groupTime.clear();
        itemTime.clear();
        tabTime.clear();
        generation = 0;
        watchedDiscovery = -1;
        watchedRevealed = -1;
        watchedIndex = -1;
        watchedSettings = -1;
    }
}
