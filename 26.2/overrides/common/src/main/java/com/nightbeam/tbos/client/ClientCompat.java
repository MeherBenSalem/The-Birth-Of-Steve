package com.nightbeam.tbos.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The 26.2 shape of the client compat seam. Overrides the shared 26.1.2 copy at
 * {@code shared/common/src/main/java/com/nightbeam/tbos/client/ClientCompat.java};
 * keep the two in step whenever a method is added.
 *
 * <p>26.2 split the old monolithic {@code Gui}: {@code Minecraft.gui} now owns
 * both the screen stack and a separate {@code Hud}, and {@code Options.hideGui}
 * became {@code Hud.isHidden()}.
 */
public final class ClientCompat {

    private ClientCompat() {
    }

    /** Whether the player has hidden the HUD (F1). */
    public static boolean isHudHidden(Minecraft minecraft) {
        return minecraft.gui.hud.isHidden();
    }

    /** The screen currently open, or null in-world. */
    public static Screen currentScreen(Minecraft minecraft) {
        return minecraft.gui.screen();
    }

    /**
     * Opens {@code screen}, or returns to the in-world HUD when it is null.
     * {@code Minecraft.setScreenAndShow} is the wrong entry point here - it forces
     * an extra render frame that the old {@code Minecraft.setScreen} never did.
     */
    public static void setScreen(Minecraft minecraft, Screen screen) {
        minecraft.gui.setScreen(screen);
    }

    /** The transient line above the hotbar. */
    public static void setOverlayMessage(Minecraft minecraft, Component message, boolean animate) {
        minecraft.gui.hud.setOverlayMessage(message, animate);
    }
}
