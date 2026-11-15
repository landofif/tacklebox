# Cryptids hud and popups

What can be caught where right now, and when the next chance comes (luna, 2026-11-01). Off by default (`cryptidHud`), popups on (`cryptidPopups`). Settings page "huds", group "cryptids", with one switch per kind of event (`cryptidOff`): moons, rainbow, storms, fabled, bigfoot, titanoboa, bessie. A switched-off kind is gone from the hud, the popups and the dry streak counting. The popups are not triggers: nothing has to be added in /triggers.

## Key files

- `hud/Cryptids`: pure (the tests run it). The spots, the moon prediction, the clock windows, the rows, the popups, dry streak counting and drop matching.
- `hud/CryptidHud`: feeds it the chat state of `ServerHud` (moon, `Rainbow`, `Storm`, fabled place), the top bar clock (`ServerInfo.time/ampm`) and the settings, draws the hud, shows popups (`TitleOverlay` + `SoundQueue`, id "cryptids"), hooks chat and catches.
- Hooks: `Tacklebox` chat (`CryptidHud.onChat`), tick (`CryptidHud.tick`, twice a second), `custom_huds` render, `HudMover` box (`cryptidPos/Scale/Align`), `CatchWatch.onCatch`.

## What it says

| Event | Where | What |
|---|---|---|
| any moon | toledo | rougarou 1/300 · blood/claw |
| any moon | great lakes | bessie 1/500 · silver scale |
| blood moon | anywhere | infusion capsule 1/300 |
| rainbow | danube | garaboncias 1/250 · concoction/essence |
| thunderstorm / supercell | anywhere | lightning bottle 1/500 / 1/250 |
| fabled | the place from chat | fabled fish 1/3000 |
| 1-3 am in-game | cypress | bigfoot 1/300 · tooth/fur |
| 2-4 am in-game | amazon | titanoboa 1/1500 · titan's fang |
| 4-6 am in-game | great lakes | bessie 1/1000 · silver scale |

Odds are the base odds luna pasted from the server's announcement and the wiki (Bigfoot 1-3 am per the wiki; the old research note said 12-3). No pet item or boost is added.

The hud: each event on now with its time left (moons from their rise, clock windows from the clock; rainbow, storms and fabled end by chat so they show none), its spots below it with a grey dry streak `(n)`, then "next": the next moon and the clock windows that are not open, soonest first.

## Moon prediction

From luna's logs (75 moonrises, 2026-09-05 to 11-01): a moon rises every 160 minutes to the second and lasts 600 s, but the phase shifted a few times (around 23:22, 22:42 from 10-11, 21:22 from 10-25), probably server restarts. Foecollab's `TimerHandler` assumes a fixed 00:03:01 UTC phase, which was right until about 10-10 and is about 100 minutes off since 10-25. So the mod keeps the last rise it saw in chat (`moonRiseAt`, saved; starts at the last logged rise, 2026-11-01 18:42:55 CET) and adds 160 minutes. The server hud's "Moon Cycle" line uses this too now, not foecollab's timer. The kind of moon looks random, so only the time is predicted.

## Clock windows

From the time in the top bar. A game day is assumed to be 20 real minutes like vanilla (8 days are the 160 minute moon cycle), so a 2 hour window is about 100 real seconds and the windows come round every 20 minutes. Only the countdowns depend on that; open or closed is read from the clock itself.

## Dry streaks

`cryptidDry` (saved): every own catch (`CatchWatch`) while an event is on adds one to each of its spots at that location (anywhere spots count everywhere; cypress lake matches `spawn` and `spawnhub`). An own `RARE CATCH! You pulled ...` line naming the drop (rougarou blood/claw, silver scale, infusion capsule, vile concoction/purifying essence, bigfoot tooth/fur, titan's fang, lightning in a bottle) sets it to 0; a fabled variant catch during the fabled event ends the fabled one. Bessie's moon and clock spots share one streak. Assumed: cryptid drops come as own rare catch lines like shards do. None is in luna's logs yet. A Bigfoot steal gives no fish, so it is not counted as a catch.

## Gotchas

- Joining during a storm gives `STORM » A Thunderstorm is ongoing`, which the server hud keeps as plain "Storm"; `Cryptids.now` treats anything not "Supercell" as a thunderstorm.
- Popups fire once per event while it lasts (`Cryptids.started`); after leaving the server they can fire again for an event still on.

## Tests

`CryptidsTest` (moon from logged rises and a phase shift, clock parsing, windows across midnight, rows, switches, dry streak counting and drop lines, popups), `CryptidsIntegrationTest` (real chat lines through the server hud and `CryptidHud`: blood moon rows, popup once and next moon; dry streak through catches and a drop; ongoing storm; fabled place with the clock and a switched-off kind).

Not run in game yet: the hud, popups, the clock speed, the cryptid drop line format, the dry streak counts.
