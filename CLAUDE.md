# Dofi's Addons

Client-side Fabric mod for FishOnMC (`play.fishonmc.net`), MC 1.21.11. Mod id `dofisaddons`, package `dev.landofif.dofisaddons`. Repo `landofif/dofis-addons` (private), branch `main`. "l" in older notes is landofif, the owner.

Server knowledge (chat lines, glyph codepoints, item NBT, scoreboard, menus) is in `docs/fishonmc-research.md`. Read the relevant section before parsing anything the server sends.

## Hard rules

- **No capital letters and no apostrophes in anything a player reads**: README, HUD presets, button and tab labels, setting names, descriptions, tooltips, screen titles, `fabric.mod.json` name and description. Write "dofis addons", "dont", "xp", "fishonmc", "#3fa9f5". Exceptions are code identifiers, ids, log messages, and strings that must match server text exactly (chat patterns, glyphs). l asked for this on 2026-10-02. `Mono.house()` enforces it at draw time for all mono text, including server values and typed HUD lines, but write literals lowercase anyway. Trigger titles can use `{name.lower}` for captures.
- **Display only.** The mod observes chat, titles, scoreboard, tab, boss bar, item NBT and container screens, and renders things. It never sends chat or commands, clicks, casts, reels or presses keys. Where FOECollab automates (auto `/pay`, `/contest` refreshes, `/find`), show a hint and let the player type the command instead.
- **Monospace font.** All text the mod draws uses the bundled DM Mono Light, with Medium for bold. It matches l's system24 Discord theme and the desktop font. Go through `util/Mono`: `Mono.text`/`Mono.bold` for screens, and `Mono.prepare` at draw time for anything scaled or drawn over the world. Never add `Formatting.BOLD` to mono text, because vanilla fake bold smears it. Server glyph text (rarity/rank tags, `U+E000`+ icons) stays in its original font so the private-use glyphs render.
- **FOECollab is AGPL.** Use `~/IdeaProjects/FOECollab` as a 1.21.11 API reference only and don't copy its code. The exception is features l wrote themselves: l allowed porting custom HUDs, which is why `%codes%` are FOECollab-compatible. Facts (strings, codepoints, NBT keys) are fine to reuse.
- `fishonmc-autofish` is a data source only. None of its automation comes over.

## UI

- **Everything looks like l's system24 Discord theme.** Colours come from `ui/S24`, converted from the theme's oklch values: greys plus accent purple `#B589D6`, flat boxes, 1px borders that turn accent on hover, and panel labels set into the top border.
  - The `S24` colours are mutable and follow the chosen flavor (settings → look → flavor, `DofiSettings.flavor`, applied in `StatsTracker.loadSettings` and on change).
  - `ui/Flavor` holds refact0r/system24's flavors (MIT), converted to sRGB. Light and auto are left out because vanilla draws white text the mod doesn't control.
  - Read `S24.X` at draw time, never copy it into a `static final`, so flavor switches show on the next frame.
- Screens extend `ui/DofiScreen`, which frames the whole screen as one labelled panel. Don't draw the title yourself. The widget mixins (`PressableWidgetMixin`, `SliderWidgetMixin`, `TextFieldWidgetMixin`, `ScrollableTextFieldWidgetMixin`, `CheckboxWidgetMixin`, `TabButtonWidgetMixin`, `EntryListWidgetMixin`, `ScrollableWidgetMixin`, `ScreenMixin`, `SeparatorMixin`, `TooltipBackgroundRendererMixin`, `ToastMixin`) restyle vanilla widgets, backgrounds, tooltips and toasts while a `DofiScreen` is open **or** in every screen when the "restyle all screens" setting is on (`S24.everywhere()`, default on), so the mod's screens can keep using vanilla `ButtonWidget`/`TextFieldWidget`. `S24.active()` still means "a DofiScreen is open": only there is an inactive button a selected tab (`S24.button`). Override `framed()` to false for screens that must show the game (the HUD move screen).
- Motion (`ui/Anim`): `DofiScreen` contents fade in and rise about 8px when a screen opens (`ScreenMixin.intro` wraps `render` in `renderWithTooltip`, then lays a fading panel-colour cover inside the frame). Tab switches call `replayIntro()`. A new `RowListWidget` cascades its rows in. `PressableWidgetMixin` fades the hover colour, ripples on press, and grows the selected-tab underline. Keep animations short (≈100–300ms) and drawn by covering and moving, since there is no global alpha.
- Look settings (settings → look, `DofiSettings.s24*`, `monoEverywhere`, `titleScreen`), all default on:
  - `HandledScreenMixin` draws chests, shulkers, hoppers, droppers and the inventory as panels with flat slots. fishonmc menu art is glyphs in the title, so `util/Glyphs.menuName` strips private-use and hangul glyphs and pack-font runs, and names art-only titles from research section 8. Furnaces, anvils and so on keep their textures, because the progress arrows and buttons are in them.
  - `InGameHudMixin.flatHotbar` draws the hotbar.
  - "Mono font everywhere": `TextRendererMixin` (widths) and `TextRendererDrawerMixin` (drawing) send every minecraft-default-font run through `Mono.everywhere`. It leaves alone the colours the fishonmc pack text shader keys on (effects and no-shadow), since the TTF goes through `rendertype_text_intensity`, where those effects don't exist.
  - `MinecraftClientMixin` swaps `TitleScreen` for `screen/DofiTitleScreen`. Its join button connects to the fishonmc server-list entry, or a hidden one so the resource pack answer sticks. Joining only happens on the player's click.
