package com.nightbeam.tbos.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nightbeam.tbos.run.ArchiveRun;
import java.util.*;

/** Optional sidecar inside ArchiveRunSavedData: absent for pre-update runs. */
public final class MemoryExpedition {
    public static final Codec<MemoryExpedition> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("run").forGetter(e -> e.run.toString()),
        Codec.INT.fieldOf("rules").forGetter(e -> e.rules),
        Codec.BOOL.fieldOf("climax").forGetter(e -> e.climax),
        MemoryBuild.CODEC.listOf().fieldOf("members").forGetter(e -> e.members),
        MemoryStructure.CODEC.listOf().optionalFieldOf("structures",List.of()).forGetter(e -> e.structures)
    ).apply(i, MemoryExpedition::new));
    public final UUID run;
    public final int rules;
    public boolean climax;
    public final List<MemoryBuild> members;
    public final List<MemoryStructure> structures;
    private MemoryExpedition(String run,int rules,boolean climax,List<MemoryBuild> members,List<MemoryStructure> structures) {
        this.run=UUID.fromString(run);
        if(structures.size()>256 || rules!=1 || members.isEmpty() || members.size()>4 || members.stream().map(b->b.player).distinct().count()!=members.size())
            throw new IllegalArgumentException("Invalid memory expedition");
        this.structures=new ArrayList<>(structures);
        this.rules=rules; this.climax=climax; this.members=new ArrayList<>(members);
    }
    public MemoryExpedition(ArchiveRun run) {
        this(run.runId().toString(),1,false,run.members().stream().map(m->new MemoryBuild(m.playerId())).toList(),List.of());
    }
    public void removeMember(UUID player) { members.removeIf(b->b.player.equals(player)); }
    public MemoryBuild member(UUID player) { return members.stream().filter(b->b.player.equals(player)).findFirst().orElse(null); }
}
