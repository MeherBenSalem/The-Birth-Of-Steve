package com.nightbeam.tbos.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Readable notes found in Fracture Coffers. The client opens {@code ArchivistNotesScreen}. */
public final class StarterTomeItem extends Item {
    public StarterTomeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tbos.starter_tome.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
