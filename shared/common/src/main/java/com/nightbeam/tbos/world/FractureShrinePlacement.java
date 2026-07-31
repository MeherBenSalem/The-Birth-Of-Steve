package com.nightbeam.tbos.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record FractureShrinePlacement(FractureShrineVariant variant, BlockPos origin) {
    public static final Codec<FractureShrinePlacement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("variant")
                    .xmap(FractureShrineVariant::bySerializedName, FractureShrineVariant::serializedName)
                    .forGetter(FractureShrinePlacement::variant),
            BlockPos.CODEC.fieldOf("origin").forGetter(FractureShrinePlacement::origin)
    ).apply(instance, FractureShrinePlacement::new));

    public FractureShrinePlacement {
        origin = origin.immutable();
    }

    public double distanceToSqr(BlockPos pos) {
        return origin.distSqr(pos);
    }
}
