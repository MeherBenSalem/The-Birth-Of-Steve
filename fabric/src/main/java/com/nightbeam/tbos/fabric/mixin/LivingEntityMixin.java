package com.nightbeam.tbos.fabric.mixin;

import com.nightbeam.tbos.run.ArchiveRunEvents;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Fabric API has no heal event, so the REDUCED_HEALING room modifier is applied
 * by scaling the argument on the way in. NeoForge gets the same effect from
 * {@code LivingHealEvent#setAmount}.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @ModifyVariable(method = "heal(F)V", at = @At("HEAD"), argsOnly = true)
    private float tbos$scaleHeal(float amount) {
        return ArchiveRunEvents.scaleHeal((LivingEntity) (Object) this, amount);
    }
}
