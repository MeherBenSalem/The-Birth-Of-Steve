package com.nightbeam.tbos.client.render;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.BabyZombieModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.zombie.Zombie;

/** Shared renderer for Archive zombie-derived mobs with original entity textures. */
public final class ArchiveZombieRenderer<T extends Zombie>
        extends AbstractZombieRenderer<T, ZombieRenderState, ZombieModel<ZombieRenderState>> {
    private final Identifier texture;

    public ArchiveZombieRenderer(EntityRendererProvider.Context context, Identifier texture, float shadowRadius) {
        super(
                context,
                new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
                new BabyZombieModel(context.bakeLayer(ModelLayers.ZOMBIE_BABY)),
                ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), ZombieModel::new),
                ArmorModelSet.bake(ModelLayers.ZOMBIE_BABY_ARMOR, context.getModelSet(), BabyZombieModel::new));
        this.texture = texture;
        this.shadowRadius = shadowRadius;
    }

    @Override
    public ZombieRenderState createRenderState() {
        return new ZombieRenderState();
    }

    @Override
    public Identifier getTextureLocation(ZombieRenderState state) {
        return texture;
    }
}
