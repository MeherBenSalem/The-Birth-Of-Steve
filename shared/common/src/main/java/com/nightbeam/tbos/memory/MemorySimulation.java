package com.nightbeam.tbos.memory;

import com.mojang.serialization.JsonOps;
import com.nightbeam.tbos.run.*;
import java.util.*;

/** Executable invariants without a client or loader; wired into every common check task. */
public final class MemorySimulation {
    private MemorySimulation(){}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args) {
        for(int seed=0;seed<1000;seed++) {
            UUID owner=new UUID(1,seed);
            MemoryBuild b=new MemoryBuild(owner),same=new MemoryBuild(owner);
            b.offer(seed,false);same.offer(seed,false);
            check(b.offers.equals(same.offers)&&b.offers.size()==3,"deterministic distinct draft");
            int choice=b.offers.get(0),draft=b.draft;
            check(!b.claim(draft+1,choice),"reject stale/future revision");
            check(b.claim(draft,choice)&&!b.claim(draft,choice),"claim exactly once");
            b.acquire(1);b.acquire(2);b.ready.set(1,900L);b.earned=4;
            check(!b.equip(-1,1)&&!b.equip(3,1)&&!b.equip(1,6)&&!b.equip(1,5),"invalid equip rejected");
            check(b.equip(0,1)&&b.slots.equals(List.of(1,0,2))&&b.ready.get(1)==900L,"swap preserves cooldown and distinct slots");
            b.nextFloor(1);
            check(b.owns(choice)&&b.ready.get(1)==900L&&b.earned==4,"floor retains build and cooldown");
            var encoded=MemoryBuild.CODEC.encodeStart(JsonOps.INSTANCE,b).result().orElseThrow();
            MemoryBuild decoded=MemoryBuild.CODEC.parse(JsonOps.INSTANCE,encoded).result().orElseThrow();
            check(decoded.slots.equals(b.slots)&&decoded.ready.equals(b.ready)&&decoded.artifacts==b.artifacts,"save roundtrip");
            b.offer(seed,false);check(b.offers.stream().noneMatch(b::owns),"no owned artifacts offered");
            var pending=MemoryBuild.CODEC.parse(JsonOps.INSTANCE,MemoryBuild.CODEC.encodeStart(JsonOps.INSTANCE,b).result().orElseThrow()).result().orElseThrow();
            check(pending.offers.equals(b.offers)&&pending.draft==b.draft,"pending choices survive reconnect");
            MemoryBuild teaching=new MemoryBuild(owner);
            for(int reward=0;reward<4;reward++) {
                teaching.offer(seed,reward==1||reward==3);
                check(teaching.claim(teaching.draft,teaching.offers.get(0)),"floor-one progression claim");
            }
            check(teaching.slots.stream().allMatch(id->id>=0),"floor one guarantees three active slots");
            for(int id=6;id<18;id++)teaching.acquire(id);
            teaching.offer(seed,false);check(teaching.offers.isEmpty(),"exhausted passives never stack");
            var generated=ArchiveRunGenerator.generateDetailed(seed,0L,MemoryRules.expedition(ArchiveDungeonSettings.DEFAULT));
            check(generated.metrics().unreachableRoomCount()==0&&generated.metrics().overlapCount()==0,"reachable non-overlapping expedition");
            check(generated.graph().rooms().size()>=9&&generated.graph().rooms().size()<=12,"expedition size");
            for(var room:generated.graph().rooms()) {
                var template=ArchiveRoomTemplates.require(room.templateId());
                for(var pos:template.memorySocketMarkers())check(pos.getX()>1&&pos.getZ()>1&&pos.getX()<template.size().width()-2&&pos.getZ()<template.size().depth()-2,"socket inset");
            }
        }
        MemoryBudget budget=new MemoryBudget();
        for(int player=0;player<4;player++)for(int n=0;n<64;n++)check(budget.take(1,new UUID(0,player),3),"budget accepts limit");
        check(!budget.take(1,new UUID(0,5),1),"run overflow rejected");
        check(budget.take(2,new UUID(0,0),0)&&!budget.take(2,new UUID(0,0),4),"new tick and depth guard");
        System.out.println("Living Memories: 1000 expedition seeds, draft/claim/codec/floor invariants and budget limits passed.");
    }
}
