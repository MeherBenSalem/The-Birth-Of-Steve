package com.nightbeam.tbos.rpg;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nightbeam.tbos.codex.Codex;
import com.nightbeam.tbos.codex.PlayerProfile;
import com.nightbeam.tbos.codex.Profiles;
import com.nightbeam.tbos.codex.Weekly;
import com.nightbeam.tbos.compat.RpgCompat;
import com.nightbeam.tbos.expedition.Distortion;
import com.nightbeam.tbos.expedition.ExpeditionKey;
import com.nightbeam.tbos.expedition.ExpeditionState;
import com.nightbeam.tbos.expedition.Expeditions;
import com.nightbeam.tbos.party.Parties;
import com.nightbeam.tbos.relic.Rarity;
import com.nightbeam.tbos.relic.RelicGenerator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Player commands for parties, the Codex and expeditions, plus operator tools for
 * handing out keys and Relics and adjusting Renown. None of them changes a run's
 * layout; the {@code /tbos} tree keeps those.
 */
public final class RpgCommands {
    private RpgCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("party")
                .then(Commands.literal("create").executes(context -> reply(context, Parties.create(player(context)))))
                .then(Commands.literal("invite").then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> reply(context, Parties.invite(player(context), EntityArgument.getPlayer(context, "player"))))))
                .then(Commands.literal("accept")
                        .executes(context -> reply(context, Parties.accept(player(context), null)))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> reply(context, Parties.accept(player(context), EntityArgument.getPlayer(context, "player"))))))
                .then(Commands.literal("decline").executes(context -> reply(context, Parties.decline(player(context)))))
                .then(Commands.literal("leave").executes(context -> reply(context, Parties.leave(player(context)))))
                .then(Commands.literal("kick").then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                            return reply(context, Parties.kick(player(context), target.getUUID(), target.getDisplayName()));
                        })))
                .then(Commands.literal("disband").executes(context -> reply(context, Parties.disband(player(context)))))
                .then(Commands.literal("list").executes(context -> {
                    for (Component line : Parties.describe(player(context))) {
                        context.getSource().sendSuccess(() -> line, false);
                    }
                    return 1;
                })));

        dispatcher.register(Commands.literal("codex")
                .executes(context -> open(player(context), 0))
                .then(Commands.literal("insights").executes(context -> open(player(context), 0)))
                .then(Commands.literal("bestiary").executes(context -> open(player(context), 1)))
                .then(Commands.literal("records").executes(context -> open(player(context), 2)))
                .then(Commands.literal("weekly").executes(context -> {
                    Codex.claimWeekly(player(context));
                    return 1;
                }))
                .then(Commands.literal("renown").requires(RpgCompat.operator())
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            Profiles.get(target).addRenown(amount);
                                            Codex.sync(target);
                                            return reply(context, Component.translatable("command.tbos.codex.renown", amount, target.getDisplayName()));
                                        }))))
                .then(Commands.literal("reset").requires(RpgCompat.operator())
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    PlayerProfile profile = Profiles.get(target);
                                    profile.renown = 0;
                                    profile.respec();
                                    Codex.sync(target);
                                    return reply(context, Component.translatable("command.tbos.codex.reset", target.getDisplayName()));
                                }))));

        dispatcher.register(Commands.literal("expedition")
                .executes(context -> status(context))
                .then(Commands.literal("status").executes(RpgCommands::status))
                .then(Commands.literal("choose").executes(context -> {
                    Expeditions.prompt(player(context));
                    return 1;
                }))
                .then(Commands.literal("reroll").executes(context -> {
                    Expeditions.reroll(player(context));
                    return 1;
                }))
                .then(Commands.literal("key").requires(RpgCompat.operator())
                        .then(Commands.argument("tier", IntegerArgumentType.integer(1, 50))
                                .executes(context -> giveKey(context, ""))
                                .then(Commands.argument("distortions", StringArgumentType.greedyString())
                                        .executes(context -> giveKey(context, StringArgumentType.getString(context, "distortions"))))))
                .then(Commands.literal("weekly_key").requires(RpgCompat.operator()).executes(context -> {
                    ServerPlayer player = player(context);
                    give(player, Weekly.key(player.level().getServer(), Weekly.currentWeek()).stack());
                    return 1;
                }))
                .then(Commands.literal("relic").requires(RpgCompat.operator())
                        .then(Commands.argument("tier", IntegerArgumentType.integer(1, 50))
                                .then(Commands.argument("rarity", StringArgumentType.word())
                                        .executes(context -> {
                                            ServerPlayer player = player(context);
                                            Rarity rarity;
                                            try {
                                                rarity = Rarity.valueOf(StringArgumentType.getString(context, "rarity").toUpperCase(java.util.Locale.ROOT));
                                            } catch (IllegalArgumentException exception) {
                                                context.getSource().sendFailure(Component.translatable("command.tbos.expedition.unknown_rarity"));
                                                return 0;
                                            }
                                            give(player, RelicGenerator.generate(player.getRandom().nextLong(),
                                                    IntegerArgumentType.getInteger(context, "tier"), rarity));
                                            return 1;
                                        })))));
    }

    private static int open(ServerPlayer player, int tab) {
        Codex.sync(player);
        JsonObject body = new JsonObject();
        body.addProperty("tab", tab);
        RpgNet.send(player, "open_codex", body);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = player(context);
        ExpeditionState state = Expeditions.stateOf(player);
        if (state == null) {
            return reply(context, Component.translatable("command.tbos.expedition.none"));
        }
        ExpeditionState.Pool pool = state.pool(player.getUUID());
        return reply(context, Component.translatable("command.tbos.expedition.status", state.key.tier(), state.key.heat(),
                state.floor + 1, String.format(java.util.Locale.ROOT, "%.2f", state.momentum),
                Math.round(pool.renown * state.momentum), pool.relics.size()));
    }

    private static int giveKey(CommandContext<CommandSourceStack> context, String names) throws CommandSyntaxException {
        int mask = 0;
        for (String name : names.trim().split("[\\s,]+")) {
            if (name.isEmpty()) {
                continue;
            }
            try {
                mask |= Distortion.valueOf(name.toUpperCase(java.util.Locale.ROOT)).bit();
            } catch (IllegalArgumentException exception) {
                context.getSource().sendFailure(Component.translatable("command.tbos.expedition.unknown_distortion", name));
                return 0;
            }
        }
        ExpeditionKey key = new ExpeditionKey(IntegerArgumentType.getInteger(context, "tier"), mask, ExpeditionKey.NOT_WEEKLY, 0L);
        give(player(context), key.stack());
        return 1;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        RpgCompat.give(player, stack);
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return context.getSource().getPlayerOrException();
    }

    private static int reply(CommandContext<CommandSourceStack> context, Component message) {
        context.getSource().sendSuccess(() -> message, false);
        return 1;
    }
}
