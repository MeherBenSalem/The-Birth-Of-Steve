package com.nightbeam.tbos.item;

import com.nightbeam.tbos.registry.ModDataComponents;
import com.nightbeam.tbos.registry.ModItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** 1.21.1's tooltip hook writes directly to a component list. */
public final class MemoryPlateItem extends Item {
    public MemoryPlateItem(Properties properties) {
        super(properties);
    }

    public static ItemStack forScene(MemoryScene scene) {
        ItemStack stack = new ItemStack(ModItems.MEMORY_PLATE.get());
        stack.set(ModDataComponents.MEMORY_SCENE.get(), scene);
        return stack;
    }

    public static MemoryScene scene(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.MEMORY_SCENE.get(), MemoryScene.ASTRONOMERS);
    }

    public static boolean hasAllScenes(Player player) {
        for (MemoryScene scene : MemoryScene.values()) {
            if (!player.getInventory().contains(stack -> stack.is(ModItems.MEMORY_PLATE.get()) && scene(stack) == scene)) {
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MemoryScene scene = scene(stack);
        tooltip.add(Component.translatable(scene.descriptionKey()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.tbos.memory_plate.tooltip").withStyle(ChatFormatting.DARK_AQUA));
    }
}
