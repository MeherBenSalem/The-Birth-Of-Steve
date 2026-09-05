package com.nightbeam.tbos.memory;

import com.nightbeam.tbos.run.*;
import com.nightbeam.tbos.compat.VanillaCompat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.*;

/** Logical projectiles, not entities. All authority stays on the server tick thread. */
public final class MemoryCombat {
    private static final Map<UUID,Runtime> RUNS=new HashMap<>();
    private static boolean resolving;
    private MemoryCombat() {}
    private record Action(long tick,int room,long floor,long root,int depth,int artifacts,int enhanced,Vec3 origin,Vec3 direction,float damage) {}
    private record Hit(UUID owner,int room,long floor,long root,int depth,int artifacts,int enhanced,
                       Vec3 point,UUID target,float damage,long due,boolean aftermath) {}
    private record Mark(Hit hit,LivingEntity target,long expires) {}
    private static final class Shot {
        final UUID owner; final int room; final long floor,root; final int artifacts,enhanced,depth;
        final Set<UUID> hit=new HashSet<>();
        Vec3 point,direction; float damage; int life=24; boolean returning, routed;
        Shot(UUID owner,int room,long floor,long root,int depth,int artifacts,int enhanced,Vec3 point,Vec3 direction,float damage) {
            this.owner=owner;this.room=room;this.floor=floor;this.root=root;this.depth=depth;
            this.artifacts=artifacts;this.enhanced=enhanced;this.point=point;this.direction=direction.normalize();this.damage=damage;
        }
        boolean has(MemoryArtifact a) { return (artifacts & (1<<a.ordinal()))!=0; }
    }
    private static final class Well {
        final Vec3 point; final int room; final long floor,until; final List<Action> stored=new ArrayList<>();
        Well(Vec3 point,int room,long floor,long until) {this.point=point;this.room=room;this.floor=floor;this.until=until;}
    }
    private static final class Runtime {
        final MemoryBudget budget=new MemoryBudget();
        final List<Shot> shots=new ArrayList<>();
        final List<Hit> delayed=new ArrayList<>();
        final Map<UUID,ArrayDeque<Action>> history=new HashMap<>();
        final Map<UUID,Long> guard=new HashMap<>();
        final Map<UUID,Mark> marks=new LinkedHashMap<>();
        final Map<UUID,Well> wells=new HashMap<>();
        long sequence,queryTick=Long.MIN_VALUE,queryFloor=-1;
        final Map<Integer,List<LivingEntity>> queryCache=new HashMap<>();
    }
    private static Runtime runtime(ArchiveRun run) {return RUNS.computeIfAbsent(run.runId(),k->new Runtime());}
    public static void clear() {RUNS.clear();}
    public static void forget(UUID player) {
        for(Runtime r:RUNS.values()) {
            r.history.remove(player);r.guard.remove(player);r.wells.remove(player);
            r.marks.values().removeIf(m->m.hit.owner.equals(player));
            r.shots.removeIf(s->s.owner.equals(player));r.delayed.removeIf(h->h.owner.equals(player));
        }
    }
    public static boolean eligible(LivingEntity e,ArchiveRun run,int room) {
        return e.isAlive() && !(e instanceof net.minecraft.world.entity.player.Player)
            && VanillaCompat.memoryTags(e).contains("tbos.run."+run.runId()) && VanillaCompat.memoryTags(e).contains("tbos.room."+room);
    }
    private static List<LivingEntity> targets(ServerLevel level,ArchiveRun run,int room) {
        return level.getEntitiesOfClass(LivingEntity.class,ArchiveRoomPlacer.roomAabb(run,room),e->eligible(e,run,room))
            .stream().sorted(Comparator.comparing(e->e.getUUID().toString())).toList();
    }
    private static List<LivingEntity> query(Runtime r,ServerLevel level,ArchiveRun run,int room,long now) {
        if(r.queryTick!=now||r.queryFloor!=run.floor()){r.queryCache.clear();r.queryTick=now;r.queryFloor=run.floor();}
        return r.queryCache.computeIfAbsent(room,k->targets(level,run,k));
    }
    public static void retainRuns(Set<UUID> runs){RUNS.keySet().retainAll(runs);}
    private static boolean visible(ServerLevel level,ServerPlayer player,Vec3 a,Vec3 b) {
        return level.clip(new ClipContext(a,b,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getType()==HitResult.Type.MISS;
    }
    public static boolean cast(ServerPlayer p,ArchiveRun run,MemoryBuild b,int ability,long now) {
        int room=ArchiveRoomPlacer.roomContaining(run,p.blockPosition()).orElse(-1);
        if(room<0) return false;
        Runtime r=runtime(run); ServerLevel level=(ServerLevel)p.level();
        Vec3 origin=p.getEyePosition(),direction=p.getLookAngle();
        switch(MemoryAbility.values()[ability]) {
            case ECHO_LANCE -> launch(r,p,run,b,room,origin,direction,6,0,true,now);
            case RECALL -> {
                ArrayDeque<Action> records=r.history.computeIfAbsent(p.getUUID(),k->new ArrayDeque<>());
                records.removeIf(a->now-a.tick>80||a.room!=room||a.floor!=run.floor());
                if(records.isEmpty()) return false;
                MemoryService.line(run,p,origin,origin.add(direction),4);
                for(int n=0;n<3 && !records.isEmpty();n++) {
                    Action a=records.removeLast();
                    emit(r,p,run,room,origin,a.direction,a.damage*0.6F,a.depth+1,a.root,a.artifacts,a.enhanced,true);
                }
            }
            case PARALLAX_STEP -> {
                Vec3 delta=new Vec3(direction.x,0,direction.z).normalize().scale(0.25);
                Vec3 start=p.position(),end=start;
                for(int n=0;n<20;n++) {
                    Vec3 next=end.add(delta);
                    if(!ArchiveRoomPlacer.roomAabb(run,room).contains(next)
                        || !level.noCollision(p,p.getBoundingBox().move(next.subtract(start)))) break;
                    end=next;
                }
                if(end.distanceToSqr(start)<0.25) return false;
                p.teleportTo(end.x,end.y,end.z);
                pulse(r,p,run,b,room,start.add(0,0.8,0),3,5,now+12);
                MemoryService.line(run,p,start.add(0,1,0),end.add(0,1,0),4);
            }
            case RESONANT_GUARD -> {r.guard.put(p.getUUID(),now+16);MemoryService.line(run,p,origin,origin.add(direction),9);}
            case RECONSTRUCT -> {
                if(!MemorySockets.reconstruct(p,run,room,now)) return false;
                if(b.has(MemoryArtifact.MASONS_REMNANT)) pulse(r,p,run,b,room,p.position().add(0,1,0),5,5+b.rank(MemoryArtifact.MASONS_REMNANT),now);
            }
            case MEMORY_WELL -> {
                if(r.wells.containsKey(p.getUUID())) return false;
                Vec3 end=origin.add(direction.scale(4));
                Vec3 point=level.clip(new ClipContext(origin,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getLocation();
                r.wells.put(p.getUUID(),new Well(point.subtract(direction.scale(.15)),room,run.floor(),now+60));
            }
        }
        return true;
    }
    private static void launch(Runtime r,ServerPlayer p,ArchiveRun run,MemoryBuild b,int room,Vec3 origin,Vec3 direction,float damage,int depth,boolean record,long now) {
        long root=++r.sequence;
        emit(r,p,run,room,origin,direction,damage,depth,root,b.artifacts,b.enhanced,true);
        if(record) remember(r,p,new Action(now,room,run.floor(),root,depth,b.artifacts,b.enhanced,origin,direction,damage));
    }
    private static void emit(Runtime r,ServerPlayer p,ArchiveRun run,int room,Vec3 origin,Vec3 direction,float damage,int depth,long root,int artifacts,int enhanced,boolean split) {
        if(r.shots.size()>=256||depth>MemoryBudget.MAX_DEPTH)return;
        r.shots.add(new Shot(p.getUUID(),room,run.floor(),root,depth,artifacts,enhanced,origin,direction,damage));
        if(split && depth<MemoryBudget.MAX_DEPTH && (artifacts&1)!=0)for(int sign:new int[]{-1,1}) {
            if(r.shots.size()>=256)break;
            double angle=sign*0.19;
            Vec3 fan=new Vec3(direction.x*Math.cos(angle)-direction.z*Math.sin(angle),direction.y,direction.z*Math.cos(angle)+direction.x*Math.sin(angle));
            r.shots.add(new Shot(p.getUUID(),room,run.floor(),root,depth+1,artifacts,enhanced,origin,fan,damage*((enhanced&1)!=0?.75F:.55F)));
        }
    }
    private static void remember(Runtime r,ServerPlayer p,Action action) {
        ArrayDeque<Action> q=r.history.computeIfAbsent(p.getUUID(),k->new ArrayDeque<>());
        q.addLast(action);while(q.size()>MemoryBudget.MAX_RECORDS)q.removeFirst();
    }
    public static void successfulAttack(ServerPlayer p,LivingEntity target,float amount) {
        if(resolving || amount<=0 || target instanceof net.minecraft.world.entity.player.Player) return;
        ArchiveRun run=MemoryService.active(p); if(run==null) return;
        int room=ArchiveRoomPlacer.roomContaining(run,p.blockPosition()).orElse(-1);
        if(room<0 || !VanillaCompat.memoryTags(target).contains("tbos.run."+run.runId()) || !VanillaCompat.memoryTags(target).contains("tbos.room."+room)) return;
        MemoryBuild b=MemoryService.build(p,run); Runtime r=runtime(run);
        long now=p.level().getGameTime();
        long root=++r.sequence;
        remember(r,p,new Action(now,room,run.floor(),root,0,b.artifacts,b.enhanced,p.getEyePosition(),target.getEyePosition().subtract(p.getEyePosition()).normalize(),Math.min(20,amount)));
        aftermath(r,p,(ServerLevel)p.level(),run,new Hit(p.getUUID(),room,run.floor(),root,0,b.artifacts,b.enhanced,target.getEyePosition(),target.getUUID(),amount,now,true),target,now);
    }
    public static float incoming(ServerPlayer p,net.minecraft.world.damagesource.DamageSource source,float amount) {
        ArchiveRun run=MemoryService.active(p);if(run==null)return amount;
        MemoryBuild b=MemoryService.build(p,run);Runtime r=runtime(run);long now=p.level().getGameTime();
        Vec3 sourcePos=source.getSourcePosition();
        if(r.guard.getOrDefault(p.getUUID(),0L)>now && sourcePos!=null
                && sourcePos.subtract(p.getEyePosition()).normalize().dot(p.getLookAngle())>0.25) {
            r.guard.remove(p.getUUID());
            int room=ArchiveRoomPlacer.roomContaining(run,p.blockPosition()).orElse(-1);
            if(room>=0) {
                for(LivingEntity enemy:query(r,(ServerLevel)p.level(),run,room,now)) {
                    Vec3 toward=enemy.getEyePosition().subtract(p.getEyePosition());
                    if(toward.lengthSqr()<=25&&toward.normalize().dot(p.getLookAngle())>.4&&r.delayed.size()<256)
                        r.delayed.add(new Hit(p.getUUID(),room,run.floor(),++r.sequence,1,b.artifacts,b.enhanced,p.getEyePosition(),enemy.getUUID(),4,now,true));
                }
                if(b.has(MemoryArtifact.WARD_FRAGMENT))launch(r,p,run,b,room,p.getEyePosition(),p.getLookAngle(),4+2*b.rank(MemoryArtifact.WARD_FRAGMENT),1,false,now);
            }
            MemoryService.line(run,p,p.getEyePosition(),p.getEyePosition().add(p.getLookAngle().scale(2)),9);
            return 0;
        }
        return amount*(b.debt==3?1.2F:1);
    }
    private static void pulse(Runtime r,ServerPlayer p,ArchiveRun run,MemoryBuild b,int room,Vec3 point,double radius,float damage,long due) {
        for(LivingEntity e:query(r,(ServerLevel)p.level(),run,room,p.level().getGameTime())) if(e.position().distanceToSqr(point)<=radius*radius && r.delayed.size()<256)
            r.delayed.add(new Hit(p.getUUID(),room,run.floor(),++r.sequence,1,b.artifacts,b.enhanced,point,e.getUUID(),damage,due,true));
    }
    public static void tick(MinecraftServer server,ArchiveRun run,long now) {
        Runtime r=RUNS.get(run.runId());if(r==null)return;
        if(run.status()!=ArchiveRunStatus.ACTIVE){RUNS.remove(run.runId());return;}
        ServerLevel level=server.getLevel(ArchiveDimensions.FRACTURED_ARCHIVE);if(level==null)return;
        Map<Integer,List<LivingEntity>> cache=new HashMap<>();
        // Resolve each marked death once, including a later fire death. Marks never survive reload.
        for(var entry:new ArrayList<>(r.marks.entrySet())) {
            Mark mark=entry.getValue(); Hit h=mark.hit; LivingEntity dead=mark.target;
            if(h.floor!=run.floor()||now>mark.expires){r.marks.remove(entry.getKey());continue;}
            if(dead.isAlive())continue;
            r.marks.remove(entry.getKey());
            ServerPlayer owner=server.getPlayerList().getPlayer(h.owner);
            if(owner==null||owner.level()!=level||MemoryService.active(owner)==null
                ||!ArchiveRoomPlacer.isInsideRoom(run,h.room,owner.blockPosition())||h.depth>=3)continue;
            MemoryService.line(run,owner,dead.position().add(0,.5,0),dead.getEyePosition(),8);
            int remaining=4;
            for(LivingEntity other:query(r,level,run,h.room,now)) {
                if(other.distanceToSqr(dead)>25)continue;
                if(r.delayed.size()<256)r.delayed.add(new Hit(h.owner,h.room,h.floor,h.root,h.depth+1,h.artifacts,h.enhanced,dead.getEyePosition(),other.getUUID(),h.damage*((h.enhanced&(1<<4))!=0?.7F:.5F),now,false));
                if(--remaining==0)break;
            }
        }
        List<Shot> shots=new ArrayList<>(r.shots);r.shots.clear();
        for(Shot s:shots) {
            ServerPlayer p=server.getPlayerList().getPlayer(s.owner);
            if(p==null || p.level()!=level || s.floor!=run.floor() || MemoryService.active(p)==null
                    || !ArchiveRoomPlacer.isInsideRoom(run,s.room,p.blockPosition())) continue;
            List<LivingEntity> enemies=cache.computeIfAbsent(s.room,k->query(r,level,run,k,now));
            if(s.has(MemoryArtifact.SEEKING_GLASS)) {
                LivingEntity nearest=enemies.stream().filter(e->e.isAlive()&&!s.hit.contains(e.getUUID()))
                    .min(Comparator.<LivingEntity>comparingInt(e->e.isOnFire()?0:1).thenComparingDouble(e->e.getEyePosition().distanceToSqr(s.point))).orElse(null);
                if(nearest!=null) {double steer=(s.enhanced&(1<<1))!=0?.35:.2;s.direction=s.direction.scale(1-steer).add(nearest.getEyePosition().subtract(s.point).normalize().scale(steer)).normalize();}
            }
            Vec3 end=s.point.add(s.direction.scale(1.1));
            HitResult block=level.clip(new ClipContext(s.point,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
            end=block.getLocation();
            final Vec3 segmentEnd=end;
            List<LivingEntity> struck=enemies.stream().filter(e->e.isAlive()&&!s.hit.contains(e.getUUID()))
                .filter(e->e.getBoundingBox().inflate(0.15).contains(s.point)||e.getBoundingBox().inflate(0.15).clip(s.point,segmentEnd).isPresent())
                .sorted(Comparator.comparingDouble(e->e.position().distanceToSqr(s.point))).toList();
            boolean stopped=false,captured=false,routedThisTick=false;
            for(LivingEntity e:struck) {
                s.hit.add(e.getUUID());
                impact(r,p,level,run,new Hit(s.owner,s.room,s.floor,s.root,s.depth,s.artifacts,s.enhanced,s.point,e.getUUID(),s.damage,now,true),e,now);
                if(s.hit.size()>((s.has(MemoryArtifact.PIERCING_INDEX))?((s.enhanced&(1<<5))!=0?2:1):0)){stopped=true;break;}
            }
            MemoryService.line(run,p,s.point,end,s.has(MemoryArtifact.EMBER_SCRIPT)?11:0);s.point=end;
            if(!s.routed && s.has(MemoryArtifact.RESONANT_NAIL)) {
                Vec3 routed=MemorySockets.route(p,run,s.room,s.point,s.direction,now);
                if(routed!=null){s.point=routed;s.damage*=((s.enhanced&(1<<8))!=0?1.25F:1F);s.routed=true;routedThisTick=true;stopped=false;}
            }
            Well well=r.wells.get(s.owner);
            if(well!=null && well.floor==s.floor && well.room==s.room && well.point.distanceToSqr(s.point)<2.25
                    && well.stored.size()<4+2*MemoryService.build(p,run).rank(MemoryArtifact.MEMORY_WICK)) {
                well.stored.add(new Action(now,s.room,s.floor,s.root,s.depth,s.artifacts,s.enhanced,s.point,s.direction,s.damage));stopped=true;captured=true;
            }
            if(block.getType()!=HitResult.Type.MISS&&!routedThisTick)stopped=true;
            if(--s.life<=0 || stopped) {
                if(!captured && !s.returning && s.has(MemoryArtifact.RETURNING_THREAD) && block.getType()==HitResult.Type.MISS) {
                    s.returning=true;s.damage*=((s.enhanced&(1<<6))!=0?1.25F:1F);s.life=16;s.direction=p.getEyePosition().subtract(s.point).normalize();s.hit.clear();
                    if(r.shots.size()<256)r.shots.add(s);
                }
            } else if(r.shots.size()<256)r.shots.add(s);
        }
        List<Hit> due=new ArrayList<>(r.delayed);r.delayed.clear();
        for(Hit h:due) {
            if(h.floor!=run.floor())continue;
            if(h.due>now){r.delayed.add(h);continue;}
            ServerPlayer p=server.getPlayerList().getPlayer(h.owner);
            if(p==null || p.level()!=level || !ArchiveRoomPlacer.isInsideRoom(run,h.room,p.blockPosition()))continue;
            LivingEntity target=cache.computeIfAbsent(h.room,k->query(r,level,run,k,now)).stream().filter(e->e.getUUID().equals(h.target)).findFirst().orElse(null);
            if(target!=null && target.getEyePosition().distanceToSqr(h.point)<144)impact(r,p,level,run,h,target,now);
        }
        for(var entry:new ArrayList<>(r.wells.entrySet())) {
            Well w=entry.getValue();ServerPlayer p=server.getPlayerList().getPlayer(entry.getKey());
            if(p==null || p.level()!=level || w.floor!=run.floor()){r.wells.remove(entry.getKey());continue;}
            if(now%4==0)MemoryService.line(run,p,w.point,w.point.add(0,1,0),6);
            if(now>=w.until) {
                r.wells.remove(entry.getKey());
                MemoryService.line(run,p,w.point,w.point.add(p.getLookAngle()),7);
                if(ArchiveRoomPlacer.isInsideRoom(run,w.room,p.blockPosition())) for(Action a:w.stored)
                    emit(r,p,run,w.room,w.point,a.direction,a.damage*(MemoryService.build(p,run).rank(MemoryArtifact.MEMORY_WICK)==2?1.5F:1.2F),a.depth+1,a.root,a.artifacts,a.enhanced,false);
            }
        }
    }
    private static void impact(Runtime r,ServerPlayer p,ServerLevel level,ArchiveRun run,Hit h,LivingEntity target,long now) {
        if(!eligible(target,run,h.room)||!r.budget.take(now,h.owner,h.depth)||!visible(level,p,h.point,target.getEyePosition()))return;
        boolean previousResolving=resolving;resolving=true;boolean hit;
        try {hit=VanillaCompat.memoryDamage(level,target,p,h.damage);} finally{resolving=previousResolving;}
        if(hit) {MemoryService.line(run,p,h.point,target.getEyePosition(),h.depth>0?5:2);aftermath(r,p,level,run,h,target,now);}
    }
    private static boolean has(Hit h,MemoryArtifact a){return (h.artifacts&(1<<a.ordinal()))!=0;}
    private static void aftermath(Runtime r,ServerPlayer p,ServerLevel level,ArchiveRun run,Hit h,LivingEntity target,long now) {
        if(has(h,MemoryArtifact.EMBER_SCRIPT)) VanillaCompat.memoryBurn(target,(h.enhanced & (1<<2))!=0?5:3);
        if(has(h,MemoryArtifact.SHATTER_SEAL)&&(r.marks.containsKey(target.getUUID())||r.marks.size()<256))
            r.marks.put(target.getUUID(),new Mark(h,target,now+120));
        if(h.depth>=3)return;
        if(h.aftermath&&has(h,MemoryArtifact.DELAYED_INK)&&target.isAlive()&&r.delayed.size()<256) {
            MemoryService.line(run,p,target.position(),target.getEyePosition(),15);
            r.delayed.add(new Hit(h.owner,h.room,h.floor,h.root,h.depth+1,h.artifacts,h.enhanced,target.getEyePosition(),target.getUUID(),h.damage*((h.enhanced&(1<<7))!=0?0.6F:0.4F),now+12,false));
        }
        if(h.aftermath&&has(h,MemoryArtifact.STORM_FILAMENT)) {
            int limit=(h.enhanced&(1<<3))!=0?2:1;
            for(LivingEntity other:query(r,level,run,h.room,now)) {
                if(other==target||other.distanceToSqr(target)>25)continue;
                if(r.delayed.size()<256)r.delayed.add(new Hit(h.owner,h.room,h.floor,h.root,h.depth+1,h.artifacts,h.enhanced,target.getEyePosition(),other.getUUID(),h.damage*0.5F,now+1,false));
                if(--limit==0)break;
            }
        }
    }
}
