package com.nightbeam.tbos.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Independent deterministic budgets prevent one party member monopolizing a run tick. */
public final class MemoryBudget {
    public static final int MAX_DEPTH=3, PER_PLAYER=64, PER_RUN=256, MAX_RECORDS=32;
    private long tick=Long.MIN_VALUE;
    private int total;
    private final Map<UUID,Integer> used=new HashMap<>();
    public boolean take(long now,UUID owner,int depth) {
        if(now!=tick) { tick=now; total=0; used.clear(); }
        int n=used.getOrDefault(owner,0);
        if(depth>MAX_DEPTH || depth<0 || total>=PER_RUN || n>=PER_PLAYER) return false;
        used.put(owner,n+1); total++; return true;
    }
}
