package dev.bluerecipebook.client.discover;

import dev.bluerecipebook.BlueRecipeBook;
import dev.bluerecipebook.client.index.RecipeIndex;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recipe ids in the order this client saw them unlocked. The file is one id per line, oldest first.
 * Anything absent sorts after the list, in recipe-manager order.
 */
public final class DiscoveryOrder {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueRecipeBook.MOD_ID);
    private static final Set<ResourceLocation> discovered = new LinkedHashSet<>();
    private static final Map<ResourceLocation, Integer> rankById = new HashMap<>();
    private static int loadedPlace = -1;
    private static int unreadable;
    private static boolean pruned;
    private static boolean readFailed;
    /** Change counter bumped on every unlock or removal, even when this order does not record the recipe, so the open book refreshes. */
    private static int generation;

    private DiscoveryOrder() {
    }

    /** Appends ids this client has not already recorded, in the given order. An id already listed stays where it is. */
    public static void onUnlocked(List<ResourceLocation> ids) {
        if (ids.isEmpty()) {
            return;
        }
        generation++;
        if (!ensurePlace()) {
            return;
        }
        boolean changed = false;
        for (ResourceLocation id : ids) {
            changed |= discovered.add(id);
        }
        if (changed) {
            reindex();
            save();
        }
    }

    /** Drops the recorded order for this world and player. A later unlock is appended again. */
    public static void clear() {
        if (!ensurePlace() || discovered.isEmpty()) {
            return;
        }
        discovered.clear();
        rankById.clear();
        generation++;
        save();
    }

    public static void onRemoved(List<ResourceLocation> ids) {
        if (ids.isEmpty()) {
            return;
        }
        generation++;
        if (!ensurePlace()) {
            return;
        }
        boolean changed = false;
        for (ResourceLocation id : ids) {
            changed |= discovered.remove(id);
        }
        if (changed) {
            reindex();
            save();
        }
    }

    /**
     * Returns the position in the discovery list, or {@code -1} when this client has not seen the unlock.
     * A higher rank sorts first.
     */
    public static int rank(ResourceLocation id) {
        ensurePlace();
        return rankById.getOrDefault(id, -1);
    }

    /** Returns the recorded ids, oldest first, or an empty list when no world is open. */
    public static List<ResourceLocation> oldestFirst() {
        if (!ensurePlace()) {
            return List.of();
        }
        return List.copyOf(discovered);
    }

    public static int generation() {
        return generation;
    }

    private static boolean ensurePlace() {
        if (!DiscoveryFiles.ready()) {
            return false;
        }
        int place = DiscoveryFiles.generation();
        if (place == loadedPlace) {
            if (!pruned) {
                pruned = pruneMissing();
            }
            return true;
        }
        loadedPlace = place;
        pruned = false;
        unreadable = 0;
        discovered.clear();
        rankById.clear();
        load();
        generation++;
        if (!pruned) {
            pruned = pruneMissing();
        }
        return true;
    }

    private static void load() {
        readFailed = false;
        Path path = DiscoveryFiles.file("unlock-order.txt");
        try {
            for (String line : DiscoveryFiles.read(path)) {
                try {
                    discovered.add(ResourceLocation.parse(line));
                } catch (RuntimeException exception) {
                    unreadable++;
                }
            }
            reindex();
        } catch (IOException exception) {
            LOGGER.error("Could not read discovery order from {}; leaving the file unchanged", path, exception);
            discovered.clear();
            rankById.clear();
            unreadable = 0;
            pruned = true;
            readFailed = true;
        }
    }

    /**
     * Removes recorded ids that the loaded recipes lack and logs them once.
     *
     * @return false when the recipe list has not arrived, so the caller retries
     */
    private static boolean pruneMissing() {
        if (discovered.isEmpty()) {
            if (unreadable > 0) {
                save();
                MissingIds.reportRecipes(List.of(), unreadable);
                unreadable = 0;
                generation++;
            }
            return true;
        }
        List<ResourceLocation> missing = new ArrayList<>();
        for (ResourceLocation id : discovered) {
            RecipeIndex.Presence present = RecipeIndex.contains(id);
            if (present == RecipeIndex.Presence.UNKNOWN) {
                return false;
            }
            if (present == RecipeIndex.Presence.ABSENT) {
                missing.add(id);
            }
        }
        if (missing.isEmpty() && unreadable == 0) {
            return true;
        }
        discovered.removeAll(missing);
        reindex();
        save();
        MissingIds.reportRecipes(missing, unreadable);
        unreadable = 0;
        generation++;
        return true;
    }

    private static void reindex() {
        rankById.clear();
        int index = 0;
        for (ResourceLocation id : discovered) {
            rankById.put(id, index++);
        }
    }

    private static void save() {
        // A thrown read left this file unread, so a later save must not replace it.
        if (readFailed) {
            return;
        }
        List<String> lines = new ArrayList<>();
        for (ResourceLocation id : discovered) {
            lines.add(id.toString());
        }
        DiscoveryFiles.write(DiscoveryFiles.file("unlock-order.txt"), lines);
    }
}
