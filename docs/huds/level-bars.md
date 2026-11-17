# Level progress hud

- `hud/LevelBars`, settings `levelBars*` (on, position `levelBarsPos` (see `positions.md`), scale, heading, fishing line, location line, bar, percent, location xp, bar width), "level progress" in the settings, a mover box. Text with a shadow, no box.
- Two lines: `Level [275]` (fishing, vanilla xp bar `experienceProgress`) and `Cypress [11]` (the location you are at, from `XpEstimate.toNext`), each label, an accent gradient bar (`Hypr.acc1/acc2`, spread over the whole track so a short fill shows only the start of it), the percent (cut, not rounded) and for the location `into/need` in `K` (`3.35K/4.20Klxp`). At level 20, the top, the location row has no percent and only the xp past the max: `Biwa [20] 14.4M` (luna, 2026-11-17; `LevelBars.locationXp`). Labels are capitalised (`ServerHud.title`), location names are the short table `LevelBars.SHORT` (key west > Key, oil rig > Rig, mediterranean > Med, cape cod > Cape).
- `levelBarsDecimals` (default on) turns the decimals of the location xp off: `367K/686Klxp`, rounded; millions keep one decimal (`3.7M`).
- `levelBarsMainXp` (default off) adds `1.02M/3.73Mxp` after the fishing bar: the vanilla progress times `XpEstimate.levelNeed()` (the /menu profile figure, else the learned `need` table; nothing shows while unknown). Not seen in game.
- `levelBarsBar` (default on) hides the bar itself; the label, percent and xp stay as text and the box gets narrower.
- Colours: only the number inside the brackets is coloured, the rest of the label stays dim. The fishing level follows the bands of foecollab's top bar (`hud/LevelColor`, written from the hex values, not copied: grey at 0 to 19, then yellow, orange, green, teal, blue, purple, pink, red every 15 levels, every 10 levels from 200, white from 300). The location level is always one orange (`LevelBars.LOCATION_LEVEL`, minecraft gold, read from a screenshot of luna's, not sampled), whatever the level.
- The location level is first read from the compendium (`/comp`, `XpEstimate.scan`) and then moved by `XpEstimate.onChat`: `LOCATION » x gained n xp` and the quest reward line `+ 3.35Klxp` (`LXP_LINE`), which counts for the location you are standing in, not the quest's location (assumed).
- The location row needs the location name from the boss bar (`ServerInfo.LOCATIONS`, matched against the bar text) and that location's level from the compendium. A location missing from `ServerInfo.LOCATIONS` never gets a row (and loses the server hud location line and "quests left" location row too): Nile River was missing until 2026-10-16. `ServerInfoLocationsTest` fails when a location in `Locations.ALL` cannot be read from the boss bar, so add new locations to both lists. The community goal bar is read by `ServerInfo.readBar` without hiding the time/location in the same bar (2026-10-19, assumed fix for the location row vanishing at Cairns, not confirmed in game). Assumes the Nile boss bar says "Nile River" (not seen in game yet).
- Scrolling a hud in the hud mover resizes it (`Foe.scaled`, 50 to 300 percent, settings `questsScale`, `levelBarsScale`, `boostsScale`, `nowPlayingScale`; the server hud has its own `serverHudScale`). Right click resets.
- Not run in game yet.

## Text alignment (all text huds)

- Every text hud has a `*Align` setting (`questsAlign`, `boostsAlign`, `levelBarsAlign`, `nowPlayingAlign`, `serverHudAlign`), a `TackleboxSettings.HudAlign` (left, centre, right; default left = the old look), a cycle row under each hud's toggle. `HudAlign.x(from, span, w)` gives the left edge, `Foe.title(..., boxW, align, ...)` lines up the heading.
- Quests and boosts: left keeps labels left and values in a right column; centre and right move each `label  value` row as one piece. Level bars: heading and the labels inside their column. Now playing: heading and text next to the art. Server hud: the title line stays centred.
- Not run in game yet.

## Someone else's profile (fixed 2026-10-30)

luna saw the level bars show another player's xp after `/stats <player>`: `XpEstimate.scan` took any item with `Fishing Level [n] (a/b)`, and `Power.scan`, `Compendium.scan` and `EventHud.scan` read that menu as hers too. Now `FishMenu.othersProfile` looks for the profile item's `Profile of <name>` line; when the name is not yours, `XpEstimate.tick` reads nothing from that menu. Menus without a profile item are read as before. Test: `OthersProfileIntegrationTest`. Not seen in game yet.

## Top location level (fixed 2026-11-17)

luna saw `Biwa [21] 100% max`; 20 is the top. The compendium count runs past its max at level 20 (`Level: [20] (14.42M/2.32M)`), `FishMenu.info` stops the progress at 1, so Biwa was saved as `[20, 2315250]`: exactly the cost of a level 21 (250 x 21^3). `XpEstimate.levelsGained` (levels passed by xp since the menu was read) had no stop at 20 and counted one more level; more Biwa xp would have made it 22. The same number gave the server hud `Location Level: 21 (max)` and quests left `to biwa level 22`.

Now `levelsGained` stops at `Power.MAX_LEVEL` (20). `FishMenu.Info.xp` is the count's first number as written, and `XpEstimate.read` keeps it at level 20 as the xp past the max; location xp from chat and quests adds to it. Quests left leaves out its location row at 20 (no level to count to). Saved settings still hold the capped `2315250` until /comp is opened once. Tests: `XpEstimateTest` (luna's saved Biwa), `LevelTopIntegrationTest` (tooltip, chat xp, the row's text). Not seen in game yet.

## Headings (all huds, 2026-11-17)

Every hud's title is off by default (luna: "remove those titles by default"), with a switch under each hud's toggle in the settings: `levelBarsHeading`, `nowPlayingHeading`, `questsHeading`, `boostsHeading` ("active"), `eventHeading` and `cryptidHeading`, and `HudSpot.heading` for the text huds drawn by `TextHud` (timers, slots left's "inventory", wrong gear's "warning", the mini event's name). The event and mini event headings are the event's name. Off moves the rows up 16 px (`20` to `4` under the hud's top) and the mover box gets shorter. Level progress and now playing had a switch that was on by default: a saved value is kept (luna's were already off), only new settings start off. Tests: `HudHeadingsTest`, `HudHeadingsIntegrationTest` (luna's settings shape through a load and a save). Not run in game yet.
