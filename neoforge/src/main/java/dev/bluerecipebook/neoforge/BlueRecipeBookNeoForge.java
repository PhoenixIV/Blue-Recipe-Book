package dev.bluerecipebook.neoforge;

import dev.bluerecipebook.BlueRecipeBook;
import dev.bluerecipebook.client.BlueRecipeBookClient;
import dev.bluerecipebook.client.BookSettingsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(BlueRecipeBook.MOD_ID)
public final class BlueRecipeBookNeoForge {
    public BlueRecipeBookNeoForge(ModContainer container) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            BlueRecipeBookClient.init();
            container.registerExtensionPoint(IConfigScreenFactory.class,
                    (mod, parent) -> BookSettingsScreen.open(parent));
        }
    }
}
