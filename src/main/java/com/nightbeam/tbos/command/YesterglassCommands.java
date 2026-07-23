package com.nightbeam.tbos.command;

import com.nightbeam.tbos.registry.ModItems;
import com.nightbeam.tbos.item.MemoryPlateItem;
import com.nightbeam.tbos.item.MemoryScene;
import com.nightbeam.tbos.site.TemporalSite;
import com.nightbeam.tbos.site.TemporalSiteManager;
import com.nightbeam.tbos.run.ArchiveRun;
import com.nightbeam.tbos.run.ArchiveRunManager;
import com.nightbeam.tbos.run.ArchiveRunSavedData;
import com.nightbeam.tbos.run.ArchiveRunStatus;
import com.nightbeam.tbos.run.ArchiveDebugOverlay;
import com.nightbeam.tbos.run.ArchiveDungeonGraph;
import com.nightbeam.tbos.run.ArchiveEncounterManager;
import com.nightbeam.tbos.run.ArchiveRoomNode;
import com.nightbeam.tbos.run.ArchiveRoomPlacer;
import com.nightbeam.tbos.run.ArchiveRoomTemplate;
import com.nightbeam.tbos.run.ArchiveRoomTemplates;
import com.nightbeam.tbos.world.AdventureWorldManager;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class YesterglassCommands {
    private YesterglassCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("tbos")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("showcase").executes(context -> showcase(context.getSource())))
                .then(Commands.literal("reset_site").executes(context -> reset(context.getSource())))
                .then(Commands.literal("locate").executes(context -> locate(context.getSource())))
                .then(Commands.literal("debug_transition").executes(context -> debugTransition(context.getSource())))
                .then(Commands.literal("run")
                        .then(Commands.literal("start").executes(context -> startRun(context.getSource())))
                        .then(Commands.literal("status").executes(context -> runStatus(context.getSource())))
                        .then(Commands.literal("complete").executes(context -> completeRun(context.getSource())))
                        .then(Commands.literal("abandon").executes(context -> abandonRun(context.getSource()))))
                .then(Commands.literal("dungeon")
                        .then(Commands.literal("generate")
                                .executes(context -> generateDungeon(
                                        context.getSource(),
                                        context.getSource().getServer().overworld().getGameTime()))
                                .then(Commands.argument("seed", LongArgumentType.longArg())
                                        .executes(context -> generateDungeon(
                                                context.getSource(),
                                                LongArgumentType.getLong(context, "seed")))))
                        .then(Commands.literal("remove").executes(context -> removeDungeon(context.getSource())))
                        .then(Commands.literal("regenerate")
                                .executes(context -> regenerateDungeon(context.getSource())))
                        .then(Commands.literal("enter").executes(context -> enterDungeon(context.getSource())))
                        .then(Commands.literal("seed").executes(context -> dungeonSeed(context.getSource())))
                        .then(Commands.literal("room").executes(context -> dungeonRoom(context.getSource())))
                        .then(Commands.literal("force_clear")
                                .executes(context -> forceClearDungeonRoom(context.getSource())))
                        .then(Commands.literal("unlock_all")
                                .executes(context -> unlockDungeonDoors(context.getSource())))
                        .then(Commands.literal("spawn_template")
                                .then(Commands.argument("template", StringArgumentType.word())
                                        .executes(context -> spawnDungeonTemplate(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "template")))))
                        .then(Commands.literal("validate_templates")
                                .executes(context -> validateDungeonTemplates(context.getSource())))
                        .then(Commands.literal("export_graph")
                                .executes(context -> exportDungeonGraph(context.getSource())))
                        .then(Commands.literal("boundaries")
                                .executes(context -> toggleDungeonBoundaries(context.getSource())))
                        .then(Commands.literal("markers")
                                .executes(context -> toggleDungeonMarkers(context.getSource()))))
                .then(Commands.literal("debug")
                        .then(Commands.literal("transition").executes(context -> debugTransition(context.getSource())))
                        .then(Commands.literal("give_cracked_lens")
                                .executes(context -> giveCrackedLens(context.getSource())))
                        .then(Commands.literal("give_survey_map")
                                .executes(context -> giveSurveyMap(context.getSource())))
                        .then(Commands.literal("place_shrines")
                                .executes(context -> placeShrines(context.getSource())))
                        .then(Commands.literal("give_memory_kit")
                                .executes(context -> giveMemoryKit(context.getSource())))));
    }

    private static int showcase(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockPos playerPos = player.blockPosition();
        BlockPos origin = new BlockPos((playerPos.getX() >> 4) << 4, playerPos.getY() - 1, (playerPos.getZ() >> 4) << 4);
        TemporalSite site = TemporalSiteManager.placePrototype(source.getLevel(), origin);
        TemporalSiteManager.placeHallOfAlignment(
                source.getLevel(),
                origin.offset(-4, 3, 16),
                Rotation.NONE);
        TemporalSiteManager.placeChoirOfHours(
                source.getLevel(),
                origin.offset(-2, 3, 34),
                Rotation.NONE);
        TemporalSiteManager.placeBrokenMeridian(
                source.getLevel(),
                origin.offset(-2, 3, 52),
                Rotation.NONE);
        TemporalSiteManager.placeGrandOrrery(
                source.getLevel(),
                origin.offset(-8, 3, 82),
                Rotation.NONE);

        ItemStack lens = new ItemStack(ModItems.YESTERGLASS_LENS.get());
        if (!player.addItem(lens)) {
            player.drop(lens, false);
        }
        player.teleportTo(origin.getX() + 8.5D, origin.getY() + 1.0D, origin.getZ() + 1.5D);
        TemporalSiteManager.sendNearbySnapshots(player);
        source.sendSuccess(() -> Component.translatable("command.tbos.showcase", site.siteId().toString()), true);
        return 1;
    }

    private static int reset(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!TemporalSiteManager.resetNearest(player)) {
            source.sendFailure(Component.translatable("command.tbos.no_site"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.tbos.reset"), true);
        return 1;
    }

    private static int locate(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return TemporalSiteManager.locateNearest(player).map(site -> {
            BlockPos origin = site.origin();
            source.sendSuccess(() -> Component.translatable(
                    "command.tbos.locate",
                    origin.getX(),
                    origin.getY(),
                    origin.getZ()), false);
            return 1;
        }).orElseGet(() -> {
            source.sendFailure(Component.translatable("command.tbos.no_site"));
            return 0;
        });
    }

    private static int debugTransition(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!TemporalSiteManager.debugToggle(player)) {
            source.sendFailure(Component.translatable("command.tbos.debug_failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.tbos.debug_transition"), false);
        return 1;
    }

    private static int giveCrackedLens(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack crackedLens = new ItemStack(ModItems.CRACKED_YESTERGLASS_LENS.get());
        if (!player.addItem(crackedLens)) {
            player.drop(crackedLens, false);
        }
        source.sendSuccess(() -> Component.translatable("command.tbos.debug_cracked_lens"), false);
        return 1;
    }

    private static int giveMemoryKit(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        giveOrDrop(player, new ItemStack(ModItems.MEMORY_LANTERN.get()));
        for (MemoryScene scene : MemoryScene.values()) {
            giveOrDrop(player, MemoryPlateItem.forScene(scene));
        }
        source.sendSuccess(() -> Component.translatable("command.tbos.debug_memory_kit"), false);
        return 1;
    }

    private static int giveSurveyMap(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        giveOrDrop(player, new ItemStack(ModItems.ARCHIVE_SURVEY_MAP.get()));
        source.sendSuccess(() -> Component.translatable("command.tbos.debug_survey_map"), false);
        return 1;
    }

    private static int placeShrines(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var placements = AdventureWorldManager.ensureShrines(source.getLevel(), player.blockPosition());
        int count = placements.size();
        source.sendSuccess(() -> Component.translatable("command.tbos.debug_shrines", count), false);
        for (var placement : placements) {
            BlockPos origin = placement.origin();
            source.sendSuccess(() -> Component.translatable(
                    "command.tbos.debug_shrine_location",
                    Component.translatable("shrine.tbos." + placement.variant().serializedName()),
                    origin.getX(),
                    origin.getY(),
                    origin.getZ()), false);
        }
        return count;
    }

    private static int startRun(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ArchiveRunManager.EntryResult result = ArchiveRunManager.enterFromThreshold(player, player.blockPosition());
        if (result != ArchiveRunManager.EntryResult.STARTED) {
            source.sendFailure(Component.translatable("command.tbos.run.start_failed", result.name().toLowerCase()));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.tbos.run.started"), false);
        return 1;
    }

    private static int runStatus(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(source.getServer());
        ArchiveRun run = storage.findByMember(player.getUUID())
                .or(() -> storage.findPendingReturnByMember(player.getUUID()))
                .orElse(null);
        if (run == null) {
            source.sendFailure(Component.translatable("command.tbos.run.none"));
            return 0;
        }
        int level = run.rooms().get(run.currentRoom()).level() + 1;
        source.sendSuccess(() -> Component.translatable(
                "command.tbos.run.status",
                run.runId().toString().substring(0, 8),
                level,
                run.currentRoom() + 1,
                run.rooms().size(),
                run.sharedRevives(),
                run.status().name().toLowerCase()), false);
        return 1;
    }

    private static int completeRun(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        long tick = source.getServer().overworld().getGameTime();
        if (ArchiveRunManager.beginVictoryReturn(
                        ArchiveRunSavedData.get(source.getServer()), player.getUUID(), tick)
                .isEmpty()) {
            source.sendFailure(Component.translatable("command.tbos.run.not_active"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.tbos.run.completing"), false);
        return 1;
    }

    private static int abandonRun(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        long tick = source.getServer().overworld().getGameTime();
        if (ArchiveRunManager.beginFailureReturn(
                        ArchiveRunSavedData.get(source.getServer()), player.getUUID(), tick)
                .isEmpty()) {
            source.sendFailure(Component.translatable("command.tbos.run.not_active"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.tbos.run.abandoning"), false);
        return 1;
    }

    private static int generateDungeon(CommandSourceStack source, long seed) throws CommandSyntaxException {
        ArchiveRunManager.EntryResult result = ArchiveRunManager.startDebugRun(source.getPlayerOrException(), seed);
        if (result != ArchiveRunManager.EntryResult.STARTED) {
            source.sendFailure(Component.literal("Dungeon generation failed: " + result.name().toLowerCase()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Queued Echoes of the Past dungeon with seed " + seed), true);
        return 1;
    }

    private static int removeDungeon(CommandSourceStack source) throws CommandSyntaxException {
        if (!ArchiveRunManager.removeCurrentRun(source.getPlayerOrException())) {
            source.sendFailure(Component.literal("No dungeon is associated with this player"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Dungeon cleanup queued"), true);
        return 1;
    }

    private static int regenerateDungeon(CommandSourceStack source) throws CommandSyntaxException {
        if (!ArchiveRunManager.regenerateCurrentRun(source.getPlayerOrException())) {
            source.sendFailure(Component.literal("No live dungeon is available to regenerate"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Dungeon regenerated from its persisted seed"), true);
        return 1;
    }

    private static int enterDungeon(CommandSourceStack source) throws CommandSyntaxException {
        if (!ArchiveRunManager.enterCurrentRun(source.getPlayerOrException())) {
            source.sendFailure(Component.literal("No active dungeon is available to enter"));
            return 0;
        }
        return 1;
    }

    private static int dungeonSeed(CommandSourceStack source) throws CommandSyntaxException {
        ArchiveRun run = currentDungeon(source.getPlayerOrException());
        if (run == null) {
            source.sendFailure(Component.literal("No dungeon is associated with this player"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Dungeon " + run.runId() + " seed: " + run.seed()), false);
        return 1;
    }

    private static int dungeonRoom(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ArchiveRun run = currentDungeon(player);
        if (run == null) {
            source.sendFailure(Component.literal("No dungeon is associated with this player"));
            return 0;
        }
        int roomIndex = ArchiveRoomPlacer.roomContaining(run, player.blockPosition()).orElse(run.currentRoom());
        ArchiveRoomNode room = run.dungeonGraph().room(roomIndex);
        source.sendSuccess(() -> Component.literal("Room " + roomIndex
                + " template=" + room.templateId()
                + " grid=" + room.placement().coordinates()
                + " category=" + room.category()
                + " difficulty=" + room.difficulty()
                + " connections=" + room.connections().size()
                + " completed=" + room.runtime().completed()
                + " locked=" + room.runtime().doorsLocked()), false);
        return 1;
    }

    private static int forceClearDungeonRoom(CommandSourceStack source) throws CommandSyntaxException {
        if (!ArchiveEncounterManager.forceClearCurrentRoom(source.getPlayerOrException())) {
            source.sendFailure(Component.literal("The current room could not be force-cleared"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Current dungeon room force-cleared"), true);
        return 1;
    }

    private static int unlockDungeonDoors(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(source.getServer());
        ArchiveRun run = storage.findByMember(player.getUUID()).orElse(null);
        if (run == null || run.status() != ArchiveRunStatus.ACTIVE) {
            source.sendFailure(Component.literal("No active dungeon is associated with this player"));
            return 0;
        }
        ArchiveRun unlocked = run.unlockAllRooms();
        storage.replace(unlocked);
        var level = source.getServer().getLevel(com.nightbeam.tbos.run.ArchiveDimensions.FRACTURED_ARCHIVE);
        if (level != null) {
            ArchiveRoomPlacer.unlockAllDoors(level, unlocked);
        }
        source.sendSuccess(() -> Component.literal("All normal dungeon doors unlocked"), true);
        return 1;
    }

    private static int spawnDungeonTemplate(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String path = name.startsWith("tbos:") ? name.substring("tbos:".length()) : name;
        Identifier id = Identifier.fromNamespaceAndPath(com.nightbeam.tbos.Yesterglass.MOD_ID, path);
        ArchiveRoomTemplate template = ArchiveRoomTemplates.find(id).orElse(null);
        if (template == null) {
            source.sendFailure(Component.literal("Unknown dungeon template: " + id));
            return 0;
        }
        int changed = ArchiveRoomPlacer.placeTemplatePreview(
                source.getLevel(), template, player.blockPosition().below());
        source.sendSuccess(() -> Component.literal("Placed template " + id + " using " + changed + " blocks"), true);
        return changed;
    }

    private static int validateDungeonTemplates(CommandSourceStack source) {
        java.util.ArrayList<String> errors = new java.util.ArrayList<>(ArchiveRoomTemplates.validateAll());
        Identifier descriptor = Identifier.fromNamespaceAndPath(
                com.nightbeam.tbos.Yesterglass.MOD_ID,
                "archive_room_templates/echoes_of_the_past.json");
        if (source.getServer().getServerResources().resourceManager().getResource(descriptor).isEmpty()) {
            errors.add("Missing data-pack template descriptor " + descriptor);
        }
        if (!errors.isEmpty()) {
            errors.forEach(error -> source.sendFailure(Component.literal(error)));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Validated " + ArchiveRoomTemplates.all().size() + " Echoes of the Past room templates"), true);
        return ArchiveRoomTemplates.all().size();
    }

    private static int exportDungeonGraph(CommandSourceStack source) throws CommandSyntaxException {
        ArchiveRun run = currentDungeon(source.getPlayerOrException());
        if (run == null) {
            source.sendFailure(Component.literal("No dungeon is associated with this player"));
            return 0;
        }
        try {
            var json = ArchiveDungeonGraph.CODEC.encodeStart(JsonOps.INSTANCE, run.dungeonGraph()).getOrThrow();
            Path directory = Path.of("debug", "tbos", "dungeons").toAbsolutePath().normalize();
            Files.createDirectories(directory);
            Path output = directory.resolve(run.runId() + ".json");
            Files.writeString(
                    output,
                    new GsonBuilder().setPrettyPrinting().create().toJson(json),
                    StandardCharsets.UTF_8);
            source.sendSuccess(() -> Component.literal("Exported dungeon graph to " + output), true);
            return 1;
        } catch (IOException | RuntimeException exception) {
            source.sendFailure(Component.literal("Could not export dungeon graph: " + exception.getMessage()));
            return 0;
        }
    }

    private static int toggleDungeonBoundaries(CommandSourceStack source) throws CommandSyntaxException {
        boolean enabled = ArchiveDebugOverlay.toggleBoundaries(source.getPlayerOrException().getUUID());
        source.sendSuccess(() -> Component.literal("Dungeon boundaries " + (enabled ? "enabled" : "disabled")), false);
        return enabled ? 1 : 0;
    }

    private static int toggleDungeonMarkers(CommandSourceStack source) throws CommandSyntaxException {
        boolean enabled = ArchiveDebugOverlay.toggleMarkers(source.getPlayerOrException().getUUID());
        source.sendSuccess(() -> Component.literal("Dungeon markers " + (enabled ? "enabled" : "disabled")), false);
        return enabled ? 1 : 0;
    }

    private static ArchiveRun currentDungeon(ServerPlayer player) {
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(player.level().getServer());
        return storage.findByMember(player.getUUID())
                .or(() -> storage.findPendingReturnByMember(player.getUUID()))
                .orElse(null);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
