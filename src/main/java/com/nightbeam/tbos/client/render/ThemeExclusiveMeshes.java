package com.nightbeam.tbos.client.render;

import static com.nightbeam.tbos.client.render.ArchiveEntityModels.TEX_SCALE;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Mesh geometry for the sixteen theme-exclusive silhouettes.
 *
 * <p>Model space runs 16 units to the block with y increasing downward from a
 * point 24 units above the feet, so {@code y = 24} is the ground. Each mesh is
 * built at the creature's true size rather than being scaled at render time,
 * which is what lets a half-block Shelf Crawler and a 2.2-block Hour Hand Wraith
 * share one renderer.
 *
 * <p>Every box passes {@code TEX_SCALE} on both axes and every layer declares a
 * 128x128 sheet: box UV is otherwise fixed at one texel per model unit, and the
 * density is what pays for material detail. See {@link ArchiveEntityModels}.
 *
 * <p>The {@code *_PARTS} arrays name the parts each poser drives, and are the
 * only place a path string is written down.
 */
final class ThemeExclusiveMeshes {
    private static final int SHEET = 128;

    private ThemeExclusiveMeshes() {
    }

    /** One box at the given UV origin. Chain {@link #add} for multi-box parts. */
    private static CubeListBuilder box(
            int u, int v, float x, float y, float z, float w, float h, float d) {
        return add(CubeListBuilder.create(), u, v, x, y, z, w, h, d);
    }

    private static CubeListBuilder add(
            CubeListBuilder builder,
            int u, int v, float x, float y, float z, float w, float h, float d) {
        return builder.texOffs(u, v).addBox(x, y, z, w, h, d, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE);
    }

    private static CubeListBuilder empty() {
        return CubeListBuilder.create();
    }

    private static LayerDefinition sheet(MeshDefinition mesh) {
        return LayerDefinition.create(mesh, SHEET, SHEET);
    }

    // --- Shard Drifter: a hollow core with plates in orbit, no legs ----------
    static final String[] ORBIT_SHARDS_PARTS = {
        "core", "core/crown", "core/ring", "core/ring/shard_a", "core/ring/shard_b",
        "core/ring/shard_c", "core/tail",
    };

