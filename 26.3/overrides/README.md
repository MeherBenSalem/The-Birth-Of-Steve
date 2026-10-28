# Overrides for Minecraft 26.3

The mod's source lives once in [`shared/`](../../shared), written against
Minecraft 26.1.2. Anything placed here at

```
overrides/<module>/src/main/<java|resources>/<same path as in shared/>
```

replaces its `shared/` twin **for this Minecraft version only** — `<module>` is
`common`, `fabric` or `neoforge`. The shared copy is excluded from compilation,
javadoc, the sources jar and resource processing, so there is never a duplicate
class. A file with no shared twin is simply added.

Every override here is a copy of its shared file with the smallest delta 26.3
forces — see the cheat-sheet below for what each delta is. When you change one
of these files in `shared/`, make the same change here.

Check what is in effect:

```bash
gradlew.bat -p 26.3 :common:sourceOverrides
```

## What is overridden and why

| Override | 26.3 delta |
| --- | --- |
| `compat/VanillaCompat.java`, `client/ClientCompat.java` | Copied unchanged from `26.2/overrides` (26.3 keeps 26.2's `EntityTypes`, `knockback` and `Gui`/`Hud` shapes). |
| `block/*Block.java` (all 14) | Blocks no longer declare codecs: `CODEC = simpleCodec(...)` and `codec()` removed. `FractureCofferBlock` also has the `drop` change. |
| `client/ModKeyMappings.java` | `GLFW.GLFW_KEY_*` → `InputConstants.KEY_*`. |
| `client/render/{ArchiveCore,MemoryLantern,AlignmentDial}Renderer.java` | `poseStack.mulPose(q)` → `poseStack.rotate(q)`. |
| `client/render/{HourCantor,MemoryLeech,MeridianSentinel,Minotaur,ParallaxWraith,PhoenixGuardian,ThemeExclusive}Renderer.java` | `entity.getAttackAnim(pt)` → `entity.getSwingAnimation(pt)`. |
| `entity/{ThemeExclusive,Minotaur,PhoenixGuardian}Entity.java` | `hurtMarked = true` → `syncVelocity = true`. |
| `entity/MemoryLeechEntity.java` | `swing(hand)` → `swing(hand, SwingAnimation.DEFAULT)`. |
| `command/YesterglassCommands.java`, `memory/MemoryService.java`, `run/ArchiveEncounterManager.java`, `world/PlayerOnboarding.java` | `player.drop(stack, false)` → `player.drop(stack, false, Prediction.SERVER_ONLY)`. |
| `site/LastCuratorEncounterTracker.java` | `setInvulnerable(b)` → `setPermanentlyInvulnerable(b)`. |
| `data/tbos/dimension_type/fractured_archive.json` (resource) | Adds the new `minecraft:gameplay/straw_bed_rule`, mirroring `bed_rule`. Its default would otherwise let players sleep in straw beds in the Archive. |

The data-pack and model JSON format changes are not overrides. They are a
mechanical rewrite in `build-logic` (`src/main/groovy/tbos/gradle/Json263.groovy`,
wired into `processResources` by `multiloader-common.gradle`), the same approach
1.20.1 uses to relocate its plural data directories. The rewrite leaves a file
byte-identical when no rule applies. Today it changes 66 loot tables (pool
`conditions`) and 2 block models (`shade`). A JSON override placed here is
rewritten by the same step.

## Build notes

- Loom is pinned to **1.17.21**, not 1.18.x. Loom 1.18+ declares Gradle plugin
  API 9.7 (1.19.x declares 9.8) and will not resolve on the repository's Gradle
  9.5.0 root wrapper, which drives every version build and the composite.
  1.17.21 was published the same day as 1.18.1 as the 1.17 line's 26.3
  maintenance release, and it builds, runs and passes the GameTest suite on 26.3.
  To move to Loom 1.18+, bump the **root** wrapper to Gradle ≥ 9.7 and re-verify
  every version. A separate wrapper only for 26.3 would not help: the root
  composite still runs every included build on the root Gradle.
- ModDevGradle 2.0.148, NeoForge 26.3.0.97, NeoForm 26.3-1, Fabric Loader 0.19.5,
  Fabric API 0.165.0+26.3, Java 25.
- Gates: `-p 26.3 build`, `:neoforge:runGameTestServer` and
  `:fabric:runGameTestServer` each pass all 62 required tests: 61 `tbos` tests
  plus vanilla's `always_pass`.

## 26.3 API cheat-sheet (26.1.2 / 26.2 → 26.3)

Read straight from the decompiled 26.3 sources (NeoForm `vanilla-26.3-1-sources.jar`,
in `26.3/common/build/moddev/artifacts/` after a first build), diffed against 26.2.

### Java: hit by this mod

```text
Block: MapCodec CODEC = simpleCodec(X::new) + @Override codec()   →  delete both (no block codecs; also BaseEntityBlock/HorizontalDirectionalBlock subclasses)
org.lwjgl.glfw.GLFW.GLFW_KEY_X                                     →  com.mojang.blaze3d.platform.InputConstants.KEY_X   (LWJGL GLFW is gone and the
                                                                       numeric codes changed, e.g. KEY_J 74 → 13, so never hard-code key numbers)
poseStack.mulPose(Quaternionf)                                     →  poseStack.rotate(Quaternionfc)   (+ rotate(Axis, rad), rotateDegrees(Axis, deg);
                                                                       mulPose(Matrix4fc|Transformation) still exists)
livingEntity.getAttackAnim(partialTick)                            →  livingEntity.getSwingAnimation(partialTick)
livingEntity.swing(hand) / swing(hand, sendToSelf)                 →  mob.swing(hand, SwingAnimation.DEFAULT) / living.swing(hand, SwingAnimation, sendToSelf)
                                                                       (net.minecraft.world.item.component.SwingAnimation; Mob.swingForAttack(hand) uses the held item's)
player.drop(stack, thrownFromHand)                                 →  player.drop(stack, thrownFromHand, Prediction.SERVER_ONLY | PREDICTED)
living.drop(stack, randomly, thrownFromHand)                           (net.minecraft.util.Prediction; drop now always swings the main hand server-side,
                                                                       PREDICTED = client already predicted it, so it is not echoed back)
entity.hurtMarked = true                                           →  entity.syncVelocity = true
entity.setInvulnerable(b)                                          →  entity.setPermanentlyInvulnerable(b)
entity.isInvulnerable()                                            →  now permanent || temporarily (invulnerableTime > 0); old meaning: isPermanentlyInvulnerable()
```

Kept from 26.2, which is why the 26.2 compat seams are reused: `EntityTypes.*`
constants, `knockback(power, x, z, source, amount)`, `Minecraft.gui.hud` /
`gui.screen()` / `gui.setScreen(...)`, `KeyMapping(name, keysym, Category)`.
The five Fabric mixin targets (`BlockItem.place`, `LivingEntity.heal` /
`hurtServer` / `actuallyHurt`, `SavedDataStorage.readTagFromDisk` →
`DataFixTypes.update`, `ServerExplosion.calculateExplodedPositions`) are also
unchanged.

### Java: verified in the 26.3 sources, not used by this mod yet

```text
PushReaction.NORMAL / PUSH_ONLY / DESTROY / BLOCK / IGNORE         →  PUSH_PULL / PUSH / POPPED / IMMOVEABLE / IGNORE_ENTITY
Properties.isViewBlocking((s, l, p) -> …)                          →  isViewBlocking((s, l, p, nearPlaneBox) -> …)   (StateArgumentPredicate<AABB>)
new LeavesBlock(particleChance, props)                             →  new LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(), props)
new UntintedParticleLeavesBlock(chance, particle, props)           →  new UntintedParticleLeavesBlock(chance, particle, AmbientLeavesBlockSoundPlayer…, props)
EntityRenderer.shouldRender(e, frustum, x, y, z)                   →  shouldRender(e, frustum, x, y, z, float partialTicks)
EntityRenderer.getBoundingBoxForCulling(e)                         →  getBoundingBoxForCulling(e, float partialTicks)
FogEnvironment.getBaseColor(level, camera, dist, pt): int          →  …: Vector3fc
renderPass.setPipeline(RenderPipeline)                             →  renderPass.setPipeline(RenderSystem.getCompiledPipeline(pipeline))
renderPass.bindTexture(name, view, sampler)                        →  renderPass.setUniform(name, view, sampler)
com.mojang.blaze3d.pipeline.{RenderPipeline, CompiledRenderPipeline, BlendFunction, BlendEquation,
    DepthStencilState, ColorTargetState, BindGroupLayout}          →  com.mojang.renderpearl.api.pipeline.*
com.mojang.blaze3d.{IndexType, PrimitiveTopology}                  →  com.mojang.renderpearl.api.pipeline.*
com.mojang.blaze3d.platform.{BlendFactor, BlendOp, CompareOp, PolygonMode}   →  com.mojang.renderpearl.api.pipeline.*
com.mojang.blaze3d.shaders.{ShaderType, ShaderSource, UniformType} →  com.mojang.renderpearl.api.pipeline.*
com.mojang.blaze3d.buffers.{GpuBuffer, GpuBufferSlice}             →  com.mojang.renderpearl.api.buffers.*   (GpuFence → …api.commands)
com.mojang.blaze3d.systems.{RenderPass, CommandEncoder, GpuQuery…} →  com.mojang.renderpearl.api.commands.*
com.mojang.blaze3d.systems.{GpuDevice, GpuBackend, DeviceInfo…}    →  com.mojang.renderpearl.api.device.*
com.mojang.blaze3d.textures.{GpuTexture, GpuTextureView, GpuSampler, AddressMode, FilterMode}  →  com.mojang.renderpearl.api.textures.*
com.mojang.blaze3d.vertex.{VertexFormat, VertexFormatElement}      →  com.mojang.renderpearl.api.vertex.*
com.mojang.blaze3d.GpuFormat                                       →  com.mojang.renderpearl.api.GpuFormat
(still in com.mojang.blaze3d: PoseStack, VertexConsumer, BufferBuilder, DefaultVertexFormat, RenderSystem, InputConstants)
```

### Data and asset formats

Handled by `Json263.groovy` for every shared file (rules 1–5), or by an override
(rule 6):

```text
1  loot pool / entry / function:  "conditions": [{"condition": "minecraft:x", …}]
                                →  "condition": {"type": "minecraft:x", …}
                                   several → {"type": "minecraft:all_of", "terms": [ … ]}; nested terms/term likewise;
                                   a bare string is now a predicate reference ("condition": "minecraft:tool/can_silk_touch")
2  loot table / pool / entry:     "functions": [{"function": "minecraft:x", …}]
                                →  "modifier": {"type": "minecraft:x", …}   (a JSON list = inline sequence;
                                   minecraft:sequence keeps its own "functions" list)
3  loot pool:                     "rolls": 1.0  →  "rolls": 1   (ContextIntProvider: int, {"type":"minecraft:uniform","min":1,"max":3},
                                   or a registry reference; bonus_rolls stays a float provider)
4  advancement criterion:         "conditions": {"player": [{"condition": …}]}  →  "conditions": {"player": {"type": …}}
                                   (inventory_changed's "items" list is unchanged)
5  block model element:           "shade": false  →  "shade_direction_override": "up"   ("shade": true → drop the key)
6  dimension_type attributes:     bed_rule "explodes": true  →  "destroy_on_use": true   (+ new "destroy_on_leave")
                                   new "minecraft:gameplay/straw_bed_rule" (default: sleep when dark, never set spawn,
                                   destroy on leave): mirror bed_rule in dimensions that forbid sleeping
```

**26.3 silently ignores the old keys.** The record codecs ignore unknown
fields, so a loot table that still says `conditions` or `functions`, or a model
that still says `shade`, loads without any error. It just loses that behaviour:
block drops stop decaying in explosions, and unshaded faces get shaded. A clean
GameTest log does not prove the data is in the 26.3 shape. Diff against the
vanilla data in the sources jar instead.

Also changed, but not used by this mod:

```text
worldgen/configured_feature  →  worldgen/feature        worldgen/configured_carver  →  worldgen/carver
new worldgen registries: block_state_provider, material_condition, material_rule
pack.mcmeta: "pack": {"min_format": 121, "max_format": 121}   (data 121, resources 97; this mod ships none and the loaders synthesize it)
dimension_type: optional "skybox" (default overworld) and "cardinal_light"
```

Unchanged, checked against vanilla: recipes (`"result": {"id": …}`), tags,
`test_instance` (including `padding`), `test_environment`, blockstates,
`items/` definitions, and advancement `display`.
