package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.client.render.ThemeExclusiveSilhouettes.Rig;
import com.nightbeam.tbos.entity.ThemeExclusiveEntity.AbilityPhase;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Animation for the sixteen theme-exclusive silhouettes.
 *
 * <p>Procedural trigonometry rather than keyframe tables, matching the five
 * hand-built Archive creatures: the vanilla {@code AnimationDefinition} API
 * expects Blockbench exports running tens of kilobytes each, which would sit
 * badly beside hand-authored meshes.
 *
 * <p>Every signature move is a server-owned phase machine. These posers only
 * read the phase and its progress out of the render state and derive a pose;
 * they never advance anything themselves, so a move interrupted by a reload
 * restarts rather than resolving mid-swing.
 *
 * <p>Poses are applied as deltas because the caller resets the rig first.
 */
final class ThemeExclusivePosers {
    private static final float PI = (float) Math.PI;

    private ThemeExclusivePosers() {
    }

    // --- shared motion ------------------------------------------------------
    /** Signed stride, -1..1, scaled by how fast the mob is actually moving. */
    private static float stride(ThemeExclusiveRenderState state, float rate) {
        return Mth.cos(state.walkAnimationPos * rate)
                * Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
    }

    /** Unsigned footfall, 0..1, for body dip and bounce. */
    private static float footfall(ThemeExclusiveRenderState state, float rate) {
        return Math.abs(Mth.sin(state.walkAnimationPos * rate))
                * Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
    }

    private static float bob(ThemeExclusiveRenderState state, float rate) {
        return Mth.sin(state.ageInTicks * rate);
    }

    /** Progress through the current phase, or 0 when idle. */
    private static float phase(ThemeExclusiveRenderState state, AbilityPhase wanted) {
        return state.abilityPhase == wanted ? state.abilityProgress : 0.0F;
    }

    /** A single rise-and-fall lobe across a phase, for strikes and snaps. */
    private static float lobe(float progress) {
        return Mth.sin(Mth.clamp(progress, 0.0F, 1.0F) * PI);
    }

    /**
     * 1 while the given phase is current, 0 otherwise.
     *
     * <p>A move that continues from where the wind-up left a limb has to carry
     * that wind-up offset into the next phase. Written bare, that offset also
     * applies at rest — {@link #phase} returns 0 when idle, which zeroes the
     * *travel* term but not the constant beside it, so the limb sits at the
     * mid-ability angle forever. Multiplying the whole expression by this is
     * what keeps the carry-over inside its own phase.
     */
    private static float during(ThemeExclusiveRenderState state, AbilityPhase wanted) {
        return state.abilityPhase == wanted ? 1.0F : 0.0F;
    }

    private static void recoil(Rig rig, String path, ThemeExclusiveRenderState state, float amount) {
        rig.get(path).zRot += Mth.sin(state.hurtTime * PI) * amount;
    }

    private static void swingLegs(Rig rig, ThemeExclusiveRenderState state, String left, String right) {
        float swing = stride(state, 0.66F) * 1.1F;
        rig.get(left).xRot += swing;
        rig.get(right).xRot -= swing;
    }

    // --- Shard Drifter ------------------------------------------------------
    static void orbitShards(Rig rig, ThemeExclusiveRenderState state) {
        ModelPart core = rig.get("core");
        ModelPart ring = rig.get("core/ring");
        core.y += bob(state, 0.08F) * 1.1F;
        core.yRot += state.ageInTicks * 0.02F;
        rig.get("core/crown").yRot -= state.ageInTicks * 0.05F;
        rig.get("core/tail").xRot += bob(state, 0.06F) * 0.2F;
        // The orbit spins up before the blink and stalls after it, so the tell
        // is in the plates rather than in a body wind-up it does not have.
        float spin = 0.06F + phase(state, AbilityPhase.WINDUP) * 0.5F;
        ring.yRot += state.ageInTicks * spin;
        float flare = phase(state, AbilityPhase.ACTIVE);
        for (String shard : new String[] {"core/ring/shard_a", "core/ring/shard_b", "core/ring/shard_c"}) {
            rig.get(shard).xRot += flare * 0.9F + bob(state, 0.11F) * 0.12F;
        }
        ring.zRot += phase(state, AbilityPhase.RECOVERY) * -0.4F;
        recoil(rig, "core", state, 0.2F);
    }

