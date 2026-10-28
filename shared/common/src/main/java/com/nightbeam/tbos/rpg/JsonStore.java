package com.nightbeam.tbos.rpg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.compat.RpgCompat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.server.MinecraftServer;

/**
 * Small JSON files under {@code <world>/data/tbos/}. The 1.0 systems keep their
 * state here rather than in {@code SavedData}, whose API differs on every target,
 * so one implementation serves them all. Writes go to a temporary file first and
 * are moved into place, so a crash never leaves half a file behind.
 */
public final class JsonStore {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private JsonStore() {
    }

    public static Path directory(MinecraftServer server, String... parts) {
        Path path = RpgCompat.worldDirectory(server).resolve("data").resolve(Yesterglass.MOD_ID);
        for (String part : parts) {
            path = path.resolve(part);
        }
        return path;
    }

    public static JsonObject read(Path file) {
        if (!Files.isRegularFile(file)) {
            return new JsonObject();
        }
        try {
            JsonElement element = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (IOException | RuntimeException exception) {
            Yesterglass.LOGGER.error("Could not read {}; starting from an empty record", file, exception);
            try {
                Files.copy(file, file.resolveSibling(file.getFileName() + ".corrupt"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // The original stays where it is; the next write replaces it.
            }
            return new JsonObject();
        }
    }

    public static void write(Path file, JsonElement json) {
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(json), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            Yesterglass.LOGGER.error("Could not write {}", file, exception);
        }
    }
}
