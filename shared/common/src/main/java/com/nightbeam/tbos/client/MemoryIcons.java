package com.nightbeam.tbos.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Generated bronze reliefs. Stable game IDs map to the documented row-major atlas cells. */
public final class MemoryIcons {
    private static final Identifier ATLAS = Identifier.fromNamespaceAndPath("tbos", "textures/gui/greek/icons.png");
    private static final int ATLAS_WIDTH=1774, ATLAS_HEIGHT=887, COLUMNS=6, ROWS=3;
    private static final String[] SYMBOLS={
        "echo_lance","recall","parallax_step","resonant_guard","reconstruct","memory_well",
        "split_prism","seeking_glass","ember_script","storm_filament","shatter_seal","piercing_index",
        "returning_thread","delayed_ink","resonant_nail","memory_wick","ward_fragment","masons_remnant"
    };
    private MemoryIcons() {}
    public static void draw(GuiGraphicsExtractor graphics, int id, int x, int y, int size) {
        if (id < 0 || id >= SYMBOLS.length) return;
        int u = id % COLUMNS * ATLAS_WIDTH / COLUMNS, v = id / COLUMNS * ATLAS_HEIGHT / ROWS;
        int sw = (id % COLUMNS + 1) * ATLAS_WIDTH / COLUMNS - u, sh = (id / COLUMNS + 1) * ATLAS_HEIGHT / ROWS - v;
        graphics.blit(RenderPipelines.GUI_TEXTURED, ATLAS, x, y, (float)u, (float)v, size, size, sw, sh, ATLAS_WIDTH, ATLAS_HEIGHT);
    }
}
