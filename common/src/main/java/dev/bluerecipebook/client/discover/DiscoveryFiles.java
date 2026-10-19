package dev.bluerecipebook.client.discover;

import dev.bluerecipebook.BlueRecipeBook;
import dev.bluerecipebook.client.data.PlayerDataDisconnect;
import dev.bluerecipebook.client.data.PlayerDataUpdater;
import dev.bluerecipebook.client.screen.mixin.MinecraftServerAccess;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-player discovery files for the open world, kept in that world's folder in singleplayer.
 * A remote world's stay under {@code config/bluerecipebook/remote}, because this client cannot write that save.
 * Call {@link #ready()} before {@link #file(String)}.
 */
public final class DiscoveryFiles {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueRecipeBook.MOD_ID);
    private static String place = "";
    private static String refused = "";
    private static Path directory;
    private static UUID playerId;
    private static int generation;
    private static volatile Realm realm;

    private DiscoveryFiles() {
    }

    /** Sets the realm the next Realms server entry belongs to. Called from the Realms connect thread. */
    public static void joiningRealm(long id, String name) {
        realm = new Realm(id, name);
    }

    /**
     * Returns whether the open world has a place for this player's files.
     * Bumps {@link #generation()} when the world or player changed since the last call.
     * Returns false, and disconnects, when {@code format.json} is newer than this jar or cannot be read.
     */
    public static boolean ready() {
        Place next = currentPlace();
        if (next == null) {
            // The visit is over, so the next join reads format.json again.
            place = "";
            directory = null;
            playerId = null;
            refused = "";
            return false;
        }
        if (next.key.equals(refused)) {
            return false;
        }
        if (!next.key.equals(place)) {
            PlayerDataUpdater.Status status = PlayerDataUpdater.accept(next.directory);
            if (!status.accepted()) {
                refuse(next, status);
                return false;
            }
            place = next.key;
            directory = next.directory;
            playerId = next.player;
            generation++;
            if (next.label != null) {
                write(directory.resolve("realm-name.txt"), List.of(next.label));
            }
        }
        return true;
    }

    public static int generation() {
        return generation;
    }

    /**
     * Returns this player's file for the open world.
     *
     * @param name file name inside the player folder, such as {@code unlock-order.txt}
     */
    public static Path file(String name) {
        return directory.resolve(playerId.toString()).resolve(name);
    }

    /** Returns the non-blank lines of the file, or an empty list when it does not exist. */
    public static List<String> read(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    /** Replaces the file with one line per entry. A failed write is logged, not thrown. */
    public static void write(Path path, Iterable<String> lines) {
        StringBuilder text = new StringBuilder();
        for (String line : lines) {
            text.append(line).append('\n');
        }
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, text.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not write {}", path, exception);
        }
    }

    private static void refuse(Place next, PlayerDataUpdater.Status status) {
        place = "";
        directory = null;
        playerId = null;
        refused = next.key;
        PlayerDataDisconnect.show(next.directory, status);
    }

    private static Place currentPlace() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        UUID player = minecraft.player.getUUID();
        if (minecraft.hasSingleplayerServer()) {
            IntegratedServer server = minecraft.getSingleplayerServer();
            if (server != null) {
                LevelStorageSource.LevelStorageAccess access = ((MinecraftServerAccess) server).blueRecipeBook$storageSource();
                Path world = access.getLevelPath(LevelResource.PLAYER_DATA_DIR).getParent();
                if (world != null) {
                    return new Place(world.toAbsolutePath().normalize() + "/" + player, world.resolve(BlueRecipeBook.MOD_ID), player, null);
                }
            }
        }
        ServerData remote = minecraft.getCurrentServer();
        if (remote == null) {
            return null;
        }
        Realm joined = realm;
        List<String> segments;
        String label = null;
        if (remote.isRealm() && joined != null) {
            segments = ServerFolder.realm(joined.id);
            label = joined.name;
        } else if (remote.isLan()) {
            segments = ServerFolder.lan(remote.name);
        } else {
            segments = ServerFolder.address(remote.ip);
        }
        if (segments.isEmpty()) {
            return null;
        }
        Path directory = configRoot();
        for (String segment : segments) {
            directory = directory.resolve(segment);
        }
        return new Place(String.join("/", segments) + "/" + player, directory, player, label);
    }

    private static Path configRoot() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve(BlueRecipeBook.MOD_ID)
                .resolve("remote");
    }

    /** Where the open world's files live. {@code label} is written to {@code realm-name.txt} beside the player folders, and null writes none. */
    private record Place(String key, Path directory, UUID player, String label) {
    }

    private record Realm(long id, String name) {
    }
}
