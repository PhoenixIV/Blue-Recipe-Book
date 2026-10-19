package dev.bluerecipebook.client.data;

import dev.bluerecipebook.BlueRecipeBook;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Leaves the world when a player-data folder cannot be used.
 * Closes the connection, which releases the world lock.
 */
public final class PlayerDataDisconnect {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueRecipeBook.MOD_ID);
    private static volatile boolean titleReturn;

    private PlayerDataDisconnect() {
    }

    /** Disconnects, naming {@code since} when the folder has one, and the folder to delete. */
    public static void show(Path directory, PlayerDataUpdater.Status status) {
        if (status.kind() == PlayerDataUpdater.Status.Kind.NEWER) {
            LOGGER.error("Refusing player data in {} because format.json is newer than this version", directory);
        } else {
            LOGGER.error("Refusing player data in {} because format.json could not be read", directory);
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener listener = minecraft.getConnection();
        if (listener == null || listener.getConnection() == null) {
            return;
        }
        String folder = shownFolder(directory);
        Component message = status.since() == null
                ? Component.translatable("disconnect.bluerecipebook.unreadable_format", folder)
                : Component.translatable("disconnect.bluerecipebook.newer_format", status.since(), folder);
        titleReturn = minecraft.hasSingleplayerServer();
        minecraft.execute(() -> listener.getConnection().disconnect(message));
    }

    /** Returns whether the disconnect screen being built should return to the title screen, once. */
    public static boolean consumeTitleReturn() {
        boolean value = titleReturn;
        titleReturn = false;
        return value;
    }

    /** Path under the game directory, with the absolute path when the folder sits elsewhere. */
    private static String shownFolder(Path directory) {
        Path game = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize();
        Path folder = directory.toAbsolutePath().normalize();
        Path shown = folder.startsWith(game) ? game.relativize(folder) : folder;
        return shown.toString().replace('\\', '/');
    }
}
