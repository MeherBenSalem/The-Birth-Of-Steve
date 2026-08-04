package com.nightbeam.tbos.fabric.mixin;

import com.nightbeam.tbos.run.ArchiveRunEvents;
import com.nightbeam.tbos.site.TemporalSiteEvents;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Filters protected blocks from the 1.20.1 explosion list before destruction. */
@Mixin(Explosion.class)
abstract class ServerExplosionMixin {
    @Shadow
    @Final
    private Level level;

    @Shadow
    public abstract List<BlockPos> getToBlow();

    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void tbos$protectArchiveBlocks(boolean spawnParticles, CallbackInfo callback) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        List<BlockPos> affected = getToBlow();
        TemporalSiteEvents.filterExplosion(serverLevel, affected);
        ArchiveRunEvents.filterExplosion(serverLevel, affected);
    }
}
