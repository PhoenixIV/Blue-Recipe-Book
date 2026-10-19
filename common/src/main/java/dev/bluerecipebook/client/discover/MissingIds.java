package dev.bluerecipebook.client.discover;

import dev.bluerecipebook.BlueRecipeBook;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One warning when saved ids are removed because the loaded game no longer has them.
 * The caller has already dropped those ids from its list. This only logs.
 */
public final class MissingIds {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueRecipeBook.MOD_ID);

    private MissingIds() {
    }

    /**
     * Logs one warning for saved item ids the loaded game no longer has.
     * Does nothing when there is nothing to report.
     *
     * @param missing item ids that are not in the item registry
     * @param unreadable the number of saved lines that were not ids
     */
    public static void reportHeldItems(List<ResourceLocation> missing, int unreadable) {
        if (missing.isEmpty() && unreadable == 0) {
            return;
        }
        if (missing.isEmpty()) {
            LOGGER.warn("Skipped {} unreadable lines in the saved item records.", unreadable);
            return;
        }
        int count = missing.size();
        String mods = modList(missing);
        String message = count == 1
                ? "Forgot 1 saved item record from " + mods + ". That item is not loaded. It will not be treated as found until it is held again."
                : "Forgot " + count + " saved item records from " + mods + ". Those items are not loaded, so this book does not treat them as found until they are held again.";
        LOGGER.warn(withUnreadable(message, unreadable));
    }

    /**
     * Logs one warning for saved recipe ids the loaded game no longer has.
     * Does nothing when there is nothing to report.
     *
     * @param missing recipe ids that are not in the recipe manager
     * @param unreadable the number of saved lines that were not ids
     */
    public static void reportRecipes(List<ResourceLocation> missing, int unreadable) {
        if (missing.isEmpty() && unreadable == 0) {
            return;
        }
        if (missing.isEmpty()) {
            LOGGER.warn("Skipped {} unreadable lines in the saved recipe records.", unreadable);
            return;
        }
        int count = missing.size();
        String mods = modList(missing);
        String message = count == 1
                ? "Forgot 1 saved recipe record from " + mods + ". That recipe is not loaded. It has to be unlocked again if that mod returns."
                : "Forgot " + count + " saved recipe records from " + mods + ". Those recipes are not loaded. They have to be unlocked again if that mod returns.";
        LOGGER.warn(withUnreadable(message, unreadable));
    }

    /**
     * Returns the mod id when every id shares it, otherwise each mod id with its own count.
     * The single mod id has no count because the caller already states the total.
     */
    private static String modList(List<ResourceLocation> missing) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ResourceLocation id : missing) {
            counts.merge(id.getNamespace(), 1, Integer::sum);
        }
        if (counts.size() == 1) {
            return counts.keySet().iterator().next();
        }
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (!text.isEmpty()) {
                text.append(", ");
            }
            text.append(entry.getKey()).append(" (").append(entry.getValue()).append(')');
        }
        return text.toString();
    }

    private static String withUnreadable(String message, int unreadable) {
        if (unreadable == 0) {
            return message;
        }
        return message + " Also skipped " + unreadable + " unreadable lines.";
    }
}
