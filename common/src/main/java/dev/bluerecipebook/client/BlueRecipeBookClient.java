package dev.bluerecipebook.client;

import dev.architectury.event.events.client.ClientRecipeUpdateEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.bluerecipebook.client.book.NewArrivals;
import dev.bluerecipebook.client.dev.DevFeed;
import dev.bluerecipebook.client.index.RecipeIndex;
import dev.bluerecipebook.client.strict.StrictDiscovery;

public final class BlueRecipeBookClient {
    private BlueRecipeBookClient() {
    }

    public static void init() {
        BookSettings.load();
        ClientRecipeUpdateEvent.EVENT.register(RecipeIndex::rebuild);
        ClientTickEvent.CLIENT_PRE.register(minecraft -> StrictDiscovery.tick());
        // Arrivals reads the strict filter after that tick has applied held items.
        ClientTickEvent.CLIENT_PRE.register(minecraft -> NewArrivals.tick());
        if (DevBooks.enabled()) {
            DevFeed.register();
        }
    }
}
