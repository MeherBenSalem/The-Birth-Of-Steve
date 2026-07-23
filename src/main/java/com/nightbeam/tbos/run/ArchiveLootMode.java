package com.nightbeam.tbos.run;

/** How ordinary room-cache claims are shared by a dungeon party. */
public enum ArchiveLootMode {
    /** Every party member can claim each generated cache once. */
    INDIVIDUAL,
    /** The first party member to open a generated cache consumes it for the party. */
    SHARED
}