- Chat (`chat/`, `screen/ChatFilterScreen`, settings `chatFilters`/`chatX`/`chatY`/`chatCopy`):
  - Hide rules run in `ALLOW_GAME` and `ALLOW_CHAT`, reusing `trigger/Matcher`.
  - `ChatHudMixin` moves the chat by wrapping the one `Backend.updatePose` call in `ChatHud.render(Backend,…)`. Drawing, hover and the `ChatScreen` click handler all go through it.
  - `chat/ChatCopy` finds the line under the mouse by running that same layout pass with its own `Backend` (via `ChatHudAccessor`).
  - The `/dofi` key is registered in `DofisAddons` (right shift, category `dofisaddons:main`, names in `lang/en_us.json`).
- Ported from l's older fishutils mod (MIT, l's own code), all only changing what the player sees:
  - Bobber: still bobber (`FishingBobberEntityMixin`, which also exposes `caughtFish` through `duck/BobberCatchAccess`), smooth cast (`EntityInterpolationMixin` + `display/SmoothCast`), and `display/BiteWatch`, which turns the rising edge of your own bobber's `caughtFish` into the bite (bobber timer and optional bite message).
  - World: `EntityRenderManagerMixin` hides other bobbers, nametag holograms and xp holograms (`display/Holograms` folds small caps and fancy digits). `PlayerNameLabelMixin` hides vanilla labels. `InGameHudOverlayMixin` hides the +xp action bar. `display/FlatLeaves` swaps leaf models with a `ModelLoadingPlugin` (models in `assets/dofisaddons/models/block`). `display/ShortTooltips` trims fish and pet tooltips.
  - Hud values: `hud/BaitInfo` (rod `activeBait` custom_data) and `hud/EventTracker` (event chat lines, estimated lengths).
  - Left out on purpose: the particle-based fish trail and bite countdown. l didnt want them.
- HUDs (`hud/HudRenderer`) use the same look: a panel with the bold first line as the border title, and `%tab%` for label/value rows.

## Git

Commit and push straight to `main`. No feature branches or PRs unless l asks for one (l said so on 2026-10-02). Each push to `main` triggers the CI build.

## Build

`java` is not on PATH on this machine:

```
env JAVA_HOME=/home/luna/.jdks/temurin-25.0.3 ./gradlew build
```

This works locally (verified 2026-10-02). The jar goes to `build/libs/`. Pushing to `main` also runs `.github/workflows/build.yml`, which uploads the jar (artifact `dofisaddons`) and, on push, a Prism test instance (`.github/scripts/prism_instance.py`, artifact `dofis-addons-prism`). The instance holds the mod, Fabric API, Mod Menu and perf mods from Modrinth, with FishOnMC in the server list. ModernFix has no 1.21.11 build yet. The local Prism instance is named `dofis-addons-prism`.

Deps: only Fabric API, plus Mod Menu 17.0.0 (optional at runtime). No config library: YACL was dropped on 2026-10-02 because l wanted a draggable colour picker and disliked YACL. The settings screen and `ColorPickerScreen` are the mod's own.

## Gotchas

- **Keep `yarn_mappings=1.21.11+build.3`.** build.6 broke `PositionedSoundInstance.master(SoundEvent,float,float)`. build.3 compiles, and FOECollab uses it too. Only bump after checking the newer API.
- **Only `@Mixin`/accessor classes go in `dofisaddons.mixin`.** Mixin owns the whole package and throws `IllegalClassLoadError` if anything else in it is loaded directly. On 2026-10-02 a duck interface there gave a black screen: entity renderers failed to bootstrap, but the process kept running and logging normally. Duck interfaces go in `dofisaddons.duck`.
- **Resource file names must be lowercase.** Also, only font definitions belong under `assets/dofisaddons/font/`.
- **TTF text rendering, which is why the font setup looks the way it does.** The glyph atlas uses nearest-neighbour sampling at the font's fixed `oversample`, hence the `mono_N` variants. Vanilla shadow and fake bold offset by a whole GUI unit, hence no shadow and the Medium face. The vanilla `rendertype_text_intensity*.fsh` shaders multiply coverage into colour *and* alpha (`.rrrr`), which gives a dark fringe. The mod overrides both in `assets/minecraft/shaders/core/`. They must stay in sync with vanilla's inputs/uniforms when MC updates; check with `glslangValidator` after inlining `#moj_import`s.
- `HudRenderCallback` is deprecated in Fabric API 0.141.6. Use `HudElementRegistry`.
