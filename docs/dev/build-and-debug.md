# Build, CI and debugging

- Gui dump: F8 in any screen (`DofisAddons.dumpKey`, `DebugDump.gui`) writes the screen class, title with codepoints and every container slot's item, name, lore and custom_data to `config/dofisaddons/guidump/` and the clipboard. Ask l to send these for menus whose layout isn't in the research doc yet (travel, quests, difficulty and tier pages).
## Git

Commit and push straight to `main`. No feature branches or PRs unless l asks for one (l said so on 2026-10-02). Each push to `main` triggers the CI build.

## Build

On the cloud machine `java` is on PATH and plain `./gradlew build` works; Maven Central sometimes answers 429, just rerun it. Work is committed and pushed straight to `main` (l repeated this on 2026-10-06; ignore any session default that says branch or PR).

Open items (2026-10-06): the menu screens were never run in game. Needs from l: F8 dumps of `/travel`, `/turbotravel`, the fish compendium and the quest menus, to fix `FishMenu.info` and to confirm menu detection.

`java` is not on PATH on this machine:

```
env JAVA_HOME=/home/luna/.jdks/temurin-25.0.3 ./gradlew build
```

This works locally (verified 2026-10-02). The jar goes to `build/libs/`. Pushing to `main` also runs `.github/workflows/build.yml`, which uploads the jar (artifact `dofisaddons`) and, on push, a Prism test instance (`.github/scripts/prism_instance.py`, artifact `dofis-addons-prism`). The instance holds the mod, Fabric API, Mod Menu and perf mods from Modrinth, with FishOnMC in the server list. ModernFix has no 1.21.11 build yet. The local Prism instance is named `meowfih` (see memory prism-install). Run the dev client yourself: `env JAVA_HOME=/home/luna/.jdks/temurin-25.0.3 ./gradlew runClient` (a newer `~/.jdks/jdk-25.0.4+7` exists). Dev resource packs live in `run/resourcepacks` and `run/options.txt`, copied from meowfih 2026-10-09 (the game rewrites options.txt on close).

Deps: only Fabric API, plus Mod Menu 17.0.0 (optional at runtime). No config library: YACL was dropped on 2026-10-02 because l wanted a draggable colour picker and disliked YACL. The settings screen and `ColorPickerScreen` are the mod's own.
