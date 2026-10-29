# Config backups

Daily zips of tacklebox's config (2026-10-29, luna's ask). FOECollab backs up only its own stats file (`/foe backup`).

## What it does

On the first game start of a day, `util/ConfigBackups.run` (called first thing in `Tacklebox.onInitializeClient`, after the dofisaddons move) zips `config/tacklebox` into `config/tacklebox/backups/yyyy-MM-dd.zip`. A second start that day does nothing. Only the newest 7 dated zips stay (`KEEP`, luna's pick); other files in `backups/` are never touched.

- In: settings.json (chat boxes, hide rules, aliases and everything else are in it), triggers.json, huds.json, vaults.nbt, stats/, sent-history.txt, older `triggers.before-*.json`, anything new.
- Out: `backups/`, `guidump/`, pictures (`.png`, `.jpg`, `.jpeg`; luna said skip them, about 7 MB a day), and `.tmp`/`.broken` files.
- Written to `yyyy-MM-dd.zip.tmp` first, then moved, so a crash leaves no half zip under the real name.
- Restore: close the game, unzip one into `config/tacklebox`.

## Gotchas

- A game left running past midnight makes the next day's zip only at the next start.
- A failed backup only logs an error; the game starts as usual.

## Tests

`util/ConfigBackupsTest` (what is kept, one zip a day, no zip for an empty folder, pruning), `ConfigBackupsIntegrationTest` (a config folder like luna's started ten days in a row).

Not run in game yet: check `config/tacklebox/backups/` after the first start.
