package com.nightbeam.tbos.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Compatibility entry point; all ability artwork comes from the Greek icon atlas. */
public final class MemoryGlyphs {
    private MemoryGlyphs() {}
    public static void draw(GuiGraphicsExtractor graphics,int id,int x,int y,int scale,int color) {
        MemoryIcons.draw(graphics,id,x,y,7*scale);
    }
}
