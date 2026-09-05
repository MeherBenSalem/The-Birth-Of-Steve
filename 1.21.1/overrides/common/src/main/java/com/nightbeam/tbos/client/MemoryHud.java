package com.nightbeam.tbos.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Compact, textured ability strip that leaves the crosshair and vanilla status bars clear. */
public final class MemoryHud {
    private MemoryHud() {}
    public static void render(GuiGraphics g) {
        var b=MemoryClient.build;var mc=Minecraft.getInstance();
        if(b==null||mc.player==null||ClientCompat.isHudHidden(mc))return;
        int x=g.guiWidth()/2-71,y=g.guiHeight()-79;
        for(int i=0;i<3;i++) {
            int id=b.slots.get(i),left=x+i*48;
            long remaining=id<0?0:Math.max(0,b.ready.get(id)-MemoryClient.now());
            MemoryUi.frame(g,left,y,44,29,0xE6101B25,id>=0&&remaining==0?MemoryUi.EDGE:0xFF253B45);
            if(id>=0)MemoryIcons.draw(g,id,left+2,y+2,23);
            else MemoryUi.text(g,Component.literal("—"),left+9,y+10,17,MemoryUi.MUTED);
            MemoryUi.text(g,ModKeyMappings.MEMORY_SLOTS[i].getTranslatedKeyMessage(),left+28,y+5,14,MemoryUi.TEXT);
            if(remaining>0) {
                String seconds=remaining<200?String.format(java.util.Locale.ROOT,"%.1f",remaining/20.0):Long.toString((remaining+19)/20);
                MemoryUi.text(g,Component.literal(seconds),left+25,y+16,18,MemoryUi.MUTED);
                int bar=(int)Math.min(40,40*remaining/com.nightbeam.tbos.memory.MemoryAbility.values()[id].cooldown);
                g.fill(left+2,y+26,left+2+bar,y+27,MemoryUi.CYAN);
            }
        }
        if(!b.offers.isEmpty()) {
            Component hint=Component.translatable("memory.tbos.pending",ModKeyMappings.MEMORY_LOADOUT.getTranslatedKeyMessage());
            int available=Math.min(g.guiWidth()-16,240),textWidth=Math.min(available,mc.font.width(hint));
            MemoryUi.text(g,hint,(g.guiWidth()-textWidth)/2,y-13,available,MemoryUi.GOLD);
        }
        if(MemoryClient.discovery>=0&&System.nanoTime()<MemoryClient.discoveryUntil) {
            int w=Math.min(246,g.guiWidth()-16),cx=(g.guiWidth()-w)/2,cy=y-66;
            MemoryUi.frame(g,cx,cy,w,43,0xF0101B25,MemoryUi.GOLD);
            MemoryIcons.draw(g,MemoryClient.discovery,cx+5,cy+5,32);
            MemoryUi.text(g,MemoryClient.name(MemoryClient.discovery),cx+44,cy+9,w-51,MemoryUi.GOLD);
            MemoryUi.text(g,Component.translatable(MemoryScreen.key(MemoryClient.discovery)+".flavor"),cx+44,cy+24,w-51,MemoryUi.MUTED);
        }
    }
}
