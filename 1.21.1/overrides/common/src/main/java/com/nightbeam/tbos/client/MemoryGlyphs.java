package com.nightbeam.tbos.client;

import net.minecraft.client.gui.GuiGraphics;

/** Original 7×7 pixel glyphs, rendered natively at integer scale. Shared across discovery and HUD. */
public final class MemoryGlyphs {
    private static final String[] GLYPHS={
        "0001000/0011100/0111110/0001000/0001000/0011100/0001000", // lance
        "0011100/0100010/1000001/1011001/1001001/0100010/0011100", // recall
        "1000100/0100010/0010001/0111111/0010001/0100010/1000100", // step
        "1111111/1000001/1001001/1011101/0101010/0011100/0001000", // guard
        "0011100/0100010/1111111/1001001/1001001/1111111/0001000", // reconstruct
        "0011100/0100010/1001001/1011101/1001001/0100010/0011100", // well
        "1001001/0101010/0011100/0001000/0001000/0001000/0001000", // split
        "0011100/0100010/1000001/1001001/1000101/0100010/0011101", // seek
        "0001000/0011000/0010100/0100010/1101011/1011101/0111110", // ember
        "0001100/0011000/0110000/1111110/0001100/0011000/0110000", // storm
        "1001001/0100010/0010100/1001001/0010100/0100010/1001001", // shatter
        "0001000/0011100/0001000/1101011/0001000/0011100/0001000", // pierce
        "0011100/0100010/1001001/0101001/0011001/0000010/0011100", // return
        "1111111/0100010/0010100/0001000/0010100/0100010/1111111", // delay
        "1111111/0001000/0001000/0011100/0001000/0001000/0001000", // nail
        "0001000/0011100/0001000/0011100/0100010/0100010/0111110", // wick
        "0011111/0010001/0010101/0011001/0100010/0111100/0100000", // fragment
        "1110111/1010101/1110111/0000000/0111110/0101010/0111110"  // mason
    };
    private MemoryGlyphs(){}
    public static void draw(GuiGraphics graphics,int id,int x,int y,int scale,int color) {
        if(id<0||id>=GLYPHS.length)return;
        String glyph=GLYPHS[id];
        for(int row=0;row<7;row++)for(int col=0;col<7;col++)if(glyph.charAt(row*8+col)=='1')
            graphics.fill(x+col*scale,y+row*scale,x+(col+1)*scale,y+(row+1)*scale,color);
    }
}
