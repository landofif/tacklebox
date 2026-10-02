# Dofi's Addons

Client-side Fabric mod for FishOnMC (`play.fishonmc.net`), MC 1.21.11. Mod id `dofisaddons`, package `dev.landofif.dofisaddons`. Repo `landofif/dofis-addons` (private), branch `main`. "l" in older notes is landofif, the owner.

Server knowledge (chat lines, glyph codepoints, item NBT, scoreboard, menus) is in `docs/fishonmc-research.md`. Read the relevant section before parsing anything the server sends.

## Hard rules

- **Display only.** The mod observes chat, titles, scoreboard, tab, boss bar, item NBT and container screens, and renders things. It never sends chat or commands, clicks, casts, reels or presses keys. Where FOECollab automates (auto `/pay`, `/contest` refreshes, `/find`), show a hint and let the player type the command instead.
- **Monospace font.** All text the mod draws uses the bundled DM Mono Light, with Medium for bold. It matches l's system24 Discord theme and the desktop font. Go through `util/Mono`: `Mono.text`/`Mono.bold` for screens, and `Mono.prepare` at draw time for anything scaled or drawn over the world. Never add `Formatting.BOLD` to mono text, because vanilla fake bold smears it. Server glyph text (rarity/rank tags, `U+E000`+ icons) stays in its original font so the private-use glyphs render.
- **FOECollab is AGPL.** Use `~/IdeaProjects/FOECollab` as a 1.21.11 API reference only and don't copy its code. The exception is features l wrote themselves: l allowed porting custom HUDs, which is why `%codes%` are FOECollab-compatible. Facts (strings, codepoints, NBT keys) are fine to reuse.
- `fishonmc-autofish` is a data source only. None of its automation comes over.

## UI

- **Everything looks like l's system24 Discord theme.** Colours come from `ui/S24`, converted from the theme's oklch values: greys plus accent purple `#B589D6`, flat boxes, 1px borders that turn accent on hover, and panel labels set into the top border.
- Screens extend `ui/DofiScreen`, which frames the whole screen as one labelled panel. Don't draw the title yourself. The widget mixins (`PressableWidgetMixin`, `SliderWidgetMixin`, `TextFieldWidgetMixin`, `ScrollableTextFieldWidgetMixin`) restyle vanilla widgets **only while a `DofiScreen` is open**, so the mod's screens can keep using vanilla `ButtonWidget`/`TextFieldWidget`. Override `framed()` to false for screens that must show the game (the HUD move screen).
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
