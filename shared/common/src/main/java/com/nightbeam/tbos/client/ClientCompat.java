package com.nightbeam.tbos.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The client-side seam where vanilla API differences between supported Minecraft
 * versions are absorbed. This is the 26.1.2 shape; 26.2 replaces the whole class
 * from {@code 26.2/overrides}.
 *
 * <p>Lives in the client package rather than next to {@link
 * com.nightbeam.tbos.compat.VanillaCompat} because it names {@code Minecraft}: a
 * dedicated server must never classload it.
 */
public final class ClientCompat {

    private ClientCompat() {
    }

    /** Whether the player has hidden the HUD (F1). 26.2 moved this onto the Hud object. */
    public static boolean isHudHidden(Minecraft minecraft) {
        return minecraft.options.hideGui;
    }

    /** The screen currently open, or null in-world. 26.2 moved screen ownership onto Gui. */
    public static Screen currentScreen(Minecraft minecraft) {
        return minecraft.screen;
    }

    /** Opens {@code screen}, or returns to the in-world HUD when it is null. */
    public static void setScreen(Minecraft minecraft, Screen screen) {
        minecraft.setScreen(screen);
    }

    /** The transient line above the hotbar. 26.2 moved it from Gui onto Gui.hud. */
    public static void setOverlayMessage(Minecraft minecraft, Component message, boolean animate) {
        if (minecraft.gui != null) {
            minecraft.gui.setOverlayMessage(message, animate);
        }
    }
}
