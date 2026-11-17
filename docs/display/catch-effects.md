# Water rings and big catch moment

Built 2026-11-17 from the research list (`tacklebox-visual-ideas.md` in the side session notes). luna picked number 1, the big catch moment with a ring on the water, and asked for water effects while a fish swims in to bite ("sure, do number 1 first. also do like water effects, for when a fish is coming to bite"). Her answers: both the bite rings and the wake; a ring on every catch (commons faint white); edge glow, title shine and sound, but no fish pop. Display only: nothing is sent, nothing plays the game.

## What it does

- **Water rings** (`waterFx`, off by default). Thin flat rings (0.06 block wide, 0.03 above the water top) that grow and fade. Only your own bobber and fish.
  - **Bite rings**: while a fish swims to your bobber, a ring at the bobber every `clamp(seconds left x 0.35, 0.25, 1.0)` seconds (1 s far out, 0.25 s right before the bite), each growing 0.2 to 1 block in 0.6 s. On the BITE! title one bigger ring (to 1.4 blocks), then they stop. Colour `waterColor` (pale water white `0xD8F0FF`).
  - **Wake**: behind the fish, a small ring (0.15 to 0.6 block, 0.7 s, 55% of the bite rings' strength) once the fish moved 0.35 block from the last one, at most every 120 ms. The server's own bubbles stay.
  - **Catch ring**: where the bobber was, one ring growing 0.3 to 1.5 blocks in 0.5 s, in the rarity colour (rare 2B85C4, epic 1CD832, legendary D98103, mythical C93832, special DD7ACF). Commons (and an unknown rarity) get white at half strength.
  - **Sparkles**: legendary 6, mythical and special 10 `minecraft:dust` particles in the rarity colour over the catch ring, rising slowly for about a second (spawned on the client).
- **Big catch moment** (`bigCatch`, off by default). For a fabled, trophy, mythical or gigantic fish (the first that fits and is switched on, in that order; albino and melanistic do not count):
  - **Edge glow**: the kind's colour at the screen edges (fabled 82171E, trophy D5BF3B, mythical C93832, gigantic AF3333): fades in 150 ms, holds 150 ms, fades out 300 ms, each step eased, peak `bigGlowOpacity` (35%). Nothing moves.
  - **Title shine**: one light band sweeps left to right across line 1 of the catch title (`huds/catch-title.md`) in 700 ms from when the lines appear. Only with the catch title on.
  - **Sound** (master volume, through `SoundQueue`): fabled `block.conduit.activate` (deep), trophy a bell chord (`block.note_block.bell` at pitch 1, 1.26, 1.5), mythical `entity.player.levelup`, gigantic `block.amethyst_block.chime` (pitch 0.8).

## Settings (settings > fishing)

- Group **water** (icon prismarine crystals): switch `waterFx` ("water rings"); `waterBiteRings`, `waterWake`, `waterCatchRing`, `waterSparkles` (all on); `waterOpacity` (5-100%, 60: a ring's strength when it starts); `waterColor`.
- Group **big catches** (icon nether star): switch `bigCatch` ("big catch moment"); kinds `bigFabled`, `bigTrophy`, `bigMythical`, `bigGigantic`; parts `bigGlow`, `bigShine`, `bigSound` (all on); `bigGlowOpacity` (5-100%, 35).
- Old settings files get both groups off (field defaults).

## Files

- `display/WaterRings`: pure rings and state (no Minecraft classes): `Ring` (radius eases out, alpha fades in over the first 12% then out), `interval`, `onBobber` (the bobber's spot, kept 2 s), `tick` (prunes, beats while the countdown runs), `onBite` (one ring per bite, re-sends within 1.5 s add none), `onWake`, `onCatch`, `catchColor`, `sparkles`, at most 48 live rings (the oldest goes), `emit` (40 flat quads).
- `display/WaterRingRenderer`: the world side. `tick` (end client tick, after `BobberTimer.tick`) gives `WaterRings` the bobber's water top and the countdown; `waterTop` is the fluid height of the block at the spot or the one under it (`blockY + FluidState.getHeight`), else the y itself; `sparkles`; `render` at `WorldRenderEvents.END_MAIN`.
- `display/BigCatch`: kinds and priority, `glow`, `shine`, `sound`, the moment's state, `renderHud` (the glow, a hud element after the vanilla `misc_overlays`, so it sits under the crosshair, hotbar and titles). The glow is `assets/tacklebox/textures/gui/edge_glow.png`, a white 256x256 frame that is clear in the middle (quadratic falloff from each edge over a quarter of the size, the two directions screen-blended), with `blur` and `clamp` in its mcmeta, tinted and stretched over the screen.
- Hooks: `BobberTimer.onParticle` (the fish particles feed the countdown and the wake while `waterFx` wants them), `BobberTimer.onTitle` (BITE!), `CatchWatch` (the catch), `InGameHudOverlayMixin` (subtitle to `CatchWatch.onSubtitle`), `CatchTitle.draw` (the shine), `Tacklebox` (register, tick, hud element, reset on disconnect).

## How it works

- **Fish swimming in**: the same approach particles as the bite countdown (`huds/bobber-timer.md`): `minecraft:fishing`, count 0, whose bobber by `OwnFish.owner` is yours. They feed `BiteCountdown` whenever the countdown style or the water rings want them (the timer's own display still follows `timerStyle`), and the wake gets the particle's spot. The bite rings beat while `BiteCountdown.active`, so they need 4 coherent particles first, stop 0.6 s after the last one and hide for 3 s after an action bar message, like the countdown.
- **The catch**: `CatchWatch` runs the effects once per catch title. Normally from the fish item it finds (`rarity`, `variant`, `size`), the tick after the title. With a full inventory the fish never arrives: 500 ms after the title the server's subtitle stands in if it came within 1 s of the title (before or after): rarity from its badge (`U+F033`-`F037`, `F092`), fabled and trophy from `U+F044`/`U+F043`, gigantic unknown. A fish item that turns up later adds nothing. The catch ring needs the bobber's spot from the last 2 s (the bobber is gone the moment the fish is caught).
- **Drawing on the water**: END_MAIN fires right before the world renderer's last `draw()` of the main pass (bytecode, build.3), after the translucent terrain, and `context.consumers()` is that same entity buffer, so the rings are drawn on top of the water and blend with it. At AFTER_ENTITIES (the chummer wall) the water would be drawn later, over them. Quads are camera relative in `RenderLayers.debugQuads()`, and the drawing only reads the rings the ticks saved.

## Gotchas

- Particle switches drop a switched off type for your own particles too: with `minecraft:dust` off there are no sparkles.
- The wake and bite rings use the "only my fish" owner rule: no rings for a neighbour's fish, whatever `ownFishOnly` says.
- The edge glow colours are the server's; fabled's dark red (and the reds of mythical and gigantic) may read like a damage vignette. Lighten them if luna says so.
- Sodium keeps the world events (the chummer wall relies on it), but how a ring blends with water (and with Fabulous graphics) is only visible in game.
- luna is motion sensitive: rings grow about a block and fade, the glow only fades, nothing shakes or flashes.

## Tests

- `display/WaterRingsTest`: radius and alpha over a ring's life, closed flat band, bite ring interval and beats, the bite ring and its re-sends, wake spacing (0.35 block, 120 ms), the 48 cap, catch ring colour per rarity (common faint white), sparkle counts, the bobber spot kept 2 s, each switch.
- `display/BigCatchTest`: which fish count and the priority, kinds switched off, nothing for a normal fish, the glow curve (in, hold, out, peak, no jumps), the shine over time, the shine belongs to its own title, a sound per kind, the subtitle badges for a full inventory.
- `display/CatchEffectsIntegrationTest`: settings json round trip (old files off), a fish swimming in (owner rule and countdown as in `BobberTimer.onParticle`) leaving a wake and quicker bite rings, BITE! through `BobberTimer.onTitle` adding the bigger ring, a catch through `CatchWatch` leaving a ring at the remembered spot in mythical red with the glow, the subtitle fallback (both orders, once, too late), the groups' rows, the wiring. `screen/SettingsTilesIntegrationTest` has the two groups and their switches.

## Not run in game yet

luna checks: the rings sit on the water (not under it, not flickering), are faint enough, the heartbeat speeds up before the bite, the wake follows the fish and is not busy, the catch ring is in the right spot and colour, sparkles show for legendary and up, the glow does not feel like a flash or the damage vignette, the shine is readable, the sounds fit.
