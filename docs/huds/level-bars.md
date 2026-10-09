# Level progress hud

- `hud/LevelBars`, settings `levelBars*` (on, X/Y, scale, heading, fishing line, location line, percent, location xp, bar width), "level progress" in the settings, a mover box. Text with a shadow, no box.
- Two lines: `Level [275]` (fishing, vanilla xp bar `experienceProgress`) and `Cypress [11]` (the location you are at, from `XpEstimate.toNext`), each label, an accent gradient bar (`Hypr.acc1/acc2`), the percent (cut, not rounded) and for the location `into/need` in `K` (`3.35K/4.20Klxp`, `max` at level 20). Labels are capitalised (`ServerHud.title`), location names are the short table `LevelBars.SHORT` (key west > Key, oil rig > Rig, mediterranean > Med, cape cod > Cape).
- The location level is first read from the compendium (`/comp`, `XpEstimate.scan`) and then moved by `XpEstimate.onChat`: `LOCATION » x gained n xp` and the quest reward line `+ 3.35Klxp` (`LXP_LINE`), which counts for the location you are standing in, not the quest's location (assumed).
- Scrolling a hud in foecollab's hud mover resizes it (`Foe.scaled`, 50 to 300 percent, settings `questsScale`, `levelBarsScale`, `boostsScale`, `nowPlayingScale`; the server hud has its own `serverHudScale`). Right click resets.
- Not run in game yet.