    // --- Wake Cutter --------------------------------------------------------
    static void scythe(Rig rig, ThemeExclusiveRenderState state) {
        ModelPart torso = rig.get("torso");
        ModelPart arm = rig.get("torso/arm_blade");
        swingLegs(rig, state, "leg_left", "leg_right");
        torso.xRot += footfall(state, 0.66F) * 0.06F;
        rig.get("torso/head").yRot += bob(state, 0.05F) * 0.15F;
        rig.get("torso/arm_free").xRot += stride(state, 0.66F) * -0.5F;
        arm.xRot += stride(state, 0.66F) * 0.4F;
        // Winds the blade back over the shoulder, then throws the whole body
        // through the dash — the mob is invulnerable for exactly that window.
        float wind = phase(state, AbilityPhase.WINDUP);
        arm.xRot -= wind * 2.2F;
        arm.zRot -= wind * 0.5F;
        torso.yRot += wind * 0.6F;
        float cut = phase(state, AbilityPhase.ACTIVE);
        arm.xRot += cut * 3.0F;
        torso.yRot -= cut * 1.2F;
        torso.xRot += lobe(cut) * 0.5F;
        rig.get("torso/arm_blade/blade").xRot += lobe(cut) * -0.6F;
        arm.xRot -= phase(state, AbilityPhase.RECOVERY) * 0.4F;
        recoil(rig, "torso", state, 0.16F);
    }

    // --- Null Portrait ------------------------------------------------------
    static void flatFrame(Rig rig, ThemeExclusiveRenderState state) {
        ModelPart frame = rig.get("frame");
        // Rests edge-on and nearly invisible. Turning face-on *is* the wind-up;
        // by the time you can see the canvas the lunge has already started.
        float turn = phase(state, AbilityPhase.WINDUP);
        float lunge = phase(state, AbilityPhase.ACTIVE);
        float settle = phase(state, AbilityPhase.RECOVERY);
        float facing = 1.0F - Math.max(turn, Math.max(lunge, 1.0F - settle));
        frame.yRot += facing * (PI / 2.0F);
        frame.y += bob(state, 0.04F) * 0.6F - footfall(state, 0.5F) * 0.4F;
        frame.xRot -= lunge * 0.45F;
        frame.z -= lunge * 3.0F;
        rig.get("frame/canvas").z -= lunge * 1.2F;
        rig.get("frame/finial").xRot += bob(state, 0.07F) * 0.1F;
        recoil(rig, "frame", state, 0.22F);
    }

    // --- Gallery Moth -------------------------------------------------------
    static void winged(Rig rig, ThemeExclusiveRenderState state) {
        ModelPart body = rig.get("body");
        // Wings beat on their own clock; the dimming pulse only widens them.
        float beat = 0.9F + phase(state, AbilityPhase.WINDUP) * 1.6F;
        float flap = Mth.sin(state.ageInTicks * beat);
        float spread = phase(state, AbilityPhase.ACTIVE);
        rig.get("body/wing_upper_left").zRot += flap * 0.7F - spread * 0.5F;
        rig.get("body/wing_upper_right").zRot -= flap * 0.7F - spread * 0.5F;
        rig.get("body/wing_lower_left").zRot += flap * 0.5F - spread * 0.35F;
        rig.get("body/wing_lower_right").zRot -= flap * 0.5F - spread * 0.35F;
        body.y += flap * 0.7F;
        body.xRot += 0.25F + spread * 0.3F;
        rig.get("body/antenna_left").xRot += flap * 0.15F;
        rig.get("body/antenna_right").xRot += flap * 0.15F;
        rig.get("body/head").xRot -= spread * 0.3F;
        recoil(rig, "body", state, 0.3F);
    }

