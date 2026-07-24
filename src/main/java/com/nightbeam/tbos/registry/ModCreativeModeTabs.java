package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeModeTabs {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Yesterglass.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> YESTERGLASS =
            CREATIVE_MODE_TABS.register(
                    "yesterglass",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.tbos.yesterglass"))
                            .icon(() -> new ItemStack(ModItems.YESTERGLASS_LENS.get()))
                            .displayItems((parameters, output) -> ModItems.addCreativeTabItems(output))
                            .build());

    private ModCreativeModeTabs() {
    }

    public static void register(IEventBus modBus) {
        CREATIVE_MODE_TABS.register(modBus);
    }
}
