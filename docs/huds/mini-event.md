# Mini event hud

The running mini event on the hud, in the event hud's look (luna, 2026-11-15): when it ends, when the daily missions reroll, and your missions with their progress. Off by default (`miniEventHud.on`).

**It sends a command by itself.** While the hud is on, and only on FishOnMC, the mod sends `/minievent` quietly: 40 seconds after joining, then every 10 minutes (every 30 with no event on), and 5 seconds after a catch during an event (never within a minute of the last send, never while a screen is open). The answer to its own send is hidden from chat. Off sends nothing. This is the one command the mod sends on a timer; luna chose it on 2026-11-15 ("like FOE+"), see `rules/text-and-automation.md`.

Credit: the idea, the timing and the local counting between answers come from FOE+ (`itshaxr/FOE-Plus`, `MiniEventHandler`), which builds on FOE-R (DannyPX) and FOECollab (Kimberlyow). The answer's format is from luna's logs. No code was copied.

## Key files

- `hud/MiniEvent`: pure (the tests run it). Parses the answer, decides what is hidden, counts catches, decides when to send, makes the rows.
- `hud/MiniEventHud`: the setting, the chat hook (first in `Tacklebox`'s `ALLOW_GAME`, a hidden line reaches no trigger), the tick that sends the command, the catch hook (`CatchWatch.tick`), the draw through `TextHud` (title = the event as the server names it, "med sea mini event"), a sample in the mover.
- Settings page "huds", group "mini event". Nothing of the event is saved: it is asked again after joining.

## The server's answer (luna's logs, 2026-11)

One chat message each; a mission is one message of two lines:

```
MED SEA MINI EVENT » Ends in 2d 1h 13m 26s
Daily missions reroll in 18h 13m 26s
 - 🎣 Mediterranean Sea  
    ┗ (59/100) ∞ x25
 - 🎣 Mediterranean Sea ɢɪɢᴀɴᴛɪᴄ  ✔
    ┗ (2/2) One Timer
 - 🎣 Mediterranean Sea <U+F041>  
    ┗ (0/1) One Timer
 - 🎣 Mediterranean Sea Rays  
    ┗ (16/18) Daily
```

- Marks: `∞ xN` (repeats, N times done), `One Timer`, `Daily`. `✔` after the name when done. Zero units are left out of the times (`2d 4h 54s`).
- A variant is the pack's badge glyph in the name (`U+F041` albino, `F042` melanistic, `F043` trophy), gigantic is in small capitals. `Matcher.normalize` would drop the glyphs, so the raw message is parsed. Two missions can have the same name (`Mediterranean Sea` repeating and daily), so a mission is told by name and mark.
- A finished mission comes as `\n<EVENT> MINI EVENT » MISSION COMPLETE`, then the mission's message with its new count (`(0/100) ∞ x7`), then the reward (` + 1x Mediterranean Shard`). The hud takes the new count from it. Other lines: `... » You have completed every Mini Event Mission in the Mediterranean today!`, `KENAI MINI EVENT » Mini Event missions are now available. (/minievent)`. These are never hidden.
- Not in the logs: what the server answers with no event on. Guess: within 3 s of the mod's send, a server line that names the mini event and is none of the lines above is hidden and clears the hud.

## How it works

- **Hiding**: after a send, a header within 3 s starts a hidden answer; its reroll line and missions (within 2 s of the header) are hidden with it. An answer to `/minievent` you typed yourself is read too and shown as usual.
- **Rows**: `ends in`, `daily reroll`, then each mission that is not done, and the repeating ones always: `mediterranean sea | 59/100 · ∞ x25`. A finished one timer or daily is left out. The value is green when the goal is reached. Nothing shows with no event or after its end.
- **Counting between answers** (`MiniEvent.counts`): a catch adds one to every unfinished mission whose whole name fits the fish: the place (the fish's `location`), the size word, the variant, the rarity, and what is left over is the fish's name or its `group` (`Rays`, with or without the plural s). Pet missions (`Lynx Pet`) and anything else are left to the server's next answer, which replaces all counts.

## Gotchas

- With the hud on and no event, `/minievent` still goes out every 30 minutes; if the server's answer then is a line this does not recognise, it shows in chat.
- A catch with a full inventory is not seen (`display/catches.md`), so it neither counts nor asks.
- The fish `group` values are not sampled; the group match is by letters only.

## Tests

`hud/MiniEventTest` (the logged answer, rows, times, catches per mission kind, the MISSION COMPLETE block, when to send, what is hidden, another answer), `MiniEventIntegrationTest` (the hud off reads nothing; on: the quiet answer hidden and read, a catch counted and asked after, the next answer replacing it; a save).

Not run in game yet: the hud, that the answer is fully hidden and nothing else is, the timing of the sends, the answer with no event on, the group names, whether the server rate-limits the command.
