package com.nightbeam.tbos.client;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "archive"));

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

    /**
     * The category and the mapping itself are plain vanilla objects; only the
     * act of handing them to the game is loader-specific, so each client entry
     * point registers {@link #CATEGORY} and {@link #TOGGLE_OBJECTIVES} itself.
     */

    public static boolean objectivesHidden() {
        return objectivesHidden;
    }

    public static boolean toggleObjectives() {
        objectivesHidden = !objectivesHidden;
        return objectivesHidden;
    }
}
