package com.nightbeam.tbos.client.rpg;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The client-side seam every 1.0 screen extends. It translates this version's
 * render and input callbacks into four version-neutral hooks, so the screens can
 * be written once. This is the 26.1.2 shape; other targets override this file.
 */
public abstract class RpgScreen extends Screen {
    public static final int KEY_ESCAPE = InputConstants.KEY_ESCAPE;
    public static final int KEY_ENTER = InputConstants.KEY_RETURN;
    public static final int KEY_TAB = InputConstants.KEY_TAB;
    public static final int KEY_RIGHT = InputConstants.KEY_RIGHT;
    public static final int KEY_LEFT = InputConstants.KEY_LEFT;
    public static final int KEY_DOWN = InputConstants.KEY_DOWN;
    public static final int KEY_UP = InputConstants.KEY_UP;

    protected RpgScreen(Component title) {
        super(title);
    }

    /** Draws the whole screen; called every frame. */
    protected abstract void draw(Canvas canvas, int mouseX, int mouseY, float partialTick);

    /** A mouse button went down at gui coordinates; return true when handled. */
    protected boolean onClick(double mouseX, double mouseY, int button) {
        return false;
    }

    /** A key went down; return true when handled (Escape still closes unless handled). */
    protected boolean onKey(int key) {
        return false;
    }

    protected boolean onScroll(double mouseX, double mouseY, double amount) {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Canvas canvas = new Canvas(graphics);
        draw(canvas, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        canvas.flush();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return onClick(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return onKey(event.key()) || super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return onScroll(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
