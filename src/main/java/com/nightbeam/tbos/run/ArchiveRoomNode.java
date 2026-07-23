package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/** Complete persisted metadata for one node in the generated dungeon graph. */
public record ArchiveRoomNode(
        int index,
        Identifier templateId,
        ArchiveRoomCategory category,
        ArchiveRoomPlacement placement,
        List<ArchiveConnection> connections,
        int graphDepth,
        int difficulty,
        List<Identifier> allowedLootTables,
        List<Identifier> allowedMonsterGroups,
        List<ArchiveRoomModifier> modifiers,
        ArchiveRoomRuntimeState runtime) {
    public static final Codec<ArchiveRoomNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("index").forGetter(ArchiveRoomNode::index),
            Identifier.CODEC.fieldOf("template_id").forGetter(ArchiveRoomNode::templateId),
            ArchiveRoomCategory.CODEC.fieldOf("category").forGetter(ArchiveRoomNode::category),
            ArchiveRoomPlacement.CODEC.fieldOf("placement").forGetter(ArchiveRoomNode::placement),
            ArchiveConnection.CODEC.listOf().optionalFieldOf("connections", List.of())
                    .forGetter(ArchiveRoomNode::connections),
            Codec.INT.optionalFieldOf("graph_depth", 0).forGetter(ArchiveRoomNode::graphDepth),
            Codec.INT.optionalFieldOf("difficulty", 1).forGetter(ArchiveRoomNode::difficulty),
            Identifier.CODEC.listOf().optionalFieldOf("allowed_loot_tables", List.of())
                    .forGetter(ArchiveRoomNode::allowedLootTables),
            Identifier.CODEC.listOf().optionalFieldOf("allowed_monster_groups", List.of())
                    .forGetter(ArchiveRoomNode::allowedMonsterGroups),
            ArchiveRoomModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of())
                    .forGetter(ArchiveRoomNode::modifiers),
            ArchiveRoomRuntimeState.CODEC.optionalFieldOf("runtime", ArchiveRoomRuntimeState.UNVISITED)
                    .forGetter(ArchiveRoomNode::runtime)
    ).apply(instance, ArchiveRoomNode::new));

    public ArchiveRoomNode {
        templateId = Objects.requireNonNull(templateId, "templateId");
        category = Objects.requireNonNull(category, "category");
        placement = Objects.requireNonNull(placement, "placement");
        connections = List.copyOf(connections);
        allowedLootTables = List.copyOf(allowedLootTables);
        allowedMonsterGroups = List.copyOf(allowedMonsterGroups);
        modifiers = List.copyOf(modifiers);
        runtime = Objects.requireNonNull(runtime, "runtime");
        if (index < 0 || index > 63) {
            throw new IllegalArgumentException("Archive room index must be between 0 and 63: " + index);
        }
        if (graphDepth < 0 || graphDepth > 63 || difficulty < 1 || difficulty > 100) {
            throw new IllegalArgumentException("Archive room depth or difficulty is out of bounds");
        }
        if (connections.stream().map(ArchiveConnection::direction).distinct().count() != connections.size()) {
            throw new IllegalArgumentException("Archive room contains two connections in the same direction: " + index);
        }
    }

    public ArchiveRoomNode withConnections(List<ArchiveConnection> value) {
        return new ArchiveRoomNode(index, templateId, category, placement, value, graphDepth, difficulty,
                allowedLootTables, allowedMonsterGroups, modifiers, runtime);
    }

    public ArchiveRoomNode withRuntime(ArchiveRoomRuntimeState value) {
        return new ArchiveRoomNode(index, templateId, category, placement, connections, graphDepth, difficulty,
                allowedLootTables, allowedMonsterGroups, modifiers, value);
    }

    public ArchiveEncounterKind encounterKind() {
        return switch (category) {
            case STARTING, SANCTUARY, LORE, MERCHANT, VERTICAL_SHAFT, ANCIENT_LIBRARY -> ArchiveEncounterKind.EXPLORATION;
            case TREASURE, SECRET, EXIT_REWARD -> ArchiveEncounterKind.REWARD;
            case TRAP -> ArchiveEncounterKind.TRAP;
            case PUZZLE -> templateId.getPath().contains("choir") ? ArchiveEncounterKind.CHOIR : ArchiveEncounterKind.HALL;
            case STANDARD_COMBAT -> ArchiveEncounterKind.SKIRMISH;
            case ELITE_COMBAT, CURSED -> ArchiveEncounterKind.HUNT;
            case MINI_BOSS -> ArchiveEncounterKind.GUARDIAN;
            case FINAL_BOSS -> ArchiveEncounterKind.BOSS;
        };
    }

    public ArchiveRoomPlan toPlan(long dungeonSeed) {
        return new ArchiveRoomPlan(
                templateId,
                Math.min(2, graphDepth / 5),
                index,
                ArchiveRunGenerator.encounterSeedFor(dungeonSeed, index, templateId.hashCode()),
                encounterKind());
    }
}
