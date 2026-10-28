# Marketing assets: 1.0 "Remember or Forget"

Listing copy and artwork for CurseForge and Modrinth. Nothing here ships in the mod JARs.

## Listing

| File | Use |
| --- | --- |
| [`curseforge/description.md`](curseforge/description.md) | Full project description (Markdown; also works on Modrinth) |
| [`curseforge/summary.txt`](curseforge/summary.txt) | Short project summary (228 characters) |
| [`../patchnotes/1.0.0.md`](../patchnotes/1.0.0.md) | File changelog for 1.0.0 |

Image links in the description point at `raw.githubusercontent.com/MeherBenSalem/The-Birth-Of-Steve/main/marketing/images/...`, so they work once this folder is on `main`. To host on CurseForge instead, upload the images to the project gallery and replace each URL.

## Adding a Minecraft version

The supported versions are listed in exactly two places:

1. `VERSIONS` at the top of [`source/render.cjs`](source/render.cjs). Add one line, for example `{ minecraft: '26.4', loaders: ['Fabric', 'NeoForge'] },`. The banner's loader pills and version line are built from it. Then rerun the script for `banner`.
2. The table under **Downloads & Compatibility** in `curseforge/description.md`. Add one row.

No other copy or artwork names a Minecraft version. A new release name or number goes in `RELEASE` in `render.cjs` and in the first lines of the description.

The description leads with Expeditions and Distortions, then Remember or Forget, the Fractured Archive and Living Memories, Recollections, the Archive Codex (Insights, Bestiary, Records and the Weekly), Relics, parties, configuration, everything else, downloads and compatibility, the FAQ and a gallery of 1.0.0 screenshots.

## Images

| File | Size | Use |
| --- | --- | --- |
| `images/banner.png` | 1800×600, 972 KB | Description header: the Wake Cutter's hall, "1.0 · Remember or Forget", the logo, loader pills and versions |
| `images/icon.png` | 512×512, 160 KB | Project avatar: the mod logo (a Memory Key in an Archive arch) in a gold ring |
| `images/card-attunement.png` | 1600×900, 1211 KB | The Attunement screen for a tier 7 key; tiers 1–20, 8 Distortions, parties of up to 4 |
| `images/card-distortions.png` | 1600×900, 639 KB | All eight Distortions with their GUI icons; +1 Heat each, +15% Renown per Heat |
| `images/card-choice.png` | 1600×900, 1130 KB | The Remember or Forget choice and the Remembered reveal of the same run |
| `images/card-archive.png` | 1600×900, 1264 KB | The Wake Cutter in its hall with the Expedition HUD; 8 themes, 20 templates, 23 creatures |
| `images/card-memories.png` | 1600×900, 1149 KB | A Living Memories draft with Rewrite Draft, and the ability bar |
| `images/card-recollections.png` | 1600×900, 1204 KB | Attunement for a Recollection of the Plains Village |
| `images/card-codex.png` | 1600×900, 1335 KB | The Codex Insights page; 30 levels, 16 Insights, free respec |
| `images/card-bestiary.png` | 1600×900, 1053 KB | The Bestiary with the Wake Cutter's entry; 23 creatures, 8 floor bosses |
| `images/card-records.png` | 1600×900, 1141 KB | Records with the Hall of Records, and the Weekly tab |
| `images/card-relics.png` | 1600×900, 864 KB | A Remnant Relic's tooltip and the six rarities |
| `images/card-parties.png` | 1600×900, 549 KB | Create, invite, gather (24 blocks), begin |
| `images/card-config.png` | 1600×900, 630 KB | Default `config/tbos-expeditions.json` values |
| `images/header-*.png` | 1400×120, transparent, 77–101 KB | 12 section headings for the description |
| `images/divider.png` | 1400×40, transparent | Section divider |
| `images/screenshots/*.jpg` | 1920×1080, 130–422 KB | 14 captures: gallery uploads and card art |

### How the screenshots were captured

Every screenshot is an unedited capture of the **26.1.2 Fabric development client** (Remnants: Roguelite Dungeons 1.0.0, default configuration) with software rendering, GUI scale 3 and Minecraft's *Menu Background Blur* set to 0, taken by [`source/capture.py`](source/capture.py) against the dev server. Its docstring lists the exact steps.

- **World:** "archive_showcase", seed `remnants-archive`. The Waystone by spawn was placed with `/setblock`; the Recollection is the seed's nearest plains village, used at its well.
- **Runs:** real Expeditions begun from the Attunement screen. Each room's creatures were killed with player-attributed damage and the room then cleared with `/tbos dungeon force_clear`, so Renown, Relics and Bestiary kills are what a fight would have earned. The Codex started fresh, and its Records show the two runs played for these shots: a tier 7 key and that week's Weekly key.
- **Lighting:** the Archive's rooms are lit only by their own lamps, so the in-Archive shots were taken with Night Vision and look brighter than in normal play.
- **Posing:** the Wake Cutter was held still with NoAI for `archive-boss.jpg` and `expedition-hud.jpg`, and `archive-boss.jpg` was framed from a spectator camera with the HUD hidden (F1). Those two and `memory-draft.jpg` come from an earlier run of the same steps, which is why the HUD there shows 336 Renown.
- **HUD:** on in every shot except `archive-boss.jpg` and `archive-room.jpg`.

## Regenerating the artwork

The cards, banner, icon and headers are HTML rendered with Playwright from the screenshots, the GUI icon sheet, the mod logo and the bundled fonts:

```bash
NODE_PATH="$(npm root -g)" node marketing/source/render.cjs            # everything
NODE_PATH="$(npm root -g)" node marketing/source/render.cjs banner icon  # selected assets
```

- `source/backdrop.jpg` is the blurred background scene, a 1280×720 copy of `screenshots/archive-room.jpg`.
- `source/logo.png` is the mod logo (`shared/common/src/main/resources/logo.png`, drawn by `tools/rpg/logo.py`).
- Icons on the cards are cells of the in-game GUI icon sheet (`textures/gui/rpg/icons.png`, painted by `tools/rpg/icons.py`).
- `source/fonts/` holds Cinzel and EB Garamond with their SIL Open Font License 1.1 texts. The script uses only these files.

To refresh screenshots, start the dev server and QA client (see `source/capture.py`), rerun the capture for the shots you need, and rerun the render script. Card crops are pixel regions of the 1920×1080 captures (`region()` in `render.cjs`), so new captures may need their crops and label positions adjusted.

## Wording

Call the mod **Remnants: Roguelite Dungeons**, and *The Birth of Steve* only for the prologue campaign. Use the in-game names: Memory Key, Attunement, Distortion, Heat, Fracture, Recollection, Remember, Forget, Momentum, Renown, Insight, Relic and the Archive Codex. Every number in the copy and on the cards is a 1.0.0 default from `config/tbos-expeditions.json`, a recipe or the code. Update the copy when those defaults change. Do not use other games' names, logos or art in listing images or titles.
