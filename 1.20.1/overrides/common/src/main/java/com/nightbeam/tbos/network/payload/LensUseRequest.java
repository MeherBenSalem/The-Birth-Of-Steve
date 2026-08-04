package com.nightbeam.tbos.network.payload;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.resources.ResourceLocation;

public record LensUseRequest() implements TbosPacket {
    public static final LensUseRequest INSTANCE = new LensUseRequest();
    public static final ResourceLocation ID = new ResourceLocation(Yesterglass.MOD_ID, "lens_use");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(net.minecraft.network.FriendlyByteBuf buffer) {
    }
}