    // --- Gnomon Knight ------------------------------------------------------
    static void heavyKnight(Rig rig, ThemeExclusiveRenderState state) {
        ModelPart torso = rig.get("torso");
        ModelPart shield = rig.get("torso/arm_shield");
        swingLegs(rig, state, "leg_left", "leg_right");
        float fall = footfall(state, 0.6F);
        rig.root().y += fall * 0.8F;
        torso.xRot += fall * 0.05F;
        rig.get("torso/arm_free").xRot += stride(state, 0.6F) * -0.35F;
        // The crest keeps its own angle to the ground like a real gnomon, so it
        // stays readable as a sundial rather than a helmet plume.
        rig.get("torso/helm/crest").xRot -= torso.xRot * 0.8F + bob(state, 0.03F) * 0.05F;
        float raise = phase(state, AbilityPhase.WINDUP);
        shield.xRot -= raise * raise * 2.4F;
        torso.xRot -= raise * 0.3F;
        rig.root().y -= raise * 1.4F;
        float slam = phase(state, AbilityPhase.ACTIVE);
        // Drives down from the raised angle the wind-up ended on, but only while
        // the slam is actually running — otherwise the shield stays up forever.
        shield.xRot -= during(state, AbilityPhase.ACTIVE) * (2.4F - slam * 3.0F);
        torso.xRot += slam * 0.5F;
        rig.root().y += slam * 1.6F;
        float rebound = phase(state, AbilityPhase.RECOVERY);
        shield.xRot += Mth.sin((1.0F - rebound) * PI * 2.0F) * (1.0F - rebound) * 0.5F;
        float swing = Mth.sin(state.attackTime * PI);
        rig.get("torso/arm_free").xRot -= swing * 1.2F;
        recoil(rig, "torso", state, 0.18F);
    }

    // --- Armillary Scout ----------------------------------------------------
    static void ringed(Rig rig, ThemeExclusiveRenderState state) {
        ModelPart hull = rig.get("hull");
        hull.y += bob(state, 0.09F) * 1.0F;
        // Counter-rotating rings that never track the target, the same trick
        // that makes the Meridian Sentinel read as a mechanism.
        float spin = 1.0F + phase(state, AbilityPhase.WINDUP) * 4.0F;
        rig.get("hull/ring_x").yRot += state.ageInTicks * 0.05F * spin;
        rig.get("hull/ring_z").xRot -= state.ageInTicks * 0.08F * spin;
        float dive = phase(state, AbilityPhase.ACTIVE);
        hull.xRot += dive * 1.2F;
        hull.y += dive * 3.0F;
        rig.get("hull/lens").z -= dive * 0.8F;
        rig.get("hull/fin_left").zRot -= dive * 0.6F;
        rig.get("hull/fin_right").zRot += dive * 0.6F;
        hull.xRot -= phase(state, AbilityPhase.RECOVERY) * 0.5F;
        recoil(rig, "hull", state, 0.26F);
    }

    // --- Dust Cantorile -----------------------------------------------------
    static void robed(Rig rig, ThemeExclusiveRenderState state) {
        ModelPart robe = rig.get("robe");
        ModelPart censerArm = rig.get("robe/sleeve_left");
        robe.y += bob(state, 0.05F) * 1.3F;
        robe.zRot += bob(state, 0.03F) * 0.04F;
        rig.get("robe/hem").xRot += bob(state, 0.045F) * 0.06F;
        rig.get("robe/cowl").yRot += bob(state, 0.02F) * 0.2F;
        float raise = phase(state, AbilityPhase.WINDUP);
        censerArm.xRot -= raise * 1.5F;
        rig.get("robe/sleeve_right").xRot -= raise * 0.9F;
        // The censer keeps swinging through the whole aura, which is what marks
        // the radius as still live rather than already spent.
        float toll = phase(state, AbilityPhase.ACTIVE);
        censerArm.xRot -= 1.5F;
        rig.get("robe/sleeve_left/censer").xRot += Mth.sin(toll * PI * 3.0F) * 0.8F;
        robe.y -= toll * 1.2F;
        censerArm.xRot += phase(state, AbilityPhase.RECOVERY) * 1.5F;
        recoil(rig, "robe", state, 0.2F);
    }

