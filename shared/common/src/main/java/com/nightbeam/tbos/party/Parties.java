package com.nightbeam.tbos.party;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nightbeam.tbos.rpg.Json;
import com.nightbeam.tbos.rpg.JsonStore;
import com.nightbeam.tbos.rpg.RpgConfig;
import com.nightbeam.tbos.rpg.RpgNet;
import com.nightbeam.tbos.rpg.RpgSounds;
import com.nightbeam.tbos.run.ArchiveDimensions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * Lightweight co-op parties: up to four players, a leader, and invitations that
 * expire. Parties are kept in {@code data/tbos/parties.json} so a restart does not
 * break them up. Membership is what decides who comes along on an expedition, and
 * it drives the party frames every member sees.
 */
public final class Parties {
    public static final class Party {
        public final UUID id;
        public UUID leader;
        public final List<UUID> members = new ArrayList<>();
        /** Display names, remembered so offline members still read in the frames. */
        public final Map<UUID, String> names = new HashMap<>();

        Party(UUID id, UUID leader) {
            this.id = id;
            this.leader = leader;
        }
    }

    private record Invite(UUID party, UUID from, long expiresTick) {
    }

    private static final Map<UUID, Party> PARTIES = new LinkedHashMap<>();
    private static final Map<UUID, UUID> MEMBERSHIP = new HashMap<>();
    private static final Map<UUID, Map<UUID, Invite>> INVITES = new HashMap<>();
    private static boolean dirty;

    private Parties() {
    }

    // ---- Queries ----------------------------------------------------------------------------------------

    public static Optional<Party> of(UUID player) {
        UUID party = MEMBERSHIP.get(player);
        return party == null ? Optional.empty() : Optional.ofNullable(PARTIES.get(party));
    }

    public static boolean together(UUID a, UUID b) {
        UUID party = MEMBERSHIP.get(a);
        return party != null && party.equals(MEMBERSHIP.get(b));
    }

    /** Online party members (including the player) within {@code radius} blocks, in the same level. */
    public static List<ServerPlayer> nearby(ServerPlayer player, double radius) {
        List<ServerPlayer> result = new ArrayList<>();
        result.add(player);
        of(player.getUUID()).ifPresent(party -> {
            for (UUID member : party.members) {
                if (member.equals(player.getUUID())) {
                    continue;
                }
                ServerPlayer online = player.level().getServer().getPlayerList().getPlayer(member);
                if (online != null && online.level() == player.level() && online.distanceTo(player) <= radius) {
                    result.add(online);
                }
            }
        });
        return result;
    }

    // ---- Commands ---------------------------------------------------------------------------------------

    public static Component create(ServerPlayer player) {
        if (MEMBERSHIP.containsKey(player.getUUID())) {
            return Component.translatable("party.tbos.already_in_party");
        }
        Party party = new Party(UUID.randomUUID(), player.getUUID());
        join(party, player);
        PARTIES.put(party.id, party);
        markDirty(player.level().getServer());
        return Component.translatable("party.tbos.created");
    }

