package com.nightbeam.tbos.item;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum MemoryScene implements StringRepresentable {
    ASTRONOMERS,
    CURATOR_SMITH,
    CELESTIAL_FAMILY,
    ARCHIVE_EVACUATION,
    FINAL_COMMAND,
    ARCHIVE_FALL;

    public static final Codec<MemoryScene> CODEC = StringRepresentable.fromEnum(MemoryScene::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, MemoryScene> STREAM_CODEC = StreamCodec.of(
            (buffer, scene) -> buffer.writeVarInt(scene.ordinal()),
            buffer -> byOrdinal(buffer.readVarInt()));

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

    private static MemoryScene byOrdinal(int ordinal) {
        MemoryScene[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("Invalid Memory Scene ordinal: " + ordinal);
        }
        return values[ordinal];
    }
}
