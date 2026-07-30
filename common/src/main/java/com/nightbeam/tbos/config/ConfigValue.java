package com.nightbeam.tbos.config;

import java.util.List;
import java.util.function.Predicate;

/**
 * One configurable value.
 *
 * <p>The accessor shape ({@code get}, {@code getAsBoolean}, {@code getAsInt})
 * and the {@link IllegalStateException} thrown before the file has been read
 * both match NeoForge's {@code ModConfigSpec} values, because callers all over
 * the mod wrap their reads in {@code try/catch (IllegalStateException)} and fall
 * back to {@code ArchiveDungeonSettings.DEFAULT}. Keeping that contract is what
 * lets GameTests and the standalone simulation run without a config file.
 */
public abstract sealed class ConfigValue<T> {
    private final ConfigSchema owner;
    private final List<String> section;
    private final String key;
    private final T defaultValue;
    private final String comment;
    private final Predicate<Object> validator;
    private volatile T value;

    ConfigValue(
            ConfigSchema owner,
            List<String> section,
            String key,
            T defaultValue,
            String comment,
            Predicate<Object> validator) {
        this.owner = owner;
        this.section = List.copyOf(section);
        this.key = key;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.validator = validator;
        this.value = defaultValue;
    }

    public T get() {
        if (!owner.isLoaded()) {
            throw new IllegalStateException("Config " + owner.fileName() + " has not been loaded");
        }
        return value;
    }

    /** The compiled-in default, readable whether or not the file has been loaded. */
    public T defaultValue() {
        return defaultValue;
    }

    List<String> section() {
        return section;
    }

    String key() {
        return key;
    }

    String comment() {
        return comment;
    }

    boolean accepts(Object candidate) {
        return validator.test(candidate);
    }

    void set(T value) {
        this.value = value;
    }

    /** Parses one JSON-decoded value, or returns null when it is unusable. */
    abstract T fromJson(com.google.gson.JsonElement element);

    abstract com.google.gson.JsonElement toJson(T value);

    com.google.gson.JsonElement currentAsJson() {
        return toJson(value);
    }

    public static final class Bool extends ConfigValue<Boolean> {
        Bool(ConfigSchema owner, List<String> section, String key, boolean defaultValue, String comment) {
            super(owner, section, key, defaultValue, comment, candidate -> candidate instanceof Boolean);
        }

        public boolean getAsBoolean() {
            return get();
        }

        @Override
        Boolean fromJson(com.google.gson.JsonElement element) {
            return element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()
                    ? element.getAsBoolean()
                    : null;
        }

        @Override
        com.google.gson.JsonElement toJson(Boolean value) {
            return new com.google.gson.JsonPrimitive(value);
        }
    }

    public static final class Int extends ConfigValue<Integer> {
        Int(ConfigSchema owner, List<String> section, String key, int defaultValue, int min, int max, String comment) {
            super(owner, section, key, defaultValue, comment,
                    candidate -> candidate instanceof Integer number && number >= min && number <= max);
        }

        public int getAsInt() {
            return get();
        }

        @Override
        Integer fromJson(com.google.gson.JsonElement element) {
            return isNumber(element) ? element.getAsInt() : null;
        }

        @Override
        com.google.gson.JsonElement toJson(Integer value) {
            return new com.google.gson.JsonPrimitive(value);
        }
    }

    public static final class Long extends ConfigValue<java.lang.Long> {
        Long(
                ConfigSchema owner,
                List<String> section,
                String key,
                long defaultValue,
                long min,
                long max,
                String comment) {
            super(owner, section, key, defaultValue, comment,
                    candidate -> candidate instanceof java.lang.Long number && number >= min && number <= max);
        }

        public long getAsLong() {
            return get();
        }

        @Override
        java.lang.Long fromJson(com.google.gson.JsonElement element) {
            return isNumber(element) ? element.getAsLong() : null;
        }

        @Override
        com.google.gson.JsonElement toJson(java.lang.Long value) {
            return new com.google.gson.JsonPrimitive(value);
        }
    }

    public static final class Double extends ConfigValue<java.lang.Double> {
        Double(
                ConfigSchema owner,
                List<String> section,
                String key,
                double defaultValue,
                double min,
                double max,
                String comment) {
            super(owner, section, key, defaultValue, comment,
                    candidate -> candidate instanceof java.lang.Double number && number >= min && number <= max);
        }

        public double getAsDouble() {
            return get();
        }

        @Override
        java.lang.Double fromJson(com.google.gson.JsonElement element) {
            return isNumber(element) ? element.getAsDouble() : null;
        }

        @Override
        com.google.gson.JsonElement toJson(java.lang.Double value) {
            return new com.google.gson.JsonPrimitive(value);
        }
    }

    public static final class Text extends ConfigValue<String> {
        Text(
                ConfigSchema owner,
                List<String> section,
                String key,
                String defaultValue,
                Predicate<Object> validator,
                String comment) {
            super(owner, section, key, defaultValue, comment, validator);
        }

        @Override
        String fromJson(com.google.gson.JsonElement element) {
            return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                    ? element.getAsString()
                    : null;
        }

        @Override
        com.google.gson.JsonElement toJson(String value) {
            return new com.google.gson.JsonPrimitive(value);
        }
    }

    /**
     * A list of strings. The declared type is {@code List<? extends String>} to
     * match what the call sites already expect from the old spec values.
     */
    public static final class TextList extends ConfigValue<List<? extends String>> {
        TextList(
                ConfigSchema owner,
                List<String> section,
                String key,
                List<String> defaultValue,
                Predicate<Object> elementValidator,
                String comment) {
            super(owner, section, key, List.copyOf(defaultValue), comment,
                    candidate -> candidate instanceof List<?> list
                            && list.stream().allMatch(elementValidator));
        }

        @Override
        List<? extends String> fromJson(com.google.gson.JsonElement element) {
            if (!element.isJsonArray()) {
                return null;
            }
            List<String> parsed = new java.util.ArrayList<>();
            for (com.google.gson.JsonElement entry : element.getAsJsonArray()) {
                if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                    return null;
                }
                parsed.add(entry.getAsString());
            }
            return List.copyOf(parsed);
        }

        @Override
        com.google.gson.JsonElement toJson(List<? extends String> value) {
            com.google.gson.JsonArray array = new com.google.gson.JsonArray();
            value.forEach(array::add);
            return array;
        }
    }

    public static final class OfEnum<E extends Enum<E>> extends ConfigValue<E> {
        private final Class<E> type;

        OfEnum(ConfigSchema owner, List<String> section, String key, E defaultValue, String comment) {
            super(owner, section, key, defaultValue, comment, candidate -> candidate != null);
            this.type = defaultValue.getDeclaringClass();
        }

        @Override
        E fromJson(com.google.gson.JsonElement element) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                return null;
            }
            try {
                return Enum.valueOf(type, element.getAsString().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }

        @Override
        com.google.gson.JsonElement toJson(E value) {
            return new com.google.gson.JsonPrimitive(value.name());
        }
    }

    private static boolean isNumber(com.google.gson.JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber();
    }
}
