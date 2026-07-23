package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public record ArchiveReturnPoint(Identifier dimension, BlockPos position, float yRot, float xRot) {
    public static final Codec<ArchiveReturnPoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("dimension").forGetter(ArchiveReturnPoint::dimension),
            BlockPos.CODEC.fieldOf("position").forGetter(ArchiveReturnPoint::position),
            Codec.FLOAT.optionalFieldOf("y_rot", 0.0F).forGetter(ArchiveReturnPoint::yRot),
            Codec.FLOAT.optionalFieldOf("x_rot", 0.0F).forGetter(ArchiveReturnPoint::xRot)
    ).apply(instance, ArchiveReturnPoint::new));

    public ArchiveReturnPoint {
        dimension = Objects.requireNonNull(dimension, "dimension");
        position = Objects.requireNonNull(position, "position").immutable();
        if (!Float.isFinite(yRot) || !Float.isFinite(xRot)) {
            throw new IllegalArgumentException("Return rotation must be finite");
        }
        yRot = Mth.wrapDegrees(yRot);
        xRot = Mth.clamp(xRot, -90.0F, 90.0F);
    }
}
