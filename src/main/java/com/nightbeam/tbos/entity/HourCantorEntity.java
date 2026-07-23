package com.nightbeam.tbos.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.BossEvent;

/** The Archive run boss; its refrain periodically slows its current target. */
public final class HourCantorEntity extends Zombie {
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            getUUID(),
            Component.translatable("entity.tbos.hour_cantor"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10);

    public HourCantorEntity(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
        setCanBreakDoors(false);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        bossEvent.setProgress(getHealth() / getMaxHealth());
        LivingEntity target = getTarget();
        if (target != null && tickCount % 80 == 0 && distanceToSqr(target) <= 144.0D) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1));
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }
}
