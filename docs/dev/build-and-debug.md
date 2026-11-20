# Build, CI and debugging

- Gui dump: F8 in any screen (`Tacklebox.dumpKey`, `DebugDump.gui`) writes the screen class, title with codepoints and every container slot's item, name, lore and custom_data (the server's bread placeholder for empty slots is skipped) to `config/tacklebox/guidump/` and the clipboard. Ask luna to send these for menus whose layout isn't in the research doc yet (travel, quests, difficulty and tier pages).
- Item dump: `/tacklebox debug item` with the item in the main hand (`DebugDump.heldItem`) prints id, lore strings and custom_data to chat. Since 2026-10-16 it also writes `config/tacklebox/guidump/item-<time>.txt` and copies it to the clipboard: custom name, item name and every lore line as JSON (styles, colours, fonts, siblings; non-ascii as `\uXXXX`), the tooltip exactly as the client builds it (basic, after other mods' tooltip callbacks, so after foecollab's) and custom_data. Used to design the short fish tooltips; the styled part is not run in game yet.
- Entity dump (2026-10-16): `/tacklebox debug entities [radius]` (`DebugDump.entities`, radius 1 to 64, default 24) writes the crosshair target, every entity and every block entity within the radius to `config/tacklebox/guidump/entities-<time>.txt` and the clipboard (type, position, name, held and worn items with `custom_data` and skull profile, display entity contents, raw nbt); chat only gets the counts. Made to find out what a placed chummer is, see `huds/chummer-range.md`. Run in game 2026-10-17 and it worked. It has no unit test: it only reads game objects, like the other dumps.

## Git

Commit and push straight to `main`. No feature branches or PRs unless luna asks for one (luna said so on 2026-10-02). Each push to `main` triggers the CI build.

Commit messages are short and clean: one line on what changed, a few short lines more only if the why isn't obvious. No AI attribution (no `Co-Authored-By`, `Claude-Session` or "Generated with" lines) and no mention of luna (no "luna asked", no quotes from her). Set on 2026-11-20.

## Build

On the cloud machine `java` is on PATH and plain `./gradlew build` works; Maven Central sometimes answers 429, just rerun it. Work is committed and pushed straight to `main` (luna repeated this on 2026-10-06; ignore any session default that says branch or PR).

Open items (2026-10-06): the menu screens were never run in game. Needs from luna: F8 dumps of `/travel`, `/turbotravel`, the fish compendium and the quest menus, to fix `FishMenu.info` and to confirm menu detection.

`java` is not on PATH on this machine:

```
env JAVA_HOME=/home/luna/.jdks/temurin-25.0.3 ./gradlew build
```

This works locally (verified 2026-10-02). The jar goes to `build/libs/`. Pushing to `main` also runs `.github/workflows/build.yml`, which uploads the jar (artifact `tacklebox`) and, on push, a Prism test instance (`.github/scripts/prism_instance.py`, artifact `tacklebox-prism`). Artifacts are kept 7 days (the jar) and 1 day (the Prism instance, 13 MB a push) since 2026-11-15: with GitHub's default 90 days the repo's artifact storage filled up (499 artifacts, 3.1 GB) and every run from 2026-11-02 on failed at the upload step, though the build passed; the old ones were deleted. The instance holds the mod, Fabric API, Mod Menu and perf mods from Modrinth, with FishOnMC in the server list. ModernFix has no 1.21.11 build yet. The local Prism instance is named `meowfih` (see memory prism-install). Run the dev client yourself: `env JAVA_HOME=/home/luna/.jdks/temurin-25.0.3 ./gradlew runClient` (a newer `~/.jdks/jdk-25.0.4+7` exists). Dev resource packs live in `run/resourcepacks` and `run/options.txt`, copied from meowfih 2026-10-09 (the game rewrites options.txt on close). Unit tests (JUnit 5, `src/test`, run by `gradlew build`) cover pure parsing only, e.g. `EventLinesTest`; classes that touch Minecraft statics cannot load in them.

Deps: only Fabric API, plus Mod Menu 17.0.0 (optional at runtime). No config library: YACL was dropped on 2026-10-02 because luna wanted a draggable colour picker and disliked YACL. The settings screen and `ColorPickerScreen` are the mod's own.
- Entity count (2026-10-18): `/tacklebox debug count` (`DebugDump.count`, tally in `EntityTally`, unit tested) lists every loaded entity by type id in chat, most common first. Made to see what the F3 `E: x/y` total is made of in a crowd (luna saw 325 in a crowd, about 145 away from it). Not run in game yet.
- Display lag test (2026-10-18): `/tacklebox debug displays` toggles `OtherBobbers.hideDisplays` (not saved), which makes `EntityRenderManagerMixin` skip every item, block and text display so fps with and without them can be compared at spawn (679 entities there, 225 item, 88 block, 47 text displays, 348 interactions). Hides server shop and sign text too while on. Not run in game yet; if fps jumps it becomes a distance setting.
