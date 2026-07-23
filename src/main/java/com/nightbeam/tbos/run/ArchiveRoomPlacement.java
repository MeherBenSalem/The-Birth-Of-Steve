package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ArchiveRoomPlacement(
        ArchiveGridPos coordinates,
        ArchiveRoomSize size,
        ArchiveTransform transform) {
    public static final Codec<ArchiveRoomPlacement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ArchiveGridPos.CODEC.fieldOf("coordinates").forGetter(ArchiveRoomPlacement::coordinates),
            ArchiveRoomSize.CODEC.fieldOf("size").forGetter(ArchiveRoomPlacement::size),
            ArchiveTransform.CODEC.optionalFieldOf("transform", ArchiveTransform.IDENTITY)
                    .forGetter(ArchiveRoomPlacement::transform)
    ).apply(instance, ArchiveRoomPlacement::new));
}