    // --- Ash Chorister ------------------------------------------------------
    static void splitCore(Rig rig, ThemeExclusiveRenderState state) {
        ModelPart shell = rig.get("shell");
        ModelPart left = rig.get("shell/half_left");
        ModelPart right = rig.get("shell/half_right");
        shell.y += bob(state, 0.07F) * 0.9F;
        float breathe = (bob(state, 0.06F) + 1.0F) * 0.5F;
        // The seam is always slightly open and gapes when the split is coming.
        float gap = 0.4F + breathe * 0.3F + phase(state, AbilityPhase.WINDUP) * 1.6F
                + phase(state, AbilityPhase.ACTIVE) * 2.4F;
        left.x += gap;
        right.x -= gap;
        left.zRot -= gap * 0.05F;
        right.zRot += gap * 0.05F;
        rig.get("shell/core").y += bob(state, 0.13F) * 0.6F;
        rig.get("shell/crown").yRot += state.ageInTicks * 0.03F;
        float drift = bob(state, 0.1F);
        rig.get("shell/wisp_left").y += drift * 1.2F;
        rig.get("shell/wisp_right").y -= drift * 1.2F;
        recoil(rig, "shell", state, 0.24F);
    }

    // --- Prism Stalker ------------------------------------------------------
    static void prism(Rig rig, ThemeExclusiveRenderState state) {
        ModelPart shellUpper = rig.get("prism/upper");
        ModelPart shellLower = rig.get("prism/lower");
        float step = stride(state, 0.9F);
        rig.get("leg_front_left").xRot += step * 0.6F;
        rig.get("leg_back_right").xRot += step * 0.6F;
        rig.get("leg_front_right").xRot -= step * 0.6F;
        rig.get("leg_back_left").xRot -= step * 0.6F;
        rig.get("prism").y += footfall(state, 0.9F) * 0.7F + bob(state, 0.05F) * 0.4F;
        rig.get("prism").yRot += state.ageInTicks * 0.015F;
        // Shut, the shells shrug off most damage. They part only during ACTIVE,
        // and that gap is the honest signal that the window is open.
        float crack = phase(state, AbilityPhase.WINDUP) * 0.4F + phase(state, AbilityPhase.ACTIVE);
        shellUpper.y -= crack * 2.2F;
        shellLower.y += crack * 2.2F;
        shellUpper.xRot -= crack * 0.18F;
        shellLower.xRot += crack * 0.18F;
        rig.get("prism/core").y += bob(state, 0.16F) * 0.5F;
        rig.get("prism/facet").z -= crack * 1.0F;
        recoil(rig, "prism", state, 0.28F);
    }

    // --- Shardling Swarm ----------------------------------------------------
    static void swarm(Rig rig, ThemeExclusiveRenderState state) {
        ModelPart hub = rig.get("hub");
        hub.y += bob(state, 0.14F) * 1.2F;
        hub.yRot += state.ageInTicks * 0.06F;
        // Each splinter keeps a different beat, so the cloud churns instead of
        // rotating as one rigid object.
        String[] shards = {
            "hub/shard_a", "hub/shard_b", "hub/shard_c", "hub/shard_d", "hub/shard_e",
        };
        float tighten = phase(state, AbilityPhase.WINDUP);
        float burst = phase(state, AbilityPhase.ACTIVE);
        for (int i = 0; i < shards.length; i++) {
            ModelPart shard = rig.get(shards[i]);
            float beat = state.ageInTicks * (0.11F + i * 0.03F) + i * 1.3F;
            float scatter = 1.0F - tighten * 0.75F + burst * 2.2F;
            shard.x *= scatter;
            shard.z *= scatter;
            shard.y += Mth.sin(beat) * 0.8F * scatter;
            shard.xRot += Mth.sin(beat * 0.7F) * 0.5F;
            shard.zRot += Mth.cos(beat * 0.5F) * 0.5F;
        }
        recoil(rig, "hub", state, 0.4F);
    }

    // --- Index Wight --------------------------------------------------------
    static void page(Rig rig, ThemeExclusiveRenderState state) {
        ModelPart torso = rig.get("torso");
        ModelPart quillArm = rig.get("torso/arm_quill");
        torso.y += bob(state, 0.05F) * 0.7F;
        torso.zRot += bob(state, 0.035F) * 0.03F;
        rig.get("torso/arm_free").xRot += stride(state, 0.6F) * -0.4F;
        rig.get("torso/skirt").xRot += footfall(state, 0.6F) * 0.08F;
        float riffle = bob(state, 0.09F);
        float fan = phase(state, AbilityPhase.WINDUP);
        rig.get("torso/page_left").yRot += riffle * 0.12F + fan * 0.5F;
        rig.get("torso/page_right").yRot -= riffle * 0.12F + fan * 0.5F;
        rig.get("torso/page_mid").xRot += riffle * 0.08F - fan * 0.3F;
        // The quill is what does the marking, so it leads the strike.
        quillArm.xRot -= fan * 1.7F;
        float mark = phase(state, AbilityPhase.ACTIVE);
        quillArm.xRot += during(state, AbilityPhase.ACTIVE) * (-1.7F + mark * 2.6F);
        rig.get("torso/arm_quill/quill").xRot -= lobe(mark) * 0.7F;
        torso.xRot += lobe(mark) * 0.25F;
        recoil(rig, "torso", state, 0.2F);
    }

