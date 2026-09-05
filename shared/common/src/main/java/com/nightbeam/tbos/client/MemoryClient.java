package com.nightbeam.tbos.client;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.nightbeam.tbos.memory.*;
import com.nightbeam.tbos.network.payload.*;
import com.nightbeam.tbos.platform.Services;
import com.nightbeam.tbos.run.ArchiveDimensions;
import com.nightbeam.tbos.config.YesterglassClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;

/** Client copies are presentation only. Requests contain intent, never damage or target authority. */
public final class MemoryClient {
    public static MemoryBuild build;
    public static boolean climax,station,safe;
    private static long serverTick,received;
    private static String run="";
    public static int discovery=-1;
    public static long discoveryUntil;
    private MemoryClient(){}
    public static void request(int action,int first,int second) {
        Services.NETWORK.sendToServer(new MemoryActionRequest(action,first,second));
    }
    public static void accept(MemorySnapshotPayload packet) {
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        JsonObject json=JsonParser.parseString(packet.json()).getAsJsonObject();
        if(json.has("effects")) {
            MemoryEffects.accept(json.getAsJsonArray("effects"));
            return;
        }
        MemoryBuild next=MemoryBuild.CODEC.parse(JsonOps.INSTANCE,json.get("build")).result().orElse(null);
        if(next==null)return;
        String nextRun=json.get("run").getAsString();
        if(build!=null&&run.equals(nextRun))for(int i=0;i<18;i++)if(next.owns(i)&&!build.owns(i)) {
            discovery=i;discoveryUntil=System.nanoTime()+2_500_000_000L;
            mc.player.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,0.45F,i>=6?1.15F:0.9F);
        }
        station=json.has("station")&&json.get("station").getAsBoolean();
        safe=json.has("safe")&&json.get("safe").getAsBoolean();
        run=nextRun;build=next;climax=json.get("climax").getAsBoolean();
        serverTick=json.get("tick").getAsLong();received=System.nanoTime();
        if(ClientCompat.currentScreen(mc) instanceof MemoryScreen screen)screen.refresh();
    }
    public static long now(){return serverTick+(System.nanoTime()-received)/50_000_000L;}
    public static Component name(int id){return Component.translatable(id<6?MemoryAbility.values()[id].key():MemoryArtifact.values()[id-6].key());}
    public static void tick(Minecraft mc) {
        if(mc.level==null||mc.player==null||!mc.level.dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)) {build=null;run="";MemoryEffects.clear();return;}
        MemoryEffects.tick(mc);
        while(ModKeyMappings.MEMORY_LOADOUT.consumeClick()) {
            request(6,0,0);
            if(build!=null)ClientCompat.setScreen(mc,new MemoryScreen());
        }
        for(int slot=0;slot<3;slot++)while(ModKeyMappings.MEMORY_SLOTS[slot].consumeClick())
            if(ClientCompat.currentScreen(mc)==null&&build!=null)request(0,slot,0);
        if(build!=null&&System.nanoTime()-received>3_000_000_000L)build=null;
    }
}
