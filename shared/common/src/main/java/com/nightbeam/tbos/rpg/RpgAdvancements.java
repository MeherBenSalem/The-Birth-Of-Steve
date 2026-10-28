package com.nightbeam.tbos.rpg;

import com.nightbeam.tbos.compat.RpgCompat;
import net.minecraft.server.level.ServerPlayer;

/** The Expeditions advancement tab ({@code data/tbos/advancement/expedition/}). Each has one impossible criterion granted from code. */
public final class RpgAdvancements {
    public static final String ROOT = "root";
    public static final String REMEMBER = "remember";
    public static final String FORGET = "forget";
    public static final String MOMENTUM = "momentum";
    public static final String RECOLLECTION = "recollection";
    public static final String HOURGLASS = "hourglass";
    public static final String FRACTURED = "fractured";
    public static final String LAST_KEY = "last_key";
    public static final String REMNANT_RELIC = "remnant_relic";
    public static final String WEEKLY = "weekly";
    public static final String FELLOWSHIP = "fellowship";
    public static final String STORIED = "storied";
    public static final String ARCHIVIST_ETERNAL = "archivist_eternal";
    public static final String KNOW_THE_ARCHIVE = "know_the_archive";

    private RpgAdvancements() {
    }

    public static void award(ServerPlayer player, String name) {
        if (player != null) {
            RpgCompat.award(player, "expedition/" + name, "done");
        }
    }
}
