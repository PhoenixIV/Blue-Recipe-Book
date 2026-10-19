package dev.bluerecipebook.client.strict;

import dev.bluerecipebook.BlueRecipeBook;
import dev.bluerecipebook.client.BookSettings;
import dev.bluerecipebook.client.discover.DiscoveryFiles;
import dev.bluerecipebook.client.discover.DiscoveryOrder;
import dev.bluerecipebook.client.discover.MissingIds;
import dev.bluerecipebook.client.index.RecipeIndex;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * View filter and the order that filter would reveal recipes.
 * The unlock packet and {@code RecipeBook.contains} stay as they are.
 * The second list is kept while the filter is off, so a tick unlock does not
 * sort as an old recipe on the day its ingredients are finally held.
 */
public final class StrictDiscovery {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueRecipeBook.MOD_ID);
    private static final List<ResourceLocation> revealed = new ArrayList<>();
    private static final Map<ResourceLocation, Integer> rankById = new HashMap<>();
    private static final Set<ResourceLocation> waitingToast = new LinkedHashSet<>();
    private static int loadedPlace = -1;
    private static int unreadableOrder;
    private static int unreadableToast;
    private static boolean orderReadFailed;
    private static boolean toastReadFailed;
    private static boolean pruned;
    private static int watchedDiscovery = -1;
    private static int watchedIndex = -1;
    /** Change counter for the revealed list. A toggle bumps {@link #settingsGeneration} as well. */
    private static int revealedGeneration;
    /** Change counter for {@code strictlyRequireEveryIngredient} only. */
    private static int settingsGeneration;
    private static Boolean enabledApplied;
    private static boolean showingToast;

    private StrictDiscovery() {
    }

    public static void tick() {
        if (!DiscoveryFiles.ready()) {
            return;
        }
        prepare();
        applySetting();
    }

    public static void onUnlocked() {
        prepare();
    }

    public static void onRemoved(List<ResourceLocation> ids) {
        if (ids.isEmpty() || !DiscoveryFiles.ready()) {
            return;
        }
        loadPlace();
        boolean orderChanged = false;
        boolean toastChanged = false;
        for (ResourceLocation id : ids) {
            orderChanged |= revealed.remove(id);
            toastChanged |= waitingToast.remove(id);
        }
        if (orderChanged) {
            reindex();
            saveOrder();
            revealedGeneration++;
        }
        if (toastChanged) {
            saveToasts();
        }
    }

    /** Empties the strict order, the toast queue, and the held items. */
    public static void clear() {
        if (!DiscoveryFiles.ready()) {
            return;
        }
        HeldItems.clear();
        loadPlace();
        if (revealed.isEmpty() && waitingToast.isEmpty()) {
            return;
        }
        revealed.clear();
        rankById.clear();
        waitingToast.clear();
        saveOrder();
        saveToasts();
        revealedGeneration++;
    }

    /**
     * Loads held items and appends recipes the filter would reveal.
     * Call it before {@link #rank(ResourceLocation)} so the rank covers what the player holds now.
     */
    public static void prepare() {
        if (!HeldItems.ready()) {
            return;
        }
        loadPlace();
        int discovered = DiscoveryOrder.generation();
        int indexed = RecipeIndex.generation();
        boolean due = HeldItems.changed();
        if (!due && discovered == watchedDiscovery && indexed == watchedIndex) {
            return;
        }
        if (!reveal()) {
            return;
        }
        HeldItems.consumeChanged();
        watchedDiscovery = discovered;
        watchedIndex = indexed;
        if (due) {
            releaseToasts();
        }
        if (due && enabled()) {
            revealedGeneration++;
        }
    }

    public static boolean enabled() {
        return BookSettings.get().strictlyRequireEveryIngredient;
    }

    /**
     * Returns whether the filter keeps an unlocked recipe listed. This is the second check beside
     * {@code RecipeBook.contains}, and it is always true while the filter is off.
     * Call {@link #prepare()} first; this does not scan the inventory.
     */
    public static boolean visible(RecipeHolder<?> holder) {
        if (!enabled()) {
            return true;
        }
        return FoundIngredients.satisfied(holder.value());
    }

    /** Returns the position in the strict order, or {@code -1} when the filter has not revealed the recipe. */
    public static int rank(ResourceLocation id) {
        return rankById.getOrDefault(id, -1);
    }

    public static int revealedGeneration() {
        return revealedGeneration;
    }

    /** Returns a counter that changes only when the strict option is toggled. */
    public static int settingsGeneration() {
        return settingsGeneration;
    }

    /**
     * Returns whether this recipe's unlock toast should wait until its ingredients have been held,
     * and queues it when it should. The recipe itself is already granted.
     */
    public static boolean delayToast(RecipeHolder<?> holder) {
        if (showingToast || !enabled() || !HeldItems.ready()) {
            return false;
        }
        if (FoundIngredients.satisfied(holder.value())) {
            return false;
        }
        if (waitingToast.add(holder.id())) {
            saveToasts();
        }
        return true;
    }

    private static void applySetting() {
        boolean enabled = enabled();
        if (enabledApplied == null) {
            enabledApplied = enabled;
        } else if (enabledApplied != enabled) {
            enabledApplied = enabled;
            revealedGeneration++;
            settingsGeneration++;
        }
        if (!enabled && !waitingToast.isEmpty()) {
            showToasts(new ArrayList<>(waitingToast));
        }
    }

    private static void loadPlace() {
        int place = DiscoveryFiles.generation();
        if (place != loadedPlace) {
            loadedPlace = place;
            pruned = false;
            revealed.clear();
            rankById.clear();
            waitingToast.clear();
            unreadableOrder = loadOrder(DiscoveryFiles.file("strict-order.txt"));
            reindex();
            unreadableToast = loadToasts(DiscoveryFiles.file("waiting-toasts.txt"));
            watchedDiscovery = -1;
            watchedIndex = -1;
            revealedGeneration++;
        }
        if (!pruned) {
            pruned = pruneMissing();
        }
    }

    private static int loadOrder(Path path) {
        orderReadFailed = false;
        int unreadable = 0;
        try {
            for (String line : DiscoveryFiles.read(path)) {
                try {
                    ResourceLocation id = ResourceLocation.parse(line);
                    if (!rankById.containsKey(id)) {
                        revealed.add(id);
                        rankById.put(id, revealed.size() - 1);
                    }
                } catch (RuntimeException exception) {
                    unreadable++;
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Could not read {}; leaving the file unchanged", path, exception);
            revealed.clear();
            rankById.clear();
            orderReadFailed = true;
            return 0;
        }
        return unreadable;
    }

    private static int loadToasts(Path path) {
        toastReadFailed = false;
        int unreadable = 0;
        try {
            for (String line : DiscoveryFiles.read(path)) {
                try {
                    waitingToast.add(ResourceLocation.parse(line));
                } catch (RuntimeException exception) {
                    unreadable++;
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Could not read {}; leaving the file unchanged", path, exception);
            waitingToast.clear();
            toastReadFailed = true;
            return 0;
        }
        return unreadable;
    }

    /**
     * Removes strict-order and toast ids that the loaded recipes lack and logs them once.
     *
     * @return false when the recipe list has not arrived, so the caller retries
     */
    private static boolean pruneMissing() {
        List<ResourceLocation> missingOrder = new ArrayList<>();
        List<ResourceLocation> missingToast = new ArrayList<>();
        if (!collectMissing(revealed, missingOrder) || !collectMissing(waitingToast, missingToast)) {
            return false;
        }
        if (missingOrder.isEmpty() && missingToast.isEmpty() && unreadableOrder == 0 && unreadableToast == 0) {
            return true;
        }
        if (!missingOrder.isEmpty() || unreadableOrder > 0) {
            revealed.removeAll(missingOrder);
            reindex();
            saveOrder();
        }
        if (!missingToast.isEmpty() || unreadableToast > 0) {
            waitingToast.removeAll(missingToast);
            saveToasts();
        }
        List<ResourceLocation> missing = new ArrayList<>(missingOrder);
        missing.addAll(missingToast);
        MissingIds.reportRecipes(missing, unreadableOrder + unreadableToast);
        unreadableOrder = 0;
        unreadableToast = 0;
        revealedGeneration++;
        return true;
    }

    private static boolean collectMissing(Collection<ResourceLocation> ids, List<ResourceLocation> missing) {
        for (ResourceLocation id : ids) {
            RecipeIndex.Presence present = RecipeIndex.contains(id);
            if (present == RecipeIndex.Presence.UNKNOWN) {
                return false;
            }
            if (present == RecipeIndex.Presence.ABSENT) {
                missing.add(id);
            }
        }
        return true;
    }

    /**
     * Appends unlocked recipes whose ingredients have all been held, in discovery order.
     *
     * @return false when the recipe manager is not ready yet, so the caller retries
     */
    private static boolean reveal() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }
        RecipeManager manager = minecraft.level.getRecipeManager();
        RecipeBook book = minecraft.player.getRecipeBook();
        boolean changed = false;
        for (ResourceLocation id : DiscoveryOrder.oldestFirst()) {
            if (rankById.containsKey(id) || !book.contains(id)) {
                continue;
            }
            Optional<RecipeHolder<?>> found = manager.byKey(id);
            if (found.isEmpty()) {
                continue;
            }
            RecipeHolder<?> holder = found.get();
            if (skipsBook(holder) || !FoundIngredients.satisfied(holder.value())) {
                continue;
            }
            revealed.add(id);
            rankById.put(id, revealed.size() - 1);
            changed = true;
        }
        if (!changed) {
            return true;
        }
        reindex();
        saveOrder();
        if (enabled()) {
            revealedGeneration++;
        }
        return true;
    }

    private static boolean skipsBook(RecipeHolder<?> holder) {
        try {
            return holder.value().isSpecial() || holder.value().isIncomplete();
        } catch (RuntimeException exception) {
            return true;
        }
    }

    private static void releaseToasts() {
        if (!enabled() || waitingToast.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        RecipeManager manager = minecraft.level.getRecipeManager();
        List<ResourceLocation> ready = new ArrayList<>();
        for (ResourceLocation id : waitingToast) {
            Optional<RecipeHolder<?>> found = manager.byKey(id);
            if (found.isPresent() && FoundIngredients.satisfied(found.get().value())) {
                ready.add(id);
            }
        }
        showToasts(ready);
    }

    private static void showToasts(List<ResourceLocation> ids) {
        if (ids.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        waitingToast.removeAll(ids);
        saveToasts();
        // delayToast sees every toast, these included, so this flag lets the released ones through.
        showingToast = true;
        try {
            RecipeManager manager = minecraft.level.getRecipeManager();
            for (ResourceLocation id : ids) {
                Optional<RecipeHolder<?>> found = manager.byKey(id);
                if (found.isEmpty() || !found.get().value().showNotification()) {
                    continue;
                }
                RecipeToast.addOrUpdate(minecraft.getToasts(), found.get());
            }
        } finally {
            showingToast = false;
        }
    }

    private static void reindex() {
        rankById.clear();
        for (int i = 0; i < revealed.size(); i++) {
            rankById.put(revealed.get(i), i);
        }
    }

    private static void saveOrder() {
        // A thrown read left this file unread, so a later save must not replace it.
        if (orderReadFailed) {
            return;
        }
        DiscoveryFiles.write(DiscoveryFiles.file("strict-order.txt"), lines(revealed));
    }

    private static void saveToasts() {
        // A thrown read left this file unread, so a later save must not replace it.
        if (toastReadFailed) {
            return;
        }
        DiscoveryFiles.write(DiscoveryFiles.file("waiting-toasts.txt"), lines(waitingToast));
    }

    private static List<String> lines(Iterable<ResourceLocation> ids) {
        List<String> lines = new ArrayList<>();
        for (ResourceLocation id : ids) {
            lines.add(id.toString());
        }
        return lines;
    }
}
