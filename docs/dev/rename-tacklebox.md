# Rename: dofisaddons to tacklebox (2026-10-12)

l renamed the mod because "dofis addons" sounded like a boast; the new name is `tacklebox`, all lowercase, in every place (display name, screen titles, jar, logs).

## What changed

- Mod id, package (`dev.landofif.tacklebox`), assets and data folders, `tacklebox.mixins.json`, `tacklebox.accesswidener`, jar name `tacklebox-*.jar`, gradle `rootProject.name`, CI artifact names, repo `landofif/tacklebox`.
- Classes: `Tacklebox` (entry point), `TackleboxSettings`, `ui/TackleboxScreen`.
- Mixin method prefixes are `tacklebox$`. Key ids are `key.tacklebox.open` and `key.tacklebox.dump_gui`, so a rebound key goes back to right shift / F8.
- Screen titles read "tacklebox · …", the chat copy message "tacklebox » message copied".
- The config folder is `config/tacklebox/`, always reached through `TackleboxSettings.dir()`.

## What keeps the old name on purpose

- `/dofi` still works as a second name for `/tacklebox` (`Tacklebox.settingsCommand` is registered twice).
- Trigger share codes now start with `tacklebox-trigger:`; `TriggerStore.importCode` also accepts the old `dofi-trigger:` ones.
- `util/ConfigMigration` moves `config/dofisaddons` to `config/tacklebox` on the first start (only if the new folder does not exist yet; if both exist the old one is left alone). It runs first in `Tacklebox.onInitializeClient`, before anything reads the config folder.
- The local project folder is still `~/IdeaProjects/dofis-addons`.

## Gotchas

- The old `dofisaddons-*.jar` must be deleted from a Prism instance's `mods` folder, or two copies of the mod load.
- Mod resources load in mod id order and `tacklebox` now sorts after `foecollab` (it used to sort before). The only foecollab asset we override is the foe logo, and that ships as a built-in pack (`Tacklebox.onInitializeClient`), which does not depend on that order.

## Tests

`util/ConfigMigrationTest` (the move and its edge cases), `ConfigMigrationIntegrationTest` (an old settings.json from before the rename, through the move and a second start), `trigger/TriggerShareCodeTest` (new and old share codes).

## Checked and not checked (2026-10-12)

- Checked in the dev client: it starts with the renamed mod, `config/dofisaddons` is moved to `config/tacklebox` with settings, triggers, huds and vaults byte-identical, `tacklebox:foelogo` is enabled, no mixin errors in the log. Minecraft logs a harmless warning once about the old `dofisaddons:foelogo` pack id it dropped from `options.txt`.
- Not run in game yet: `/tacklebox` and `/dofi` both opening the settings, the Mod Menu entry, the "tacklebox" key category in controls, the foe logo, flat leaves and bobber looks, and the Prism instance with the new jar.
