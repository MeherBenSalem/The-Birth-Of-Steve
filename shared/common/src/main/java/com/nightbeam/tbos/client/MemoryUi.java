package com.nightbeam.tbos.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Shared palette and accessible native buttons for the memory journal. */
public final class MemoryUi {
    public static final int PANEL=0xFA101B25, CARD=0xFF192B36, EDGE=0xFF34505B,
        TEXT=0xFFE7F0ED, MUTED=0xFFACBDBF, CYAN=0xFF96DDD8, GOLD=0xFFEAC17A, VIOLET=0xFFC2A8D8;
    private MemoryUi() {}
    public static void frame(GuiGraphicsExtractor g,int x,int y,int w,int h,int fill,int edge) {
        g.fill(x,y,x+w,y+h,fill);
        g.fill(x,y,x+w,y+1,edge);g.fill(x,y+h-1,x+w,y+h,edge);
        g.fill(x,y,x+1,y+h,edge);g.fill(x+w-1,y,x+w,y+h,edge);
    }
    public static void text(GuiGraphicsExtractor g,Component text,int x,int y,int width,int color) {
        var font=Minecraft.getInstance().font;
        String value=text.getString();
        if(font.width(value)>width)value=font.plainSubstrByWidth(value,Math.max(0,width-font.width("…")))+"…";
        g.text(font,value,x,y,color,false);
    }
    public static final class Action extends Button {
        private final int icon;
        private final boolean selected;
        private final Component subtitle;
        public Action(int x,int y,int w,int h,Component label,Component subtitle,int icon,boolean selected,boolean enabled,Runnable action,Component hint) {
            super(x,y,w,h,label,b->action.run(),DEFAULT_NARRATION);
            this.icon=icon;this.selected=selected;this.subtitle=subtitle;this.active=enabled;
            setTooltip(Tooltip.create(hint==null?label:hint));
        }
        @Override protected void extractContents(GuiGraphicsExtractor g,int mouseX,int mouseY,float partialTick) {
            boolean hover=isHoveredOrFocused();
            int edge=selected?CYAN:hover&&active?GOLD:EDGE;
            frame(g,getX(),getY(),getWidth(),getHeight(),hover&&active?0xFF24404B:CARD,edge);
            if(selected)g.fill(getX()+1,getY()+2,getX()+3,getY()+getHeight()-2,CYAN);
            int size=Math.min(28,getHeight()-4),inset=icon>=0?size+7:7;
            if(icon>=0)MemoryIcons.draw(g,icon,getX()+4,getY()+2,size);
            int y=getY()+(getHeight()-9)/2;
            if(subtitle!=null)y=getY()+5;
            text(g,getMessage(),getX()+inset,y,getWidth()-inset-5,active?TEXT:MUTED);
            if(subtitle!=null)text(g,subtitle,getX()+inset,y+12,getWidth()-inset-5,MUTED);
        }
    }
}
