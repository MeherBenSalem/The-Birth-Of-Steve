package com.nightbeam.tbos.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** 1.20.1 represents a key category by its translation key rather than an object. */
public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.tbos.archive";
    public static final KeyMapping TOGGLE_OBJECTIVES = new KeyMapping(
            "key.tbos.toggle_objectives", GLFW.GLFW_KEY_J, CATEGORY);

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
