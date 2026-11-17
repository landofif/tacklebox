# Timers hud

Countdowns to the tackle shop restock and the cosmetic store reset, and your votes (luna, 2026-11-15). Off by default (`timersHud.on`). The titles for a restock and a reset always show, whether the hud is on or not, and have no switch (luna: "always a title").

Credit: the idea and the cosmetic store and vote facts come from FOE+ (`itshaxr/FOE-Plus`, `StoreTimerHandler`), which builds on FOE-R (DannyPX) and FOECollab (Kimberlyow). No code was copied.

## Key files

- `hud/Timers`: pure (the tests run it). The clocks, the chat line patterns, the vote list, the rows, the cosmetic reset title.
- `hud/TimersHud`: chat hook, tick (twice a second), the titles (`TitleOverlay` + `SoundQueue`, id "timers"), the draw.
- `hud/TextHud`: the shared look of the text huds (title in the accent while the heading is on, label and value rows with a shadow, no box, like the event hud). `hud/HudSpot`: what such a hud saves (on, pos, scale, align, heading, off by default: see `level-bars.md` Headings); `HudMover.spot` gives it a mover box.
- Hooks: `Tacklebox` chat (`TimersHud.onChat`), tick, `custom_huds` render, `HudMover`. Settings page "huds", group "timers".

## How it works

| Row | From |
|---|---|
| tackle shop | a fixed grid: every 4 hours at 23, 3, 7, 11, 15, 19 UTC |
| cosmetic store | midnight UTC, every day |
| votes | `n/7`: the `VOTE » You received ...` lines since the last midnight UTC, when the votes reset (`votes`, saved; luna, 2026-11-17, before that the last 24 hours); switch `timersVotes` |

- Tackle shop grid: all 52 `TACKLE SHOP » » Tackle items restocked! Check the Fish Pro Shop to see your selection.` lines in luna's logs (2026-10-07 to 11-14) are within 12 seconds after a grid time, through the EU clock change (10-25) and the US one (11-01). So no anchor is saved and the chat line does not re-sync anything.
- Tackle shop title: shown when the restock line comes in chat (the server's own announcement), not from the countdown.
- Cosmetic store title: there is no chat line for the reset, so it comes from the clock: the first tick after joining only remembers the last midnight, a later midnight gives one title.

## Gotchas

- The restock line has `» »` twice; a player quoting it has `name »` in front and does not match.
- Votes are only counted while the game is running: a vote made while offline is not seen.

## Tests

`hud/TimersTest` (grid against logged restock times, midnight, title once and not on join, chat lines, votes reset at midnight UTC, rows), `TimersIntegrationTest` (log lines through `Matcher.normalize`, votes saved, luna's 7 votes back to 0/7 at midnight UTC, old settings get the hud at its home, a moved hud survives a save).

Not run in game yet: the hud's look, both titles, the votes going back to 0 at midnight UTC (when luna says the server resets them). Not verified at all (from FOE+ only): the cosmetic store reset time, 7 vote sites.
