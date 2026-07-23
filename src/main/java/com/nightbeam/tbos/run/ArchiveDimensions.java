package com.nightbeam.tbos.run;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

public final class ArchiveDimensions {
    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "fractured_archive");
    public static final ResourceKey<Level> FRACTURED_ARCHIVE =
            ResourceKey.create(Registries.DIMENSION, ID);
    public static final ResourceKey<DimensionType> FRACTURED_ARCHIVE_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, ID);
    public static final ResourceKey<LevelStem> FRACTURED_ARCHIVE_STEM =
            ResourceKey.create(Registries.LEVEL_STEM, ID);

    private ArchiveDimensions() {
    }
}
