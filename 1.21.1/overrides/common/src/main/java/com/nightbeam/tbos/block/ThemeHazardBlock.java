package com.nightbeam.tbos.block;

import com.mojang.serialization.MapCodec;
import com.nightbeam.tbos.compat.VanillaCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Server-owned Archive theme hazard block. Modes are fixed per registered instance. */
public final class ThemeHazardBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public enum Mode {
        PARALLAX_PANEL,
        LIGHT_DUST,
        COLLAPSING_TILE,
        BRITTLE_ASH,
        SHATTER_PANE,
        FALSE_SHELF,
        RESONANT_PLATE,
        INK_POOL
    }

    private final Mode mode;
    private final MapCodec<ThemeHazardBlock> codec;

    public ThemeHazardBlock(Mode mode, BlockBehaviour.Properties properties) {
        super(properties);
        this.mode = mode;
        this.codec = simpleCodec(props -> new ThemeHazardBlock(mode, props));
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, Boolean.FALSE));
    }

    public Mode mode() {
        return mode;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (mode) {
            case PARALLAX_PANEL -> state.getValue(ACTIVE) ? Shapes.empty() : Shapes.block();
            case LIGHT_DUST, INK_POOL -> Shapes.empty();
            case SHATTER_PANE -> Shapes.box(0.05D, 0.0D, 0.05D, 0.95D, 1.0D, 0.95D);
            case FALSE_SHELF -> Shapes.box(0.05D, 0.0D, 0.05D, 0.95D, 0.9D, 0.95D);
            default -> Shapes.block();
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getCollisionShape(state, level, pos, context);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return mode == Mode.LIGHT_DUST
                || mode == Mode.INK_POOL
                || (mode == Mode.PARALLAX_PANEL && state.getValue(ACTIVE));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide() && mode == Mode.PARALLAX_PANEL) {
            level.scheduleTick(pos, this, 40);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        switch (mode) {
            case PARALLAX_PANEL -> {
                boolean next = !state.getValue(ACTIVE);
                level.setBlock(pos, state.setValue(ACTIVE, next), Block.UPDATE_CLIENTS);
                level.scheduleTick(pos, this, next ? 12 : 40 + random.nextInt(20));
            }
            case COLLAPSING_TILE, BRITTLE_ASH -> {
                if (state.getValue(ACTIVE)) {
                    level.removeBlock(pos, false);
                }
            }
            case RESONANT_PLATE -> {
                if (state.getValue(ACTIVE)) {
                    level.setBlock(pos, state.setValue(ACTIVE, false), Block.UPDATE_CLIENTS);
                }
            }
            case LIGHT_DUST -> dimAround(level, pos, 2.5D);
            default -> {
            }
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) {
            return;
        }
        switch (mode) {
            case INK_POOL -> living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false, true));
            case LIGHT_DUST -> {
                if (living instanceof Player) {
                    living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false, true));
                }
            }
            case RESONANT_PLATE -> {
                if (living instanceof Player && !state.getValue(ACTIVE)) {
                    level.setBlock(pos, state.setValue(ACTIVE, true), Block.UPDATE_CLIENTS);
                    VanillaCompat.knockback(
                            living, 0.55D, living.getX() - (pos.getX() + 0.5D), living.getZ() - (pos.getZ() + 0.5D));
                    level.scheduleTick(pos, this, 30);
                }
            }
            case COLLAPSING_TILE, BRITTLE_ASH -> {
                if (living instanceof Player && !state.getValue(ACTIVE)) {
                    level.setBlock(pos, state.setValue(ACTIVE, true), Block.UPDATE_CLIENTS);
                    level.scheduleTick(pos, this, mode == Mode.BRITTLE_ASH ? 18 : 10);
                }
            }
            default -> {
            }
        }
    }

    public static void shatterNearby(ServerLevel level, BlockPos origin, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-radius, -1, -radius), origin.offset(radius, 1, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ThemeHazardBlock hazard && hazard.mode == Mode.SHATTER_PANE) {
                level.destroyBlock(pos, false);
            }
        }
    }

    public static void dimAround(ServerLevel level, BlockPos origin, double radius) {
        AABB box = new AABB(origin).inflate(radius);
        for (Player player : level.getEntitiesOfClass(Player.class, box)) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));
        }
    }
}
