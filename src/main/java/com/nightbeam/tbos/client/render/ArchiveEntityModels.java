package com.nightbeam.tbos.client.render;

/** Shared mesh-building constants for the Archive creature models. */
public final class ArchiveEntityModels {
    /**
     * Halves the UV divisor so a 128x128 sheet carries two texels per model unit.
     *
     * <p>Box UV is otherwise fixed at 1:1 and a larger sheet would only add empty
     * space. {@code CubeDefinition.bake} passes {@code texWidth * texScale.u()} to
     * {@code ModelPart.Cube} as the divisor, so density comes from halving this
     * rather than from the sheet size — every cube must pass it, and the layer
     * must declare 128x128. Existing {@code texOffs} values stay in 64-unit space.
     *
     * <p>{@code tools/textures/archive_entities.py} mirrors this with its own
     * {@code SCALE}; the two have to move together.
     */
    public static final float TEX_SCALE = 0.5F;

    private ArchiveEntityModels() {
    }
}
