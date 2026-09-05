package com.nightbeam.tbos.advancement;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ModAdvancements {
    public static final int DISCOVER_FRACTURE_SHRINE_BIT = 1 << 0;
    public static final int OBTAIN_CRACKED_LENS_BIT = 1 << 1;
    public static final int REPAIR_LENS_BIT = 1 << 2;
    public static final int ENTER_MERIDIAN_ARCHIVE_BIT = 1 << 3;
    public static final int FIRST_RECONSTRUCTION_BIT = 1 << 4;
    public static final int HALL_ALIGNMENT_BIT = 1 << 5;
    public static final int CHOIR_OF_HOURS_BIT = 1 << 6;
    public static final int BROKEN_MERIDIAN_BIT = 1 << 7;
    public static final int LAST_CURATOR_BIT = 1 << 8;
    public static final int ENTER_FRACTURED_ARCHIVE_BIT = 1 << 9;

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
    private static final AdvancementStep ENTER_FRACTURED_ARCHIVE =
            new AdvancementStep("story/enter_fractured_archive", "entered");
    private static final AdvancementStep MEMORY_LANTERN =
            new AdvancementStep("story/obtain_memory_lantern", "obtained");
    private static final AdvancementStep ALL_MEMORY_PLATES =
            new AdvancementStep("story/collect_all_memory_plates", "collected");
    private static final AdvancementStep COMPLETE_MEMORY_SCENE =
            new AdvancementStep("story/display_complete_memory_scene", "displayed");

    public static void awardMemoryVictory(ServerPlayer player) {
        award(player,new AdvancementStep("story/living_memories","completed"));
    }
    private ModAdvancements() {
    }

    public static void awardDiscoverFractureShrine(ServerPlayer player) {
        award(player, DISCOVER_FRACTURE_SHRINE);
    }

    /** @return true when the shrine discovery criterion was newly granted */
    public static boolean tryAwardDiscoverFractureShrine(ServerPlayer player) {
        return awardIfNew(player, DISCOVER_FRACTURE_SHRINE);
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

    public static void awardEnterFracturedArchive(ServerPlayer player) {
        award(player, ENTER_FRACTURED_ARCHIVE);
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

    public static int journalStoryMask(ServerPlayer player) {
        int mask = 0;
        mask |= completed(player, "story/root") ? DISCOVER_FRACTURE_SHRINE_BIT : 0;
        mask |= completed(player, "story/obtain_cracked_lens") ? OBTAIN_CRACKED_LENS_BIT : 0;
        mask |= completed(player, "story/repair_lens") ? REPAIR_LENS_BIT : 0;
        mask |= completed(player, "story/enter_archive") ? ENTER_MERIDIAN_ARCHIVE_BIT : 0;
        mask |= completed(player, "story/reconstruct_first_room") ? FIRST_RECONSTRUCTION_BIT : 0;
        mask |= completed(player, "story/solve_hall_of_alignment") ? HALL_ALIGNMENT_BIT : 0;
        mask |= completed(player, "story/complete_choir_of_hours") ? CHOIR_OF_HOURS_BIT : 0;
        mask |= completed(player, "story/cross_broken_meridian") ? BROKEN_MERIDIAN_BIT : 0;
        mask |= completed(player, "story/defeat_last_curator") ? LAST_CURATOR_BIT : 0;
        mask |= completed(player, "story/enter_fractured_archive") ? ENTER_FRACTURED_ARCHIVE_BIT : 0;
        return mask;
    }

    private static void award(ServerPlayer player, AdvancementStep step) {
        awardIfNew(player, step);
    }

    private static boolean awardIfNew(ServerPlayer player, AdvancementStep step) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, step.path());
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(id);
        if (advancement == null) {
            return false;
        }
        return player.getAdvancements().award(advancement, step.criterion());
    }

    private static boolean completed(ServerPlayer player, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, path);
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(id);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private record AdvancementStep(String path, String criterion) {
    }
}
