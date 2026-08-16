package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.block.GraveyardPropBlock;
import com.nightbeam.tbos.item.ArchiveSurveyMapItem;
import com.nightbeam.tbos.item.MemoryPlateItem;
import com.nightbeam.tbos.item.MemoryScene;
import com.nightbeam.tbos.item.YesterglassLensItem;
import com.nightbeam.tbos.item.RecalledHourItem;
import com.nightbeam.tbos.item.StarterTomeItem;
import com.nightbeam.tbos.platform.registry.ModRegistries;
import com.nightbeam.tbos.platform.registry.RegistryEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;

public final class ModItems {
    public static final RegistryEntry<Item> CRACKED_YESTERGLASS_LENS = ModRegistries.ITEMS.registerSimpleItem(
            "cracked_yesterglass_lens",
            properties -> properties
                    .stacksTo(1)
                    .component(
                            DataComponents.LORE,
                            new ItemLore(List.of(Component.translatable(
                                    "item.tbos.cracked_yesterglass_lens.tooltip")))));

    public static final RegistryEntry<YesterglassLensItem> YESTERGLASS_LENS = ModRegistries.ITEMS.registerItem(
            "yesterglass_lens",
            YesterglassLensItem::new,
            properties -> properties
                    .stacksTo(1)
                    .component(
                            DataComponents.LORE,
                            new ItemLore(List.of(Component.translatable("item.tbos.yesterglass_lens.tooltip")))));
    public static final RegistryEntry<Item> CURATOR_CORE = ModRegistries.ITEMS.registerSimpleItem(
            "curator_core",
            properties -> properties
                    .stacksTo(1)
                    .component(
                            DataComponents.LORE,
                            new ItemLore(List.of(Component.translatable("item.tbos.curator_core.tooltip")))));
    public static final RegistryEntry<StarterTomeItem> STARTER_TOME = ModRegistries.ITEMS.registerItem(
            "starter_tome",
            StarterTomeItem::new,
            properties -> properties
                    .stacksTo(1)
                    .component(
                            DataComponents.LORE,
                            new ItemLore(List.of(Component.translatable("item.tbos.starter_tome.tooltip")))));
    public static final RegistryEntry<Item> CHRONICLE_SHARD = ModRegistries.ITEMS.registerSimpleItem(
            "chronicle_shard",
            properties -> properties.component(
                    DataComponents.LORE,
                    new ItemLore(List.of(Component.translatable("item.tbos.chronicle_shard.tooltip")))));
    public static final RegistryEntry<RecalledHourItem> RECALLED_HOUR = ModRegistries.ITEMS.registerItem(
            "recalled_hour",
            RecalledHourItem::new,
            properties -> properties
                    .stacksTo(4)
                    .component(
                            DataComponents.LORE,
                            new ItemLore(List.of(Component.translatable("item.tbos.recalled_hour.tooltip")))));
    public static final RegistryEntry<Item> CANTOR_SIGIL = ModRegistries.ITEMS.registerSimpleItem(
            "cantor_sigil",
            properties -> properties
                    .stacksTo(1)
                    .component(
                            DataComponents.LORE,
                            new ItemLore(List.of(Component.translatable("item.tbos.cantor_sigil.tooltip")))));
    public static final RegistryEntry<ArchiveSurveyMapItem> ARCHIVE_SURVEY_MAP = ModRegistries.ITEMS.registerItem(
            "archive_survey_map",
            ArchiveSurveyMapItem::new,
            properties -> properties
                    .stacksTo(1)
                    .component(
                            DataComponents.LORE,
                            new ItemLore(List.of(Component.translatable(
                                    "item.tbos.archive_survey_map.tooltip")))));
    public static final RegistryEntry<Item> ARCHIVISTS_JOURNAL = ModRegistries.ITEMS.registerSimpleItem(
            "archivists_journal",
            properties -> properties
                    .stacksTo(1)
                    .component(
                            DataComponents.LORE,
                            new ItemLore(List.of(Component.translatable(
                                    "item.tbos.archivists_journal.tooltip")))));
    public static final RegistryEntry<MemoryPlateItem> MEMORY_PLATE = ModRegistries.ITEMS.registerItem(
            "memory_plate",
            MemoryPlateItem::new,
            properties -> properties
                    .stacksTo(1)
                    .component(ModDataComponents.MEMORY_SCENE.get(), MemoryScene.ASTRONOMERS));
    public static final RegistryEntry<BlockItem> ARCHIVE_STONE = ModRegistries.ITEMS.registerSimpleBlockItem("archive_stone", ModBlocks.ARCHIVE_STONE);
    public static final RegistryEntry<BlockItem> CRACKED_ARCHIVE_STONE =
            ModRegistries.ITEMS.registerSimpleBlockItem("cracked_archive_stone", ModBlocks.CRACKED_ARCHIVE_STONE);
    public static final RegistryEntry<BlockItem> ARCHIVE_BRICKS =
            ModRegistries.ITEMS.registerSimpleBlockItem("archive_bricks", ModBlocks.ARCHIVE_BRICKS);
    public static final RegistryEntry<BlockItem> WEATHERED_ARCHIVE_BRICKS =
            ModRegistries.ITEMS.registerSimpleBlockItem("weathered_archive_bricks", ModBlocks.WEATHERED_ARCHIVE_BRICKS);
    public static final RegistryEntry<BlockItem> MOSSY_ARCHIVE_STONE =
            ModRegistries.ITEMS.registerSimpleBlockItem("mossy_archive_stone", ModBlocks.MOSSY_ARCHIVE_STONE);
    public static final RegistryEntry<BlockItem> CHISELED_ARCHIVE_STONE =
            ModRegistries.ITEMS.registerSimpleBlockItem("chiseled_archive_stone", ModBlocks.CHISELED_ARCHIVE_STONE);
    public static final RegistryEntry<BlockItem> CHRONICLE_TILE =
            ModRegistries.ITEMS.registerSimpleBlockItem("chronicle_tile", ModBlocks.CHRONICLE_TILE);
    public static final RegistryEntry<BlockItem> CANTOR_FLOOR =
            ModRegistries.ITEMS.registerSimpleBlockItem("cantor_floor", ModBlocks.CANTOR_FLOOR);
    public static final RegistryEntry<BlockItem> CANTOR_WALL =
            ModRegistries.ITEMS.registerSimpleBlockItem("cantor_wall", ModBlocks.CANTOR_WALL);
    public static final RegistryEntry<BlockItem> CANTOR_RUNE =
            ModRegistries.ITEMS.registerSimpleBlockItem("cantor_rune", ModBlocks.CANTOR_RUNE);
    public static final RegistryEntry<BlockItem> MERIDIAN_TILE =
            ModRegistries.ITEMS.registerSimpleBlockItem("meridian_tile", ModBlocks.MERIDIAN_TILE);
    public static final RegistryEntry<BlockItem> ENGRAVED_MERIDIAN_TILE =
            ModRegistries.ITEMS.registerSimpleBlockItem("engraved_meridian_tile", ModBlocks.ENGRAVED_MERIDIAN_TILE);
    public static final RegistryEntry<BlockItem> YESTERGLASS =
            ModRegistries.ITEMS.registerSimpleBlockItem("yesterglass", ModBlocks.YESTERGLASS);
    public static final RegistryEntry<BlockItem> MEMORY_ANCHOR = ModRegistries.ITEMS.registerSimpleBlockItem("memory_anchor", ModBlocks.MEMORY_ANCHOR);
    public static final RegistryEntry<BlockItem> WAYSTONE = ModRegistries.ITEMS.registerSimpleBlockItem("waystone", ModBlocks.WAYSTONE);
    public static final RegistryEntry<BlockItem> PHASE_PLATFORM = ModRegistries.ITEMS.registerSimpleBlockItem("phase_platform", ModBlocks.PHASE_PLATFORM);
    public static final RegistryEntry<BlockItem> RESONANCE_LAMP =
            ModRegistries.ITEMS.registerSimpleBlockItem("resonance_lamp", ModBlocks.RESONANCE_LAMP);
    public static final RegistryEntry<BlockItem> CHRONICLE_BRONZE =
            ModRegistries.ITEMS.registerSimpleBlockItem("chronicle_bronze", ModBlocks.CHRONICLE_BRONZE);
    public static final RegistryEntry<BlockItem> LENSWORK_CRYSTAL =
            ModRegistries.ITEMS.registerSimpleBlockItem("lenswork_crystal", ModBlocks.LENSWORK_CRYSTAL);
    public static final RegistryEntry<BlockItem> ARCHIVE_SEAL =
            ModRegistries.ITEMS.registerSimpleBlockItem("archive_seal", ModBlocks.ARCHIVE_SEAL);
    public static final RegistryEntry<BlockItem> ARCHIVE_CACHE =
            ModRegistries.ITEMS.registerSimpleBlockItem("archive_cache", ModBlocks.ARCHIVE_CACHE);
    public static final RegistryEntry<BlockItem> FRACTURE_COFFER =
            ModRegistries.ITEMS.registerSimpleBlockItem("fracture_coffer", ModBlocks.FRACTURE_COFFER);
    public static final RegistryEntry<BlockItem> ALIGNMENT_DIAL =
            ModRegistries.ITEMS.registerSimpleBlockItem("alignment_dial", ModBlocks.ALIGNMENT_DIAL);
    public static final RegistryEntry<BlockItem> RESONANT_BELL =
            ModRegistries.ITEMS.registerSimpleBlockItem("resonant_bell", ModBlocks.RESONANT_BELL);
    public static final RegistryEntry<BlockItem> MERIDIAN_RELAY =
            ModRegistries.ITEMS.registerSimpleBlockItem("meridian_relay", ModBlocks.MERIDIAN_RELAY);
    public static final RegistryEntry<BlockItem> ARCHIVE_CORE =
            ModRegistries.ITEMS.registerSimpleBlockItem("archive_core", ModBlocks.ARCHIVE_CORE);
    public static final RegistryEntry<BlockItem> MEMORY_LANTERN =
            ModRegistries.ITEMS.registerSimpleBlockItem("memory_lantern", ModBlocks.MEMORY_LANTERN);
    public static final RegistryEntry<BlockItem> RIFT_THRESHOLD =
            ModRegistries.ITEMS.registerSimpleBlockItem("rift_threshold", ModBlocks.RIFT_THRESHOLD);
    public static final RegistryEntry<BlockItem> ARCHIVE_CRATE =
            ModRegistries.ITEMS.registerSimpleBlockItem("archive_crate", ModBlocks.ARCHIVE_CRATE);
    public static final RegistryEntry<BlockItem> ARCHIVE_CRATE_STACK =
            ModRegistries.ITEMS.registerSimpleBlockItem("archive_crate_stack", ModBlocks.ARCHIVE_CRATE_STACK);
    public static final RegistryEntry<BlockItem> ARCHIVE_LARGE_CRATE =
            ModRegistries.ITEMS.registerSimpleBlockItem("archive_large_crate", ModBlocks.ARCHIVE_LARGE_CRATE);
    public static final RegistryEntry<BlockItem> ARCHIVE_LARGE_CRATE_STACK =
            ModRegistries.ITEMS.registerSimpleBlockItem("archive_large_crate_stack", ModBlocks.ARCHIVE_LARGE_CRATE_STACK);
    public static final RegistryEntry<BlockItem> ARCHIVE_BARREL =
            ModRegistries.ITEMS.registerSimpleBlockItem("archive_barrel", ModBlocks.ARCHIVE_BARREL);
    public static final RegistryEntry<BlockItem> ARCHIVE_BARREL_STACK =
            ModRegistries.ITEMS.registerSimpleBlockItem("archive_barrel_stack", ModBlocks.ARCHIVE_BARREL_STACK);
    public static final RegistryEntry<BlockItem> ARCHIVE_MIXED_STACK_1 =
            ModRegistries.ITEMS.registerSimpleBlockItem("archive_mixed_stack_1", ModBlocks.ARCHIVE_MIXED_STACK_1);
    public static final RegistryEntry<BlockItem> ARCHIVE_MIXED_STACK_2 =
            ModRegistries.ITEMS.registerSimpleBlockItem("archive_mixed_stack_2", ModBlocks.ARCHIVE_MIXED_STACK_2);
    public static final RegistryEntry<BlockItem> ARCHIVE_MIXED_STACK_3 =
            ModRegistries.ITEMS.registerSimpleBlockItem("archive_mixed_stack_3", ModBlocks.ARCHIVE_MIXED_STACK_3);

