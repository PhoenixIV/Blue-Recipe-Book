package dev.bluerecipebook.client.search;

import java.util.Locale;
import net.minecraft.world.item.ItemStack;

public final class ResultNames {
    private ResultNames() {
    }

    /** Returns the lowercase display name a search compares against. */
    public static String lowerName(ItemStack stack) {
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT);
    }

    /** Both arguments must already be lowercase. */
    public static boolean matches(String lowerName, String lowerQuery) {
        return lowerName.contains(lowerQuery);
    }
}
