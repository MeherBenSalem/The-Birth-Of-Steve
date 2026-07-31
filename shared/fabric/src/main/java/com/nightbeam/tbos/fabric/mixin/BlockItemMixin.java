package com.nightbeam.tbos.fabric.mixin;

import com.nightbeam.tbos.run.ArchiveRunEvents;
import com.nightbeam.tbos.site.TemporalSiteEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric API has no block-placement event that covers non-player placers, so
 * placement into protected geometry is refused here. NeoForge does the same
 * thing from {@code BlockEvent.EntityPlaceEvent}.
 */
@Mixin(BlockItem.class)
abstract class BlockItemMixin {
    @Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"),
            cancellable = true)
    private void tbos$denyProtectedPlacement(
            BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> callback) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos position = context.getClickedPos();
        boolean allowed = TemporalSiteEvents.allowPlace(level, position, context.getPlayer())
                && ArchiveRunEvents.allowPlace(level, position, context.getPlayer());
        if (!allowed) {
            callback.setReturnValue(InteractionResult.FAIL);
        }
    }
}