    static LayerDefinition orbitShards() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition core = root.addOrReplaceChild(
                "core", box(0, 0, -2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        core.addOrReplaceChild(
                "crown", box(24, 0, -3.0F, -5.5F, -3.0F, 6.0F, 1.0F, 6.0F), PartPose.ZERO);
        core.addOrReplaceChild(
                "tail", box(24, 10, -1.5F, 3.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.ZERO);
        PartDefinition ring = core.addOrReplaceChild("ring", empty(), PartPose.ZERO);
        // Plates ride a shared ring so one rotation drives the whole orbit.
        CubeListBuilder shard = box(0, 12, -1.0F, -3.5F, -2.0F, 2.0F, 7.0F, 4.0F);
        ring.addOrReplaceChild("shard_a", shard, PartPose.offset(0.0F, 0.0F, 5.5F));
        ring.addOrReplaceChild(
                "shard_b", shard,
                PartPose.offsetAndRotation(4.76F, 0.0F, -2.75F, 0.0F, 2.094F, 0.0F));
        ring.addOrReplaceChild(
                "shard_c", shard,
                PartPose.offsetAndRotation(-4.76F, 0.0F, -2.75F, 0.0F, -2.094F, 0.0F));
        return sheet(mesh);
    }

    // --- Wake Cutter: thin body carrying one long blade ----------------------
    static final String[] SCYTHE_PARTS = {
        "torso", "torso/head", "torso/shoulder", "torso/arm_blade",
        "torso/arm_blade/blade", "torso/arm_free", "leg_left", "leg_right",
    };

    static LayerDefinition scythe() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder leg = box(14, 20, -1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F);
        root.addOrReplaceChild("leg_left", leg, PartPose.offset(2.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("leg_right", leg, PartPose.offset(-2.0F, 14.0F, 0.0F));
        PartDefinition torso = root.addOrReplaceChild(
                "torso", box(0, 0, -3.0F, -14.0F, -2.0F, 6.0F, 14.0F, 4.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        torso.addOrReplaceChild(
                "head", box(22, 0, -2.5F, -4.0F, -2.5F, 5.0F, 4.0F, 5.0F),
                PartPose.offset(0.0F, -14.5F, 0.0F));
        torso.addOrReplaceChild(
                "shoulder", box(28, 20, -4.0F, -3.0F, -2.5F, 8.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, -11.0F, 0.0F));
        PartDefinition armBlade = torso.addOrReplaceChild(
                "arm_blade", box(44, 0, -1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(5.0F, -11.0F, 0.0F));
        // The blade hangs past the hand and trails; that overhang is the read.
        armBlade.addOrReplaceChild(
                "blade", box(0, 20, -0.5F, 0.0F, -2.5F, 1.0F, 16.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, 0.0F, -0.45F, 0.0F, 0.0F));
        torso.addOrReplaceChild(
                "arm_free", box(44, 0, -1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(-5.0F, -11.0F, 0.0F));
        return sheet(mesh);
    }

    // --- Null Portrait: a gilt frame around nothing --------------------------
    static final String[] FLAT_FRAME_PARTS = {
        "frame", "frame/canvas", "frame/finial", "frame/rail_top", "frame/rail_bottom",
        "frame/post_left", "frame/post_right",
    };

    static LayerDefinition flatFrame() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition frame = root.addOrReplaceChild(
                "frame", empty(), PartPose.offset(0.0F, 24.0F, 0.0F));
        CubeListBuilder rail = box(0, 0, -7.0F, -3.0F, -1.0F, 14.0F, 3.0F, 2.0F);
        frame.addOrReplaceChild("rail_top", rail, PartPose.offset(0.0F, -27.0F, 0.0F));
        frame.addOrReplaceChild("rail_bottom", rail, PartPose.offset(0.0F, 0.0F, 0.0F));
        CubeListBuilder post = box(0, 7, -1.0F, -24.0F, -1.0F, 2.0F, 24.0F, 2.0F);
        frame.addOrReplaceChild("post_left", post, PartPose.offset(-6.0F, -3.0F, 0.0F));
        frame.addOrReplaceChild("post_right", post, PartPose.offset(6.0F, -3.0F, 0.0F));
        // The canvas clears the frame on every side; coplanar faces z-fight.
        frame.addOrReplaceChild(
                "canvas", box(10, 7, -4.5F, -22.0F, -0.5F, 9.0F, 22.0F, 1.0F),
                PartPose.offset(0.0F, -4.0F, 0.0F));
        frame.addOrReplaceChild(
                "finial", box(34, 0, -1.5F, -2.0F, -1.5F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, -30.0F, 0.0F));
        return sheet(mesh);
    }

    // --- Gallery Moth: small body under four broad wings ---------------------
    static final String[] WINGED_PARTS = {
        "body", "body/head", "body/antenna_left", "body/antenna_right",
        "body/wing_upper_left", "body/wing_upper_right",
        "body/wing_lower_left", "body/wing_lower_right",
    };

    static LayerDefinition winged() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild(
                "body", box(0, 0, -2.0F, -3.0F, -1.5F, 4.0F, 6.0F, 3.0F),
                PartPose.offset(0.0F, 17.0F, 0.0F));
        body.addOrReplaceChild(
                "head", box(16, 0, -1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, -3.0F, 0.0F));
        CubeListBuilder antenna = box(34, 0, -0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F);
        body.addOrReplaceChild(
                "antenna_left", antenna,
                PartPose.offsetAndRotation(1.0F, -5.5F, 0.0F, -0.3F, 0.0F, -0.4F));
        body.addOrReplaceChild(
                "antenna_right", antenna,
                PartPose.offsetAndRotation(-1.0F, -5.5F, 0.0F, -0.3F, 0.0F, 0.4F));
        CubeListBuilder wingUpper = box(0, 11, 0.0F, -0.5F, -3.5F, 9.0F, 1.0F, 7.0F);
        body.addOrReplaceChild("wing_upper_left", wingUpper, PartPose.offset(1.5F, -2.0F, 0.0F));
        body.addOrReplaceChild(
                "wing_upper_right", wingUpper.mirror(),
                PartPose.offset(-1.5F, -2.0F, 0.0F));
        CubeListBuilder wingLower = box(0, 21, 0.0F, -0.5F, -2.5F, 6.0F, 1.0F, 5.0F);
        body.addOrReplaceChild("wing_lower_left", wingLower, PartPose.offset(1.5F, 1.5F, 0.0F));
        body.addOrReplaceChild(
                "wing_lower_right", wingLower.mirror(), PartPose.offset(-1.5F, 1.5F, 0.0F));
        return sheet(mesh);
    }

    // --- Gnomon Knight: heavy plate under a sundial crest --------------------
    static final String[] HEAVY_KNIGHT_PARTS = {
        "torso", "torso/helm", "torso/helm/crest", "torso/pauldron_left",
        "torso/pauldron_right", "torso/arm_shield", "torso/arm_shield/shield",
        "torso/arm_free", "leg_left", "leg_right",
    };

    static LayerDefinition heavyKnight() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder leg = box(0, 34, -2.0F, 0.0F, -2.0F, 4.0F, 11.0F, 4.0F);
        root.addOrReplaceChild("leg_left", leg, PartPose.offset(2.5F, 13.0F, 0.0F));
        root.addOrReplaceChild("leg_right", leg, PartPose.offset(-2.5F, 13.0F, 0.0F));
        PartDefinition torso = root.addOrReplaceChild(
                "torso", box(0, 0, -5.0F, -12.0F, -3.0F, 10.0F, 12.0F, 6.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        PartDefinition helm = torso.addOrReplaceChild(
                "helm", box(14, 20, -3.5F, -6.0F, -3.5F, 7.0F, 6.0F, 7.0F),
                PartPose.offset(0.0F, -12.5F, 0.0F));
        // A gnomon, not a plume: the blade that casts the hour.
        helm.addOrReplaceChild(
                "crest", box(0, 20, -0.5F, -7.0F, -2.5F, 1.0F, 7.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.35F, 0.0F, 0.0F));
        CubeListBuilder pauldron = box(34, 0, -2.5F, -2.0F, -3.5F, 5.0F, 4.0F, 7.0F);
        torso.addOrReplaceChild("pauldron_left", pauldron, PartPose.offset(6.0F, -10.0F, 0.0F));
        torso.addOrReplaceChild(
                "pauldron_right", pauldron.mirror(), PartPose.offset(-6.0F, -10.0F, 0.0F));
        PartDefinition armShield = torso.addOrReplaceChild(
                "arm_shield", box(44, 20, -2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(6.5F, -7.0F, 0.0F));
        armShield.addOrReplaceChild(
                "shield", box(18, 34, -4.0F, -1.0F, -0.5F, 8.0F, 12.0F, 1.0F),
                PartPose.offset(0.0F, 3.0F, -3.5F));
        torso.addOrReplaceChild(
                "arm_free", box(44, 20, -2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-6.5F, -7.0F, 0.0F));
        return sheet(mesh);
    }

    // --- Armillary Scout: a lens inside two rings ----------------------------
    static final String[] RINGED_PARTS = {
        "hull", "hull/lens", "hull/ring_x", "hull/ring_z", "hull/fin_left", "hull/fin_right",
    };

    static LayerDefinition ringed() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition hull = root.addOrReplaceChild(
                "hull", box(0, 0, -2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        hull.addOrReplaceChild(
                "lens", box(0, 12, -1.5F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, -3.0F));
        // Rings are open frames, four bars each, so the lens shows through.
        hull.addOrReplaceChild(
                "ring_x",
                add(add(add(box(22, 0, -4.5F, -0.5F, -4.5F, 9.0F, 1.0F, 1.0F),
                        22, 0, -4.5F, -0.5F, 3.5F, 9.0F, 1.0F, 1.0F),
                        22, 4, -4.5F, -0.5F, -3.5F, 1.0F, 1.0F, 7.0F),
                        22, 4, 3.5F, -0.5F, -3.5F, 1.0F, 1.0F, 7.0F),
                PartPose.rotation(0.35F, 0.0F, 0.0F));
        hull.addOrReplaceChild(
                "ring_z",
                add(add(add(box(22, 0, -4.5F, -4.5F, -0.5F, 9.0F, 1.0F, 1.0F),
                        22, 0, -4.5F, 3.5F, -0.5F, 9.0F, 1.0F, 1.0F),
                        22, 14, -4.5F, -3.5F, -0.5F, 1.0F, 7.0F, 1.0F),
                        22, 14, 3.5F, -3.5F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.rotation(0.0F, 0.0F, 0.55F));
        CubeListBuilder fin = box(10, 12, -1.0F, -1.0F, 0.0F, 2.0F, 5.0F, 2.0F);
        hull.addOrReplaceChild(
                "fin_left", fin, PartPose.offsetAndRotation(2.5F, 2.0F, 2.5F, 0.3F, 0.6F, 0.0F));
        hull.addOrReplaceChild(
                "fin_right", fin, PartPose.offsetAndRotation(-2.5F, 2.0F, 2.5F, 0.3F, -0.6F, 0.0F));
        return sheet(mesh);
    }

    // --- Dust Cantorile: robed, hovering, no legs ----------------------------
    static final String[] ROBED_PARTS = {
        "robe", "robe/cowl", "robe/hem", "robe/sleeve_left", "robe/sleeve_right",
        "robe/sleeve_left/censer",
    };

    static LayerDefinition robed() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition robe = root.addOrReplaceChild(
                "robe", box(0, 0, -4.5F, -16.0F, -4.0F, 9.0F, 16.0F, 8.0F),
                PartPose.offset(0.0F, 20.0F, 0.0F));
        robe.addOrReplaceChild(
                "cowl", box(36, 0, -3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, -16.0F, 0.0F));
        // The hem flares below and never touches the floor; it hovers.
        robe.addOrReplaceChild(
                "hem", box(14, 26, -5.5F, 0.0F, -5.0F, 11.0F, 3.0F, 10.0F),
                PartPose.ZERO);
        CubeListBuilder sleeve = box(0, 26, -1.5F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F);
        PartDefinition sleeveLeft = robe.addOrReplaceChild(
                "sleeve_left", sleeve, PartPose.offset(5.0F, -13.0F, 0.0F));
        sleeveLeft.addOrReplaceChild(
                "censer", box(0, 42, -1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, 11.0F, 0.0F));
        robe.addOrReplaceChild("sleeve_right", sleeve, PartPose.offset(-5.0F, -13.0F, 0.0F));
        return sheet(mesh);
    }

    // --- Ash Chorister: one body already cracked down the middle -------------
    static final String[] SPLIT_CORE_PARTS = {
        "shell", "shell/half_left", "shell/half_right", "shell/crown", "shell/core",
        "shell/wisp_left", "shell/wisp_right",
    };

    static LayerDefinition splitCore() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition shell = root.addOrReplaceChild(
                "shell", empty(), PartPose.offset(0.0F, 22.0F, 0.0F));
        CubeListBuilder half = box(0, 0, -3.0F, -12.0F, -2.5F, 3.0F, 12.0F, 5.0F);
        // The seam is the tell: the halves are separate parts from the start, so
        // the split at 45% health is a pose change rather than a model swap.
        shell.addOrReplaceChild("half_left", half, PartPose.offset(3.2F, 0.0F, 0.0F));
        shell.addOrReplaceChild("half_right", half.mirror(), PartPose.offset(-0.2F, 0.0F, 0.0F));
        shell.addOrReplaceChild(
                "crown", box(18, 0, -3.5F, -2.0F, -2.5F, 7.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, -12.0F, 0.0F));
        shell.addOrReplaceChild(
                "core", box(44, 0, -1.5F, -8.0F, -1.5F, 3.0F, 5.0F, 3.0F), PartPose.ZERO);
        CubeListBuilder wisp = box(0, 19, -1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F);
        shell.addOrReplaceChild("wisp_left", wisp, PartPose.offset(5.5F, -6.0F, 0.0F));
        shell.addOrReplaceChild("wisp_right", wisp, PartPose.offset(-5.5F, -6.0F, 0.0F));
        return sheet(mesh);
    }

    // --- Prism Stalker: a faceted crystal that opens when vulnerable ---------
    static final String[] PRISM_PARTS = {
        "prism", "prism/upper", "prism/lower", "prism/core", "prism/facet",
        "leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right",
    };

    static LayerDefinition prism() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder leg = box(34, 8, -1.0F, 0.0F, -1.0F, 2.0F, 13.0F, 2.0F);
        root.addOrReplaceChild(
                "leg_front_left", leg,
                PartPose.offsetAndRotation(3.5F, 11.0F, -3.5F, -0.25F, 0.0F, -0.25F));
        root.addOrReplaceChild(
                "leg_front_right", leg,
                PartPose.offsetAndRotation(-3.5F, 11.0F, -3.5F, -0.25F, 0.0F, 0.25F));
        root.addOrReplaceChild(
                "leg_back_left", leg,
                PartPose.offsetAndRotation(3.5F, 11.0F, 3.5F, 0.25F, 0.0F, -0.25F));
        root.addOrReplaceChild(
                "leg_back_right", leg,
                PartPose.offsetAndRotation(-3.5F, 11.0F, 3.5F, 0.25F, 0.0F, 0.25F));
        PartDefinition prism = root.addOrReplaceChild(
                "prism", empty(), PartPose.offset(0.0F, 11.0F, 0.0F));
        // Two shells around an exposed core; they part during the ACTIVE window.
        prism.addOrReplaceChild(
                "upper", box(0, 0, -4.0F, -7.0F, -4.0F, 8.0F, 7.0F, 8.0F),
                PartPose.offset(0.0F, -0.5F, 0.0F));
        prism.addOrReplaceChild(
                "lower", box(0, 17, -4.0F, 0.0F, -4.0F, 8.0F, 7.0F, 8.0F),
                PartPose.offset(0.0F, 0.5F, 0.0F));
        prism.addOrReplaceChild(
                "core", box(44, 8, -2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.ZERO);
        prism.addOrReplaceChild(
                "facet", box(34, 0, -3.0F, -2.5F, -0.5F, 6.0F, 5.0F, 1.0F),
                PartPose.offset(0.0F, -3.0F, -4.5F));
        return sheet(mesh);
    }

    // --- Shardling Swarm: five splinters around a shared hub -----------------
    static final String[] SWARM_PARTS = {
        "hub", "hub/shard_a", "hub/shard_b", "hub/shard_c", "hub/shard_d", "hub/shard_e",
    };

    static LayerDefinition swarm() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition hub = root.addOrReplaceChild(
                "hub", box(24, 0, -1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 19.0F, 0.0F));
        CubeListBuilder big = box(0, 0, -1.5F, -2.0F, -1.5F, 3.0F, 4.0F, 3.0F);
        CubeListBuilder small = box(14, 0, -1.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F);
        // No body to hold them: the swarm reads as a cloud that keeps formation.
        hub.addOrReplaceChild("shard_a", big, PartPose.offset(0.0F, -2.5F, 0.0F));
        hub.addOrReplaceChild("shard_b", big, PartPose.offset(3.0F, 1.0F, -2.0F));
        hub.addOrReplaceChild("shard_c", small, PartPose.offset(-3.0F, 0.5F, 2.0F));
        hub.addOrReplaceChild("shard_d", small, PartPose.offset(2.5F, 2.5F, 2.5F));
        hub.addOrReplaceChild("shard_e", small, PartPose.offset(-2.5F, -1.5F, -2.5F));
        return sheet(mesh);
    }

    // --- Index Wight: a reader with a fan of pages for a back ----------------
    static final String[] PAGE_PARTS = {
        "torso", "torso/head", "torso/skirt", "torso/page_left", "torso/page_mid",
        "torso/page_right", "torso/arm_quill", "torso/arm_quill/quill", "torso/arm_free",
    };

    static LayerDefinition page() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition torso = root.addOrReplaceChild(
                "torso", box(0, 0, -3.0F, -13.0F, -2.0F, 6.0F, 13.0F, 4.0F),
                PartPose.offset(0.0F, 17.0F, 0.0F));
        torso.addOrReplaceChild(
                "head", box(22, 0, -2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, -13.0F, 0.0F));
        torso.addOrReplaceChild(
                "skirt", box(16, 19, -4.0F, 0.0F, -3.0F, 8.0F, 7.0F, 6.0F), PartPose.ZERO);
        // Pages fan behind rather than hanging flat, so the outline stays open.
        CubeListBuilder leaf = box(44, 0, -3.5F, -9.0F, 0.0F, 7.0F, 9.0F, 1.0F);
        torso.addOrReplaceChild(
                "page_left", leaf,
                PartPose.offsetAndRotation(2.0F, -6.0F, 2.0F, 0.0F, 0.6F, 0.12F));
        torso.addOrReplaceChild(
                "page_mid", leaf, PartPose.offsetAndRotation(0.0F, -7.0F, 2.5F, 0.0F, 0.0F, 0.0F));
        torso.addOrReplaceChild(
                "page_right", leaf,
                PartPose.offsetAndRotation(-2.0F, -6.0F, 2.0F, 0.0F, -0.6F, -0.12F));
        CubeListBuilder arm = box(0, 19, -1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F);
        PartDefinition armQuill = torso.addOrReplaceChild(
                "arm_quill", arm, PartPose.offset(4.0F, -11.5F, 0.0F));
        armQuill.addOrReplaceChild(
                "quill", box(10, 19, -0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, -0.8F, 0.0F, 0.0F));
        torso.addOrReplaceChild("arm_free", arm, PartPose.offset(-4.0F, -11.5F, 0.0F));
        return sheet(mesh);
    }

    // --- Shelf Crawler: wide, flat, six hooked limbs -------------------------
    static final String[] CLINGER_PARTS = {
        "carapace", "carapace/head", "carapace/head/mandible_left",
        "carapace/head/mandible_right", "limb_a", "limb_b", "limb_c",
        "limb_d", "limb_e", "limb_f",
    };

    static LayerDefinition clinger() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition carapace = root.addOrReplaceChild(
                "carapace", box(0, 0, -6.0F, -3.0F, -5.0F, 12.0F, 3.0F, 10.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));
        PartDefinition head = carapace.addOrReplaceChild(
                "head", box(46, 0, -2.5F, -3.0F, -4.0F, 5.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, -5.0F));
        CubeListBuilder mandible = box(6, 15, -0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 4.0F);
        head.addOrReplaceChild(
                "mandible_left", mandible,
                PartPose.offsetAndRotation(1.5F, -1.0F, -4.0F, 0.0F, -0.35F, 0.0F));
        head.addOrReplaceChild(
                "mandible_right", mandible,
                PartPose.offsetAndRotation(-1.5F, -1.0F, -4.0F, 0.0F, 0.35F, 0.0F));
        // Limbs splay outward and hook down, so it reads as clinging even flat.
        CubeListBuilder limb = box(0, 15, -0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F);
        float[][] mounts = {
            {5.5F, -3.5F, -1.0F}, {5.5F, 0.5F, -1.0F}, {5.5F, 4.0F, -1.0F},
            {-5.5F, -3.5F, 1.0F}, {-5.5F, 0.5F, 1.0F}, {-5.5F, 4.0F, 1.0F},
        };
        String[] names = {"limb_a", "limb_b", "limb_c", "limb_d", "limb_e", "limb_f"};
        for (int i = 0; i < names.length; i++) {
            float[] mount = mounts[i];
            root.addOrReplaceChild(
                    names[i], limb,
                    PartPose.offsetAndRotation(
                            mount[0], 21.0F, mount[1], 0.0F, 0.0F, mount[2]));
        }
        return sheet(mesh);
    }

    // --- Metronome Hound: quadruped with a pendulum for a tail ---------------
    static final String[] HOUND_PARTS = {
        "body", "body/head", "body/head/jaw", "body/tail_rod", "body/tail_rod/pendulum",
        "leg_front_left", "leg_front_right", "leg_back_left", "leg_back_right",
    };

    static LayerDefinition hound() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder leg = box(18, 21, -1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F);
        root.addOrReplaceChild("leg_front_left", leg, PartPose.offset(2.5F, 17.0F, -4.0F));
        root.addOrReplaceChild("leg_front_right", leg, PartPose.offset(-2.5F, 17.0F, -4.0F));
        root.addOrReplaceChild("leg_back_left", leg, PartPose.offset(2.5F, 17.0F, 4.0F));
        root.addOrReplaceChild("leg_back_right", leg, PartPose.offset(-2.5F, 17.0F, 4.0F));
        PartDefinition body = root.addOrReplaceChild(
                "body", box(0, 0, -3.5F, -3.5F, -6.0F, 7.0F, 7.0F, 12.0F),
                PartPose.offset(0.0F, 13.5F, 0.0F));
        PartDefinition head = body.addOrReplaceChild(
                "head", box(40, 0, -2.5F, -2.5F, -6.0F, 5.0F, 5.0F, 6.0F),
                PartPose.offset(0.0F, -0.5F, -6.0F));
        head.addOrReplaceChild(
                "jaw", box(0, 21, -2.0F, 0.0F, -4.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 2.0F, -2.0F));
        // The tail keeps time whether or not the hound is moving. That is the
        // whole read for its on-beat bite, so it swings from its own clock.
        PartDefinition rod = body.addOrReplaceChild(
                "tail_rod", box(28, 21, -0.5F, 0.0F, -0.5F, 1.0F, 9.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -2.5F, 6.0F, -0.5F, 0.0F, 0.0F));
        rod.addOrReplaceChild(
                "pendulum", box(34, 21, -1.5F, 0.0F, -1.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 9.0F, 0.0F));
        return sheet(mesh);
    }

    // --- Labyrinth Usher: tall, faceless, carries the lantern ----------------
    static final String[] USHER_PARTS = {
        "coat", "coat/head", "coat/collar", "coat/hem", "coat/arm_lantern",
        "coat/arm_lantern/lantern", "coat/arm_free",
    };

    static LayerDefinition usher() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition coat = root.addOrReplaceChild(
                "coat", box(0, 0, -4.0F, -18.0F, -3.0F, 8.0F, 18.0F, 6.0F),
                PartPose.offset(0.0F, 21.0F, 0.0F));
        coat.addOrReplaceChild(
                "head", box(30, 0, -2.5F, -6.0F, -2.5F, 5.0F, 6.0F, 5.0F),
                PartPose.offset(0.0F, -19.0F, 0.0F));
        coat.addOrReplaceChild(
                "collar", box(0, 26, -5.0F, -1.0F, -4.0F, 10.0F, 2.0F, 8.0F),
                PartPose.offset(0.0F, -18.0F, 0.0F));
        coat.addOrReplaceChild(
                "hem", box(18, 48, -4.5F, 0.0F, -3.5F, 9.0F, 4.0F, 7.0F), PartPose.ZERO);
        CubeListBuilder arm = box(38, 26, -1.5F, 0.0F, -1.5F, 3.0F, 13.0F, 3.0F);
        PartDefinition armLantern = coat.addOrReplaceChild(
                "arm_lantern", arm, PartPose.offset(5.0F, -16.0F, 0.0F));
        armLantern.addOrReplaceChild(
                "lantern", box(0, 38, -2.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        coat.addOrReplaceChild("arm_free", arm, PartPose.offset(-5.0F, -16.0F, 0.0F));
        return sheet(mesh);
    }

    // --- Blank Chronist: a slab where the face should be ---------------------
    static final String[] BLANK_PARTS = {
        "torso", "torso/slab", "torso/skirt", "torso/arm_left", "torso/arm_right",
        "leg_left", "leg_right",
    };

    static LayerDefinition blank() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder leg = box(0, 19, -1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F);
        root.addOrReplaceChild("leg_left", leg, PartPose.offset(2.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("leg_right", leg, PartPose.offset(-2.0F, 14.0F, 0.0F));
        PartDefinition torso = root.addOrReplaceChild(
                "torso", box(0, 0, -3.5F, -12.0F, -2.5F, 7.0F, 12.0F, 5.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        // A blank tablet stood on end, not a head: nothing to read it back.
        torso.addOrReplaceChild(
                "slab", box(26, 0, -3.0F, -7.0F, -1.0F, 6.0F, 7.0F, 2.0F),
                PartPose.offset(0.0F, -12.0F, 0.0F));
        torso.addOrReplaceChild(
                "skirt", box(14, 19, -4.5F, 0.0F, -3.5F, 9.0F, 6.0F, 7.0F), PartPose.ZERO);
        CubeListBuilder arm = box(48, 0, -1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F);
        torso.addOrReplaceChild("arm_left", arm, PartPose.offset(5.0F, -10.5F, 0.0F));
        torso.addOrReplaceChild("arm_right", arm, PartPose.offset(-5.0F, -10.5F, 0.0F));
        return sheet(mesh);
    }

    // --- Hour Hand Wraith: one arm far too long for the body -----------------
    static final String[] LONG_ARM_PARTS = {
        "torso", "torso/head", "torso/shroud", "torso/arm_hour",
        "torso/arm_hour/hand_blade", "torso/arm_small",
    };

    static LayerDefinition longArm() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition torso = root.addOrReplaceChild(
                "torso", box(0, 0, -3.0F, -13.0F, -2.5F, 6.0F, 13.0F, 5.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        torso.addOrReplaceChild(
                "head", box(24, 0, -2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, -13.0F, 0.0F));
        // The shroud replaces legs; the wraith drags rather than walks.
        torso.addOrReplaceChild(
                "shroud", box(22, 26, -4.0F, 0.0F, -3.0F, 8.0F, 8.0F, 6.0F), PartPose.ZERO);
        PartDefinition armHour = torso.addOrReplaceChild(
                "arm_hour", box(46, 0, -1.5F, 0.0F, -1.5F, 3.0F, 22.0F, 3.0F),
                PartPose.offsetAndRotation(5.0F, -11.0F, 0.0F, 0.0F, 0.0F, -0.18F));
        armHour.addOrReplaceChild(
                "hand_blade", box(10, 20, -0.5F, 0.0F, -2.0F, 1.0F, 14.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 21.0F, 0.0F, -0.25F, 0.0F, 0.0F));
        torso.addOrReplaceChild(
                "arm_small", box(0, 20, -1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offset(-4.5F, -11.0F, 0.0F));
        return sheet(mesh);
    }
}
