package com.nightbeam.tbos.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nightbeam.tbos.Yesterglass;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

/**
 * A tiny JSON-backed config, shared by both loaders.
 *
 * <p>NeoForge's {@code ModConfigSpec} is not available in common code and Fabric
 * has no equivalent, so the schema, the defaults and the validation all live
 * here and each loader only has to hand over its config directory.
 *
 * <p>Behaviour deliberately copied from the spec it replaces: an invalid or
 * missing entry falls back to the compiled-in default and the file is rewritten,
 * and reading before {@link #load} throws {@link IllegalStateException} so the
 * existing {@code DEFAULT} fallbacks still cover GameTests and the standalone
 * dungeon simulation.
 *
 * <p>Comments survive a rewrite because they are emitted as ordinary
 * {@code "// key"} string entries, which stay valid JSON and are ignored on the
 * way back in.
 */
public final class ConfigSchema {
    private static final String COMMENT_PREFIX = "// ";

    private final String fileName;
    private final List<ConfigValue<?>> values = new ArrayList<>();
    private final Deque<String> section = new ArrayDeque<>();
    private volatile boolean loaded;

    public ConfigSchema(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void push(String name) {
        section.addLast(name);
    }

    public void pop() {
        section.removeLast();
    }

    private List<String> path() {
        return List.copyOf(section);
    }

    private <V extends ConfigValue<?>> V add(V value) {
        values.add(value);
        return value;
    }

    public ConfigValue.Bool define(String key, boolean defaultValue, String comment) {
        return add(new ConfigValue.Bool(this, path(), key, defaultValue, comment));
    }

    public ConfigValue.Int defineInRange(String key, int defaultValue, int min, int max, String comment) {
        return add(new ConfigValue.Int(this, path(), key, defaultValue, min, max, comment));
    }

    public ConfigValue.Long defineInRange(String key, long defaultValue, long min, long max, String comment) {
        return add(new ConfigValue.Long(this, path(), key, defaultValue, min, max, comment));
    }

    public ConfigValue.Double defineInRange(
            String key, double defaultValue, double min, double max, String comment) {
        return add(new ConfigValue.Double(this, path(), key, defaultValue, min, max, comment));
    }

    public ConfigValue.Text define(
            String key, String defaultValue, Predicate<Object> validator, String comment) {
        return add(new ConfigValue.Text(this, path(), key, defaultValue, validator, comment));
    }

    public ConfigValue.TextList defineList(
            String key, List<String> defaultValue, Predicate<Object> elementValidator, String comment) {
        return add(new ConfigValue.TextList(this, path(), key, defaultValue, elementValidator, comment));
    }

    public <E extends Enum<E>> ConfigValue.OfEnum<E> defineEnum(String key, E defaultValue, String comment) {
        return add(new ConfigValue.OfEnum<>(this, path(), key, defaultValue, comment));
    }

    /**
     * Reads {@code <dir>/<fileName>}, correcting anything unusable, then writes
     * the file back so new keys appear and stale ones disappear.
     *
     * <p>A read failure is never fatal: the mod runs on defaults and says so.
     */
    public void load(Path directory) {
        JsonObject root = read(directory.resolve(fileName));
        for (ConfigValue<?> value : values) {
            apply(value, root);
        }
        loaded = true;
        write(directory.resolve(fileName));
    }

    /** Marks the schema loaded on defaults, without touching the filesystem. */
    public void loadDefaults() {
        values.forEach(ConfigSchema::reset);
        loaded = true;
    }

    private static <T> void reset(ConfigValue<T> value) {
        value.set(value.defaultValue());
    }

    private JsonObject read(Path file) {
        if (!Files.isRegularFile(file)) {
            return new JsonObject();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (IOException | RuntimeException exception) {
            Yesterglass.LOGGER.error(
                    "Could not read {}; falling back to defaults for this run", file, exception);
            return new JsonObject();
        }
    }

    private <T> void apply(ConfigValue<T> value, JsonObject root) {
        JsonObject owner = root;
        for (String name : value.section()) {
            JsonElement child = owner.get(name);
            if (child == null || !child.isJsonObject()) {
                value.set(value.defaultValue());
                return;
            }
            owner = child.getAsJsonObject();
        }
        JsonElement element = owner.get(value.key());
        if (element == null) {
            value.set(value.defaultValue());
            return;
        }
        T parsed = value.fromJson(element);
        if (parsed == null || !value.accepts(parsed)) {
            Yesterglass.LOGGER.warn(
                    "{}: '{}' is not a valid value for {}; using the default {}",
                    fileName,
                    element,
                    qualified(value),
                    value.defaultValue());
            value.set(value.defaultValue());
            return;
        }
        value.set(parsed);
    }

    private static String qualified(ConfigValue<?> value) {
        return value.section().isEmpty()
                ? value.key()
                : String.join(".", value.section()) + "." + value.key();
    }

    private void write(Path file) {
        JsonObject root = new JsonObject();
        for (ConfigValue<?> value : values) {
            JsonObject owner = root;
            for (String name : value.section()) {
                JsonElement child = owner.get(name);
                if (child == null || !child.isJsonObject()) {
                    JsonObject created = new JsonObject();
                    owner.add(name, created);
                    owner = created;
                } else {
                    owner = child.getAsJsonObject();
                }
            }
            if (value.comment() != null && !value.comment().isEmpty()) {
                owner.addProperty(COMMENT_PREFIX + value.key(), value.comment());
            }
            owner.add(value.key(), value.currentAsJson());
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                new com.google.gson.GsonBuilder()
                        .setPrettyPrinting()
                        // The comments are prose, and HTML escaping turns an
                        // ordinary "=" in them into =.
                        .disableHtmlEscaping()
                        .create()
                        .toJson(root, writer);
                writer.write(System.lineSeparator());
            }
        } catch (IOException | RuntimeException exception) {
            Yesterglass.LOGGER.error("Could not write {}", file, exception);
        }
    }
}
