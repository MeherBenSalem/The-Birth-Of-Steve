package com.nightbeam.tbos.client;

import com.nightbeam.tbos.memory.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Objects;

/** Abilities and statistics chapter of the Archivist's Journal; K is a direct shortcut. */
public final class MemoryScreen extends Screen {
    private final ArchivistQuestScreen journal;
    private int selectedSlot, page, artifact=-1, debt=1;
    private int stateHash=Integer.MIN_VALUE;
    public MemoryScreen() { this(null); }
    public MemoryScreen(ArchivistQuestScreen journal) {
        super(Component.translatable("memory.tbos.chapter"));
        this.journal=journal;
        if(MemoryClient.build==null)page=2;
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override protected void init() { stateHash=Integer.MIN_VALUE;refresh(); }
    @Override public void onClose() {
        if(journal!=null)ClientCompat.setScreen(Minecraft.getInstance(),journal);
        else super.onClose();
    }
    private int panelWidth() { return Math.min(380,width-12); }
    private int panelHeight() { return Math.min(244,height-12); }
    private int left() { return (width-panelWidth())/2; }
    private int top() { return (height-panelHeight())/2; }
    private void page(int value) { page=value;stateHash=Integer.MIN_VALUE;refresh(); }
    private Component tr(String key,Object... args) { return Component.translatable(key,args); }
    private void action(int x,int y,int w,int h,Component label,Component subtitle,int icon,boolean selected,boolean enabled,Runnable run,Component hint) {
        addRenderableWidget(new MemoryUi.Action(x,y,w,h,label,subtitle,icon,selected,enabled,run,hint));
    }
    private void journalTab(boolean story) {
        if(journal!=null)journal.openChapter(story);
        else ArchivistQuestScreen.requestChapter(story);
    }
    private Component abilityHint(int id) {
        var b=MemoryClient.build;
        double ticks=Math.round(MemoryAbility.values()[id].cooldown*(b!=null&&b.debt==1?1.25:1));
        return MemoryClient.name(id).copy().append("\n").append(tr(key(id)+".description")).append("\n")
            .append(tr("memory.tbos.cooldown_hint",String.format(java.util.Locale.ROOT,"%.1f",ticks/20.0)));
    }
    public void refresh() {
        MemoryBuild b=MemoryClient.build;
        int hash=Objects.hash(b==null?0:b.slots,b==null?0:b.offers,b==null?0:b.draft,b==null?0:b.abilities,
            b==null?0:b.artifacts,b==null?0:b.enhanced,b!=null&&b.overwritten,b!=null&&b.continueVote,
            MemoryClient.climax,MemoryClient.station,MemoryClient.safe,selectedSlot,page,artifact,debt,width,height);
        if(hash==stateHash&&!children().isEmpty())return;
        stateHash=hash;clearWidgets();
        int x=left()+12,y=top(),w=panelWidth()-24,third=(w-6)/3;
        action(x,y+24,third,19,tr("journal.tbos.quests.tab.story"),null,-1,false,true,()->journalTab(true),null);
        action(x+third+3,y+24,third,19,tr("journal.tbos.quests.tab.run"),null,-1,false,true,()->journalTab(false),null);
        action(x+(third+3)*2,y+24,w-2*(third+3),19,tr("memory.tbos.chapter"),null,-1,true,true,()->{},null);
        int tabWidth=(w-9)/4;
        for(int i=0;i<4;i++) {
            int value=i;
            Component label=tr("memory.tbos.page."+i);
            if(i==3&&b!=null&&!b.offers.isEmpty())label=label.copy().append(" •");
            action(x+i*(tabWidth+3),y+49,tabWidth,18,label,null,-1,page==i||(page==4&&i==1),true,()->page(value),null);
        }
        int body=y+76, row=Math.max(18,Math.min(28,(panelHeight()-146)/2));
        if(b!=null&&page==0) {
            for(int i=0;i<3;i++) {
                int slot=i,id=b.slots.get(i);
                Component label=id<0?tr("memory.tbos.slot.empty"):MemoryClient.name(id);
                action(x+i*(third+3),body,third,28,label,null,id,selectedSlot==i,true,()->{selectedSlot=slot;refresh();},id<0?tr("memory.tbos.slot.select",i+1):abilityHint(id));
            }
            for(int id=0;id<6;id++) {
                int ability=id;
                boolean owned=b.owns(id);
                Component label=owned?MemoryClient.name(id):tr("memory.tbos.undiscovered");
                action(x+(id%3)*(third+3),body+43+(id/3)*(row+3),third,row,label,null,owned?id:-1,b.slots.get(selectedSlot)==id,owned&&MemoryClient.safe,
                    ()->MemoryClient.request(1,selectedSlot,ability),owned?abilityHint(id):tr("memory.tbos.discovery_hint"));
            }
        } else if(b!=null&&page==1) {
            int n=0,half=(w-4)/2;
            for(int id=6;id<18;id++)if(b.owns(id)) {
                int choice=id;
                boolean enhanced=(b.enhanced&(1<<(id-6)))!=0;
                Component label=MemoryClient.name(id).copy().append(enhanced?" +":"");
                action(x+(n%2)*(half+4),body+(n/2)*20,half,18,label,null,id,false,true,()->{artifact=choice;page(4);},tr(key(id)+".description"));n++;
            }
        } else if(b!=null&&page==3) {
            for(int i=0;i<b.offers.size();i++) {
                int choice=b.offers.get(i),revision=b.draft;
                action(x,body+i*37,w,34,MemoryClient.name(choice),tr(key(choice)+".description"),choice,false,true,
                    ()->MemoryClient.request(2,revision,choice),MemoryClient.name(choice).copy().append("\n").append(tr(key(choice)+".description")));
            }
        } else if(b!=null&&page==4&&artifact>=6) {
            action(x,body,w,29,MemoryClient.name(artifact),null,artifact,true,true,()->page(1),tr("memory.tbos.pick_artifact"));
            action(x,body+65,w,20,tr("memory.tbos.debt."+debt),null,-1,false,!b.overwritten,()->{debt=debt%3+1;refresh();},tr("memory.tbos.debt.duration"));
            boolean enabled=MemoryClient.safe&&MemoryClient.station&&!b.overwritten&&(b.enhanced&(1<<(artifact-6)))==0;
            action(x,body+91,w,22,tr("memory.tbos.overwrite.accept"),null,-1,false,enabled,()->MemoryClient.request(3,artifact,debt),tr("memory.tbos.trial"));
        }
        action(x+w-72,y+panelHeight()-25,72,18,tr(journal==null?"gui.done":"memory.tbos.back_book"),null,-1,false,true,this::onClose,null);
        if(b!=null&&MemoryClient.climax&&page==2) {
            action(x,y+panelHeight()-25,90,18,tr("memory.tbos.extract"),null,-1,false,true,()->MemoryClient.request(5,0,0),tr("memory.tbos.extract.description"));
            action(x+94,y+panelHeight()-25,w-174,18,tr(b.continueVote?"memory.tbos.voted":"memory.tbos.continue"),null,-1,b.continueVote,!b.continueVote,()->MemoryClient.request(4,0,0),tr("memory.tbos.continue.description"));
        }
    }
    public static String key(int id) { return id<6?MemoryAbility.values()[id].key():MemoryArtifact.values()[id-6].key(); }
    private void line(GuiGraphics g,Component value,int x,int y,int w,int color) { MemoryUi.text(g,value,x,y,w,color); }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        int x=left(),y=top(),w=panelWidth(),h=panelHeight(),body=y+76;
        MemoryUi.frame(g,x,y,w,h,MemoryUi.PANEL,MemoryUi.EDGE);
        g.fill(x+12,y+8,x+15,y+17,MemoryUi.GOLD);
        line(g,tr("journal.tbos.quests.title"),x+22,y+9,w-36,MemoryUi.GOLD);
        var b=MemoryClient.build;
        if(page==2)drawStats(g,x+12,body,w-24,b);
        else if(b==null) {
            MemoryIcons.draw(g,4,x+w/2-20,body+9,40);
            line(g,tr("memory.tbos.outside"),x+18,body+59,w-36,MemoryUi.TEXT);
            line(g,tr("memory.tbos.outside_hint"),x+18,body+75,w-36,MemoryUi.MUTED);
        } else if(page==0) {
            line(g,tr(MemoryClient.safe?"memory.tbos.equip":"memory.tbos.combat_locked",selectedSlot+1),x+12,body+32,w-24,MemoryUi.MUTED);
        } else if(page==1&&b.artifacts==0) {
            MemoryIcons.draw(g,6,x+w/2-20,body+8,40);
            line(g,tr("memory.tbos.artifacts_empty"),x+18,body+58,w-36,MemoryUi.TEXT);
            line(g,tr("memory.tbos.discovery_hint"),x+18,body+74,w-36,MemoryUi.MUTED);
        } else if(page==3&&b.offers.isEmpty()) {
            line(g,tr("memory.tbos.rewards_empty"),x+18,body+30,w-36,MemoryUi.TEXT);
            line(g,tr("memory.tbos.discovery_hint"),x+18,body+48,w-36,MemoryUi.MUTED);
        } else if(page==4&&artifact>=6) {
            line(g,tr(key(artifact)+".upgrade"),x+16,body+35,w-32,MemoryUi.GOLD);
            String reason=b.overwritten?"memory.tbos.overwrite_used":(b.enhanced&(1<<(artifact-6)))!=0?"memory.tbos.already_enhanced":!MemoryClient.station?"memory.tbos.station_required":"memory.tbos.debt.duration";
            line(g,tr(reason),x+16,body+50,w-32,MemoryUi.VIOLET);
        }
        super.render(g,mouseX,mouseY,partialTick);
    }
    private void drawStats(GuiGraphics g,int x,int y,int w,MemoryBuild b) {
        var player=Minecraft.getInstance().player;if(player==null)return;
        String[] values={String.format(java.util.Locale.ROOT,"%.0f / %.0f",player.getHealth(),player.getMaxHealth()),
            Integer.toString(player.getArmorValue()),Integer.toString(player.getFoodData().getFoodLevel()),
            b==null?"—":Long.toString(b.floor+1),b==null?"0 / 6":Integer.bitCount(b.abilities)+" / 6",
            b==null?"0 / 12":Integer.bitCount(b.artifacts)+" / 12",
            b!=null&&b.debt==1?"125%":"100%",b!=null&&b.debt==2?"75%":"100%",b!=null&&b.debt==3?"120%":"100%",b==null?"0":Integer.toString(Integer.bitCount(b.enhanced))};
        int half=(w-5)/2;
        for(int i=0;i<values.length;i++) {
            int cx=x+i%2*(half+5),cy=y+i/2*24;
            MemoryUi.frame(g,cx,cy,half,23,MemoryUi.CARD,MemoryUi.EDGE);
            line(g,tr("memory.tbos.stat."+i),cx+6,cy+3,half-12,MemoryUi.MUTED);
            line(g,Component.literal(values[i]),cx+6,cy+13,half-12,MemoryUi.CYAN);
        }
    }
}
