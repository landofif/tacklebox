# tacklebox

Client-side Fabric mod for FishOnMC (`play.fishonmc.net`), MC 1.21.11. Mod id `tacklebox`, package `dev.landofif.tacklebox`. Repo `landofif/tacklebox` (private), branch `main`. "l" in older notes is landofif, the owner.
It was called dofisaddons until 2026-10-12; the local folder is still `dofis-addons`. See `docs/dev/rename-tacklebox.md`.

## Project map

- `src/main/java/dev/landofif/tacklebox/`: `chat/`, `compat/` (FOECollab by reflection), `display/`, `hud/`, `input/`, `menu/`, `mixin/`, `duck/`, `screen/`, `trigger/`, `ui/`, `util/`, plus `Tacklebox` and `TackleboxSettings`
- `docs/`: all project documentation (see below). Server facts: `docs/fishonmc-research.md`

## Hard rules

- **Text is small capitals.** `Foe.text()`/`Foe.bold()` for labels, titles, buttons; `Foe.sentence()` for long text. Details: `docs/rules/text-and-display-only.md`.
- **Display only.** The mod observes and renders. It never sends chat or commands, clicks, casts, reels or presses keys, except the exceptions listed in `docs/rules/text-and-display-only.md`, each only on the player's own click or key.
- **Foecollab already has it, so the mod does not.** Before adding a feature, check foecollab's config for the same thing and ask l.
- **Addon to foecollab, not a theme.** No themes, fonts or restyling of foecollab or vanilla. Keep it minimal: l wants less, not more.
- **FOECollab is a runtime dependency.** Never compile against it: `compat/` reaches it by hud id, class names and reflection.
- **FOECollab is AGPL.** API reference only, don't copy its code. Facts (strings, codepoints, NBT keys) are fine.
- `fishonmc-autofish` is a data source only. None of its automation comes over.

## Gotchas

- **Keep `yarn_mappings=1.21.11+build.3`.** build.6 broke `PositionedSoundInstance.master(SoundEvent,float,float)`.
- **Only `@Mixin`/accessor classes go in `tacklebox.mixin`.** Anything else there causes `IllegalClassLoadError` (black screen on 2026-10-02). Duck interfaces go in `tacklebox.duck`.
- **Resource file names must be lowercase.** Only font definitions belong under `assets/tacklebox/font/`.
- `HudRenderCallback` is deprecated. Use `HudElementRegistry`.
- Never `@Redirect` a call FOECollab also redirects (crash at load); use `@WrapOperation`. Dev runs keep foecollab on intermediary names, see `docs/foecollab/integration.md`.

## Build and git

- `java` is not on PATH here: `env JAVA_HOME=/home/luna/.jdks/temurin-25.0.3 ./gradlew build`. More in `docs/dev/build-and-debug.md`.
- Commit and push straight to `main`. No branches or PRs unless l asks. Each push runs CI.

## Documentation rules (IMPORTANT)

1. Do NOT read source code to get context. Start with `docs/index.md`, then the area index, then the topic doc.
2. Only read code when the docs don't answer the question, or you are about to edit that file.
3. If documentation is missing, outdated or wrong, update it. No doc for the topic: create one and add it to the area's index.
4. Before finishing, check: did I change behavior the docs describe? If yes, update the docs in the same change and say which docs you touched.

## Testing rules

- Every major feature ships with unit tests AND one integration test. Run the full suite before saying a task is done, and report the result.
- If a test fails, do not delete or weaken it to make it pass. Fix the code, or tell l why the test is wrong.
- Most features here need a look in the game: say what l still has to check in the real client ("not run in game yet" stays in the docs until then).

## Workflow

- Ask clarifying questions before writing a plan if anything is ambiguous. Present a plan and wait for l's approval before writing code.
- Bugs: find the root cause first, explain it, add a reproducing test, then fix.
