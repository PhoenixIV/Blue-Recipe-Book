package dev.bluerecipebook.client.book;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** The recipe last placed from each group, keyed by {@link RecipeGroup#key}, during this client run. Not written to disk. */
public final class LastGroupUse {
    private static final Map<String, ResourceLocation> byGroup = new HashMap<>();

    private LastGroupUse() {
    }

    public static void remember(String key, ResourceLocation id) {
        byGroup.put(key, id);
    }

    /** Returns the recipe last placed under {@code key}, or null when none was. */
    public static ResourceLocation get(String key) {
        return byGroup.get(key);
    }
}
