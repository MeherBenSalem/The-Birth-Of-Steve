package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.item.MemoryScene;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public final class MemoryLanternRenderState extends BlockEntityRenderState {
    public MemoryScene scene;
    public boolean playing;
    public float time;
}
