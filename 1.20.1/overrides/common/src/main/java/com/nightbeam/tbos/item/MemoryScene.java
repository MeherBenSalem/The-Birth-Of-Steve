package com.nightbeam.tbos.item;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum MemoryScene implements StringRepresentable {
    ASTRONOMERS,
    CURATOR_SMITH,
    CELESTIAL_FAMILY,
    ARCHIVE_EVACUATION,
    FINAL_COMMAND,
    ARCHIVE_FALL;

    public static final Codec<MemoryScene> CODEC = StringRepresentable.fromEnum(MemoryScene::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String titleKey() {
        return "memory_scene.tbos." + getSerializedName() + ".title";
    }

    public String descriptionKey() {
        return "memory_scene.tbos." + getSerializedName() + ".description";
    }
}
