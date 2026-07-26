package com.nightbeam.tbos.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/**
 * A seed-derived shrine location that has not necessarily been built yet. The
 * plan is persisted so a world keeps the same three shrine sites even if the
 * chunks around them are never generated.
 */
public record FractureShrinePlan(FractureShrineVariant variant, BlockPos target) {
    public static final Codec<FractureShrinePlan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("variant")
                    .xmap(FractureShrineVariant::bySerializedName, FractureShrineVariant::serializedName)
                    .forGetter(FractureShrinePlan::variant),
            BlockPos.CODEC.fieldOf("target").forGetter(FractureShrinePlan::target)
    ).apply(instance, FractureShrinePlan::new));

    public FractureShrinePlan {
        target = target.immutable();
    }

    public ChunkPos chunk() {
        return ChunkPos.containing(target);
    }

    public double distanceToSqr(BlockPos pos) {
        // Plans are horizontal targets; their surface Y is only resolved when the
        // shrine is actually built, so distance must ignore height.
        double dx = target.getX() - pos.getX();
        double dz = target.getZ() - pos.getZ();
        return dx * dx + dz * dz;
    }
}
