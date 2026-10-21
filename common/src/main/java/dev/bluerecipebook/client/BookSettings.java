package dev.bluerecipebook.client;

import dev.architectury.platform.Platform;
import dev.bluerecipebook.BlueRecipeBook;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.minecraft.resources.ResourceLocation;

public class BookSettings {
    /**
     * Strict discovery, which keeps an unlocked recipe off this book's page until every ingredient
     * has been in the inventory. Vanilla still grants the recipe, and the new-recipe toast waits
     * until the ingredients have been found.
     */
    @SerialEntry
    public boolean strictlyRequireEveryIngredient = false;

    /**
     * Lists every recipe, including ones the player has not unlocked. Off is vanilla: the page lists
     * unlocked recipes, and strict discovery still applies. On, a click on a locked recipe draws a ghost,
     * and strict discovery does not hide recipes.
     */
    @SerialEntry
    public boolean revealAllRecipes = false;

    /**
     * Grouping by group string, which is vanilla's behavior. The string is not keyed by vanilla's
     * recipe-book tabs, so Search joins recipes those tabs would keep apart. Off, the string does
     * not join recipes that make different items, and recipes that make the same item still share a slot.
     */
    @SerialEntry
    public boolean groupRecipes = true;

    /**
     * The wheel places one craft or takes one back, and Ctrl or Alt turns the page.
     * Off, the wheel turns the page, and Ctrl or Alt places or takes back.
     * The strip under the recipes only turns the page either way.
     */
    @SerialEntry
    public boolean scrollToMoveItems = false;

    /**
     * The tab, the page, and the search text from the last time the book was open.
     * Off opens on the search tab, the first page, and an empty search, as vanilla does.
     * The inventory and the crafting table share the place. It lasts for this client session.
     */
    @SerialEntry
    public boolean openWhereYouLeftOff = false;

    /**
     * Craftable-first ordering, where craftable recipes come first and the rest follow newest first. Off keeps only
     * the newest order. The button in the book's top right sets this, so the settings screen leaves it out.
     */
    @SerialEntry
    public boolean craftableFirst = true;

    private static final ConfigClassHandler<BookSettings> HANDLER = ConfigClassHandler.createBuilder(BookSettings.class)
            .id(ResourceLocation.fromNamespaceAndPath(BlueRecipeBook.MOD_ID, "settings"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(Platform.getConfigFolder().resolve(BlueRecipeBook.MOD_ID + ".json"))
                    .build())
            .build();

    public static void load() {
        HANDLER.load();
    }

    public static BookSettings get() {
        return HANDLER.instance();
    }

    public static void save() {
        HANDLER.save();
    }

    static ConfigClassHandler<BookSettings> handler() {
        return HANDLER;
    }
}
