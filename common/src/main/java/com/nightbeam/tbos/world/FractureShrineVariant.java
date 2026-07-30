package com.nightbeam.tbos.world;

import com.nightbeam.tbos.item.MemoryScene;
import java.util.List;

public enum FractureShrineVariant {
    OBSERVATORY(
            "observatory",
            List.of(MemoryScene.ASTRONOMERS, MemoryScene.CELESTIAL_FAMILY)),
    CURATOR_WORKSHOP(
            "curator_workshop",
            List.of(MemoryScene.CURATOR_SMITH, MemoryScene.FINAL_COMMAND)),
    EVACUATION_GATE(
            "evacuation_gate",
            List.of(MemoryScene.ARCHIVE_EVACUATION, MemoryScene.ARCHIVE_FALL));

    private final String serializedName;
    private final List<MemoryScene> memoryScenes;

    FractureShrineVariant(String serializedName, List<MemoryScene> memoryScenes) {
        this.serializedName = serializedName;
        this.memoryScenes = List.copyOf(memoryScenes);
    }

    public String serializedName() {
        return serializedName;
    }

    public List<MemoryScene> memoryScenes() {
        return memoryScenes;
    }

    public static FractureShrineVariant bySerializedName(String name) {
        for (FractureShrineVariant variant : values()) {
            if (variant.serializedName.equals(name)) {
                return variant;
            }
        }
        return OBSERVATORY;
    }
}
