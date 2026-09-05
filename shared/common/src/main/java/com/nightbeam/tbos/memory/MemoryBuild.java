package com.nightbeam.tbos.memory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;

/** Durable personal build. Mutations occur only on the server thread and dirty the owning save. */
public final class MemoryBuild {
    public static final Codec<MemoryBuild> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("player").forGetter(b -> b.player.toString()),
        Codec.INT.fieldOf("abilities").forGetter(b -> b.abilities),
        Codec.INT.fieldOf("artifacts").forGetter(b -> b.artifacts),
        Codec.INT.fieldOf("enhanced").forGetter(b -> b.enhanced),
        Codec.INT.listOf().fieldOf("slots").forGetter(b -> b.slots),
        Codec.LONG.listOf().fieldOf("ready").forGetter(b -> b.ready),
        Codec.LONG.fieldOf("floor").forGetter(b -> b.floor),
        Codec.INT.listOf().fieldOf("clears").forGetter(b -> b.clears),
        Codec.INT.listOf().fieldOf("offers").forGetter(b -> b.offers),
        Codec.INT.fieldOf("draft").forGetter(b -> b.draft),
        Codec.INT.optionalFieldOf("earned",0).forGetter(b -> b.earned),
        Codec.INT.fieldOf("debt").forGetter(b -> b.debt),
        Codec.BOOL.fieldOf("overwritten").forGetter(b -> b.overwritten),
        Codec.BOOL.fieldOf("continue").forGetter(b -> b.continueVote),
        MemoryStructure.CODEC.optionalFieldOf("trial",new MemoryStructure(0,0,0,0,1)).forGetter(b->b.trial),
        Codec.INT.optionalFieldOf("rare_credits",0).forGetter(b->b.rareCredits)
    ).apply(i, MemoryBuild::new));
    public final UUID player;
    public int abilities, artifacts, enhanced, draft, debt, earned;
    public List<Integer> slots, clears, offers;
    public List<Long> ready;
    public long floor;
    public boolean overwritten, continueVote;
    public MemoryStructure trial;
    public int rareCredits;

    public MemoryBuild(UUID player) {
        this(player.toString(), 1, 0, 0, List.of(0,-1,-1), List.of(0L,0L,0L,0L,0L,0L),
             0L, List.of(), List.of(), 0, 0, 0, false, false,new MemoryStructure(0,0,0,0,1),0);
    }
    private MemoryBuild(String player, int abilities, int artifacts, int enhanced, List<Integer> slots,
            List<Long> ready, long floor, List<Integer> clears, List<Integer> offers, int draft, int earned,
            int debt, boolean overwritten, boolean continueVote,MemoryStructure trial,int rareCredits) {
        this.player = UUID.fromString(player);
        if ((abilities & ~63) != 0 || abilities < 1 || (artifacts & ~4095) != 0
                || (enhanced & ~artifacts) != 0 || slots.size() != 3 || ready.size() != 6
                || slots.stream().anyMatch(s -> s < -1 || s > 5 || (s >= 0 && (abilities & (1 << s)) == 0))
                || slots.stream().filter(s -> s >= 0).distinct().count() != slots.stream().filter(s -> s >= 0).count()
                || ready.stream().anyMatch(t -> t < 0) || floor < 0 || clears.size() > 48
                || clears.stream().anyMatch(r -> r < 0 || r >= 48) || offers.size() > 3
                || offers.stream().anyMatch(o -> o < 0 || o >= 18) || debt < 0 || debt > 3 || draft < 0 || earned < 0 || rareCredits < 0) {
            throw new IllegalArgumentException("Invalid memory build");
        }
        this.abilities=abilities; this.artifacts=artifacts; this.enhanced=enhanced;
        this.slots=new ArrayList<>(slots); this.ready=new ArrayList<>(ready);
        this.floor=floor; this.clears=new ArrayList<>(clears); this.offers=new ArrayList<>(offers);
        this.earned=earned; this.draft=draft; this.debt=debt; this.overwritten=overwritten; this.continueVote=continueVote;
        this.trial=trial;this.rareCredits=rareCredits;
    }
    public boolean has(MemoryArtifact a) { return (artifacts & (1 << a.ordinal())) != 0; }
    public int rank(MemoryArtifact a) { return !has(a) ? 0 : (enhanced & (1 << a.ordinal())) == 0 ? 1 : 2; }
    public boolean owns(int choice) { return choice < 6 ? (abilities & (1 << choice)) != 0 : (artifacts & (1 << (choice-6))) != 0; }
    public void acquire(int choice) {
        if (choice < 0 || choice >= 18) throw new IllegalArgumentException("Unknown memory");
        if (choice < 6) {
            abilities |= 1 << choice;
            if (!slots.contains(choice)) { int empty=slots.indexOf(-1); if(empty>=0) slots.set(empty,choice); }
        } else artifacts |= 1 << (choice-6);
    }
    public void nextFloor(long value) {
        floor=value; clears.clear(); overwritten=false; continueVote=false;
        trial=new MemoryStructure(value,0,0,0,1);
        // Pending offers and ability cooldowns survive the transition.
    }
    /** Stable personal offers; reopening/reconnecting never rerolls them. */
    public void offer(long seed, boolean activeOnly) {
        if (!offers.isEmpty()) return;
        List<Integer> pool=new ArrayList<>();
        for(int id=activeOnly?0:6; id<(activeOnly?6:18); id++) if(!owns(id) && (activeOnly || (id!=14&&id!=17||owns(4)) && (id!=15||owns(5)) && (id!=16||owns(3)))) pool.add(id);
        Collections.shuffle(pool,new Random(seed ^ player.getMostSignificantBits() ^ player.getLeastSignificantBits() ^ (0x9E3779B97F4A7C15L * draft)));
        offers.addAll(pool.subList(0,Math.min(3,pool.size())));
    }
    public boolean equip(int slot,int ability) {
        if(slot<0||slot>=3||ability<0||ability>=6||!owns(ability))return false;
        int previous=slots.indexOf(ability),old=slots.get(slot);
        if(previous>=0)slots.set(previous,old);
        slots.set(slot,ability);return true;
    }
    public boolean claim(int revision, int choice) {
        if (revision != draft || !offers.contains(choice) || owns(choice)) return false;
        acquire(choice); offers.clear(); draft++; return true;
    }
}
