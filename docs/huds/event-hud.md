# Event hud

- Event hud (2026-10-24, asked for by luna for the Halloween 2026 event; `hud/EventHud`, settings `eventHud/eventPos/eventScale/eventAlign`, "event" in the cloth tab, a mover box, drawn in the `custom_huds` element): text with a shadow and no box, like quests left. Off by default.
- Source: `/event`, a vanilla chest (title art only, `U+EEE4 U+D568`, so `EventHud.isEventMenu` looks for `U+D568`; `FishMenu` does not take it over). `XpEstimate.tick` calls `EventHud.scan` every half second for the open screen. Items read: `Threshold n` player heads (slots 10-16; lore `[<have> pts|<need> pts]`, then reward lines: money, items, a cosmetic, then a blank line and the bait package with a `┗ <biome>` line that is dropped; `<none>` means 0, `need` is the total points, not per step), the book `Event Missions → <n> pts` (slot 22), the leaderboard head (slot 41: `Your Placement: Unranked`, `Event Points: <n> pts`, refreshed by the server every 5 minutes) and the `Welcome to Halloween 2026!` gunpowder (slot 39, gives the hud title).
- Rows: points, the next threshold with points to go and its rewards, thresholds reached/total, placement. When every threshold is reached it says so.
- Not saved: blank after a restart until `/event` was opened once. Points only update when `/event` is opened (no chat line for points is known).
- Not tracked yet: the missions behind the book (slot 22 opens another menu, not dumped yet), the River Monster species and the event booster text (slot 37/39, static).
- Tests: `EventHudTest` (dump lines in, rows out). Not run in game yet.
