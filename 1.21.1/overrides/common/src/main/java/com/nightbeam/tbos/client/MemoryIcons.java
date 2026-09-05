package com.nightbeam.tbos.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** One resident atlas; integer cell bounds prevent sampling a neighbouring icon. */
public final class MemoryIcons {
    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath("tbos", "textures/gui/memory/icons.png");
    private MemoryIcons() {}
    public static void draw(GuiGraphics graphics, int id, int x, int y, int size) {
        if (id < 0 || id >= 18) return;
        int u = id % 6 * 1774 / 6, v = id / 6 * 887 / 3;
        int sw = (id % 6 + 1) * 1774 / 6 - u, sh = (id / 6 + 1) * 887 / 3 - v;
        graphics.blit(ATLAS, x, y, size, size, (float)u, (float)v, sw, sh, 1774, 887);
    }
}
