package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.block.GraveyardPropBlock;
import com.nightbeam.tbos.item.ArchiveSurveyMapItem;
import com.nightbeam.tbos.item.MemoryPlateItem;
import com.nightbeam.tbos.item.MemoryScene;
import com.nightbeam.tbos.item.YesterglassLensItem;
import com.nightbeam.tbos.item.RecalledHourItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Yesterglass.MOD_ID);

    public static final DeferredItem<Item> CRACKED_YESTERGLASS_LENS = ITEMS.registerSimpleItem(
            "cracked_yesterglass_lens",
            properties -> properties
                    .stacksTo(1)
                    .component(
                            DataComponents.LORE,
                            new ItemLore(List.of(Component.translatable(
                                    "item.tbos.cracked_yesterglass_lens.tooltip")))));

    public static final DeferredItem<YesterglassLensItem> YESTERGLASS_LENS = ITEMS.registerItem(
            "yesterglass_lens",
            YesterglassLensItem::new,
            properties -> properties
                    .stacksTo(1)
                    .component(
                            DataComponents.LORE,
                            new ItemLore(List.of(Component.translatable("item.tbos.yesterglass_lens.tooltip")))));
    public static final DeferredItem<Item> CURATOR_CORE = ITEMS.registerSimpleItem(
            "curator_core",
            properties -> properties
                    .stacksTo(1)
                    .component(
                            DataComponents.LORE,
                            new ItemLore(List.of(Component.translatable("item.tbos.curator_core.tooltip")))));
    public static final DeferredItem<Item> CHRONICLE_SHARD = ITEMS.registerSimpleItem(
            "chronicle_shard",
            properties -> properties.component(
                    DataComponents.LORE,
                    new ItemLore(List.of(Component.translatable("item.tbos.chronicle_shard.tooltip")))));
    public static final DeferredItem<RecalledHourItem> RECALLED_HOUR = ITEMS.registerItem(
            "recalled_hour",
            RecalledHourItem::new,
            properties -> properties
                    .stacksTo(4)
                    .component(
                            DataComponents.LORE,
                            new ItemLore(List.of(Component.translatable("item.tbos.recalled_hour.tooltip")))));
    public static final DeferredItem<Item> CANTOR_SIGIL = ITEMS.registerSimpleItem(
            "cantor_sigil",
            properties -> properties
                    .stacksTo(1)
                    .component(
                            DataComponents.LORE,
                            new ItemLore(List.of(Component.translatable("item.tbos.cantor_sigil.tooltip")))));
    public static final DeferredItem<ArchiveSurveyMapItem> ARCHIVE_SURVEY_MAP = ITEMS.registerItem(
            "archive_survey_map",
            ArchiveSurveyMapItem::new,
            properties -> properties
                    .stacksTo(1)
                    .component(
                            DataComponents.LORE,
                            new ItemLore(List.of(Component.translatable(
                                    "item.tbos.archive_survey_map.tooltip")))));
    public static final DeferredItem<MemoryPlateItem> MEMORY_PLATE = ITEMS.registerItem(
            "memory_plate",
            MemoryPlateItem::new,
            properties -> properties
                    .stacksTo(1)
                    .component(ModDataComponents.MEMORY_SCENE.get(), MemoryScene.ASTRONOMERS));
    public static final DeferredItem<BlockItem> ARCHIVE_STONE = ITEMS.registerSimpleBlockItem("archive_stone", ModBlocks.ARCHIVE_STONE);
    public static final DeferredItem<BlockItem> CRACKED_ARCHIVE_STONE =
            ITEMS.registerSimpleBlockItem("cracked_archive_stone", ModBlocks.CRACKED_ARCHIVE_STONE);
    public static final DeferredItem<BlockItem> ARCHIVE_BRICKS =
            ITEMS.registerSimpleBlockItem("archive_bricks", ModBlocks.ARCHIVE_BRICKS);
    public static final DeferredItem<BlockItem> WEATHERED_ARCHIVE_BRICKS =
            ITEMS.registerSimpleBlockItem("weathered_archive_bricks", ModBlocks.WEATHERED_ARCHIVE_BRICKS);
    public static final DeferredItem<BlockItem> MOSSY_ARCHIVE_STONE =
            ITEMS.registerSimpleBlockItem("mossy_archive_stone", ModBlocks.MOSSY_ARCHIVE_STONE);
    public static final DeferredItem<BlockItem> CHISELED_ARCHIVE_STONE =
            ITEMS.registerSimpleBlockItem("chiseled_archive_stone", ModBlocks.CHISELED_ARCHIVE_STONE);
    public static final DeferredItem<BlockItem> CHRONICLE_TILE =
            ITEMS.registerSimpleBlockItem("chronicle_tile", ModBlocks.CHRONICLE_TILE);
    public static final DeferredItem<BlockItem> CANTOR_FLOOR =
            ITEMS.registerSimpleBlockItem("cantor_floor", ModBlocks.CANTOR_FLOOR);
    public static final DeferredItem<BlockItem> CANTOR_WALL =
            ITEMS.registerSimpleBlockItem("cantor_wall", ModBlocks.CANTOR_WALL);
    public static final DeferredItem<BlockItem> CANTOR_RUNE =
            ITEMS.registerSimpleBlockItem("cantor_rune", ModBlocks.CANTOR_RUNE);
    public static final DeferredItem<BlockItem> MERIDIAN_TILE =
            ITEMS.registerSimpleBlockItem("meridian_tile", ModBlocks.MERIDIAN_TILE);
    public static final DeferredItem<BlockItem> ENGRAVED_MERIDIAN_TILE =
            ITEMS.registerSimpleBlockItem("engraved_meridian_tile", ModBlocks.ENGRAVED_MERIDIAN_TILE);
    public static final DeferredItem<BlockItem> YESTERGLASS =
            ITEMS.registerSimpleBlockItem("yesterglass", ModBlocks.YESTERGLASS);
    public static final DeferredItem<BlockItem> MEMORY_ANCHOR = ITEMS.registerSimpleBlockItem("memory_anchor", ModBlocks.MEMORY_ANCHOR);
    public static final DeferredItem<BlockItem> PHASE_PLATFORM = ITEMS.registerSimpleBlockItem("phase_platform", ModBlocks.PHASE_PLATFORM);
    public static final DeferredItem<BlockItem> RESONANCE_LAMP =
            ITEMS.registerSimpleBlockItem("resonance_lamp", ModBlocks.RESONANCE_LAMP);
    public static final DeferredItem<BlockItem> CHRONICLE_BRONZE =
            ITEMS.registerSimpleBlockItem("chronicle_bronze", ModBlocks.CHRONICLE_BRONZE);
    public static final DeferredItem<BlockItem> LENSWORK_CRYSTAL =
            ITEMS.registerSimpleBlockItem("lenswork_crystal", ModBlocks.LENSWORK_CRYSTAL);
    public static final DeferredItem<BlockItem> ARCHIVE_SEAL =
            ITEMS.registerSimpleBlockItem("archive_seal", ModBlocks.ARCHIVE_SEAL);
    public static final DeferredItem<BlockItem> ARCHIVE_CACHE =
            ITEMS.registerSimpleBlockItem("archive_cache", ModBlocks.ARCHIVE_CACHE);
    public static final DeferredItem<BlockItem> FRACTURE_COFFER =
            ITEMS.registerSimpleBlockItem("fracture_coffer", ModBlocks.FRACTURE_COFFER);
    public static final DeferredItem<BlockItem> ALIGNMENT_DIAL =
            ITEMS.registerSimpleBlockItem("alignment_dial", ModBlocks.ALIGNMENT_DIAL);
    public static final DeferredItem<BlockItem> RESONANT_BELL =
            ITEMS.registerSimpleBlockItem("resonant_bell", ModBlocks.RESONANT_BELL);
    public static final DeferredItem<BlockItem> MERIDIAN_RELAY =
            ITEMS.registerSimpleBlockItem("meridian_relay", ModBlocks.MERIDIAN_RELAY);
    public static final DeferredItem<BlockItem> ARCHIVE_CORE =
            ITEMS.registerSimpleBlockItem("archive_core", ModBlocks.ARCHIVE_CORE);
    public static final DeferredItem<BlockItem> MEMORY_LANTERN =
            ITEMS.registerSimpleBlockItem("memory_lantern", ModBlocks.MEMORY_LANTERN);
    public static final DeferredItem<BlockItem> RIFT_THRESHOLD =
            ITEMS.registerSimpleBlockItem("rift_threshold", ModBlocks.RIFT_THRESHOLD);
    public static final DeferredItem<BlockItem> ARCHIVE_CRATE =
            ITEMS.registerSimpleBlockItem("archive_crate", ModBlocks.ARCHIVE_CRATE);
    public static final DeferredItem<BlockItem> ARCHIVE_CRATE_STACK =
            ITEMS.registerSimpleBlockItem("archive_crate_stack", ModBlocks.ARCHIVE_CRATE_STACK);
    public static final DeferredItem<BlockItem> ARCHIVE_LARGE_CRATE =
            ITEMS.registerSimpleBlockItem("archive_large_crate", ModBlocks.ARCHIVE_LARGE_CRATE);
    public static final DeferredItem<BlockItem> ARCHIVE_LARGE_CRATE_STACK =
            ITEMS.registerSimpleBlockItem("archive_large_crate_stack", ModBlocks.ARCHIVE_LARGE_CRATE_STACK);
    public static final DeferredItem<BlockItem> ARCHIVE_BARREL =
            ITEMS.registerSimpleBlockItem("archive_barrel", ModBlocks.ARCHIVE_BARREL);
    public static final DeferredItem<BlockItem> ARCHIVE_BARREL_STACK =
            ITEMS.registerSimpleBlockItem("archive_barrel_stack", ModBlocks.ARCHIVE_BARREL_STACK);
    public static final DeferredItem<BlockItem> ARCHIVE_MIXED_STACK_1 =
            ITEMS.registerSimpleBlockItem("archive_mixed_stack_1", ModBlocks.ARCHIVE_MIXED_STACK_1);
    public static final DeferredItem<BlockItem> ARCHIVE_MIXED_STACK_2 =
            ITEMS.registerSimpleBlockItem("archive_mixed_stack_2", ModBlocks.ARCHIVE_MIXED_STACK_2);
    public static final DeferredItem<BlockItem> ARCHIVE_MIXED_STACK_3 =
            ITEMS.registerSimpleBlockItem("archive_mixed_stack_3", ModBlocks.ARCHIVE_MIXED_STACK_3);

    public static final List<DeferredItem<BlockItem>> GRAVEYARD_PROP_ITEMS = registerGraveyardPropItems();

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static void addCreativeTabItems(CreativeModeTab.Output output) {
        for (var entry : ITEMS.getEntries()) {
            if (entry == MEMORY_PLATE) {
                for (MemoryScene scene : MemoryScene.values()) {
                    output.accept(MemoryPlateItem.forScene(scene));
                }
            } else {
                output.accept(entry.get());
            }
        }
    }

    private static List<DeferredItem<BlockItem>> registerGraveyardPropItems() {
        List<DeferredItem<BlockItem>> items = new ArrayList<>(ModBlocks.GRAVEYARD_PROPS.size());
        for (DeferredBlock<GraveyardPropBlock> prop : ModBlocks.GRAVEYARD_PROPS) {
            items.add(ITEMS.registerSimpleBlockItem(prop.getId().getPath(), prop));
        }
        return List.copyOf(items);
    }
}
