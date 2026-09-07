package com.nightbeam.tbos.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Shared palette and accessible native buttons for the memory journal. */
public final class MemoryUi {
    public static final int PANEL=GreekGui.FRAME, CARD=GreekGui.CARD, EDGE=GreekGui.BRONZE,
        TEXT=GreekGui.INK, MUTED=GreekGui.MUTED, CYAN=GreekGui.ACCENT, GOLD=GreekGui.BRONZE, VIOLET=0xFF784734;
    private MemoryUi() {}
    public static void frame(GuiGraphics g,int x,int y,int w,int h,int fill,int edge) {
        GreekGui.panel(g,fill==PANEL?GreekGui.FRAME:GreekGui.CARD,x,y,w,h);
    }
    public static void text(GuiGraphics g,Component text,int x,int y,int width,int color) {
        if(width<=0)return;
        var font=Minecraft.getInstance().font;
        String value=text.getString();
        if(font.width(value)>width)value=font.plainSubstrByWidth(value,Math.max(0,width-font.width("…")))+"…";
        g.drawString(font,value,x,y,color,false);
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
        @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
            boolean hover=isHoveredOrFocused();
            int tile=!active?GreekGui.DISABLED:hover?GreekGui.HOVER:selected?GreekGui.SELECTED:GreekGui.BUTTON;
            GreekGui.panel(g,tile,getX(),getY(),getWidth(),getHeight());
            int size=Math.min(28,getHeight()-4),inset=icon>=0?size+7:7;
            if(icon>=0)MemoryIcons.draw(g,icon,getX()+4,getY()+2,size);
            int y=getY()+(getHeight()-9)/2;
            if(subtitle!=null)y=getY()+5;
            text(g,getMessage(),getX()+inset,y,getWidth()-inset-5,active?TEXT:MUTED);
            if(subtitle!=null)text(g,subtitle,getX()+inset,y+12,getWidth()-inset-5,MUTED);
        }
    }
}
