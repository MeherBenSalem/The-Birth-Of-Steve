package com.nightbeam.tbos.fabric.mixin;

import com.mojang.datafixers.DataFixer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets a {@code SavedDataType} carry a null {@code DataFixTypes}.
 *
 * <p>The mod's two saved-data types have no DFU schema, so they declare null,
 * exactly as they do on NeoForge — which patches this same call to be
 * null-tolerant. Without the redirect vanilla dereferences it, the NPE is
 * swallowed by the surrounding catch, and every load silently resets Archive
 * runs and site progress to empty.
 */
@Mixin(SavedDataStorage.class)
abstract class SavedDataStorageMixin {
    @Redirect(
            method = "readTagFromDisk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/datafix/DataFixTypes;update"
                            + "(Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/nbt/CompoundTag;II)"
                            + "Lnet/minecraft/nbt/CompoundTag;"))
    private CompoundTag tbos$skipFixersWithoutType(
            @Nullable DataFixTypes type, DataFixer fixer, CompoundTag tag, int fromVersion, int toVersion) {
        return type == null ? tag : type.update(fixer, tag, fromVersion, toVersion);
    }
}
