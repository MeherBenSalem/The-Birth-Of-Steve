package com.nightbeam.tbos.memory;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.nightbeam.tbos.network.payload.MemorySnapshotPayload;
import com.nightbeam.tbos.platform.Services;
import com.nightbeam.tbos.run.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Run lifecycle, server-validated input and personally claimed discoveries. */
public final class MemoryService {
    private static final Map<UUID,Long> REQUESTS=new HashMap<>();
    private static final Map<UUID,JsonArray> EFFECTS=new HashMap<>();
    private MemoryService() {}
    public static ArchiveRun active(ServerPlayer p) {
        if(!p.level().dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)||p.isSpectator()||!p.isAlive())return null;
        ArchiveRunSavedData save=ArchiveRunSavedData.get(p.level().getServer());
        ArchiveRun run=save.findByMember(p.getUUID()).orElse(null);
        return run!=null && run.status()==ArchiveRunStatus.ACTIVE && save.memories(run.runId())!=null?run:null;
    }
    public static MemoryBuild build(ServerPlayer p,ArchiveRun run) {
        return ArchiveRunSavedData.get(p.level().getServer()).memories(run.runId()).member(p.getUUID());
    }
    public static void request(ServerPlayer p,int action,int first,int second) {
        ArchiveRun run=active(p);if(run==null)return;
        long now=p.level().getGameTime();
        if(REQUESTS.getOrDefault(p.getUUID(),Long.MIN_VALUE)==now)return;
        REQUESTS.put(p.getUUID(),now);
        ArchiveRunSavedData save=ArchiveRunSavedData.get(p.level().getServer());
        MemoryBuild b=build(p,run);if(b==null)return;
        int room=ArchiveRoomPlacer.roomContaining(run,p.blockPosition()).orElse(-1);
        if(room<0)return;
        boolean safe=MemorySockets.safe(p,run,room);
        switch(action) {
            case 0 -> {
                if(first<0||first>=3)return;
                int ability=b.slots.get(first);
                if(ability<0||b.ready.get(ability)>now)return;
                if(MemoryCombat.cast(p,run,b,ability,now))
                    b.ready.set(ability,now+Math.round(MemoryAbility.values()[ability].cooldown*(b.debt==1?1.25:1)));
            }
            case 1 -> {
                if(!safe||!b.equip(first,second))return;
            }
            case 2 -> {if(!b.claim(first,second))return;}
            case 3 -> {
                if(!safe||b.trial.until()>now||b.overwritten||first<6||first>=18||!b.owns(first)||second<1||second>3
                    ||(b.enhanced&(1<<(first-6)))!=0||!MemorySockets.atStation(p,run,room))return;
                if(!MemorySockets.startTrial(p,run,room,now))return;
                b.enhanced|=1<<(first-6);b.debt=second;b.overwritten=true;
            }
            case 4 -> {
                if(!save.memories(run.runId()).climax || run.floor()<2 || !safe
                    || p.distanceToSqr(Vec3.atCenterOf(ArchiveRoomPlacer.rewardGatewayPosition(run)))>36)return;
                b.continueVote=true;
            }
            case 5 -> {
                if(!save.memories(run.runId()).climax||!safe
                    || p.distanceToSqr(Vec3.atCenterOf(ArchiveRoomPlacer.rewardGatewayPosition(run)))>36)return;
                save.replace(run.beginReturn(now+40));
            }
            case 6 -> {snapshot(p,run,b,now);return;}
            case 7 -> {
                if(!safe||run.members().size()<2)return;
                if(!ArchiveRunManager.leaveViaWaystone(p))return;
                var current=save.find(run.runId()).orElseThrow();
                save.replace(current.removeMemoryMember(p.getUUID()));
                save.memories(run.runId()).removeMember(p.getUUID());
                MemoryCombat.forget(p.getUUID());save.setDirty();return;
            }
            default -> {return;}
        }
        save.setDirty();snapshot(p,run,b,now);
    }
    public static void tick(MinecraftServer server) {
        ArchiveRunSavedData save=ArchiveRunSavedData.get(server);long now=server.overworld().getGameTime();
        MemoryCombat.retainRuns(save.all().stream().map(ArchiveRun::runId).collect(java.util.stream.Collectors.toSet()));
        for(ArchiveRun run:save.all()) {
            MemoryExpedition expedition=save.memories(run.runId());if(expedition==null)continue;
            MemoryCombat.tick(server,run,now);
            MemorySockets.tick(server,run,now);
            if(run.status()!=ArchiveRunStatus.ACTIVE)continue;
            for(MemoryBuild b:expedition.members) {
                ServerPlayer p=server.getPlayerList().getPlayer(b.player);

                if(b.floor!=run.floor()){b.nextFloor(run.floor());MemoryCombat.forget(b.player);save.setDirty();}
                // Grant personal credit to everyone in the party, including members who reconnect later.
                for(int i=0;i<run.rooms().size();i++) {
                    ArchiveEncounterKind kind=run.rooms().get(i).encounterKind();
                    if(kind==ArchiveEncounterKind.EXPLORATION||kind==ArchiveEncounterKind.REWARD)continue;
                    if(run.roomEncounterStates().get(i).complete()&&!b.clears.contains(i)) {
                        b.clears.add(i);b.earned++;save.setDirty();
                        if(p!=null&&p.level().dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE))line(run,p,p.position(),p.getEyePosition(),kind==ArchiveEncounterKind.BOSS?12:13);
                        if(kind==ArchiveEncounterKind.BOSS){b.debt=0;if(run.floor()==0)b.earned=Math.max(4,b.earned);if(run.floor()==2) {
                            expedition.climax=true;
                            if(p!=null)com.nightbeam.tbos.advancement.ModAdvancements.awardMemoryVictory(p);
                        }}
                    }
                }
                if(p==null || !p.level().dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)) {
                    MemoryCombat.forget(b.player);EFFECTS.remove(b.player);continue;
                }
                if(b.offers.isEmpty() && b.draft<b.earned) {
                    boolean active=b.draft==1||b.draft==3||(b.draft>=5&&b.draft%4==1&&b.abilities!=63);
                    if(b.rareCredits>0) {
                        for(int id:new int[]{10,13,15})if(!b.owns(id))b.offers.add(id);
                        b.rareCredits--;
                    }
                    if(b.offers.isEmpty())b.offer(run.seed(),active);
                    if(b.offers.isEmpty()) {b.draft++;var fallback=new net.minecraft.world.item.ItemStack(com.nightbeam.tbos.registry.ModItems.CHRONICLE_SHARD.get());if(!p.getInventory().add(fallback))p.drop(fallback,false);}
                    save.setDirty();
                }
                if(now%10==0)snapshot(p,run,b,now);
                if(now%2==0)flush(p);
            }
        }
    }
    public static boolean mayAdvance(ArchiveRunSavedData save,ArchiveRun run) {
        MemoryExpedition e=save.memories(run.runId());
        return e==null||run.floor()<2||e.members.stream().allMatch(b->b.continueVote);
    }
    public static void snapshot(ServerPlayer p,ArchiveRun run,MemoryBuild b,long now) {
        JsonObject json=new JsonObject();json.add("build",MemoryBuild.CODEC.encodeStart(JsonOps.INSTANCE,b).result().orElseThrow());
        json.addProperty("run",run.runId().toString());json.addProperty("tick",now);
        int room=ArchiveRoomPlacer.roomContaining(run,p.blockPosition()).orElse(-1);
        json.addProperty("station",room>=0&&MemorySockets.atStation(p,run,room));
        json.addProperty("safe",room>=0&&MemorySockets.safe(p,run,room));
        json.addProperty("climax",ArchiveRunSavedData.get(p.level().getServer()).memories(run.runId()).climax);
        Services.NETWORK.sendToPlayer(p,new MemorySnapshotPayload(json.toString()));
    }
    public static void line(ArchiveRun run,ServerPlayer source,Vec3 from,Vec3 to,int kind) {
        for(ArchiveRunMember member:run.members()) {
            ServerPlayer p=source.level().getServer().getPlayerList().getPlayer(member.playerId());
            if(p==null||p.level()!=source.level()||p.position().distanceToSqr(from)>48*48)continue;
            JsonArray events=EFFECTS.computeIfAbsent(p.getUUID(),id->new JsonArray());
            if(events.size()>=32) {
                if(kind!=3)continue;
                int discard=-1;
                for(int i=0;i<events.size();i++)if(events.get(i).getAsJsonArray().get(6).getAsInt()!=3){discard=i;break;}
                if(discard<0)continue;events.remove(discard);
            }
            JsonArray event=new JsonArray();
            event.add(from.x);event.add(from.y);event.add(from.z);event.add(to.x);event.add(to.y);event.add(to.z);event.add(kind);
            events.add(event);
        }
    }
    private static void flush(ServerPlayer p) {
        JsonArray events=EFFECTS.remove(p.getUUID());if(events==null)return;
        JsonObject json=new JsonObject();json.add("effects",events);
        Services.NETWORK.sendToPlayer(p,new MemorySnapshotPayload(json.toString()));
    }
    public static void clear(){REQUESTS.clear();EFFECTS.clear();MemoryCombat.clear();MemorySockets.clear();}
}
