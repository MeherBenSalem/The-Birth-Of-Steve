package com.nightbeam.tbos.compat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The server-side seam for the 1.0 RPG systems (expeditions, the Codex, relics and
 * parties). This is the 1.20.1 shape: item data lives in the stack's NBT tag,
 * attribute modifiers are keyed by a UUID derived from their name, effects and
 * attributes are plain registry objects and advancements resolve through
 * {@code getAdvancement}. Keep it in step with the shared file.
 */
public final class RpgCompat {
    /** Player and item data key; also the attribute-modifier id namespace. */
    public static final String NAMESPACE = "tbos";
    private static final String DATA_KEY = "tbos_rpg";
    /** An enchantment id no registry knows: it lights the glint without a tooltip line. */
    private static final String GLINT_ID = "tbos:glint";

    private RpgCompat() {
    }

    /** Version-neutral stat names for transient attribute modifiers. */
    public enum Stat {
        MAX_HEALTH, ARMOR, ARMOR_TOUGHNESS, ATTACK_DAMAGE, ATTACK_SPEED, MOVEMENT_SPEED, KNOCKBACK_RESISTANCE, LUCK
    }

    /** Version-neutral status effects the RPG systems apply. */
    public enum Effect {
        SPEED, RESISTANCE, STRENGTH, SLOWNESS, GLOWING, REGENERATION, ABSORPTION, WEAKNESS
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(NAMESPACE, path);
    }

    // ---- Attributes -------------------------------------------------------------------------------------

    private static Attribute attribute(Stat stat) {
        return switch (stat) {
            case MAX_HEALTH -> Attributes.MAX_HEALTH;
            case ARMOR -> Attributes.ARMOR;
            case ARMOR_TOUGHNESS -> Attributes.ARMOR_TOUGHNESS;
            case ATTACK_DAMAGE -> Attributes.ATTACK_DAMAGE;
            case ATTACK_SPEED -> Attributes.ATTACK_SPEED;
            case MOVEMENT_SPEED -> Attributes.MOVEMENT_SPEED;
            case KNOCKBACK_RESISTANCE -> Attributes.KNOCKBACK_RESISTANCE;
            case LUCK -> Attributes.LUCK;
        };
    }

