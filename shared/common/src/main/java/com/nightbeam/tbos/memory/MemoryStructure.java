package com.nightbeam.tbos.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persisted temporary geometry and circuit progress, scoped to its allocated floor and room. */
public record MemoryStructure(long floor,int room,long position,long until,int role) {
    public static final Codec<MemoryStructure> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.LONG.fieldOf("floor").forGetter(MemoryStructure::floor),
        Codec.INT.fieldOf("room").forGetter(MemoryStructure::room),
        Codec.LONG.fieldOf("position").forGetter(MemoryStructure::position),
        Codec.LONG.fieldOf("until").forGetter(MemoryStructure::until),
        Codec.INT.fieldOf("role").forGetter(MemoryStructure::role)
    ).apply(i,MemoryStructure::new));
    public MemoryStructure {
        if(floor<0||room<0||room>=48||until<0||role<0||role>5)throw new IllegalArgumentException("Invalid memory structure");
    }
}
