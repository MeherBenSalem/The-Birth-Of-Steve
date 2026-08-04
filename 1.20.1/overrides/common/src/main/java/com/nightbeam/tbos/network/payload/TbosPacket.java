package com.nightbeam.tbos.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Loader-neutral wire packet used by the pre-1.20.2 networking adapters. */
public interface TbosPacket {
    ResourceLocation id();

    void write(FriendlyByteBuf buffer);
}