    /** 1.20.1 keys modifiers by UUID; a name-derived one keeps each modifier unique and stable. */
    private static UUID uuid(String key) {
        return UUID.nameUUIDFromBytes((NAMESPACE + ":" + key).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sets (or replaces) a transient modifier named {@code key}. {@code multiply}
     * selects a multiplier of the total instead of a flat addition. Returns false
     * when the entity has no such attribute.
     */
    public static boolean setModifier(LivingEntity entity, Stat stat, String key, double amount, boolean multiply) {
        AttributeInstance instance = entity.getAttribute(attribute(stat));
        if (instance == null) {
            return false;
        }
        UUID id = uuid(key);
        AttributeModifier existing = instance.getModifier(id);
        AttributeModifier.Operation operation = multiply
                ? AttributeModifier.Operation.MULTIPLY_TOTAL
                : AttributeModifier.Operation.ADDITION;
        if (existing != null) {
            if (existing.getAmount() == amount && existing.getOperation() == operation) {
                return true;
            }
            instance.removeModifier(id);
        }
        instance.addTransientModifier(new AttributeModifier(id, NAMESPACE + "." + key, amount, operation));
        return true;
    }

    public static void removeModifier(LivingEntity entity, Stat stat, String key) {
        AttributeInstance instance = entity.getAttribute(attribute(stat));
        if (instance != null) {
            instance.removeModifier(uuid(key));
        }
    }

    public static boolean hasModifier(LivingEntity entity, Stat stat, String key) {
        AttributeInstance instance = entity.getAttribute(attribute(stat));
        return instance != null && instance.getModifier(uuid(key)) != null;
    }

    // ---- Effects ----------------------------------------------------------------------------------------

    private static MobEffect effect(Effect effect) {
        return switch (effect) {
            case SPEED -> MobEffects.MOVEMENT_SPEED;
            case RESISTANCE -> MobEffects.DAMAGE_RESISTANCE;
            case STRENGTH -> MobEffects.DAMAGE_BOOST;
            case SLOWNESS -> MobEffects.MOVEMENT_SLOWDOWN;
            case GLOWING -> MobEffects.GLOWING;
            case REGENERATION -> MobEffects.REGENERATION;
            case ABSORPTION -> MobEffects.ABSORPTION;
            case WEAKNESS -> MobEffects.WEAKNESS;
        };
    }

    public static void addEffect(LivingEntity entity, Effect effect, int ticks, int amplifier, boolean visible) {
        entity.addEffect(new MobEffectInstance(effect(effect), ticks, amplifier, false, visible, true));
    }

    /** The current amplifier of {@code effect}, or -1 when the entity does not have it. */
    public static int effectAmplifier(LivingEntity entity, Effect effect) {
        MobEffectInstance instance = entity.getEffect(effect(effect));
        return instance == null ? -1 : instance.getAmplifier();
    }

    // ---- Items ------------------------------------------------------------------------------------------

    /** The mod's JSON blob on a stack, or an empty string. */
    public static String readData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(DATA_KEY);
    }

    public static void writeData(ItemStack stack, String json) {
        stack.getOrCreateTag().putString(DATA_KEY, json);
    }

    /** A display name that survives anvils and renames like any other. */
    public static void setName(ItemStack stack, Component name) {
        stack.setHoverName(name);
    }

    public static void setLore(ItemStack stack, List<Component> lines) {
        ListTag lore = new ListTag();
        for (Component line : lines) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(line)));
        }
        stack.getOrCreateTagElement("display").put("Lore", lore);
    }

    /**
     * 1.20.1 has no glint override. Memory Keys get the glint from an enchantment
     * entry whose id no registry knows, which draws no tooltip line. Relics skip
     * it so the enchanting table keeps accepting them; their rarity colour and
     * lore still mark them.
     */
    public static void setGlint(ItemStack stack, boolean glint) {
        if (!(stack.getItem() instanceof com.nightbeam.tbos.item.MemoryKeyItem)) {
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        ListTag enchantments = tag.getList("Enchantments", Tag.TAG_COMPOUND);
        enchantments.removeIf(entry -> entry instanceof CompoundTag compound && GLINT_ID.equals(compound.getString("id")));
        if (glint) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", GLINT_ID);
            entry.putShort("lvl", (short) 1);
            enchantments.add(entry);
        }
        if (enchantments.isEmpty()) {
            tag.remove("Enchantments");
        } else {
            tag.put("Enchantments", enchantments);
        }
    }

    public static Optional<Item> item(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(BuiltInRegistries.ITEM.get(key));
    }

    /** Into the player's inventory, or dropped at their feet when it is full. */
    public static void give(net.minecraft.server.level.ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    // ---- Entities ---------------------------------------------------------------------------------------

    public static String entityId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    public static <T extends Entity> T create(EntityType<T> type, ServerLevel level) {
        return type.create(level);
    }

    public static java.util.Set<String> tags(Entity entity) {
        return entity.getTags();
    }

    /** Lets the next hit land at once, skipping the half-second damage cooldown. */
    public static void resetHurtCooldown(Entity entity) {
        entity.invulnerableTime = 0;
    }

    // ---- World ------------------------------------------------------------------------------------------

    public static void dust(ServerLevel level, double x, double y, double z, int rgb, float scale,
            int count, double spread, double speed) {
        level.sendParticles(VanillaCompat.dust(rgb, scale), x, y, z, count, spread, spread, spread, speed);
    }

    public static Path worldDirectory(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT);
    }

    /** The structure with a piece at {@code pos}, as {@code id} plus a stable start key. */
    public record StructureHit(String id, long startKey, BlockPos center) {
    }

    public static Optional<StructureHit> structureAt(ServerLevel level, BlockPos pos) {
        // 1.20.1 has no predicate overload: test each structure that references this chunk.
        StructureStart start = null;
        for (var structure : level.structureManager().getAllStructuresAt(pos).keySet()) {
            StructureStart candidate = level.structureManager().getStructureWithPieceAt(pos, structure);
            if (candidate != null && candidate.isValid()) {
                start = candidate;
                break;
            }
        }
        if (start == null) {
            return Optional.empty();
        }
        ResourceLocation key = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(start.getStructure());
        if (key == null) {
            return Optional.empty();
        }
        return Optional.of(new StructureHit(key.toString(), start.getChunkPos().toLong(), start.getBoundingBox().getCenter()));
    }

    public static boolean structureTagged(ServerLevel level, String structureId, String tag) {
        ResourceLocation key = ResourceLocation.tryParse(structureId);
        ResourceLocation tagId = ResourceLocation.tryParse(tag);
        if (key == null || tagId == null) {
            return false;
        }
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        return registry.getHolder(ResourceKey.create(Registries.STRUCTURE, key))
                .map(holder -> holder.is(net.minecraft.tags.TagKey.create(Registries.STRUCTURE, tagId)))
                .orElse(false);
    }

    // ---- Commands ---------------------------------------------------------------------------------------

    public static Predicate<CommandSourceStack> operator() {
        return source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS);
    }

    public static String playerName(net.minecraft.world.entity.player.Player player) {
        return player.getGameProfile().getName();
    }

    // ---- Advancements -----------------------------------------------------------------------------------

    /** Grants {@code criterion} of {@code tbos:<path>}; false when it was already done or does not exist. */
    public static boolean award(net.minecraft.server.level.ServerPlayer player, String path, String criterion) {
        net.minecraft.advancements.Advancement advancement = player.server.getAdvancements().getAdvancement(id(path));
        return advancement != null && player.getAdvancements().award(advancement, criterion);
    }
}
