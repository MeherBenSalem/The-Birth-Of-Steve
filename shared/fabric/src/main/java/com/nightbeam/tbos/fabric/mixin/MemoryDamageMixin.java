package com.nightbeam.tbos.fabric.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LivingEntity.class)
abstract class MemoryDamageMixin {
    @Unique private float tbos$memoryHealth;
    @ModifyVariable(method="hurtServer",at=@At("HEAD"),argsOnly=true)
    private float tbos$memoryIncoming(float value,net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount) {
        return (Object)this instanceof ServerPlayer player ? com.nightbeam.tbos.memory.MemoryCombat.incoming(player,source,value):value;
    }
    @Inject(method="actuallyHurt",at=@At("HEAD"))
    private void tbos$memoryBefore(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount,CallbackInfo ci) {tbos$memoryHealth=((LivingEntity)(Object)this).getHealth();}
    @Inject(method="actuallyHurt",at=@At("RETURN"))
    private void tbos$memoryAfter(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount,CallbackInfo ci) {
        LivingEntity target=(LivingEntity)(Object)this;
        if(source.getEntity() instanceof ServerPlayer player)
            com.nightbeam.tbos.memory.MemoryCombat.successfulAttack(player,target,Math.max(0,tbos$memoryHealth-target.getHealth()));
    }
}
