package dev.bluerecipebook.fabric;

import dev.bluerecipebook.client.BlueRecipeBookClient;
import net.fabricmc.api.ClientModInitializer;

public final class BlueRecipeBookFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlueRecipeBookClient.init();
    }
}
