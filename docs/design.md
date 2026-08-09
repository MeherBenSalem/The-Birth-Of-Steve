# Design

The first playable slice is a bounded 16x16 memory site. A shared, authoritative
site state switches between `RUIN` and `REMEMBERED`; transitional states are timed
from a server-provided start tick. Decorative remembered architecture is additive,
while collision-critical phase geometry stays small and server controlled.

The spike teaches one action: use the Lens near the Memory Anchor. It avoids an
energy grind, rejects unsafe transitions, and uses shape, particles, sound
subtitles, and block-state contrast rather than color alone.

The first production-facing authored room is the Parallax Atrium. Its ruin shell
uses cracked archive masonry and a meridian floor; reconstruction adds a broad
fourteen-block staircase and four high-contrast resonance lamps. The structure is
stored as a rotation-aware definition so the same room can be placed without
changing its persistent identity or puzzle marker semantics.

The onboarding chain is now explicit: discover a cracked Lens, repair it with
amethyst, copper, and glass, then reconstruct the first room. The guarded showcase
path reconciles those advancement prerequisites when it intentionally skips the
normal acquisition steps.

That chain is only discoverable if the player knows it exists, so the first
Overworld join hands out the **Archivist's Journal** and a two-line chat pointer
at the Fracture Shrines. The Journal is diegetic instruction rather than a tutorial
overlay: nine pages of the last archivist's own route, readable at any time and
never modal. It renders through the vanilla book interface so it costs no new
client surface, and its pages are translation keys so the route stays localisable.
The greeting is recorded per player in overworld SavedData, not on the item, so a
player who loses or drops the Journal is not re-greeted and can recover one with
`/tbos debug give_journal`.

Fracture Shrines are planned from the world seed but constructed only when their
own chunk generates. Discovery is therefore an act of travel rather than a
world-load side effect, and no login forces three distant regions into existence.

### Utility block presentation

The five blocks the player actually operates — Curator Gateway, Meridian Relay,
Archive Core, Memory Lantern, Astronomical Alignment Dial — are read as controls,
so each has silhouette-distinct geometry rather than a cube skin. Motion carries
meaning where it can: the gateway pulls particles inward because it takes you
somewhere, the relay only arcs while powered, and the dial's sparks trace its ring
so it still reads as an instrument. The Alignment Dial and Archive Core carry
block-entity renderers whose armillary rings and inner core are deliberately
**cosmetic and stateless** — puzzle state stays packed in each site's
`progressFlags`, and no visual blockstate property mirrors it. Every animation
stops under the `reducedMotion` client setting.

Two rendering rules fall out of that geometry and are easy to regress:

- **No overlapping model elements.** Coplanar faces z-fight in world. Elements may
  share an edge but never a volume.
- **Non-cube models need `.noOcclusion()`.** Without it neighbouring blocks cull
  their faces against the block and it reads as an x-ray hole through the floor.
  `noOcclusion` is a per-*block* property, not per-state, so a block that is solid
  in one state and thin or transparent in another — the Parallax Panel phasing out
  — needs it declared regardless of which state you are looking at.
- **A model must match its `getCollisionShape`.** The theme hazards shipped as
  `cube_all` while their shapes said otherwise, which put a solid-looking cube
  around an ink pool you walk straight through. `tools/textures/check_assets.py`
  now walks blockstates to models to textures and flags both this and any
  reference that does not resolve.

### Archive art direction

Block and item art for the Archive family is **final, owned original work for this
project** — not placeholder. It is authored to a fixed standard:

- 16×16, hard pixel edges, no anti-aliasing or smooth gradients.
- One shared palette: a five-step bronze ramp, a five-step slate ramp, four-step
  cyan glass, four-step indigo void, two gold accents, and a bone/parchment pair.
- Light always comes from the top-left; bevels are lit on the top and left edges
  and shaded on the bottom and right.
