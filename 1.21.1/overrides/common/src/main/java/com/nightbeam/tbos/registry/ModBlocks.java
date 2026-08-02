package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.block.AlignmentDialBlock;
import com.nightbeam.tbos.block.ArchiveCrateBlock;
import com.nightbeam.tbos.block.ArchiveCoreBlock;
import com.nightbeam.tbos.block.ArchiveCacheBlock;
import com.nightbeam.tbos.block.EngravedMeridianTileBlock;
import com.nightbeam.tbos.block.GraveyardPropBlock;
import com.nightbeam.tbos.block.MemoryAnchorBlock;
import com.nightbeam.tbos.block.FractureCofferBlock;
import com.nightbeam.tbos.block.MemoryLanternBlock;
import com.nightbeam.tbos.block.MeridianRelayBlock;
import com.nightbeam.tbos.block.ResonantBellBlock;
import com.nightbeam.tbos.block.RiftThresholdBlock;
import com.nightbeam.tbos.block.ThemeHazardBlock;
import com.nightbeam.tbos.platform.registry.ModRegistries;
import com.nightbeam.tbos.platform.registry.RegistryEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final RegistryEntry<Block> ARCHIVE_STONE = ModRegistries.BLOCKS.registerSimpleBlock(
            "archive_stone", properties -> properties.mapColor(MapColor.STONE).strength(2.0F, 6.0F));
    public static final RegistryEntry<Block> CRACKED_ARCHIVE_STONE = ModRegistries.BLOCKS.registerSimpleBlock(
            "cracked_archive_stone", properties -> properties.mapColor(MapColor.STONE).strength(1.8F, 5.0F));
    public static final RegistryEntry<Block> ARCHIVE_BRICKS = ModRegistries.BLOCKS.registerSimpleBlock(
            "archive_bricks", properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(2.2F, 7.0F));
    public static final RegistryEntry<StairBlock> ARCHIVE_STAIRS = ModRegistries.BLOCKS.registerBlock(
            "archive_stairs",
            properties -> new StairBlock(ARCHIVE_BRICKS.get().defaultBlockState(), properties),
            properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(2.2F, 7.0F));
    public static final RegistryEntry<Block> WEATHERED_ARCHIVE_BRICKS = ModRegistries.BLOCKS.registerSimpleBlock(
            "weathered_archive_bricks", properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(2.0F, 6.0F));
    public static final RegistryEntry<Block> MOSSY_ARCHIVE_STONE = ModRegistries.BLOCKS.registerSimpleBlock(
            "mossy_archive_stone", properties -> properties.mapColor(MapColor.COLOR_GREEN).strength(1.9F, 5.0F));
    public static final RegistryEntry<Block> CHISELED_ARCHIVE_STONE = ModRegistries.BLOCKS.registerSimpleBlock(
            "chiseled_archive_stone", properties -> properties.mapColor(MapColor.STONE).strength(2.4F, 7.0F));
    public static final RegistryEntry<Block> CHRONICLE_TILE = ModRegistries.BLOCKS.registerSimpleBlock(
            "chronicle_tile", properties -> properties.mapColor(MapColor.COLOR_CYAN).strength(2.3F, 7.0F));
    public static final RegistryEntry<Block> CANTOR_FLOOR = ModRegistries.BLOCKS.registerSimpleBlock(
            "cantor_floor", properties -> properties.mapColor(MapColor.COLOR_PURPLE).strength(3.0F, 10.0F));
    public static final RegistryEntry<Block> CANTOR_WALL = ModRegistries.BLOCKS.registerSimpleBlock(
            "cantor_wall", properties -> properties.mapColor(MapColor.COLOR_PURPLE).strength(3.0F, 10.0F));
    public static final RegistryEntry<Block> CANTOR_RUNE = ModRegistries.BLOCKS.registerSimpleBlock(
            "cantor_rune",
            properties -> properties.mapColor(MapColor.GOLD).strength(3.2F, 10.0F).lightLevel(state -> 8));
    public static final RegistryEntry<Block> MERIDIAN_TILE = ModRegistries.BLOCKS.registerSimpleBlock(
            "meridian_tile", properties -> properties.mapColor(MapColor.COLOR_CYAN).strength(2.2F, 6.0F));
    public static final RegistryEntry<EngravedMeridianTileBlock> ENGRAVED_MERIDIAN_TILE = ModRegistries.BLOCKS.registerBlock(
            "engraved_meridian_tile",
            EngravedMeridianTileBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(2.2F, 6.0F)
                    .lightLevel(state -> state.getValue(EngravedMeridianTileBlock.CHARGED) ? 8 : 0));
    public static final RegistryEntry<Block> YESTERGLASS = ModRegistries.BLOCKS.registerSimpleBlock(
            "yesterglass",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.6F)
                    .lightLevel(state -> 10)
                    .noOcclusion());
    public static final RegistryEntry<MemoryAnchorBlock> MEMORY_ANCHOR = ModRegistries.BLOCKS.registerBlock(
            "memory_anchor",
            MemoryAnchorBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.0F, 9.0F)
                    .lightLevel(state -> 7)
                    .noOcclusion());
    public static final RegistryEntry<Block> PHASE_PLATFORM = ModRegistries.BLOCKS.registerSimpleBlock(
            "phase_platform", properties -> properties.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(2.0F, 6.0F).lightLevel(state -> 4));
    public static final RegistryEntry<Block> RESONANCE_LAMP = ModRegistries.BLOCKS.registerSimpleBlock(
            "resonance_lamp", properties -> properties.mapColor(MapColor.GOLD).strength(2.0F, 6.0F).lightLevel(state -> 15));
    public static final RegistryEntry<Block> CHRONICLE_BRONZE = ModRegistries.BLOCKS.registerSimpleBlock(
            "chronicle_bronze", properties -> properties.mapColor(MapColor.COLOR_ORANGE).strength(3.0F, 9.0F));
    public static final RegistryEntry<Block> LENSWORK_CRYSTAL = ModRegistries.BLOCKS.registerSimpleBlock(
            "lenswork_crystal",
            properties -> properties.mapColor(MapColor.COLOR_CYAN).strength(1.7F, 6.0F).lightLevel(state -> 9));
    public static final RegistryEntry<Block> ARCHIVE_SEAL = ModRegistries.BLOCKS.registerSimpleBlock(
            "archive_seal",
            properties -> properties
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3_600_000.0F)
                    .lightLevel(state -> 7)
                    .noOcclusion());
    /**
     * The Seal's louder sibling, used only on the way into a boss room.
     *
     * <p>Every locked door in the Archive looked identical, so a player had no
     * way to tell an ordinary held door from the one with a boss behind it until
     * they were already through. Same silhouette and same unbreakable strength;
     * cyan instead of gold, and brighter, so the difference carries across a
     * dark room.
     */
    public static final RegistryEntry<Block> CANTOR_GATE = ModRegistries.BLOCKS.registerSimpleBlock(
            "cantor_gate",
            properties -> properties
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(-1.0F, 3_600_000.0F)
                    .lightLevel(state -> 10)
                    .noOcclusion());
    public static final RegistryEntry<ArchiveCacheBlock> ARCHIVE_CACHE = ModRegistries.BLOCKS.registerBlock(
            "archive_cache",
            ArchiveCacheBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5F, 12.0F)
                    .lightLevel(state -> 14)
                    .noOcclusion());
    public static final RegistryEntry<FractureCofferBlock> FRACTURE_COFFER = ModRegistries.BLOCKS.registerBlock(
            "fracture_coffer",
            FractureCofferBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F, 12.0F)
                    .lightLevel(state -> state.getValue(FractureCofferBlock.OPENED) ? 2 : 8));
    public static final RegistryEntry<AlignmentDialBlock> ALIGNMENT_DIAL = ModRegistries.BLOCKS.registerBlock(
            "alignment_dial",
            AlignmentDialBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.5F, 7.0F)
                    .lightLevel(state -> 5)
                    .noOcclusion());
    public static final RegistryEntry<ResonantBellBlock> RESONANT_BELL = ModRegistries.BLOCKS.registerBlock(
            "resonant_bell",
            ResonantBellBlock::new,
            properties -> properties
                    .mapColor(MapColor.GOLD)
                    .strength(2.5F, 7.0F)
                    .lightLevel(state -> state.getValue(ResonantBellBlock.LIT) ? 13 : 2));
    public static final RegistryEntry<MeridianRelayBlock> MERIDIAN_RELAY = ModRegistries.BLOCKS.registerBlock(
            "meridian_relay",
            MeridianRelayBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F, 9.0F)
                    .lightLevel(state -> state.getValue(MeridianRelayBlock.POWERED) ? 11 : 3)
                    .noOcclusion());
    public static final RegistryEntry<ArchiveCoreBlock> ARCHIVE_CORE = ModRegistries.BLOCKS.registerBlock(
            "archive_core",
            ArchiveCoreBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(4.0F, 12.0F)
                    .lightLevel(state -> 12)
                    .noOcclusion());
    public static final RegistryEntry<MemoryLanternBlock> MEMORY_LANTERN = ModRegistries.BLOCKS.registerBlock(
            "memory_lantern",
            MemoryLanternBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(2.5F, 7.0F)
                    .lightLevel(state -> 11)
                    .noCollission()
                    .noOcclusion());
    public static final RegistryEntry<Block> MEMORY_IMPRINT = ModRegistries.BLOCKS.registerSimpleBlock(
            "memory_imprint",
            properties -> properties
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3_600_000.0F)
                    .noCollission()
                    .noOcclusion()
                    .lightLevel(state -> 9));
    public static final RegistryEntry<RiftThresholdBlock> RIFT_THRESHOLD = ModRegistries.BLOCKS.registerBlock(
            "rift_threshold",
            RiftThresholdBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(-1.0F, 3_600_000.0F)
                    .lightLevel(state -> 8)
                    .noOcclusion());
    public static final RegistryEntry<ArchiveCrateBlock> ARCHIVE_CRATE = crate("archive_crate");
    public static final RegistryEntry<ArchiveCrateBlock> ARCHIVE_CRATE_STACK = crate("archive_crate_stack");
    public static final RegistryEntry<ArchiveCrateBlock> ARCHIVE_LARGE_CRATE = crate("archive_large_crate");
    public static final RegistryEntry<ArchiveCrateBlock> ARCHIVE_LARGE_CRATE_STACK = crate("archive_large_crate_stack");
    public static final RegistryEntry<ArchiveCrateBlock> ARCHIVE_BARREL = crate("archive_barrel");
    public static final RegistryEntry<ArchiveCrateBlock> ARCHIVE_BARREL_STACK = crate("archive_barrel_stack");
    public static final RegistryEntry<ArchiveCrateBlock> ARCHIVE_MIXED_STACK_1 = crate("archive_mixed_stack_1");
    public static final RegistryEntry<ArchiveCrateBlock> ARCHIVE_MIXED_STACK_2 = crate("archive_mixed_stack_2");
    public static final RegistryEntry<ArchiveCrateBlock> ARCHIVE_MIXED_STACK_3 = crate("archive_mixed_stack_3");

    public static final List<RegistryEntry<ArchiveCrateBlock>> ARCHIVE_CRATES = List.of(
            ARCHIVE_CRATE,
            ARCHIVE_CRATE_STACK,
            ARCHIVE_LARGE_CRATE,
            ARCHIVE_LARGE_CRATE_STACK,
            ARCHIVE_BARREL,
            ARCHIVE_BARREL_STACK,
            ARCHIVE_MIXED_STACK_1,
            ARCHIVE_MIXED_STACK_2,
            ARCHIVE_MIXED_STACK_3);

    public static final RegistryEntry<Block> WAKE_FLOOR = themeSolid("wake_floor", MapColor.COLOR_PURPLE);
    public static final RegistryEntry<Block> WAKE_WALL = themeSolid("wake_wall", MapColor.COLOR_PURPLE);
    public static final RegistryEntry<Block> WAKE_ROOF = themeSolid("wake_roof", MapColor.COLOR_PURPLE);
    public static final RegistryEntry<Block> WAKE_TRIM = themeSolid("wake_trim", MapColor.COLOR_LIGHT_BLUE);
    public static final RegistryEntry<Block> GALLERY_FLOOR = themeSolid("gallery_floor", MapColor.STONE);
    public static final RegistryEntry<Block> GALLERY_WALL = themeSolid("gallery_wall", MapColor.STONE);
    public static final RegistryEntry<Block> GALLERY_ROOF = themeSolid("gallery_roof", MapColor.STONE);
    public static final RegistryEntry<Block> GALLERY_TRIM = themeSolid("gallery_trim", MapColor.SAND);
    public static final RegistryEntry<Block> DESCENT_FLOOR = themeSolid("descent_floor", MapColor.COLOR_ORANGE);
    public static final RegistryEntry<Block> DESCENT_WALL = themeSolid("descent_wall", MapColor.COLOR_ORANGE);
    public static final RegistryEntry<Block> DESCENT_ROOF = themeSolid("descent_roof", MapColor.COLOR_ORANGE);
    public static final RegistryEntry<Block> DESCENT_TRIM = themeSolid("descent_trim", MapColor.COLOR_GREEN);
    public static final RegistryEntry<Block> CHOIR_FLOOR = themeSolid("choir_floor", MapColor.SAND);
    public static final RegistryEntry<Block> CHOIR_WALL = themeSolid("choir_wall", MapColor.SAND);
    public static final RegistryEntry<Block> CHOIR_ROOF = themeSolid("choir_roof", MapColor.SAND);
    public static final RegistryEntry<Block> CHOIR_TRIM = themeSolid("choir_trim", MapColor.COLOR_ORANGE);
    public static final RegistryEntry<Block> VAULT_FLOOR = themeSolid("vault_floor", MapColor.COLOR_CYAN);
    public static final RegistryEntry<Block> VAULT_WALL = themeSolid("vault_wall", MapColor.COLOR_CYAN);
    public static final RegistryEntry<Block> VAULT_ROOF = themeSolid("vault_roof", MapColor.COLOR_CYAN);
    public static final RegistryEntry<Block> VAULT_TRIM = themeSolid("vault_trim", MapColor.COLOR_LIGHT_BLUE);
    public static final RegistryEntry<Block> CATALOGUE_FLOOR = themeSolid("catalogue_floor", MapColor.WOOD);
    public static final RegistryEntry<Block> CATALOGUE_WALL = themeSolid("catalogue_wall", MapColor.WOOD);
    public static final RegistryEntry<Block> CATALOGUE_ROOF = themeSolid("catalogue_roof", MapColor.WOOD);
    public static final RegistryEntry<Block> CATALOGUE_TRIM = themeSolid("catalogue_trim", MapColor.GOLD);
    public static final RegistryEntry<Block> LABYRINTH_FLOOR = themeSolid("labyrinth_floor", MapColor.COLOR_PURPLE);
    public static final RegistryEntry<Block> LABYRINTH_WALL = themeSolid("labyrinth_wall", MapColor.COLOR_PURPLE);
    public static final RegistryEntry<Block> LABYRINTH_ROOF = themeSolid("labyrinth_roof", MapColor.COLOR_PURPLE);
    public static final RegistryEntry<Block> LABYRINTH_TRIM = themeSolid("labyrinth_trim", MapColor.GOLD);
    public static final RegistryEntry<Block> UNWRITTEN_FLOOR = themeSolid("unwritten_floor", MapColor.COLOR_BLACK);
    public static final RegistryEntry<Block> UNWRITTEN_WALL = themeSolid("unwritten_wall", MapColor.COLOR_BLACK);
    public static final RegistryEntry<Block> UNWRITTEN_ROOF = themeSolid("unwritten_roof", MapColor.COLOR_BLACK);
    public static final RegistryEntry<Block> UNWRITTEN_TRIM = themeSolid("unwritten_trim", MapColor.GOLD);

    // Phases to a walk-through ghost on a timer, and noOcclusion is per-block
    // rather than per-state: without it neighbours cull against the solid state
    // and the phased state reads as a hole through the room.
    public static final RegistryEntry<ThemeHazardBlock> PARALLAX_PANEL = themeHazard(
            "parallax_panel", ThemeHazardBlock.Mode.PARALLAX_PANEL, MapColor.COLOR_PURPLE, true);
    public static final RegistryEntry<ThemeHazardBlock> LIGHT_DUST = themeHazard(
            "light_dust", ThemeHazardBlock.Mode.LIGHT_DUST, MapColor.SAND, true);
    public static final RegistryEntry<ThemeHazardBlock> COLLAPSING_MERIDIAN = themeHazard(
            "collapsing_meridian", ThemeHazardBlock.Mode.COLLAPSING_TILE, MapColor.COLOR_ORANGE, false);
    public static final RegistryEntry<ThemeHazardBlock> BRITTLE_ASH = themeHazard(
            "brittle_ash", ThemeHazardBlock.Mode.BRITTLE_ASH, MapColor.SAND, false);
    public static final RegistryEntry<ThemeHazardBlock> SHATTER_PANE = themeHazard(
            "shatter_pane", ThemeHazardBlock.Mode.SHATTER_PANE, MapColor.COLOR_LIGHT_BLUE, true);
    public static final RegistryEntry<ThemeHazardBlock> FALSE_SHELF = themeHazard(
            "false_shelf", ThemeHazardBlock.Mode.FALSE_SHELF, MapColor.WOOD, true);
    public static final RegistryEntry<ThemeHazardBlock> RESONANT_PLATE = themeHazard(
            "resonant_plate", ThemeHazardBlock.Mode.RESONANT_PLATE, MapColor.GOLD, false);
    public static final RegistryEntry<ThemeHazardBlock> INK_POOL = themeHazard(
            "ink_pool", ThemeHazardBlock.Mode.INK_POOL, MapColor.COLOR_BLACK, true);

    public static final List<RegistryEntry<? extends Block>> THEME_PALETTE_BLOCKS = List.of(
            WAKE_FLOOR, WAKE_WALL, WAKE_ROOF, WAKE_TRIM,
            GALLERY_FLOOR, GALLERY_WALL, GALLERY_ROOF, GALLERY_TRIM,
            DESCENT_FLOOR, DESCENT_WALL, DESCENT_ROOF, DESCENT_TRIM,
            CHOIR_FLOOR, CHOIR_WALL, CHOIR_ROOF, CHOIR_TRIM,
            VAULT_FLOOR, VAULT_WALL, VAULT_ROOF, VAULT_TRIM,
            CATALOGUE_FLOOR, CATALOGUE_WALL, CATALOGUE_ROOF, CATALOGUE_TRIM,
            LABYRINTH_FLOOR, LABYRINTH_WALL, LABYRINTH_ROOF, LABYRINTH_TRIM,
            UNWRITTEN_FLOOR, UNWRITTEN_WALL, UNWRITTEN_ROOF, UNWRITTEN_TRIM,
            PARALLAX_PANEL, LIGHT_DUST, COLLAPSING_MERIDIAN, BRITTLE_ASH,
            SHATTER_PANE, FALSE_SHELF, RESONANT_PLATE, INK_POOL);

    private static final Set<String> GRAVEYARD_FLOOR_FLUFF = Set.of(
            "grave_flowers",
            "dead_grave_flowers",
            "wilted_grave_flowers",
            "skeleton_arm",
            "skeleton_leg");

    private static final String[] GRAVEYARD_PROP_IDS = {
        "broken_wooden_grave_cross",
        "damaged_wooden_grave_cross",
        "dead_grave_flower_vase",
        "dead_grave_flowers",
        "grave_flower_vase",
        "grave_flowers",
        "gravestone_type_1",
        "gravestone_type_2",
        "gravestone_type_3",
        "gravestone_type_4",
        "gravestone_type_5",
        "gravestone_type_6",
        "gravestone_type_7",
        "gravestone_type_8",
        "gravestone_type_9",
        "gravestone_type_10",
        "gravestone_type_11",
        "gravestone_type_12",
        "gravestone_type_13",
        "gravestone_type_14",
        "gravestone_type_15",
        "gravestone_type_16",
        "iron_cemetery_gate_arch",
        "iron_fence_1",
        "iron_fence_2",
        "lying_skeleton",
        "sitting_skeleton",
        "skeleton_arm",
        "skeleton_leg",
        "skeleton_skull",
        "skeleton_torso",
        "wilted_grave_flower_vase",
        "wilted_grave_flowers",
        "wooden_coffin",
        "wooden_coffin_lid",
        "wooden_coffin_skeleton",
        "wooden_grave_cross",
        "wooden_open_coffin"
    };

    public static final List<RegistryEntry<GraveyardPropBlock>> GRAVEYARD_PROPS = registerGraveyardProps();

    private ModBlocks() {
    }

    /** Touching this class is what queues every block; the loader flushes later. */
    public static void register() {
    }

    private static RegistryEntry<ArchiveCrateBlock> crate(String name) {
        return ModRegistries.BLOCKS.registerBlock(
                name,
                ArchiveCrateBlock::new,
                properties -> properties
                        .mapColor(MapColor.WOOD)
                        .strength(0.8F, 1.5F)
                        .noOcclusion());
    }

    private static List<RegistryEntry<GraveyardPropBlock>> registerGraveyardProps() {
        List<RegistryEntry<GraveyardPropBlock>> props = new ArrayList<>(GRAVEYARD_PROP_IDS.length);
        for (String id : GRAVEYARD_PROP_IDS) {
            props.add(graveyardProp(id));
        }
        return List.copyOf(props);
    }

    private static RegistryEntry<GraveyardPropBlock> graveyardProp(String name) {
        boolean floorFluff = GRAVEYARD_FLOOR_FLUFF.contains(name);
        MapColor mapColor = graveyardMapColor(name);
        return ModRegistries.BLOCKS.registerBlock(
                name,
                GraveyardPropBlock::new,
                properties -> {
                    BlockBehaviour.Properties configured = properties
                            .mapColor(mapColor)
                            .strength(0.8F, 1.5F)
                            .noOcclusion();
                    if (floorFluff) {
                        configured = configured.noCollission();
                    }
                    return configured;
                });
    }

    private static MapColor graveyardMapColor(String name) {
        if (name.contains("flower")) {
            return MapColor.PLANT;
        }
        if (name.contains("skeleton") || name.contains("lying") || name.contains("sitting")) {
            return MapColor.SAND;
        }
        if (name.contains("iron") || name.contains("fence") || name.contains("gate")) {
            return MapColor.METAL;
        }
        if (name.contains("gravestone")) {
            return MapColor.STONE;
        }
        return MapColor.WOOD;
    }

    private static RegistryEntry<Block> themeSolid(String name, MapColor color) {
        return ModRegistries.BLOCKS.registerSimpleBlock(name, properties -> properties.mapColor(color).strength(2.1F, 6.5F));
    }

    private static RegistryEntry<ThemeHazardBlock> themeHazard(
            String name, ThemeHazardBlock.Mode mode, MapColor color, boolean noOcclusion) {
        return ModRegistries.BLOCKS.registerBlock(
                name,
                properties -> new ThemeHazardBlock(mode, properties),
                properties -> {
                    BlockBehaviour.Properties configured = properties.mapColor(color).strength(1.4F, 4.0F);
                    if (noOcclusion) {
                        configured = configured.noOcclusion();
                    }
                    if (mode == ThemeHazardBlock.Mode.LIGHT_DUST || mode == ThemeHazardBlock.Mode.INK_POOL) {
                        configured = configured.noCollission().lightLevel(state -> mode == ThemeHazardBlock.Mode.LIGHT_DUST ? 2 : 1);
                    }
                    if (mode == ThemeHazardBlock.Mode.SHATTER_PANE) {
                        configured = configured.strength(0.4F).noOcclusion();
                    }
                    return configured;
                });
    }
}
