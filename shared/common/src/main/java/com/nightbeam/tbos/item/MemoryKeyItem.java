package com.nightbeam.tbos.item;

import com.nightbeam.tbos.expedition.Attunement;
import com.nightbeam.tbos.expedition.ExpeditionKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A Memory Key. Used on a Waystone, the Curator Gateway, or any block inside a
 * structure, it opens the Attunement screen; nothing is spent until the holder
 * presses Begin. Its tier and Distortions live in the stack's data (see
 * {@link ExpeditionKey}); its name and lore are refreshed whenever they change.
 */
public class MemoryKeyItem extends Item {
    public MemoryKeyItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        return Attunement.useKey(context.getLevel(), context.getClickedPos(), context.getPlayer(), context.getHand());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        ExpeditionKey key = ExpeditionKey.read(stack);
        return key.weekly() || key.tier() >= 10 || super.isFoil(stack);
    }
}
