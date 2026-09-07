package com.nightbeam.tbos.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Adapter that lays out and samples the shared Greek GUI atlases. */
public final class GreekGui {
    public static final int FRAME=0, WELL=1, BUTTON=2, HOVER=3, SELECTED=4, DISABLED=5,
            CARD=6, HUD=7, TAB_ACTIVE=8, TAB_IDLE=9, COMPLETE=10, LOCKED=11,
            TRACK=12, TEAL=13, GOLD=14, WARNING=15;
    public static final int INK=0xFF302B24, MUTED=0xFF655A49, ACCENT=0xFF205C59,
            BRONZE=0xFF765021, LIGHT=0xFFF3EBDD, LIGHT_TEAL=0xFFA5D4CA;
    private static final Identifier PANELS = Identifier.fromNamespaceAndPath("tbos", "textures/gui/greek/panels.png");
    private static final Identifier ORNAMENTS = Identifier.fromNamespaceAndPath("tbos", "textures/gui/greek/ornaments.png");
    private static final int PANEL_SIZE=1254;
    private static final int ORNAMENT_SIZE=1254;
    private GreekGui() {}

    /** Nine-slicing keeps the relief corners intact even on narrow widgets. */
    public static void panel(GuiGraphicsExtractor g,int tile,int x,int y,int w,int h) {
        if(w<=0||h<=0)return;
        int u=tile%4*PANEL_SIZE/4+2,v=tile/4*PANEL_SIZE/4+2;
        int sw=(tile%4+1)*PANEL_SIZE/4-2-u,sh=(tile/4+1)*PANEL_SIZE/4-2-v;
        int sourceBorder=72, bx=Math.min(tile==FRAME?8:4,w/2),by=Math.min(tile==FRAME?8:4,h/2);
        int[] dx={x,x+bx,x+w-bx,x+w},dy={y,y+by,y+h-by,y+h};
        int[] sx={u,u+sourceBorder,u+sw-sourceBorder,u+sw},sy={v,v+sourceBorder,v+sh-sourceBorder,v+sh};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++)
            region(g,PANELS,dx[col],dy[row],dx[col+1]-dx[col],dy[row+1]-dy[row],
                    sx[col],sy[row],sx[col+1]-sx[col],sy[row+1]-sy[row],PANEL_SIZE,PANEL_SIZE);
    }

    /** Sample the quiet center of the panel material for fine rules and progress fills. */
    public static void material(GuiGraphicsExtractor g,int tile,int x,int y,int w,int h) {
        region(g,PANELS,x,y,w,h,tile%4*PANEL_SIZE/4+112,tile/4*PANEL_SIZE/4+112,88,88,PANEL_SIZE,PANEL_SIZE);
    }

    public static void progress(GuiGraphicsExtractor g,int x,int y,int w,int h,float fraction,boolean complete) {
        material(g,TRACK,x,y,w,h);
        int filled=Math.round(w*Math.max(0,Math.min(1,fraction)));
        material(g,complete?GOLD:TEAL,x,y,filled,h);
    }

    public static void ornament(GuiGraphicsExtractor g,int id,int x,int y,int w,int h) {
        int u=id%2*ORNAMENT_SIZE/2,v=id/2*ORNAMENT_SIZE/2;
        region(g,ORNAMENTS,x,y,w,h,u,v,ORNAMENT_SIZE/2,ORNAMENT_SIZE/2,ORNAMENT_SIZE,ORNAMENT_SIZE);
    }

    private static void region(GuiGraphicsExtractor g,Identifier texture,int x,int y,int w,int h,
            int u,int v,int sw,int sh,int tw,int th) {
        if(w<=0||h<=0||sw<=0||sh<=0)return;
        g.blit(RenderPipelines.GUI_TEXTURED,texture,x,y,(float)u,(float)v,w,h,sw,sh,tw,th);
    }
}
