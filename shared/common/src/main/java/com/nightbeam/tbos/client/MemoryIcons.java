package com.nightbeam.tbos.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** One resident atlas; integer cell bounds prevent sampling a neighbouring icon. */
public final class MemoryIcons {
    private static final Identifier ATLAS = Identifier.fromNamespaceAndPath("tbos", "textures/gui/memory/icons.png");
    private MemoryIcons() {}
    public static void draw(GuiGraphicsExtractor graphics, int id, int x, int y, int size) {
        if (id < 0 || id >= 18) return;
        int u = id % 6 * 1774 / 6, v = id / 6 * 887 / 3;
        int sw = (id % 6 + 1) * 1774 / 6 - u, sh = (id / 6 + 1) * 887 / 3 - v;
        graphics.blit(RenderPipelines.GUI_TEXTURED, ATLAS, x, y, (float)u, (float)v, size, size, sw, sh, 1774, 887);
    }
}
