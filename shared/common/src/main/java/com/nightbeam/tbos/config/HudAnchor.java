package com.nightbeam.tbos.config;

/** Screen edge or corner a configurable HUD element is placed from. */
public enum HudAnchor {
    BOTTOM_CENTER,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT;

    public boolean top() {
        return this == TOP_LEFT || this == TOP_CENTER || this == TOP_RIGHT;
    }

    public boolean left() {
        return this == TOP_LEFT || this == BOTTOM_LEFT;
    }

    public boolean right() {
        return this == TOP_RIGHT || this == BOTTOM_RIGHT;
    }
}
