package com.nightbeam.tbos.fabric.mixin;

import com.nightbeam.tbos.run.ArchiveRunEvents;
import com.nightbeam.tbos.site.TemporalSiteEvents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric API has no explosion event, so protected positions are stripped from
 * the block list the explosion is about to act on. NeoForge does the same thing
 * from {@code ExplosionEvent.Detonate}.
 */
@Mixin(ServerExplosion.class)
abstract class ServerExplosionMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @Inject(method = "calculateExplodedPositions", at = @At("RETURN"), cancellable = true)
    private void tbos$protectArchiveBlocks(CallbackInfoReturnable<List<BlockPos>> callback) {
        List<BlockPos> affected = new ArrayList<>(callback.getReturnValue());
        int before = affected.size();
        TemporalSiteEvents.filterExplosion(level, affected);
        ArchiveRunEvents.filterExplosion(level, affected);
        if (affected.size() != before) {
            callback.setReturnValue(affected);
        }
    }
}
