package com.nightbeam.tbos.advancement;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class ModAdvancements {
    private static final AdvancementStep DISCOVER_FRACTURE_SHRINE =
            new AdvancementStep("story/root", "discovered");
    private static final AdvancementStep ENTER_MERIDIAN_ARCHIVE =
            new AdvancementStep("story/enter_archive", "entered");
    private static final AdvancementStep FIRST_RECONSTRUCTION =
            new AdvancementStep("story/reconstruct_first_room", "reconstructed");
    private static final AdvancementStep HALL_ALIGNMENT =
            new AdvancementStep("story/solve_hall_of_alignment", "aligned");
    private static final AdvancementStep CHOIR_OF_HOURS =
            new AdvancementStep("story/complete_choir_of_hours", "remembered");
    private static final AdvancementStep BROKEN_MERIDIAN =
            new AdvancementStep("story/cross_broken_meridian", "crossed");
    private static final AdvancementStep LAST_CURATOR =
            new AdvancementStep("story/defeat_last_curator", "defeated");
    private static final AdvancementStep MEMORY_LANTERN =
            new AdvancementStep("story/obtain_memory_lantern", "obtained");
    private static final AdvancementStep ALL_MEMORY_PLATES =
            new AdvancementStep("story/collect_all_memory_plates", "collected");
    private static final AdvancementStep COMPLETE_MEMORY_SCENE =
            new AdvancementStep("story/display_complete_memory_scene", "displayed");

    private ModAdvancements() {
    }

    public static void awardDiscoverFractureShrine(ServerPlayer player) {
        award(player, DISCOVER_FRACTURE_SHRINE);
    }

    public static void awardEnterMeridianArchive(ServerPlayer player) {
        award(player, ENTER_MERIDIAN_ARCHIVE);
    }

    public static void awardFirstReconstruction(ServerPlayer player) {
        award(player, FIRST_RECONSTRUCTION);
    }

    public static void awardHallAlignment(ServerPlayer player) {
        award(player, HALL_ALIGNMENT);
    }

    public static void awardChoirOfHours(ServerPlayer player) {
        award(player, CHOIR_OF_HOURS);
    }

    public static void awardBrokenMeridian(ServerPlayer player) {
        award(player, BROKEN_MERIDIAN);
    }

    public static void awardLastCurator(ServerPlayer player) {
        award(player, LAST_CURATOR);
    }

    public static void awardMemoryLantern(ServerPlayer player) {
        award(player, MEMORY_LANTERN);
    }

    public static void awardAllMemoryPlates(ServerPlayer player) {
        award(player, ALL_MEMORY_PLATES);
    }

    public static void awardCompleteMemoryScene(ServerPlayer player) {
        award(player, COMPLETE_MEMORY_SCENE);
    }

    private static void award(ServerPlayer player, AdvancementStep step) {
        Identifier id = Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, step.path());
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(id);
        if (advancement != null) {
            player.getAdvancements().award(advancement, step.criterion());
        }
    }

    private record AdvancementStep(String path, String criterion) {
    }
}
