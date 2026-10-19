package dev.bluerecipebook.client.category;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.block.Blocks;

/** The book's tabs, one per vanilla crafting category. */
public final class BookCategories {
    private static final List<BookCategory> ALL = build();

    private BookCategories() {
    }

    private static List<BookCategory> build() {
        return List.of(
                category("equipment", "gui.bluerecipebook.category.equipment",
                        List.of(new ItemStack(Items.IRON_AXE), new ItemStack(Items.GOLDEN_SWORD)),
                        CraftingBookCategory.EQUIPMENT),
                category("building", "gui.bluerecipebook.category.building",
                        List.of(new ItemStack(Blocks.BRICKS)),
                        CraftingBookCategory.BUILDING),
                category("miscellaneous", "gui.bluerecipebook.category.miscellaneous",
                        List.of(new ItemStack(Items.LAVA_BUCKET), new ItemStack(Items.APPLE)),
                        CraftingBookCategory.MISC),
                category("redstone", "gui.bluerecipebook.category.redstone",
                        List.of(new ItemStack(Items.REDSTONE)),
                        CraftingBookCategory.REDSTONE));
    }

    public static List<BookCategory> all() {
        return ALL;
    }

    /** Returns the tab with {@code id}, or null when there is none. */
    public static BookCategory byId(String id) {
        for (BookCategory category : ALL) {
            if (category.id().equals(id)) {
                return category;
            }
        }
        return null;
    }

    private static BookCategory category(String id, String translationKey, List<ItemStack> icons, CraftingBookCategory craftingCategory) {
        return new BookCategory(id, Component.translatable(translationKey), icons, craftingCategory);
    }
}
