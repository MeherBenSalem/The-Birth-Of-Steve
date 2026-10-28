package com.nightbeam.tbos.client.rpg;

import com.nightbeam.tbos.client.GreekGui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * The client-side seam for the 1.0 screens and HUD: every drawing call they make
 * goes through here. This is the 1.20.1 shape around {@code GuiGraphics}: a pose
 * stack, shader-colour tinting and immediate tooltips.
 */
public final class Canvas {
    public static final String ICONS = "textures/gui/rpg/icons.png";
    public static final int ICON_COLUMNS = 8;
    public static final int ICON_CELL = 128;
    public static final int ICON_SHEET = 1024;

    private static final ResourceLocation ICON_TEXTURE = new ResourceLocation("tbos", ICONS);

    private final GuiGraphics g;
    private List<Component> tooltip;
    private int tooltipX;
    private int tooltipY;

    public Canvas(GuiGraphics graphics) {
        this.g = graphics;
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    public int width() {
        return g.guiWidth();
    }

    public int height() {
        return g.guiHeight();
    }

    public void fill(int x, int y, int w, int h, int argb) {
        if (w > 0 && h > 0) {
            g.fill(x, y, x + w, y + h, argb);
        }
    }

    public void gradient(int x, int y, int w, int h, int top, int bottom) {
        if (w > 0 && h > 0) {
            g.fillGradient(x, y, x + w, y + h, top, bottom);
        }
    }

    public void outline(int x, int y, int w, int h, int argb) {
        fill(x, y, w, 1, argb);
        fill(x, y + h - 1, w, 1, argb);
        fill(x, y + 1, 1, h - 2, argb);
        fill(x + w - 1, y + 1, 1, h - 2, argb);
    }

    public void text(Component text, int x, int y, int argb, boolean shadow) {
        g.drawString(font(), text, x, y, argb, shadow);
    }

    public void text(String text, int x, int y, int argb, boolean shadow) {
        g.drawString(font(), text, x, y, argb, shadow);
    }

    public void text(FormattedCharSequence text, int x, int y, int argb, boolean shadow) {
        g.drawString(font(), text, x, y, argb, shadow);
    }

    public static int width(Component text) {
        return font().width(text);
    }

    public static int width(String text) {
        return font().width(text);
    }

    public static List<FormattedCharSequence> split(FormattedText text, int width) {
        return font().split(text, Math.max(1, width));
    }

    public void panel(int tile, int x, int y, int w, int h) {
        GreekGui.panel(g, tile, x, y, w, h);
    }

    public void material(int tile, int x, int y, int w, int h) {
        GreekGui.material(g, tile, x, y, w, h);
    }

    public void progress(int x, int y, int w, int h, float fraction, boolean complete) {
        GreekGui.progress(g, x, y, w, h, fraction, complete);
    }

    public void ornament(int id, int x, int y, int w, int h) {
        GreekGui.ornament(g, id, x, y, w, h);
    }

    /** One cell of the 1.0 icon sheet, scaled to {@code size}, tinted by {@code argb}. */
    public void icon(int index, int x, int y, int size, int argb) {
        int u = index % ICON_COLUMNS * ICON_CELL;
        int v = index / ICON_COLUMNS * ICON_CELL;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(((argb >> 16) & 0xFF) / 255.0F, ((argb >> 8) & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F,
                ((argb >>> 24) & 0xFF) / 255.0F);
        g.blit(ICON_TEXTURE, x, y, size, size, u, v, ICON_CELL, ICON_CELL, ICON_SHEET, ICON_SHEET);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    public void item(ItemStack stack, int x, int y) {
        g.renderItem(stack, x, y);
    }

    public void push() {
        g.pose().pushPose();
    }

    public void pop() {
        g.pose().popPose();
    }

    public void translate(float x, float y) {
        g.pose().translate(x, y, 0.0F);
    }

    public void scale(float factor) {
        g.pose().scale(factor, factor, 1.0F);
    }

    public void clip(int x, int y, int w, int h) {
        g.enableScissor(x, y, x + w, y + h);
    }

    public void unclip() {
        g.disableScissor();
    }

    /** Queues a tooltip; {@link #flush()} draws it above everything else. */
    public void tooltip(List<Component> lines, int x, int y) {
        tooltip = new ArrayList<>(lines);
        tooltipX = x;
        tooltipY = y;
    }

    public void flush() {
        if (tooltip != null && !tooltip.isEmpty()) {
            g.renderComponentTooltip(font(), tooltip, tooltipX, tooltipY);
        }
        tooltip = null;
    }

    public static void playUi(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    public static void open(Screen screen) {
        com.nightbeam.tbos.client.ClientCompat.setScreen(Minecraft.getInstance(), screen);
    }

    public static Screen current() {
        return com.nightbeam.tbos.client.ClientCompat.currentScreen(Minecraft.getInstance());
    }

    public static boolean hudHidden() {
        return com.nightbeam.tbos.client.ClientCompat.isHudHidden(Minecraft.getInstance());
    }
}
