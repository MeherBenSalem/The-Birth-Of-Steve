package com.nightbeam.tbos.item;

import com.nightbeam.tbos.registry.ModItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Minecraft 1.20.1 stores the selected scene in NBT; there is no component system. */
public final class MemoryPlateItem extends Item {
    private static final String SCENE_TAG = "tbos:memory_scene";

    public MemoryPlateItem(Properties properties) {
        super(properties);
    }

    public static ItemStack forScene(MemoryScene scene) {
        ItemStack stack = new ItemStack(ModItems.MEMORY_PLATE.get());
        stack.getOrCreateTag().putString(SCENE_TAG, scene.getSerializedName());
        return stack;
    }

    public static MemoryScene scene(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(SCENE_TAG)) {
            String name = tag.getString(SCENE_TAG);
            for (MemoryScene scene : MemoryScene.values()) {
                if (scene.getSerializedName().equals(name)) {
                    return scene;
                }
            }
        }
        return MemoryScene.ASTRONOMERS;
    }

    public static boolean hasAllScenes(Player player) {
        for (MemoryScene scene : MemoryScene.values()) {
            boolean found = false;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.is(ModItems.MEMORY_PLATE.get()) && scene(stack) == scene) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.tbos.memory_plate.named", Component.translatable(scene(stack).titleKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        MemoryScene scene = scene(stack);
        tooltip.add(Component.translatable(scene.descriptionKey()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.tbos.memory_plate.tooltip").withStyle(ChatFormatting.DARK_AQUA));
    }
}