- Animated textures are vertical frame strips driven by `.png.mcmeta`, quantised
  onto the same discrete ramps — at 16px a smooth gradient turns to mush, while
  flat bands still read as a spinning coil or an opening aperture.

`tools/textures/archive_blocks.py` is the authoring source for this family. It is
stdlib-only and deterministic, so the art can be revised and regenerated rather
than hand-patched pixel by pixel; it also emits contact sheets for review. Editing
that script and re-running it is the supported way to change these textures.

#### The hand-authored set is authoritative — do not regenerate it

Eighteen blocks are **finished hand-authored art with no generator behind them**:

> `archive_stone`, `archive_bricks`, `weathered_archive_bricks`,
> `cracked_archive_stone`, `mossy_archive_stone`, `chiseled_archive_stone`,
> `chronicle_tile`, `chronicle_bronze`, `cantor_wall`, `cantor_floor`,
> `cantor_rune`, `meridian_tile`, `phase_platform`, `archive_seal`,
> `memory_anchor`, `memory_anchor_side`, `archive_cache`, `lenswork_crystal`

They define how an Archive block looks and **everything else answers to them**. A
generator was once written to "bring them up to match" the procedural family and
promptly overwrote art that was already finished; that generator was deleted.
Nothing in `tools/` writes to these files, and nothing should.

`tools/textures/_archive_style.py` is that look written down, so the generated
floor-theme family can sit beside them without giving itself away. Four moves
account for nearly all of it:

- **A mottled face.** Visible lighter and darker clusters *inside* every block
  face. A smooth fill with a bevel on it is the clearest tell of a generated
  texture and the mistake that had to be undone to arrive here.
- **A clean material edge.** Each block tiles into its neighbour without a
  near-black perimeter; dark pixels are reserved for internal grout rather than
  fading out at its boundary.
- **A centred device.** Concentric rings around a lit core, on anything that
  carries meaning.
- **Corner braces.** Short accent arms at the corners, as on the Memory Anchor.

Accent colour is structural and sparing — it lands on a device, a band or a
joint, never scattered across the face. `tools/textures/_theme_recipes.py` maps
each theme role onto those moves: walls are coursed masonry like
`archive_bricks`, floors carry a centred mark like `chronicle_tile`, roofs stay
quiet like `meridian_tile`, trims band like `chronicle_bronze`.

Entity art is held to the same standard on the same palette, from
`tools/textures/archive_entities.py`. Each creature has a base sheet and a
transparent emissive sheet whose lit pixels sit on the same UV rectangles, because
the emissive layer reuses the model's own geometry. The UV constants at the top of
each section of that script must stay in step with the `texOffs` calls in the
matching model class; they are the one place the two files agree, and the script's
`validate()` refuses to generate if two boxes collide or leave the sheet.

#### Two texels per model unit

The three rebuilt creatures are 128×128 at double texel density; the Memory Leech
and Lensward remain 64×64 at single density and should be brought up to match.

Sheet size alone buys nothing here. Minecraft's box UV is fixed at one texel per
model unit, so a larger sheet with default scaling only adds empty space.
`CubeDefinition.bake` passes `texWidth × texScale.u()` to `ModelPart.Cube` as the
UV divisor, so density comes from **halving `texScale`, not from the sheet**: the
models pass `CubeDeformation.NONE, 0.5F, 0.5F` on every `addBox` and declare
`LayerDefinition.create(mesh, 128, 128)`. Every `texOffs` value stays in the
original 64-unit space; each box simply covers twice as many pixels. The generator
mirrors this with `SCALE = 2`, keeping its own UV constants in model units.

That extra density is what pays for the material detail: panel grids with
individually bevelled plates, rivets, a specular sweep, verdigris creeping out of
the grooves, faceted slate, and cloth that folds rather than bands.

Two rules learned the hard way and worth not relearning. Seams running one way
only, each with a lit lip beneath, reads as **plank siding** no matter what colour
it is — metal needs a panel grid on both axes. And per-pixel random noise reads as
television static rather than material; the grain must be clustered value noise
that survives being quantised onto a ramp.

