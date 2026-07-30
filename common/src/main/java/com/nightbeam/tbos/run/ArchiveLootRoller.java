package com.nightbeam.tbos.run;

import com.nightbeam.tbos.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Deterministic weighted table selection and server-side loot-table evaluation. */
public final class ArchiveLootRoller {
    private ArchiveLootRoller() {
    }

    public static Identifier selectTable(
            ArchiveRoomNode room, ArchiveDungeonRules rules, RandomSource random) {
        List<Identifier> tables = room.allowedLootTables();
        if (tables.isEmpty()) {
            return Identifier.fromNamespaceAndPath("tbos", "loot/common");
        }
        int total = tables.stream().mapToInt(rules::lootTableWeight).sum();
        int choice = random.nextInt(total);
        for (Identifier table : tables) {
            choice -= rules.lootTableWeight(table);
            if (choice < 0) {
                return table;
            }
        }
        return tables.getLast();
    }

    public static List<ItemStack> roll(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            ArchiveRoomNode room,
            ArchiveDungeonRules rules,
            long seed,
            boolean mandatory) {
        RandomSource selector = RandomSource.create(mix64(seed ^ 0x5441424C455F5345L));
        Identifier tableId = selectTable(room, rules, selector);
        ResourceKey<LootTable> tableKey = ResourceKey.create(Registries.LOOT_TABLE, tableId);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(tableKey);
        LootParams.Builder params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin));
        if (player != null) {
            params.withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                    .withLuck(player.getLuck());
        }
        List<ItemStack> rolled = List.copyOf(table.getRandomItems(
                params.create(LootContextParamSets.CHEST),
                mix64(seed ^ tableId.hashCode())));
        if (!rolled.isEmpty() || !mandatory) {
            return rolled;
        }
        // A mandatory reward is never allowed to become empty because a data
        // pack removed or accidentally emptied its selected table.
        return List.of(new ItemStack(ModItems.CHRONICLE_SHARD.get()));
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
