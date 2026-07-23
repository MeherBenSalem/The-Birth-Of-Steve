package com.nightbeam.tbos.site;

import com.nightbeam.tbos.Yesterglass;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public final class BuiltInTemporalSites {
    public static final Identifier PARALLAX_ATRIUM_ID =
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "parallax_atrium");
    public static final Identifier HALL_OF_ALIGNMENT_ID =
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "hall_of_alignment");
    public static final Identifier CHOIR_OF_HOURS_ID =
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "choir_of_hours");
    public static final Identifier BROKEN_MERIDIAN_ID =
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "broken_meridian");
    public static final Identifier GRAND_ORRERY_ID =
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "grand_orrery");

    private static final Map<Identifier, TemporalSiteDefinition> DEFINITIONS = createDefinitions();

    private BuiltInTemporalSites() {
    }

    public static Optional<TemporalSiteDefinition> get(Identifier id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    public static TemporalSiteDefinition require(Identifier id) {
        TemporalSiteDefinition definition = DEFINITIONS.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown temporal site definition: " + id);
        }
        return definition;
    }

    public static TemporalSiteDefinition parallaxAtrium() {
        return require(PARALLAX_ATRIUM_ID);
    }

    public static TemporalSiteDefinition hallOfAlignment() {
        return require(HALL_OF_ALIGNMENT_ID);
    }

    public static TemporalSiteDefinition choirOfHours() {
        return require(CHOIR_OF_HOURS_ID);
    }

    public static TemporalSiteDefinition brokenMeridian() {
        return require(BROKEN_MERIDIAN_ID);
    }

    public static TemporalSiteDefinition grandOrrery() {
        return require(GRAND_ORRERY_ID);
    }

    private static Map<Identifier, TemporalSiteDefinition> createDefinitions() {
        Map<Identifier, TemporalSiteDefinition> definitions = new LinkedHashMap<>();
        List<BlockPos> staircase = new ArrayList<>();
        for (int x = 7; x <= 8; x++) {
            staircase.add(new BlockPos(x, 1, 8));
            staircase.add(new BlockPos(x, 2, 9));
            staircase.add(new BlockPos(x, 3, 10));
            for (int z = 11; z <= 14; z++) {
                staircase.add(new BlockPos(x, 3, z));
            }
        }
        TemporalSiteDefinition parallaxAtrium = new TemporalSiteDefinition(
                PARALLAX_ATRIUM_ID,
                16,
                16,
                -1,
                8,
                new BlockPos(8, 2, 8),
                new BlockPos(8, 1, 4),
                staircase,
                List.of(),
                List.of(
                        new BlockPos(3, 2, 3),
                        new BlockPos(12, 2, 3),
                        new BlockPos(3, 2, 12),
                        new BlockPos(12, 2, 12)),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        definitions.put(parallaxAtrium.id(), parallaxAtrium);

        List<BlockPos> rewardBridge = new ArrayList<>();
        for (int z = 9; z <= 16; z++) {
            rewardBridge.add(new BlockPos(11, 0, z));
            rewardBridge.add(new BlockPos(12, 0, z));
        }
        TemporalSiteDefinition hallOfAlignment = new TemporalSiteDefinition(
                HALL_OF_ALIGNMENT_ID,
                24,
                18,
                -1,
                8,
                new BlockPos(12, 2, 9),
                new BlockPos(12, 1, 3),
                List.of(),
                rewardBridge,
                List.of(
                        new BlockPos(3, 3, 3),
                        new BlockPos(20, 3, 3),
                        new BlockPos(3, 3, 14),
                        new BlockPos(20, 3, 14)),
                List.of(
                        new AlignmentMechanismDefinition(
                                new BlockPos(6, 1, 8),
                                Direction.WEST,
                                new BlockPos(1, 2, 8),
                                lineX(2, 5, 2, 8)),
                        new AlignmentMechanismDefinition(
                                new BlockPos(12, 1, 8),
                                Direction.SOUTH,
                                new BlockPos(12, 2, 16),
                                lineZ(12, 2, 9, 15)),
                        new AlignmentMechanismDefinition(
                                new BlockPos(18, 1, 8),
                                Direction.EAST,
                                new BlockPos(22, 2, 8),
                                lineX(19, 21, 2, 8))),
                List.of(),
                List.of(),
                List.of());
        definitions.put(hallOfAlignment.id(), hallOfAlignment);

        List<BlockPos> choirRewardBridge = new ArrayList<>();
        for (int z = 13; z <= 16; z++) {
            choirRewardBridge.add(new BlockPos(9, 0, z));
            choirRewardBridge.add(new BlockPos(10, 0, z));
        }
        TemporalSiteDefinition choirOfHours = new TemporalSiteDefinition(
                CHOIR_OF_HOURS_ID,
                20,
                18,
                -1,
                8,
                new BlockPos(10, 2, 9),
                new BlockPos(10, 1, 3),
                List.of(),
                choirRewardBridge,
                List.of(
                        new BlockPos(2, 3, 3),
                        new BlockPos(17, 3, 3),
                        new BlockPos(2, 3, 14),
                        new BlockPos(17, 3, 14)),
                List.of(),
                List.of(
                        choirBell(4, 0, 0.70F),
                        choirBell(8, 1, 0.90F),
                        choirBell(12, 2, 1.15F),
                        choirBell(16, 3, 1.40F)),
                List.of(),
                List.of());
        definitions.put(choirOfHours.id(), choirOfHours);

        List<BlockPos> rememberedBridge = new ArrayList<>();
        for (int z = 5; z <= 8; z++) {
            rememberedBridge.add(new BlockPos(9, 0, z));
            rememberedBridge.add(new BlockPos(10, 0, z));
        }
        List<BlockPos> decayedBridge = new ArrayList<>();
        for (int z = 19; z <= 25; z++) {
            decayedBridge.add(new BlockPos(9, 0, z));
            decayedBridge.add(new BlockPos(10, 0, z));
        }
        MeridianRelayDefinition relay = new MeridianRelayDefinition(
                List.of(
                        new BlockPos(5, 1, 13),
                        new BlockPos(10, 1, 13),
                        new BlockPos(15, 1, 13)),
                List.of(
                        lineZ(5, 0, 14, 18),
                        lineZ(10, 0, 14, 18),
                        lineZ(15, 0, 14, 18)),
                BrokenMeridianPuzzle.TARGET_POSITION);
        TemporalSiteDefinition brokenMeridian = new TemporalSiteDefinition(
                BROKEN_MERIDIAN_ID,
                20,
                30,
                -4,
                8,
                new BlockPos(10, 2, 15),
                new BlockPos(10, 1, 11),
                rememberedBridge,
                decayedBridge,
                List.of(
                        new BlockPos(2, 3, 3),
                        new BlockPos(17, 3, 3),
                        new BlockPos(2, 3, 27),
                        new BlockPos(17, 3, 27)),
                List.of(),
                List.of(),
                List.of(relay),
                List.of());
        definitions.put(brokenMeridian.id(), brokenMeridian);

        List<BlockPos> orreryCover = new ArrayList<>();
        for (BlockPos center : List.of(
                new BlockPos(7, 1, 16),
                new BlockPos(24, 1, 16),
                new BlockPos(16, 1, 7),
                new BlockPos(16, 1, 24))) {
            orreryCover.add(center);
            orreryCover.add(center.east());
            orreryCover.add(center.north());
            orreryCover.add(center.east().north());
        }
        BlockPos orreryCore = new BlockPos(16, 1, 16);
        BlockPos curatorSpawn = new BlockPos(16, 1, 20);
        List<BlockPos> orreryAnchors = List.of(
                new BlockPos(16, 1, 4),
                new BlockPos(27, 1, 16),
                new BlockPos(16, 1, 27),
                new BlockPos(4, 1, 16));
        List<BlockPos> rememberedRings = orreryRings().stream()
                .filter(segment -> !segment.equals(orreryCore)
                        && !segment.equals(curatorSpawn)
                        && !orreryAnchors.contains(segment))
                .toList();
        OrreryDefinition orrery = new OrreryDefinition(
                orreryCore,
                curatorSpawn,
                orreryAnchors,
                rememberedRings);
        TemporalSiteDefinition grandOrrery = new TemporalSiteDefinition(
                GRAND_ORRERY_ID,
                32,
                32,
                -1,
                16,
                new BlockPos(16, 3, 16),
                new BlockPos(16, 1, 4),
                orreryCover,
                List.of(),
                List.of(
                        new BlockPos(5, 2, 5),
                        new BlockPos(16, 2, 3),
                        new BlockPos(26, 2, 5),
                        new BlockPos(28, 2, 16),
                        new BlockPos(26, 2, 26),
                        new BlockPos(16, 2, 28),
                        new BlockPos(5, 2, 26),
                        new BlockPos(3, 2, 16)),
                List.of(),
                List.of(),
                List.of(),
                List.of(orrery));
        definitions.put(grandOrrery.id(), grandOrrery);
        return Map.copyOf(definitions);
    }

    private static ChoirBellDefinition choirBell(int x, int symbol, float pitch) {
        return new ChoirBellDefinition(
                new BlockPos(x, 1, 8),
                symbol,
                pitch,
                List.of(new BlockPos(x, 1, 10), new BlockPos(x, 2, 10)));
    }

    private static List<BlockPos> lineX(int fromX, int toX, int y, int z) {
        List<BlockPos> line = new ArrayList<>();
        for (int x = fromX; x <= toX; x++) {
            line.add(new BlockPos(x, y, z));
        }
        return List.copyOf(line);
    }

    private static List<BlockPos> lineZ(int x, int y, int fromZ, int toZ) {
        List<BlockPos> line = new ArrayList<>();
        for (int z = fromZ; z <= toZ; z++) {
            line.add(new BlockPos(x, y, z));
        }
        return List.copyOf(line);
    }

    private static List<BlockPos> orreryRings() {
        LinkedHashSet<BlockPos> segments = new LinkedHashSet<>();
        for (int step = 0; step < 40; step++) {
            double angle = Math.PI * 2.0D * step / 40.0D;
            int horizontalX = 16 + (int) Math.round(Math.cos(angle) * 12.0D);
            int horizontalZ = 16 + (int) Math.round(Math.sin(angle) * 12.0D);
            segments.add(new BlockPos(horizontalX, 8, horizontalZ));

            int verticalOffset = (int) Math.round(Math.cos(angle) * 10.0D);
            int verticalY = 8 + (int) Math.round(Math.sin(angle) * 7.0D);
            segments.add(new BlockPos(16 + verticalOffset, verticalY, 16));
            segments.add(new BlockPos(16, verticalY, 16 + verticalOffset));
        }
        return List.copyOf(segments);
    }
}
