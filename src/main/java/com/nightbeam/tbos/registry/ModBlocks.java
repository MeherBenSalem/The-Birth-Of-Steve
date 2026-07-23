package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.block.AlignmentDialBlock;
import com.nightbeam.tbos.block.ArchiveCrateBlock;
import com.nightbeam.tbos.block.ArchiveCoreBlock;
import com.nightbeam.tbos.block.ArchiveCacheBlock;
import com.nightbeam.tbos.block.MemoryAnchorBlock;
import com.nightbeam.tbos.block.FractureCofferBlock;
import com.nightbeam.tbos.block.MemoryLanternBlock;
import com.nightbeam.tbos.block.MeridianRelayBlock;
import com.nightbeam.tbos.block.ResonantBellBlock;
import com.nightbeam.tbos.block.RiftThresholdBlock;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Yesterglass.MOD_ID);

    public static final DeferredBlock<Block> ARCHIVE_STONE = BLOCKS.registerSimpleBlock(
            "archive_stone", properties -> properties.mapColor(MapColor.STONE).strength(2.0F, 6.0F));
    public static final DeferredBlock<Block> CRACKED_ARCHIVE_STONE = BLOCKS.registerSimpleBlock(
            "cracked_archive_stone", properties -> properties.mapColor(MapColor.STONE).strength(1.8F, 5.0F));
    public static final DeferredBlock<Block> ARCHIVE_BRICKS = BLOCKS.registerSimpleBlock(
            "archive_bricks", properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(2.2F, 7.0F));
    public static final DeferredBlock<StairBlock> ARCHIVE_STAIRS = BLOCKS.registerBlock(
            "archive_stairs",
            properties -> new StairBlock(ARCHIVE_BRICKS.get().defaultBlockState(), properties),
            properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(2.2F, 7.0F));
    public static final DeferredBlock<Block> WEATHERED_ARCHIVE_BRICKS = BLOCKS.registerSimpleBlock(
            "weathered_archive_bricks", properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(2.0F, 6.0F));
    public static final DeferredBlock<Block> MOSSY_ARCHIVE_STONE = BLOCKS.registerSimpleBlock(
            "mossy_archive_stone", properties -> properties.mapColor(MapColor.COLOR_GREEN).strength(1.9F, 5.0F));
    public static final DeferredBlock<Block> CHISELED_ARCHIVE_STONE = BLOCKS.registerSimpleBlock(
            "chiseled_archive_stone", properties -> properties.mapColor(MapColor.STONE).strength(2.4F, 7.0F));
    public static final DeferredBlock<Block> CHRONICLE_TILE = BLOCKS.registerSimpleBlock(
            "chronicle_tile", properties -> properties.mapColor(MapColor.COLOR_CYAN).strength(2.3F, 7.0F));
    public static final DeferredBlock<Block> CANTOR_FLOOR = BLOCKS.registerSimpleBlock(
            "cantor_floor", properties -> properties.mapColor(MapColor.COLOR_PURPLE).strength(3.0F, 10.0F));
    public static final DeferredBlock<Block> CANTOR_WALL = BLOCKS.registerSimpleBlock(
            "cantor_wall", properties -> properties.mapColor(MapColor.COLOR_PURPLE).strength(3.0F, 10.0F));
    public static final DeferredBlock<Block> CANTOR_RUNE = BLOCKS.registerSimpleBlock(
            "cantor_rune",
            properties -> properties.mapColor(MapColor.GOLD).strength(3.2F, 10.0F).lightLevel(state -> 8));
    public static final DeferredBlock<Block> MERIDIAN_TILE = BLOCKS.registerSimpleBlock(
            "meridian_tile", properties -> properties.mapColor(MapColor.COLOR_CYAN).strength(2.2F, 6.0F));
    public static final DeferredBlock<Block> ENGRAVED_MERIDIAN_TILE = BLOCKS.registerSimpleBlock(
            "engraved_meridian_tile", properties -> properties.mapColor(MapColor.COLOR_CYAN).strength(2.2F, 6.0F));
    public static final DeferredBlock<Block> YESTERGLASS = BLOCKS.registerSimpleBlock(
            "yesterglass",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.6F)
                    .lightLevel(state -> 10)
                    .noOcclusion());
    public static final DeferredBlock<MemoryAnchorBlock> MEMORY_ANCHOR = BLOCKS.registerBlock(
            "memory_anchor",
            MemoryAnchorBlock::new,
            properties -> properties.mapColor(MapColor.COLOR_CYAN).strength(3.0F, 9.0F).lightLevel(state -> 7));
    public static final DeferredBlock<Block> PHASE_PLATFORM = BLOCKS.registerSimpleBlock(
            "phase_platform", properties -> properties.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(2.0F, 6.0F).lightLevel(state -> 4));
    public static final DeferredBlock<Block> RESONANCE_LAMP = BLOCKS.registerSimpleBlock(
            "resonance_lamp", properties -> properties.mapColor(MapColor.GOLD).strength(2.0F, 6.0F).lightLevel(state -> 15));
    public static final DeferredBlock<Block> CHRONICLE_BRONZE = BLOCKS.registerSimpleBlock(
            "chronicle_bronze", properties -> properties.mapColor(MapColor.COLOR_ORANGE).strength(3.0F, 9.0F));
    public static final DeferredBlock<Block> LENSWORK_CRYSTAL = BLOCKS.registerSimpleBlock(
            "lenswork_crystal",
            properties -> properties.mapColor(MapColor.COLOR_CYAN).strength(1.7F, 6.0F).lightLevel(state -> 9));
    public static final DeferredBlock<Block> ARCHIVE_SEAL = BLOCKS.registerSimpleBlock(
            "archive_seal",
            properties -> properties
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3_600_000.0F)
                    .lightLevel(state -> 7)
                    .noOcclusion());
    public static final DeferredBlock<ArchiveCacheBlock> ARCHIVE_CACHE = BLOCKS.registerBlock(
            "archive_cache",
            ArchiveCacheBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5F, 12.0F)
                    .lightLevel(state -> 14)
                    .noOcclusion());
    public static final DeferredBlock<FractureCofferBlock> FRACTURE_COFFER = BLOCKS.registerBlock(
            "fracture_coffer",
            FractureCofferBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F, 12.0F)
                    .lightLevel(state -> state.getValue(FractureCofferBlock.OPENED) ? 2 : 8));
    public static final DeferredBlock<AlignmentDialBlock> ALIGNMENT_DIAL = BLOCKS.registerBlock(
            "alignment_dial",
            AlignmentDialBlock::new,
            properties -> properties.mapColor(MapColor.COLOR_ORANGE).strength(2.5F, 7.0F).lightLevel(state -> 5));
    public static final DeferredBlock<ResonantBellBlock> RESONANT_BELL = BLOCKS.registerBlock(
            "resonant_bell",
            ResonantBellBlock::new,
            properties -> properties
                    .mapColor(MapColor.GOLD)
                    .strength(2.5F, 7.0F)
                    .lightLevel(state -> state.getValue(ResonantBellBlock.LIT) ? 13 : 2));
    public static final DeferredBlock<MeridianRelayBlock> MERIDIAN_RELAY = BLOCKS.registerBlock(
            "meridian_relay",
            MeridianRelayBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F, 9.0F)
                    .lightLevel(state -> state.getValue(MeridianRelayBlock.POWERED) ? 11 : 3));
    public static final DeferredBlock<ArchiveCoreBlock> ARCHIVE_CORE = BLOCKS.registerBlock(
            "archive_core",
            ArchiveCoreBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(4.0F, 12.0F)
                    .lightLevel(state -> 12));
    public static final DeferredBlock<MemoryLanternBlock> MEMORY_LANTERN = BLOCKS.registerBlock(
            "memory_lantern",
            MemoryLanternBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(2.5F, 7.0F)
                    .lightLevel(state -> 11)
                    .noCollision()
                    .noOcclusion());
    public static final DeferredBlock<Block> MEMORY_IMPRINT = BLOCKS.registerSimpleBlock(
            "memory_imprint",
            properties -> properties
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3_600_000.0F)
                    .noCollision()
                    .noOcclusion()
                    .lightLevel(state -> 9));
    public static final DeferredBlock<RiftThresholdBlock> RIFT_THRESHOLD = BLOCKS.registerBlock(
            "rift_threshold",
            RiftThresholdBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3_600_000.0F)
                    .lightLevel(state -> 8)
                    .noOcclusion());
    public static final DeferredBlock<ArchiveCrateBlock> ARCHIVE_CRATE = crate("archive_crate");
    public static final DeferredBlock<ArchiveCrateBlock> ARCHIVE_CRATE_STACK = crate("archive_crate_stack");
    public static final DeferredBlock<ArchiveCrateBlock> ARCHIVE_LARGE_CRATE = crate("archive_large_crate");
    public static final DeferredBlock<ArchiveCrateBlock> ARCHIVE_LARGE_CRATE_STACK = crate("archive_large_crate_stack");
    public static final DeferredBlock<ArchiveCrateBlock> ARCHIVE_BARREL = crate("archive_barrel");
    public static final DeferredBlock<ArchiveCrateBlock> ARCHIVE_BARREL_STACK = crate("archive_barrel_stack");
    public static final DeferredBlock<ArchiveCrateBlock> ARCHIVE_MIXED_STACK_1 = crate("archive_mixed_stack_1");
    public static final DeferredBlock<ArchiveCrateBlock> ARCHIVE_MIXED_STACK_2 = crate("archive_mixed_stack_2");
    public static final DeferredBlock<ArchiveCrateBlock> ARCHIVE_MIXED_STACK_3 = crate("archive_mixed_stack_3");

    public static final List<DeferredBlock<ArchiveCrateBlock>> ARCHIVE_CRATES = List.of(
            ARCHIVE_CRATE,
            ARCHIVE_CRATE_STACK,
            ARCHIVE_LARGE_CRATE,
            ARCHIVE_LARGE_CRATE_STACK,
            ARCHIVE_BARREL,
            ARCHIVE_BARREL_STACK,
            ARCHIVE_MIXED_STACK_1,
            ARCHIVE_MIXED_STACK_2,
            ARCHIVE_MIXED_STACK_3);

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }

    private static DeferredBlock<ArchiveCrateBlock> crate(String name) {
        return BLOCKS.registerBlock(
                name,
                ArchiveCrateBlock::new,
                properties -> properties
                        .mapColor(MapColor.WOOD)
                        .strength(0.8F, 1.5F)
                        .noOcclusion());
    }
}
