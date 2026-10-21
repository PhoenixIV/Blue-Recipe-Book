package dev.bluerecipebook.client;

import dev.bluerecipebook.client.category.BookCategories;

/**
 * Session state for the book. The open state stays in the vanilla recipe book, which the server stores.
 * With "Open where you left off" off, {@link #searchGroup()}, {@link #page()}, and {@link #query()}
 * report the search tab, the first page, and an empty search. Writes still record the place.
 */
public final class BookMemory {
    private static boolean searchGroup = true;
    private static String categoryId = "equipment";
    private static String query = "";
    private static int page;

    private BookMemory() {
    }

    public static boolean searchGroup() {
        if (!BookSettings.get().openWhereYouLeftOff) {
            return true;
        }
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
        if (!BookSettings.get().openWhereYouLeftOff) {
            return "";
        }
        return query;
    }

    public static void setQuery(String value) {
        query = value;
    }

    public static int page() {
        if (!BookSettings.get().openWhereYouLeftOff) {
            return 0;
        }
        return page;
    }

    public static void setPage(int value) {
        page = Math.max(0, value);
    }
}
