package com.nightbeam.tbos.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** 1.21.1 represents a key category by its translation key rather than an object. */
public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.tbos.archive";
    public static final KeyMapping TOGGLE_OBJECTIVES = new KeyMapping(
            "key.tbos.toggle_objectives", GLFW.GLFW_KEY_J, CATEGORY);

    public static final KeyMapping[] MEMORY_SLOTS = {
        new KeyMapping("key.tbos.memory_1", GLFW.GLFW_KEY_R, CATEGORY),
        new KeyMapping("key.tbos.memory_2", GLFW.GLFW_KEY_G, CATEGORY),
        new KeyMapping("key.tbos.memory_3", GLFW.GLFW_KEY_V, CATEGORY)
    };
    public static final KeyMapping MEMORY_LOADOUT = new KeyMapping("key.tbos.memory_loadout", GLFW.GLFW_KEY_K, CATEGORY);
    private static boolean objectivesHidden;

    private ModKeyMappings() {
    }

    public static boolean objectivesHidden() {
        return objectivesHidden;
    }

    public static boolean toggleObjectives() {
        objectivesHidden = !objectivesHidden;
        return objectivesHidden;
    }
}
