package com.nightbeam.tbos.client;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "archive"));

    public static final KeyMapping TOGGLE_OBJECTIVES = new KeyMapping(
            "key.tbos.toggle_objectives", GLFW.GLFW_KEY_J, CATEGORY);

    private static boolean objectivesHidden;

    private ModKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(TOGGLE_OBJECTIVES);
    }

    public static boolean objectivesHidden() {
        return objectivesHidden;
    }

    public static boolean toggleObjectives() {
        objectivesHidden = !objectivesHidden;
        return objectivesHidden;
    }
}