    // --- Shelf Crawler ------------------------------------------------------
    static void clinger(Rig rig, ThemeExclusiveRenderState state) {
        ModelPart carapace = rig.get("carapace");
        String[] limbs = {"limb_a", "limb_b", "limb_c", "limb_d", "limb_e", "limb_f"};
        float tuck = phase(state, AbilityPhase.WINDUP);
        float splay = phase(state, AbilityPhase.ACTIVE);
        for (int i = 0; i < limbs.length; i++) {
            ModelPart limb = rig.get(limbs[i]);
            // Alternating tripod gait: opposite corners move together.
            float gait = Mth.cos(state.walkAnimationPos * 1.2F + (i % 2 == 0 ? 0.0F : PI))
                    * Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
            limb.xRot += gait * 0.7F;
            float side = i < 3 ? 1.0F : -1.0F;
            limb.zRot += side * (splay * 0.8F - tuck * 0.5F);
        }
        carapace.y += footfall(state, 1.2F) * 0.5F - splay * 2.5F;
        carapace.xRot += splay * 0.35F;
        rig.get("carapace/head").xRot -= tuck * 0.3F;
        float bite = Math.max(splay, Mth.sin(state.attackTime * PI));
        rig.get("carapace/head/mandible_left").yRot -= bite * 0.5F;
        rig.get("carapace/head/mandible_right").yRot += bite * 0.5F;
        recoil(rig, "carapace", state, 0.22F);
    }

    // --- Metronome Hound ----------------------------------------------------
    static void hound(Rig rig, ThemeExclusiveRenderState state) {
        ModelPart body = rig.get("body");
        float gait = stride(state, 0.9F);
        rig.get("leg_front_left").xRot += gait * 1.2F;
        rig.get("leg_back_right").xRot += gait * 1.2F;
        rig.get("leg_front_right").xRot -= gait * 1.2F;
        rig.get("leg_back_left").xRot -= gait * 1.2F;
        body.y += footfall(state, 0.9F) * 0.6F;
        body.xRot += footfall(state, 0.9F) * 0.06F;
        // The pendulum runs off ageInTicks and nothing else. Its bite lands on
        // the beat, so a player who watches the tail knows when to move.
        float tick = Mth.sin(state.ageInTicks * (PI / 10.0F));
        rig.get("body/tail_rod").zRot += tick * 0.55F;
        rig.get("body/tail_rod/pendulum").zRot -= tick * 0.25F;
        float crouch = phase(state, AbilityPhase.WINDUP);
        body.xRot -= crouch * 0.4F;
        body.y += crouch * 1.2F;
        float snap = phase(state, AbilityPhase.ACTIVE);
        body.xRot += snap * 0.7F;
        body.y -= snap * 1.6F;
        float jaw = Math.max(crouch * 0.8F, lobe(snap));
        rig.get("body/head/jaw").xRot += jaw * 0.8F;
        rig.get("body/head").xRot -= snap * 0.3F;
        recoil(rig, "body", state, 0.2F);
    }