#### Face directions

`faces()` in the generator names the six rectangles for the direction they
actually face. Vanilla's `ModelPart.Cube` emits polygons as `DOWN, UP, WEST,
NORTH, EAST, SOUTH`, which is *not* the order a reader would guess from the
unwrap: the rectangle that looks like the top of the cross is the DOWN face. An
earlier pass had these labels transposed and lit every underside while shading
every top, which is most of why that art read flat. Light falls from above and to
the left, so UP is brightest, DOWN darkest, and the two side faces differ.

### Journal interface

The Archivist's Journal opens one screen, `ArchivistQuestScreen`, on an ordinary
right-click. It previously had two interfaces — a vanilla `BookViewScreen` of nine
text pages on right-click and the quest screen behind shift+right-click — and the
book has been removed outright.

`tools/textures/archive_gui.py` authors the mod's only GUI sprite family into
`assets/tbos/textures/gui/sprites/journal/`, on the same palette and the same
deterministic, stdlib-only terms as the other two generators. The frame, well,
tabs and progress bars are nine-sliced through `.png.mcmeta` so their corners stay
crisp at any panel size; the three 12×12 quest seals are fixed-size. Quest rows
carry a real item for their step — cracked Lens, repaired Lens, Survey Map,
Curator Core — rather than describing it in text.

Hovering a step shows it in the detail pane rather than raising a tooltip, so the
pointer never occludes the list it is pointing at.

### Archive enemy presentation

The five original creatures are silhouette-distinct before they are anything else,
because in a dark procedural room shape is all that arrives first:

- The **Parallax Wraith** has no legs and no solid body — four shard plates orbit a
  hollow core. Those plates lag the head's turn rather than following it, so
  turning smears the outline. That lag *is* the creature; a solid body animated the
  same way would read as an ordinary floating mob.
- The **Meridian Sentinel** is headless. Two counter-rotating armillary rings sit
  where a head belongs, and they never track the player — they keep their own time,
  which is what makes it read as a mechanism rather than a soldier. Its weight is
  carried by a body dip on each footfall.
- The **Hour Cantor** hovers, has four arms, and beats its intone out in four
  strokes. Its caged pendulum is a genuine tell rather than decoration: the swing
  tightens as the refrain approaches and tightens again below half health, so a
  player who watches the chest knows what a player who watches the health bar knows.

Two later creatures come from **Blockbench sources rather than code-native meshes**,
and are the first in the mod to do so:

- **The Last Curator** (`phoenix_guardian`) closes the Overworld campaign, and is
  legless like the Cantor — the whole silhouette is a crowned head over four
  wing-arms and a hanging torso, with nothing below it. Its tell is spatial rather
  than rhythmic: the wing-arms fan wide during the charge and stay wide until the
  ember burst lands, so the danger zone is drawn in the air before it exists on the
  ground. It reads as *one* creature in two states because the crown ring keeps
  turning through both.
- The **Minotaur** is the only chamber enemy that plants itself. Where the theme
  exclusives skirmish, it coils its whole body away from the target during the
  wind-up and drops through the swing, which is what makes being cornered by one
  the actual threat rather than being hit by one.

#### Model provenance

`phoenix_guardian` and `minotaur` were imported from Blockbench `.bbmodel`
sources supplied for this project. They carry Bedrock `bedrock_binding` bones and
their original geometry names (`guardian_of_the_phoenix.geo`, `minotaur.geo`), so
unlike the rest of the art in this document they were **not** authored in-repo.
Ownership is the supplier's to confirm; the conversion, animation and texture
placement are project work. The `.bbmodel` files themselves are not committed —
`tools/models/raw/` holds only the Blockbench Java export each generated class is
built from.

Sixteen floor-theme exclusives (two per cycling floor name) each have **their own
silhouette**, on the same palette with a theme accent bias. They briefly shared one
box-and-two-legs mesh, and the result was that every theme's pair read as the same
creature — the exact failure the five hand-built creatures above are designed to
avoid. `ThemeExclusiveMeshes` holds the geometry, `ThemeExclusivePosers` the
motion, and `ThemeExclusiveSilhouettes` the table binding each kind to a model
layer, a poser and a shadow radius. Shape telegraphs the signature move: the Wake
Cutter's overlong blade, the Metronome Hound's pendulum tail keeping time whether
or not it is moving, the Ash Chorister's seam already open before it splits.

Meshes are authored at **true size**, 16 units to the block, so a half-block Shelf
Crawler and a 2.2-block Hour Hand Wraith match their declared hitboxes without
render-time scaling.

Two generators feed this family:

- `tools/textures/archive_exclusives.py` — the sixteen creature sheets. It reads
  its UV rectangles **out of the Java mesh** through `_meshsource.py` rather than
  restating them. The previous pass chose rectangles independently of the model and
  shipped sixteen creatures whose textures missed their own geometry almost
  entirely; reading the mesh makes that unrepresentable, and the generator also
  refuses to run if any box lacks a material recipe.
- `tools/textures/archive_theme_variety.py` — the theme block family. Palette
  blocks use material recipes on the same craft standard as `archive_blocks.py`
  (beveled tiles, ashlar, coffered soffits, moulding profiles), not dotted fills.
  Floors and roofs are `cube_bottom_top` with their own edge texture and trims are
  `cube_column`, so a floor slab reads as something laid rather than as a cube
  wearing the same picture on all six faces.

`tools/textures/_material.py` holds the materials all three generators share, so a
fix to `stone` or `metal` lands everywhere at once instead of in one family.

Animation is procedural trigonometry inside `setupAnim`, not keyframe tables. The
vanilla `AnimationDefinition`/`KeyframeAnimation` API is available and was
considered; it was rejected because its definitions are Blockbench exports running
tens of kilobytes each, which are impractical to hand-author and would sit badly
beside the hand-written Memory Leech and Lensward models. Revisit this only if the
models start being authored in Blockbench.

Every signature move is a server-owned phase machine. The client receives the phase
and a tick count and derives the pose; it never runs the state machine itself, and
a move interrupted by a reload restarts rather than resolving. This is the same
rule the authored sites follow for transitions.

The Hall of Alignment is the first complete principal puzzle loop. Three mechanisms
begin at broad cardinal stops rather than continuous angles, so aiming is never
pixel-perfect. Each has a spatially distinct engraved target. A correct stop forms
a luminous particle conduit and produces explicit text without placing collision
blocks; an incorrect stop leaves the target dark and reports which numbered dial
moved. The Memory Anchor is the in-world reset control. Once all three align, the
remembered solution persists and immediately projects a two-wide crossing.

The Choir of Hours turns observation into a short, accessible memory test. Its
four-beat Sun, Crown, Moon, Gate melody is encoded redundantly through bell
position, distinct patterned symbols, pitch, light, temporary imprint geometry,
particles, and text overlays. Wrong input resets only the attempt and never harms
the player. Two failures reveal the complete symbolic order, and the room's Memory
Anchor provides an explicit manual reset. Solving in Ruin projects the next
two-wide crossing without requiring rhythm-perfect timing.

The Broken Meridian combines state traversal with controlled causality. A first
bridge exists only in Remembered state. Beyond it, the player follows the active
particle channel to a remote engraved seal, arms the route, then returns to
activate the newly charged destination socket. This spatial route advances one
solid Meridian Relay across three sockets without repeated relay clicking. Setting
the eastern channel causes the relay's future remains—not an arbitrary recorded
player action—to become a cracked Ruin crossing over the second chasm. Deep water,
one-sided return ladders, an anchor reset, occupied-socket rejection, and normal
transition collision checks prevent punishment, suffocation, and permanent traps.
