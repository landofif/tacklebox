# Dry streak chat messages

When a rare drop finally lands, a chat line says how many catches it took (luna, 2026-11-15). Only you see the line. Every kind is off by default (`dryOn`); the counting always runs, so a kind switched on later has real numbers.

Credit: the idea comes from FOE+ (`itshaxr/FOE-Plus`, `RareCatchHandler` and its chat lines), which builds on FOE-R (DannyPX) and FOECollab (Kimberlyow). No code was copied. FOECollab has its own dry streak lines; switch those off there if both are installed.

## Key files

- `chat/DryStreaks`: pure (the tests run it). The kinds, the drop line patterns, the counting, the texts and their placeholders.
- `chat/DryStreakChat`: feeds it the catches (`CatchWatch.tick`), the chat (`Tacklebox` chat hook) and the moon and storm (`CryptidHud.state`), and prints the lines on the next tick so they come after the server's own line.
- `screen/DryStreakScreen`: per kind a switch, its text and a "default" button. Settings > world > "dry streaks": a switch per kind and "dry streak texts" opens the screen.
- Saved: `dryOn` (kinds switched on), `dryText` (edited texts only), `dryCount` (catches since the last drop).

## How it works

| Kind | Ends on | Counts |
|---|---|---|
| pet | own `PET DROP! You pulled a <rarity> <pet> Pet` line | every catch |
| shard | own `RARE CATCH! You pulled a <n>x <climate> Shard` line | every catch |
| capsule | own rare catch line naming Infusion Capsule | catches while a blood moon is up |
| bottle | own rare catch line naming Lightning in a Bottle | catches during a thunderstorm or supercell |
| variant | a caught fish whose `variant` is albino, melanistic, trophy or fabled | every catch, one count per variant (`variant:albino` ...) |
| gigantic | a caught fish whose `size` is gigantic | every catch |

- The number is the catches it took, the one that brought the drop included (1 = the very next catch after the last drop).
- A drop line and its catch can come in either order (the catch is seen when the fish shows up in the inventory, up to 3 s after the title). A drop line within 3 s after a counted catch uses the count as it is; otherwise it adds that catch itself and the next catch within 3 s is not counted again for that kind.
- Texts: one per kind, with `%item%` (pet, shard or fish name as written), `%streak%` (grouped: 1,234), `%count%` (the n of `nx`), `%variant%`, `%rarity%` (pets, when the line names it). Colour codes as in triggers (`&a`, `&#rrggbb`, `&l`). An empty text is the default again.
- Other rare catches (prospecting amulet, bigfoot and cryptid items) have no streak here; the cryptids hud counts the cryptid ones (`huds/cryptids.md`).

## Gotchas

- Simplest reading, luna has not seen it yet: the fabled variant counts every catch like the others (as FOE+ does), not only catches during the fabled event; event variants (alternate, spooky, frozen) have no streak.
- The first line of each kind only counts since the mod started counting.
- A catch with a full inventory is not seen (`display/catches.md`), so it is not counted.
- The capsule and bottle counts are separate from the cryptids hud's own `cryptidDry`.

## Tests

`chat/DryStreaksTest` (line patterns, both orders of line and catch, moon and storm only counts, variants and gigantic, switched-off kinds, placeholders, default texts), `DryStreaksIntegrationTest` (log lines through the normalizer with a blood moon from the server hud's chat state, the default and an edited text, a save).

Not run in game yet: the lines in chat and their order after the server's line, the edit screen, whether a pet or shard line really comes within 3 s of its catch, what the fish item's name looks like in `%item%`.