    static {
        for (RegistryEntry<? extends Block> block : ModBlocks.THEME_PALETTE_BLOCKS) {
            ModRegistries.ITEMS.registerSimpleBlockItem(block.id().getPath(), block);
        }
    }

    public static final List<RegistryEntry<BlockItem>> GRAVEYARD_PROP_ITEMS = registerGraveyardPropItems();

    private ModItems() {
    }

    /** Touching this class is what queues every item; the loader flushes later. */
    public static void register() {
    }

    public static void addCreativeTabItems(CreativeModeTab.Output output) {
        for (var entry : ModRegistries.ITEMS.entries()) {
            if (entry.id().equals(MEMORY_PLATE.id())) {
                for (MemoryScene scene : MemoryScene.values()) {
                    output.accept(MemoryPlateItem.forScene(scene));
                }
            } else {
                output.accept(entry.get());
            }
        }
    }

    private static List<RegistryEntry<BlockItem>> registerGraveyardPropItems() {
        List<RegistryEntry<BlockItem>> items = new ArrayList<>(ModBlocks.GRAVEYARD_PROPS.size());
        for (RegistryEntry<GraveyardPropBlock> prop : ModBlocks.GRAVEYARD_PROPS) {
            items.add(ModRegistries.ITEMS.registerSimpleBlockItem(prop.id().getPath(), prop));
        }
        return List.copyOf(items);
    }
}
