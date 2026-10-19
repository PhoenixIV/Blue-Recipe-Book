package dev.bluerecipebook.client.data;

import dev.bluerecipebook.BlueRecipeBook;
import dev.bluerecipebook.client.discover.DiscoveryFiles;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Brings a player-data folder up to the format this jar writes.
 * A missing {@code format.json} is format {@link #FORMAT} and is written. A newer or unreadable file is left unchanged.
 */
public final class PlayerDataUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueRecipeBook.MOD_ID);
    static final int FORMAT = 1;
    /** Mod version that introduced {@link #FORMAT}. Change this only when {@link #FORMAT} changes. */
    static final String SINCE = "0.1.0";
    private static final String FILE_NAME = "format.json";
    /** Longer than any marker this jar writes. A larger file is not this object. */
    private static final int MAX_BYTES = 8192;
    private static final int MAX_SINCE = 64;

    private PlayerDataUpdater() {
    }

    /**
     * Returns whether this jar can use the folder, writing {@code format.json} when the file is missing.
     * A newer or unreadable file is left unchanged.
     *
     * @param directory the player-data folder, created when the marker is missing
     */
    public static Status accept(Path directory) {
        Path path = directory.resolve(FILE_NAME);
        if (!Files.isRegularFile(path)) {
            DiscoveryFiles.write(path, List.of(marker()));
            return Status.accept();
        }
        try {
            if (Files.size(path) > MAX_BYTES) {
                return Status.unreadable();
            }
            JsonElement element = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (!element.isJsonObject()) {
                return Status.unreadable();
            }
            return decide(element.getAsJsonObject());
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            LOGGER.error("Could not read {}", path, exception);
            return Status.unreadable();
        }
    }

    private static Status decide(JsonObject object) {
        if (!object.has("format") || !object.get("format").isJsonPrimitive()) {
            return Status.unreadable();
        }
        JsonPrimitive formatValue = object.get("format").getAsJsonPrimitive();
        if (!formatValue.isNumber()) {
            return Status.unreadable();
        }
        int format;
        try {
            format = Integer.parseInt(formatValue.getAsNumber().toString());
        } catch (NumberFormatException exception) {
            return Status.unreadable();
        }
        if (format == FORMAT) {
            return Status.accept();
        }
        if (format > FORMAT) {
            return Status.newer(since(object));
        }
        return Status.unreadable();
    }

    private static String since(JsonObject object) {
        if (!object.has("since") || !object.get("since").isJsonPrimitive()) {
            return null;
        }
        JsonPrimitive value = object.get("since").getAsJsonPrimitive();
        if (!value.isString()) {
            return null;
        }
        String text = value.getAsString().trim();
        if (text.isEmpty() || text.length() > MAX_SINCE) {
            return null;
        }
        return text;
    }

    private static String marker() {
        JsonObject object = new JsonObject();
        object.addProperty("format", FORMAT);
        object.addProperty("since", SINCE);
        return object.toString();
    }

    /** {@code since} is the mod version the file names, and null when the screen has no version to show. */
    public record Status(Kind kind, String since) {
        public enum Kind {
            ACCEPT,
            NEWER,
            UNREADABLE
        }

        static Status accept() {
            return new Status(Kind.ACCEPT, null);
        }

        static Status newer(String since) {
            return new Status(Kind.NEWER, since);
        }

        static Status unreadable() {
            return new Status(Kind.UNREADABLE, null);
        }

        public boolean accepted() {
            return this.kind == Kind.ACCEPT;
        }
    }
}
