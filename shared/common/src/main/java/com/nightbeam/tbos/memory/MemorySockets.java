package com.nightbeam.tbos.memory;

import com.nightbeam.tbos.run.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;

/** Authored inset sockets: no room shell, puzzle marker or gate can be reconstructed. */
public final class MemorySockets {
    private MemorySockets(){}
    public static void clear(){}
    public static List<BlockPos> positions(ArchiveRun run,int room) {
        ArchiveEncounterKind kind=run.rooms().get(room).encounterKind();
        return kind==ArchiveEncounterKind.HALL||kind==ArchiveEncounterKind.CHOIR?List.of():ArchiveRoomPlacer.memorySocketPositions(run,room);
    }
    public static boolean protectedCover(ServerLevel level,ArchiveRun run,BlockPos pos) {
        var e=ArchiveRunSavedData.get(level.getServer()).memories(run.runId());
        return e!=null&&e.structures.stream().anyMatch(c->c.role()==0&&c.position()==pos.asLong());
    }
    public static boolean reconstruct(ServerPlayer p,ArchiveRun run,int room,long now) {
        ServerLevel level=(ServerLevel)p.level();
        BlockPos chosen=positions(run,room).stream().filter(pos->p.distanceToSqr(Vec3.atCenterOf(pos))<=100)
            .filter(pos->level.clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),Vec3.atCenterOf(pos),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS)
            .filter(pos->Vec3.atCenterOf(pos).subtract(p.getEyePosition()).normalize().dot(p.getLookAngle())>0.65)
            .min(Comparator.comparingDouble(pos->p.distanceToSqr(Vec3.atCenterOf(pos)))).orElse(null);
        if(chosen==null||!level.getBlockState(chosen).isAir()||!level.getBlockState(chosen.above()).isAir()
            ||!level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new AABB(chosen).expandTowards(0,1,0)).isEmpty())return false;
        // A single inset column cannot close a corridor. Replace air only; no drops or block entities.
        level.setBlock(chosen,Blocks.GLASS.defaultBlockState(),3);
        var save=ArchiveRunSavedData.get(level.getServer());
        save.memories(run.runId()).structures.add(new MemoryStructure(run.floor(),room,chosen.asLong(),now+100,0));
        save.setDirty();
        MemoryService.line(run,p,Vec3.atCenterOf(chosen),Vec3.atCenterOf(chosen).add(0,1,0),10);
        return true;
    }
    public static Vec3 route(ServerPlayer p,ArchiveRun run,int room,Vec3 point,Vec3 direction,long now) {
        List<BlockPos> sockets=positions(run,room);
        for(int i=0;i<sockets.size();i++) if(Vec3.atCenterOf(sockets.get(i)).distanceToSqr(point)<2.25) {
            Vec3 next=Vec3.atCenterOf(sockets.get((i+1)%sockets.size())).add(0,1,0);
            var save=ArchiveRunSavedData.get(levelOf(p).getServer());
            var e=save.memories(run.runId());
            if(e.structures.stream().noneMatch(c->c.role()==0&&c.floor()==run.floor()&&c.room()==room))return null;
            MemoryService.line(run,p,point,next,1);
            secret(p,run,room,sockets.get(i),now);
            return next;
        }
        return null;
    }
    public static boolean atStation(ServerPlayer p,ArchiveRun run,int room) {
        return room==run.dungeonGraph().startingRoom()&&p.distanceToSqr(Vec3.atCenterOf(ArchiveRoomPlacer.roomSpawn(run,room)))<36;
    }
    public static boolean safe(ServerPlayer p,ArchiveRun run,int room) {
        ArchiveEncounterKind kind=run.rooms().get(room).encounterKind();
        return (kind==ArchiveEncounterKind.EXPLORATION||kind==ArchiveEncounterKind.REWARD||run.roomEncounterStates().get(room).complete()) && levelOf(p).getEntitiesOfClass(
            net.minecraft.world.entity.LivingEntity.class,ArchiveRoomPlacer.roomAabb(run,room),e->MemoryCombat.eligible(e,run,room)).isEmpty();
    }
    public static boolean startTrial(ServerPlayer p,ArchiveRun run,int room,long now) {
        return spawnTrial(p,run,room,now,1);
    }
    private static boolean spawnTrial(ServerPlayer p,ArchiveRun run,int room,long now,int role) {
        ServerLevel level=levelOf(p);int spawned=0;
        for(BlockPos position:positions(run,room)) {
            if(p.distanceToSqr(Vec3.atCenterOf(position))<9)continue;
            var mob=com.nightbeam.tbos.compat.VanillaCompat.memoryTrialMob(level,spawned%2==1);
            if(mob==null)continue;
            mob.setPos(position.getX()+.5,position.getY(),position.getZ()+.5);
            if(!level.noCollision(mob)||level.getBlockState(position.below()).isAir())continue;
            mob.addTag("tbos.run."+run.runId());mob.addTag("tbos.room."+room);mob.addTag("tbos.memory_trial."+p.getUUID());
            mob.setPersistenceRequired();mob.setTarget(p);
            if(spawned%2==1)mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW));
            if(!level.addFreshEntity(mob))continue;
            MemoryService.line(run,p,mob.position(),mob.getEyePosition(),12);
            if(++spawned==2)break;
        }
        if(spawned==0)return false;
        MemoryService.build(p,run).trial=new MemoryStructure(run.floor(),room,p.blockPosition().asLong(),now+160,role);
        return true;
    }
    private static ServerLevel levelOf(ServerPlayer p){return (ServerLevel)p.level();}
    private static void secret(ServerPlayer p,ArchiveRun run,int room,BlockPos socket,long now) {
        var save=ArchiveRunSavedData.get(p.level().getServer());var e=save.memories(run.runId());
        if(e.structures.stream().noneMatch(c->c.role()==3&&c.floor()==run.floor()&&c.position()==socket.asLong())) {
            e.structures.add(new MemoryStructure(run.floor(),room,socket.asLong(),now,3));save.setDirty();
        }
        long count=e.structures.stream().filter(c->c.role()==3&&c.floor()==run.floor()&&c.room()==room).count();
        if(count==1&&e.structures.stream().noneMatch(c->c.role()==2&&c.floor()==run.floor())) {
            var hidden=run.dungeonGraph().rooms().stream().filter(r->r.category()==ArchiveRoomCategory.SECRET&&!r.runtime().secretDiscovered()).findFirst().orElse(null);
            if(hidden!=null) {
                ArchiveRun revealed=run.discoverSecretRoom(hidden.index());save.replace(revealed);
                ArchiveRoomPlacer.revealSecretConnection(levelOf(p),revealed,hidden.index());
                e.structures.add(new MemoryStructure(run.floor(),room,socket.asLong(),now,2));save.setDirty();
                p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("memory.tbos.secret"));
            }
        }
        if(count==3&&e.structures.stream().noneMatch(c->c.role()==4&&c.floor()==run.floor()&&c.room()==room)) {
            if(MemoryService.build(p,run).trial.until()==0&&spawnTrial(p,run,room,now,4)) {
                e.structures.add(new MemoryStructure(run.floor(),room,socket.asLong(),now,4));save.setDirty();
            }
        }
    }
    public static void tick(MinecraftServer server,ArchiveRun run,long now) {
        ServerLevel level=server.getLevel(ArchiveDimensions.FRACTURED_ARCHIVE);if(level==null)return;
        var save=ArchiveRunSavedData.get(server);
        var expedition=save.memories(run.runId());
        Iterator<MemoryStructure> it=expedition.structures.iterator();
        while(it.hasNext()) {
            MemoryStructure cover=it.next();
            if(cover.role()!=0){if(cover.floor()!=run.floor()){it.remove();save.setDirty();}continue;}
            if(now>=cover.until()||cover.floor()!=run.floor()||run.status()!=ArchiveRunStatus.ACTIVE) {
                BlockPos position=BlockPos.of(cover.position());
                if(level.getBlockState(position).is(Blocks.GLASS))level.setBlock(position,Blocks.AIR.defaultBlockState(),3);
                it.remove();save.setDirty();
            }
        }
        if(run.status()!=ArchiveRunStatus.ACTIVE)return;
        for(ArchiveRunMember member:run.members()) {
            ServerPlayer p=server.getPlayerList().getPlayer(member.playerId());if(p==null||p.level()!=level)continue;
            int room=ArchiveRoomPlacer.roomContaining(run,p.blockPosition()).orElse(-1);if(room<0)continue;
            if(now%20==0&&atStation(p,run,room))MemoryService.line(run,p,Vec3.atCenterOf(ArchiveRoomPlacer.roomSpawn(run,room)),p.getEyePosition(),14);
            if(now%20==0)for(BlockPos socket:positions(run,room))
                MemoryService.line(run,p,Vec3.atCenterOf(socket).add(-0.25,-0.4,0),Vec3.atCenterOf(socket).add(0.25,-0.4,0),1);
            var build=MemoryService.build(p,run);var trial=build.trial;
            if(trial.until()>0&&trial.floor()==run.floor()) {
                if(now>=trial.until()&&room==trial.room()&&level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    ArchiveRoomPlacer.roomAabb(run,trial.room()),mob->mob.isAlive()&&com.nightbeam.tbos.compat.VanillaCompat.memoryTags(mob).contains("tbos.memory_trial."+p.getUUID())).isEmpty()) {
                    if(trial.role()==4){build.earned++;build.rareCredits++;}
                    build.trial=new MemoryStructure(run.floor(),room,0,0,1);save.setDirty();continue;
                }
                if(now>=trial.until()||room!=trial.room())continue;
                int phase=(int)((trial.until()-now)%40);
                Vec3 point=Vec3.atCenterOf(BlockPos.of(trial.position()));
                if(phase>10)ring(run,p,point,2,3);
                if(phase==0&&p.position().distanceToSqr(point)<4)
                    com.nightbeam.tbos.compat.VanillaCompat.memoryDamage(level,p,null,3);
            }
            if(run.floor()==2&&run.rooms().get(room).encounterKind()==ArchiveEncounterKind.BOSS) bossPattern(p,run,room,now);

        }
    }
    public static void ring(ArchiveRun run,ServerPlayer p,Vec3 center,double radius,int kind) {
        for(int n=0;n<8;n++) {
            double a=n*Math.PI/4,b=(n+1)*Math.PI/4;
            MemoryService.line(run,p,center.add(Math.cos(a)*radius,0,Math.sin(a)*radius),center.add(Math.cos(b)*radius,0,Math.sin(b)*radius),kind);
        }
    }
    private static void bossPattern(ServerPlayer p,ArchiveRun run,int room,long now) {
        ServerLevel level=levelOf(p);
        var boss=level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,ArchiveRoomPlacer.roomAabb(run,room),
            mob->MemoryCombat.eligible(mob,run,room)&&com.nightbeam.tbos.compat.VanillaCompat.memoryTags(mob).contains("tbos.final_boss")).stream().findFirst().orElse(null);
        if(boss==null)return;
        int stage=boss.getHealth()>boss.getMaxHealth()*0.66?1:boss.getHealth()>boss.getMaxHealth()*0.33?2:3;
        var sockets=positions(run,room);int phase=(int)(now%100);
        for(int i=0;i<Math.min(stage,sockets.size());i++) {
            Vec3 center=Vec3.atCenterOf(sockets.get((i+(int)(now/100))%sockets.size()));
            boolean safe=protectedCover(level,run,BlockPos.containing(center));
            if(phase<35&&now%4==0)ring(run,p,center,3,safe?1:3);
            if(phase==35&&!safe&&p.position().distanceToSqr(center)<9)
                com.nightbeam.tbos.compat.VanillaCompat.memoryDamage(level,p,null,4);
        }
    }
}