    public static Component invite(ServerPlayer from, ServerPlayer target) {
        if (from == target) {
            return Component.translatable("party.tbos.invite_self");
        }
        Party party = of(from.getUUID()).orElse(null);
        if (party == null) {
            create(from);
            party = of(from.getUUID()).orElseThrow();
        }
        if (!party.leader.equals(from.getUUID())) {
            return Component.translatable("party.tbos.not_leader");
        }
        if (party.members.contains(target.getUUID())) {
            return Component.translatable("party.tbos.already_member", target.getDisplayName());
        }
        if (party.members.size() >= RpgConfig.get().party.maxSize) {
            return Component.translatable("party.tbos.full", RpgConfig.get().party.maxSize);
        }
        long expires = from.level().getServer().getTickCount() + RpgConfig.get().party.inviteSeconds * 20L;
        INVITES.computeIfAbsent(target.getUUID(), id -> new HashMap<>())
                .put(party.id, new Invite(party.id, from.getUUID(), expires));
        target.sendSystemMessage(Component.translatable("party.tbos.invited", from.getDisplayName(),
                RpgConfig.get().party.inviteSeconds));
        target.level().playSound(null, target.blockPosition(), RpgSounds.PARTY_INVITE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        return Component.translatable("party.tbos.invite_sent", target.getDisplayName());
    }

    /** Accepts the invitation from {@code from}, or the newest one when {@code from} is null. */
    public static Component accept(ServerPlayer player, ServerPlayer from) {
        long now = player.level().getServer().getTickCount();
        Map<UUID, Invite> invites = INVITES.getOrDefault(player.getUUID(), Map.of());
        Invite chosen = null;
        for (Invite invite : invites.values()) {
            if (invite.expiresTick < now || !PARTIES.containsKey(invite.party)) {
                continue;
            }
            if (from != null && !invite.from.equals(from.getUUID())) {
                continue;
            }
            if (chosen == null || invite.expiresTick > chosen.expiresTick) {
                chosen = invite;
            }
        }
        if (chosen == null) {
            return Component.translatable("party.tbos.no_invite");
        }
        Party party = PARTIES.get(chosen.party);
        if (party.members.size() >= RpgConfig.get().party.maxSize) {
            return Component.translatable("party.tbos.full", RpgConfig.get().party.maxSize);
        }
        if (MEMBERSHIP.containsKey(player.getUUID())) {
            leave(player);
        }
        INVITES.remove(player.getUUID());
        join(party, player);
        broadcast(player.level().getServer(), party, Component.translatable("party.tbos.joined", player.getDisplayName()), true);
        markDirty(player.level().getServer());
        return Component.translatable("party.tbos.you_joined");
    }

    public static Component decline(ServerPlayer player) {
        INVITES.remove(player.getUUID());
        return Component.translatable("party.tbos.declined");
    }

    public static Component leave(ServerPlayer player) {
        Party party = of(player.getUUID()).orElse(null);
        if (party == null) {
            return Component.translatable("party.tbos.not_in_party");
        }
        remove(player.level().getServer(), party, player.getUUID());
        broadcast(player.level().getServer(), party, Component.translatable("party.tbos.left", player.getDisplayName()), false);
        clearFrames(player);
        return Component.translatable("party.tbos.you_left");
    }

    public static Component kick(ServerPlayer leader, UUID target, Component targetName) {
        Party party = of(leader.getUUID()).orElse(null);
        if (party == null || !party.leader.equals(leader.getUUID())) {
            return Component.translatable("party.tbos.not_leader");
        }
        if (!party.members.contains(target) || target.equals(leader.getUUID())) {
            return Component.translatable("party.tbos.not_member", targetName);
        }
        MinecraftServer server = leader.level().getServer();
        remove(server, party, target);
        ServerPlayer kicked = server.getPlayerList().getPlayer(target);
        if (kicked != null) {
            kicked.sendSystemMessage(Component.translatable("party.tbos.kicked"));
            clearFrames(kicked);
        }
        broadcast(server, party, Component.translatable("party.tbos.removed", targetName), false);
        return Component.translatable("party.tbos.removed", targetName);
    }

    public static Component disband(ServerPlayer leader) {
        Party party = of(leader.getUUID()).orElse(null);
        if (party == null || !party.leader.equals(leader.getUUID())) {
            return Component.translatable("party.tbos.not_leader");
        }
        MinecraftServer server = leader.level().getServer();
        broadcast(server, party, Component.translatable("party.tbos.disbanded"), false);
        for (UUID member : List.copyOf(party.members)) {
            MEMBERSHIP.remove(member);
            ServerPlayer online = server.getPlayerList().getPlayer(member);
            if (online != null) {
                clearFrames(online);
            }
        }
        PARTIES.remove(party.id);
        markDirty(server);
        return Component.translatable("party.tbos.disbanded");
    }

    public static List<Component> describe(ServerPlayer player) {
        Party party = of(player.getUUID()).orElse(null);
        List<Component> lines = new ArrayList<>();
        if (party == null) {
            lines.add(Component.translatable("party.tbos.not_in_party"));
            return lines;
        }
        lines.add(Component.translatable("party.tbos.list_header", party.members.size(), RpgConfig.get().party.maxSize));
        for (UUID member : party.members) {
            boolean online = player.level().getServer().getPlayerList().getPlayer(member) != null;
            lines.add(Component.translatable(member.equals(party.leader) ? "party.tbos.list_leader" : "party.tbos.list_member",
                    party.names.getOrDefault(member, member.toString().substring(0, 8)),
                    Component.translatable(online ? "party.tbos.online" : "party.tbos.offline")));
        }
        return lines;
    }

    // ---- Internals --------------------------------------------------------------------------------------

    private static void join(Party party, ServerPlayer player) {
        party.members.add(player.getUUID());
        party.names.put(player.getUUID(), com.nightbeam.tbos.compat.RpgCompat.playerName(player));
        MEMBERSHIP.put(player.getUUID(), party.id);
    }

    private static void remove(MinecraftServer server, Party party, UUID player) {
        party.members.remove(player);
        party.names.remove(player);
        MEMBERSHIP.remove(player);
        if (party.members.isEmpty()) {
            PARTIES.remove(party.id);
        } else if (party.leader.equals(player)) {
            party.leader = party.members.get(0);
            broadcast(server, party, Component.translatable("party.tbos.new_leader",
                    party.names.getOrDefault(party.leader, "?")), false);
        }
        markDirty(server);
    }

    private static void broadcast(MinecraftServer server, Party party, Component message, boolean chime) {
        for (UUID member : party.members) {
            ServerPlayer online = server.getPlayerList().getPlayer(member);
            if (online != null) {
                online.sendSystemMessage(message);
                if (chime) {
                    online.level().playSound(null, online.blockPosition(), RpgSounds.PARTY_JOIN.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
                }
            }
        }
    }

    private static void markDirty(MinecraftServer server) {
        dirty = true;
    }

    /** Every 10 ticks: refresh name caches, expire invites and push the frames. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 10 != 0) {
            return;
        }
        long now = server.getTickCount();
        INVITES.values().forEach(map -> map.values().removeIf(invite -> invite.expiresTick < now));
        INVITES.values().removeIf(Map::isEmpty);
        if (!RpgConfig.get().party.frames) {
            return;
        }
        for (Party party : PARTIES.values()) {
            JsonArray frames = new JsonArray();
            for (UUID member : party.members) {
                ServerPlayer online = server.getPlayerList().getPlayer(member);
                JsonObject frame = new JsonObject();
                frame.addProperty("id", member.toString());
                frame.addProperty("name", party.names.getOrDefault(member, "?"));
                frame.addProperty("leader", member.equals(party.leader));
                if (online != null) {
                    party.names.put(member, com.nightbeam.tbos.compat.RpgCompat.playerName(online));
                    frame.addProperty("hp", online.getHealth());
                    frame.addProperty("max", online.getMaxHealth());
                    frame.addProperty("abs", online.getAbsorptionAmount());
                    frame.addProperty("archive", online.level().dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE));
                    frame.addProperty("dead", !online.isAlive());
                } else {
                    frame.addProperty("offline", true);
                }
                frames.add(frame);
            }
            for (UUID member : party.members) {
                ServerPlayer online = server.getPlayerList().getPlayer(member);
                if (online != null) {
                    JsonObject body = new JsonObject();
                    body.add("members", frames);
                    body.addProperty("self", member.toString());
                    RpgNet.send(online, "party", body);
                }
            }
        }
        if (dirty) {
            save(server);
        }
    }

    private static void clearFrames(ServerPlayer player) {
        JsonObject body = new JsonObject();
        body.add("members", new JsonArray());
        RpgNet.send(player, "party", body);
    }

    public static void load(MinecraftServer server) {
        PARTIES.clear();
        MEMBERSHIP.clear();
        INVITES.clear();
        JsonObject json = JsonStore.read(JsonStore.directory(server, "parties.json"));
        for (var element : Json.arr(json, "parties")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            try {
                Party party = new Party(UUID.fromString(Json.str(object, "id", "")),
                        UUID.fromString(Json.str(object, "leader", "")));
                for (var memberElement : Json.arr(object, "members")) {
                    JsonObject member = memberElement.getAsJsonObject();
                    UUID id = UUID.fromString(Json.str(member, "id", ""));
                    if (MEMBERSHIP.containsKey(id) || party.members.size() >= RpgConfig.get().party.maxSize) {
                        continue;
                    }
                    party.members.add(id);
                    party.names.put(id, Json.str(member, "name", "?"));
                    MEMBERSHIP.put(id, party.id);
                }
                if (!party.members.isEmpty()) {
                    if (!party.members.contains(party.leader)) {
                        party.leader = party.members.get(0);
                    }
                    PARTIES.put(party.id, party);
                }
            } catch (RuntimeException ignored) {
                // A hand-edited entry that no longer parses is dropped rather than failing the load.
            }
        }
        dirty = false;
    }

    public static void save(MinecraftServer server) {
        JsonArray list = new JsonArray();
        for (Party party : PARTIES.values()) {
            JsonObject object = new JsonObject();
            object.addProperty("id", party.id.toString());
            object.addProperty("leader", party.leader.toString());
            JsonArray members = new JsonArray();
            for (UUID member : party.members) {
                JsonObject entry = new JsonObject();
                entry.addProperty("id", member.toString());
                entry.addProperty("name", party.names.getOrDefault(member, "?"));
                members.add(entry);
            }
            object.add("members", members);
            list.add(object);
        }
        JsonObject json = new JsonObject();
        json.add("parties", list);
        JsonStore.write(JsonStore.directory(server, "parties.json"), json);
        dirty = false;
    }

    public static void clear() {
        PARTIES.clear();
        MEMBERSHIP.clear();
        INVITES.clear();
        dirty = false;
    }
}
