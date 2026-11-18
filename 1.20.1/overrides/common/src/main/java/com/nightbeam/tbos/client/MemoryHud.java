package com.nightbeam.tbos.client;

import com.nightbeam.tbos.config.HudAnchor;
import com.nightbeam.tbos.config.YesterglassClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Compact, textured ability strip that leaves the crosshair and vanilla status bars clear. */
public final class MemoryHud {
    /** Three 44-pixel cards, 48 pixels apart. */
    static final int STRIP_WIDTH=140,STRIP_HEIGHT=29;
    /** Distance from the bottom edge that clears the hotbar and status bars. */
    static final int DEFAULT_BOTTOM=79;
    private static final int MARGIN=4;
    private MemoryHud() {}
    /** Left edge of the strip: the anchor's column plus the offset, kept on screen. */
    static int stripX(int width,HudAnchor anchor,int offset) {
        int base=anchor.left()?MARGIN:anchor.right()?width-STRIP_WIDTH-MARGIN:width/2-71;
        return clamp(base+offset,width-STRIP_WIDTH);
    }
    /** Top edge of the strip: the anchor's row plus the offset, kept on screen. */
    static int stripY(int height,HudAnchor anchor,int offset) {
        int base=anchor.top()?MARGIN:anchor==HudAnchor.BOTTOM_CENTER?height-DEFAULT_BOTTOM:height-STRIP_HEIGHT-MARGIN;
        return clamp(base+offset,height-STRIP_HEIGHT);
    }
    private static int clamp(int value,int max) {
        return Math.max(0,Math.min(Math.max(0,max),value));
    }
    public static void render(GuiGraphics g) {
        var b=MemoryClient.build;var mc=Minecraft.getInstance();
        if(b==null||mc.player==null||ClientCompat.isHudHidden(mc))return;
        boolean visible=YesterglassClientConfig.ABILITY_HUD_VISIBLE.getAsBoolean();
        HudAnchor anchor=YesterglassClientConfig.ABILITY_HUD_ANCHOR.get();
        int x=stripX(g.guiWidth(),anchor,YesterglassClientConfig.ABILITY_HUD_OFFSET_X.getAsInt());
        int y=stripY(g.guiHeight(),anchor,YesterglassClientConfig.ABILITY_HUD_OFFSET_Y.getAsInt());
        // The discovery card is a one-off announcement, so it keeps its place
        // above the hotbar wherever the strip has been moved, and shows even
        // when the strip is hidden.
        int defaultY=g.guiHeight()-DEFAULT_BOTTOM;
        for(int i=0;visible&&i<3;i++) {
            int id=b.slots.get(i),left=x+i*48;
            long remaining=id<0?0:Math.max(0,b.ready.get(id)-MemoryClient.now());
            GreekGui.panel(g,id>=0&&remaining==0?GreekGui.CARD:GreekGui.DISABLED,left,y,44,29);
            if(id>=0)MemoryIcons.draw(g,id,left+2,y+2,23);
            else MemoryUi.text(g,Component.literal("—"),left+9,y+10,17,MemoryUi.MUTED);
            MemoryUi.text(g,ModKeyMappings.MEMORY_SLOTS[i].getTranslatedKeyMessage(),left+28,y+5,14,MemoryUi.TEXT);
            if(remaining>0) {
                String seconds=remaining<200?String.format(java.util.Locale.ROOT,"%.1f",remaining/20.0):Long.toString((remaining+19)/20);
                MemoryUi.text(g,Component.literal(seconds),left+25,y+16,18,MemoryUi.MUTED);
                int bar=(int)Math.min(40,40*remaining/com.nightbeam.tbos.memory.MemoryAbility.values()[id].cooldown);
                GreekGui.progress(g,left+4,y+25,36,2,bar/40.0F,false);
            }
        }
        if(visible&&!b.offers.isEmpty()) {
            Component hint=Component.translatable("memory.tbos.pending",ModKeyMappings.MEMORY_LOADOUT.getTranslatedKeyMessage());
            int available=Math.min(g.guiWidth()-16,240),textWidth=Math.min(available,mc.font.width(hint));
            // Centred over the strip, kept on screen, and below it when the
            // strip sits at the top edge.
            int hintX=Math.max(0,Math.min(g.guiWidth()-textWidth,(2*x+142-textWidth)/2));
            int hintY=y<13?y+STRIP_HEIGHT+4:y-13;
            MemoryUi.text(g,hint,hintX,hintY,available,MemoryUi.GOLD);
        }
        if(MemoryClient.discovery>=0&&System.nanoTime()<MemoryClient.discoveryUntil) {
            int w=Math.min(246,g.guiWidth()-16),cx=(g.guiWidth()-w)/2,cy=defaultY-66;
            GreekGui.panel(g,GreekGui.FRAME,cx,cy,w,43);
            MemoryIcons.draw(g,MemoryClient.discovery,cx+5,cy+5,32);
            MemoryUi.text(g,MemoryClient.name(MemoryClient.discovery),cx+44,cy+9,w-51,MemoryUi.GOLD);
            MemoryUi.text(g,Component.translatable(MemoryScreen.key(MemoryClient.discovery)+".flavor"),cx+44,cy+24,w-51,MemoryUi.MUTED);
        }
    }
}
