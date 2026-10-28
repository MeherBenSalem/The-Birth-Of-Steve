package com.nightbeam.tbos.compat;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The server-side seam for the 1.0 RPG systems (expeditions, the Codex, relics and
 * parties). This is the 1.21.1 shape: {@code ResourceLocation}, the long-form effect
 * names, {@code registryOrThrow} and spawn-reasonless entity creation.
 *
 * <p>The feature code never names a type that moved between versions. Where a
 * vanilla type differs (an {@code Identifier} here, a {@code ResourceLocation} on
 * 1.21.1), methods hand it straight back to vanilla calls, so shared callers can
 * pass it along without spelling it. Keep every override in step with this file.
 */
public final class RpgCompat {
    /** Player and item data key; also the attribute-modifier id namespace. */
    public static final String NAMESPACE = "tbos";
    private static final String DATA_KEY = "tbos_rpg";

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
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
    }

    // ---- Attributes -------------------------------------------------------------------------------------

    private static Holder<Attribute> attribute(Stat stat) {
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
        ResourceLocation id = id(key);
        AttributeModifier existing = instance.getModifier(id);
        AttributeModifier.Operation operation = multiply
                ? AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                : AttributeModifier.Operation.ADD_VALUE;
        if (existing != null) {
            if (existing.amount() == amount && existing.operation() == operation) {
                return true;
            }
            instance.removeModifier(id);
        }
        instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        return true;
    }

    public static void removeModifier(LivingEntity entity, Stat stat, String key) {
        AttributeInstance instance = entity.getAttribute(attribute(stat));
        if (instance != null) {
            instance.removeModifier(id(key));
        }
    }

    public static boolean hasModifier(LivingEntity entity, Stat stat, String key) {
        AttributeInstance instance = entity.getAttribute(attribute(stat));
        return instance != null && instance.getModifier(id(key)) != null;
    }

    // ---- Effects ----------------------------------------------------------------------------------------

    private static Holder<MobEffect> effect(Effect effect) {
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
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return "";
        }
        return data.copyTag().getString(DATA_KEY);
    }

    public static void writeData(ItemStack stack, String json) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(DATA_KEY, json));
    }

    /** A non-italic display name that survives anvils and renames like any other. */
    public static void setName(ItemStack stack, Component name) {
        stack.set(DataComponents.CUSTOM_NAME, name);
    }

    public static void setLore(ItemStack stack, List<Component> lines) {
        stack.set(DataComponents.LORE, new ItemLore(List.copyOf(lines)));
    }

    public static void setGlint(ItemStack stack, boolean glint) {
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, glint);
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
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, holder -> true);
        if (start == null || !start.isValid()) {
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
        net.minecraft.advancements.AdvancementHolder advancement = player.server.getAdvancements().get(id(path));
        return advancement != null && player.getAdvancements().award(advancement, criterion);
    }
}
