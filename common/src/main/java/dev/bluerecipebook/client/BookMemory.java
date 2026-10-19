package dev.bluerecipebook.client;

import dev.bluerecipebook.client.category.BookCategories;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.inventory.RecipeBookType;

/** Session state for the open book. Unlock flags stay in the vanilla recipe book. */
public final class BookMemory {
    private static boolean prepared;
    private static boolean open;
    private static boolean searchGroup = true;
    private static String categoryId = "equipment";
    private static String query = "";
    private static int page;

    private BookMemory() {
    }

    /** Seeds the open state from the vanilla book's saved crafting state on the first call only. */
    public static void prepare(RecipeBook book) {
        if (!prepared) {
            open = book.isOpen(RecipeBookType.CRAFTING);
            prepared = true;
        }
    }

    public static boolean open() {
        return open;
    }

    public static void setOpen(boolean value) {
        open = value;
    }

    public static boolean searchGroup() {
        return searchGroup;
    }

    public static void setSearchGroup(boolean value) {
        searchGroup = value;
    }

    /** Returns the remembered category id, or the first category's id when that category no longer exists. */
    public static String categoryId() {
        if (BookCategories.byId(categoryId) == null) {
            return BookCategories.all().getFirst().id();
        }
        return categoryId;
    }

    /** Remembers the category and leaves the Search tab. */
    public static void setCategoryId(String id) {
        categoryId = id;
        searchGroup = false;
    }

    public static String query() {
        return query;
    }

    public static void setQuery(String value) {
        query = value;
    }

    public static int page() {
        return page;
    }

    public static void setPage(int value) {
        page = Math.max(0, value);
    }
}
