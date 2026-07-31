package com.nightbeam.tbos.registry;

import com.nightbeam.tbos.platform.registry.ModRegistries;
import com.nightbeam.tbos.platform.registry.RegistryEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeModeTabs {
    public static final RegistryEntry<CreativeModeTab> YESTERGLASS =
            ModRegistries.CREATIVE_MODE_TABS.register(
                    "yesterglass",
                    () -> tabBuilder()
                            .title(Component.translatable("itemGroup.tbos.yesterglass"))
                            .icon(() -> new ItemStack(ModItems.YESTERGLASS_LENS.get()))
                            .displayItems((parameters, output) -> ModItems.addCreativeTabItems(output))
                            .build());

    private ModCreativeModeTabs() {
    }

    /**
     * NeoForge adds a no-argument {@code CreativeModeTab.builder()} that is
     * exactly {@code builder(Row.TOP, 0)}, but vanilla — which the common project
     * compiles against — only has the two-argument form. Calling it directly
     * gives the same tab on both loaders.
     */
    @SuppressWarnings("deprecation")
    private static CreativeModeTab.Builder tabBuilder() {
        return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0);
    }

    /** Touching this class is what queues the tab; the loader flushes later. */
    public static void register() {
    }
}
