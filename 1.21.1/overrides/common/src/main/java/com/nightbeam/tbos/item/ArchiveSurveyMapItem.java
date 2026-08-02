package com.nightbeam.tbos.item;

import com.nightbeam.tbos.world.AdventureWorldManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ArchiveSurveyMapItem extends Item {
    public ArchiveSurveyMapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        ServerLevel archiveLevel = serverLevel.getServer().getLevel(Level.OVERWORLD);
        if (archiveLevel == null) {
            return InteractionResultHolder.fail(stack);
        }
        BlockPos entrance = AdventureWorldManager.ensureArchive(archiveLevel, serverPlayer.blockPosition());
        int dx = entrance.getX() - serverPlayer.blockPosition().getX();
        int dz = entrance.getZ() - serverPlayer.blockPosition().getZ();
        int distance = (int) Math.round(Math.hypot(dx, dz));
        serverPlayer.sendSystemMessage(Component.translatable(
                        "message.tbos.survey_map.located", direction(dx, dz), distance, entrance.getX(), entrance.getZ())
                .withStyle(ChatFormatting.AQUA));
        return InteractionResultHolder.success(stack);
    }

    public static Component direction(int dx, int dz) {
        String key = Math.abs(dx) > Math.abs(dz)
                ? dx >= 0 ? "direction.tbos.east" : "direction.tbos.west"
                : dz >= 0 ? "direction.tbos.south" : "direction.tbos.north";
        return Component.translatable(key);
    }
}