    // --- Labyrinth Usher ----------------------------------------------------
    static void usher(Rig rig, ThemeExclusiveRenderState state) {
        ModelPart coat = rig.get("coat");
        ModelPart lanternArm = rig.get("coat/arm_lantern");
        coat.y += bob(state, 0.04F) * 0.8F;
        coat.zRot += bob(state, 0.028F) * 0.03F;
        rig.get("coat/hem").xRot += footfall(state, 0.5F) * 0.1F;
        rig.get("coat/arm_free").xRot += stride(state, 0.5F) * -0.35F;
        lanternArm.xRot += stride(state, 0.5F) * 0.3F;
        // Raises the lantern before it seals a way through, so the door closing
        // is announced by the arm rather than by the block appearing.
        float raise = phase(state, AbilityPhase.WINDUP);
        lanternArm.xRot -= raise * 2.3F;
        rig.get("coat/head").xRot -= raise * 0.25F;
        float seal = phase(state, AbilityPhase.ACTIVE);
        lanternArm.xRot -= during(state, AbilityPhase.ACTIVE) * (2.3F - seal * 1.1F);
        lanternArm.zRot -= seal * 0.5F;
        coat.yRot += seal * 0.3F;
        rig.get("coat/arm_lantern/lantern").zRot += Mth.sin(seal * PI * 2.0F) * 0.4F;
        lanternArm.xRot += phase(state, AbilityPhase.RECOVERY) * 1.2F;
        recoil(rig, "coat", state, 0.18F);
    }

    // --- Blank Chronist -----------------------------------------------------
    static void blank(Rig rig, ThemeExclusiveRenderState state) {
        ModelPart torso = rig.get("torso");
        ModelPart slab = rig.get("torso/slab");
        swingLegs(rig, state, "leg_left", "leg_right");
        float swingArms = stride(state, 0.66F) * 0.5F;
        rig.get("torso/arm_left").xRot -= swingArms;
        rig.get("torso/arm_right").xRot += swingArms;
        rig.get("torso/skirt").xRot += footfall(state, 0.66F) * 0.09F;
        // The slab tips like a page being turned to a blank side. Nothing on it
        // ever faces the player squarely, which is the point of the creature.
        slab.yRot += bob(state, 0.03F) * 0.35F;
        float tilt = phase(state, AbilityPhase.WINDUP);
        slab.xRot -= tilt * 0.6F;
        rig.get("torso/arm_left").xRot -= tilt * 1.4F;
        rig.get("torso/arm_right").xRot -= tilt * 1.4F;
        float erase = phase(state, AbilityPhase.ACTIVE);
        float sweep = lobe(erase);
        float erasing = during(state, AbilityPhase.ACTIVE);
        rig.get("torso/arm_left").xRot += erasing * (-1.4F + sweep * 1.0F);
        rig.get("torso/arm_left").zRot -= sweep * 1.1F;
        rig.get("torso/arm_right").xRot += erasing * (-1.4F + sweep * 1.0F);
        rig.get("torso/arm_right").zRot += sweep * 1.1F;
        slab.xRot += erase * 0.5F;
        torso.xRot += sweep * 0.2F;
        recoil(rig, "torso", state, 0.2F);
    }

    // --- Hour Hand Wraith ---------------------------------------------------
    static void longArm(Rig rig, ThemeExclusiveRenderState state) {
        ModelPart torso = rig.get("torso");
        ModelPart hourArm = rig.get("torso/arm_hour");
        torso.y += bob(state, 0.045F) * 1.1F;
        torso.zRot += bob(state, 0.03F) * 0.05F;
        rig.get("torso/shroud").xRot += bob(state, 0.04F) * 0.07F;
        rig.get("torso/arm_small").xRot += stride(state, 0.5F) * 0.4F;
        // The long arm hangs like a clock hand at rest, cocks back across the
        // body, then comes round level — the sweep is horizontal, not a chop.
        hourArm.zRot += bob(state, 0.035F) * 0.08F;
        float cock = phase(state, AbilityPhase.WINDUP);
        hourArm.zRot -= cock * 1.5F;
        hourArm.yRot -= cock * 0.8F;
        torso.yRot -= cock * 0.4F;
        float sweep = phase(state, AbilityPhase.ACTIVE);
        // Comes round level from the cocked angle, and hangs like a clock hand
        // the rest of the time rather than jutting out sideways at rest.
        float sweeping = during(state, AbilityPhase.ACTIVE);
        hourArm.zRot += sweeping * (-1.5F + sweep * 1.1F);
        hourArm.yRot += sweeping * (-0.8F + sweep * 2.6F);
        torso.yRot += sweeping * (-0.4F + sweep * 1.2F);
        rig.get("torso/arm_hour/hand_blade").xRot += lobe(sweep) * -0.5F;
        hourArm.yRot -= phase(state, AbilityPhase.RECOVERY) * 0.5F;
        recoil(rig, "torso", state, 0.18F);
    }
}
