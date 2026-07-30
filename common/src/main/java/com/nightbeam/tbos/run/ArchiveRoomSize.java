package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Template dimensions in blocks. */
public record ArchiveRoomSize(int width, int height, int depth) {
    public static final Codec<ArchiveRoomSize> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("width").forGetter(ArchiveRoomSize::width),
            Codec.INT.fieldOf("height").forGetter(ArchiveRoomSize::height),
            Codec.INT.fieldOf("depth").forGetter(ArchiveRoomSize::depth)
    ).apply(instance, ArchiveRoomSize::new));

    public ArchiveRoomSize {
        if (width < 9 || width > 40 || depth < 9 || depth > 40 || height < 5 || height > 12) {
            throw new IllegalArgumentException("Archive room dimensions exceed one safe grid cell: "
                    + width + "x" + height + "x" + depth);
        }
    }
}
