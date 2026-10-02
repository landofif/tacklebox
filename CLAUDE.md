# Dofi's Addons

Client-side Fabric mod for FishOnMC (`play.fishonmc.net`), MC 1.21.11. Mod id `dofisaddons`, package `dev.landofif.dofisaddons`. Repo `landofif/dofis-addons` (private), branch `main`. "l" in older notes is landofif, the owner.

Server knowledge (chat lines, glyph codepoints, item NBT, scoreboard, menus) is in `docs/fishonmc-research.md`. Read the relevant section before parsing anything the server sends.

## Hard rules

- **Display only.** The mod observes chat, titles, scoreboard, tab, boss bar, item NBT and container screens, and renders things. It never sends chat or commands, clicks, casts, reels or presses keys. Where FOECollab automates (auto `/pay`, `/contest` refreshes, `/find`), show a hint and let the player type the command instead.
- **Monospace font.** All text the mod draws uses the bundled Courier Prime (`dofisaddons:mono`, see `util/Mono`). Server glyph text (rarity/rank tags, `U+E000`+ icons) stays in its original font so the private-use glyphs render.
- **FOECollab is AGPL.** Use `~/IdeaProjects/FOECollab` as a 1.21.11 API reference only and don't copy its code. The exception is features l wrote themselves: l allowed porting custom HUDs, which is why `%codes%` are FOECollab-compatible. Facts (strings, codepoints, NBT keys) are fine to reuse.
- `fishonmc-autofish` is a data source only. None of its automation comes over.

## Build

`java` is not on PATH on this machine:

```
env JAVA_HOME=/home/luna/.jdks/temurin-25.0.3 ./gradlew build
```

This works locally (verified 2026-10-02). The jar goes to `build/libs/`. Pushing to `main` also runs `.github/workflows/build.yml`, which uploads the jar (artifact `dofisaddons`) and, on push, a Prism test instance (`.github/scripts/prism_instance.py`, artifact `dofis-addons-prism`). The instance holds the mod, Fabric API, YACL, Mod Menu and perf mods from Modrinth, with FishOnMC in the server list. ModernFix has no 1.21.11 build yet. The local Prism instance is named `dofis-addons-prism`.

Deps: YACL `dev.isxander:yet-another-config-lib:3.8.2+1.21.11-fabric` (repos maven.isxander.dev + maven.quiltmc.org), Mod Menu 17.0.0 (optional at runtime).

## Gotchas

- **Keep `yarn_mappings=1.21.11+build.3`.** build.6 broke `PositionedSoundInstance.master(SoundEvent,float,float)`. build.3 compiles, and FOECollab uses it too. Only bump after checking the newer API.
- **Only `@Mixin`/accessor classes go in `dofisaddons.mixin`.** Mixin owns the whole package and throws `IllegalClassLoadError` if anything else in it is loaded directly. On 2026-10-02 a duck interface there gave a black screen: entity renderers failed to bootstrap, but the process kept running and logging normally. Duck interfaces go in `dofisaddons.duck`.
- **Resource file names must be lowercase.** Also, only font definitions belong under `assets/dofisaddons/font/`.
- `HudRenderCallback` is deprecated in Fabric API 0.141.6. Use `HudElementRegistry`.
