# Fish key

- Asked for by luna 2026-11-11: fishing on one button. If the line is not cast the button casts it, on a bite it starts the minigame, in the minigame it controls the bar (normally sneak), and after the minigame it is a right click again as fast as possible so the next cast is quick. `input/FishKey`, key `key.tacklebox.fish` ("fish key: right click with a rod, sneak in the minigame") in the tacklebox key category, unbound by default; unbound is the off switch, there is no setting.
- **It is a rebind, not automation** (luna 2026-11-11: "it just rebinds existing keys, it doesnt automate anything"). Every input is the player's own press. Nothing is sent on a bite, on a timer or when the minigame ends, and the mod does not read the bite at all. Keep it that way: no auto reel, no auto recast, no steering.
- What the key does, with a fishing rod in either hand, no screen open, on FishOnMC:
  - Minigame not on screen: one press is one right click, through the game's own `MinecraftClient.doItemUse` (access widener), so it casts, reels in, starts the minigame and casts again exactly like the use key, and `BobberTimer.onUse` still sees it. Holding the key does not click again: a held keyboard key repeats its press event, so presses only count when the key was up the tick before.
  - Minigame on screen: the key is sneak while it is held. `mixin/KeyboardInputMixin` adds sneak to the player's input at the end of `KeyboardInput.tick`, so it works whatever the sneak key is bound to and with toggle sneak. Holding the key from the reel click into the minigame counts as sneak from the first frame.
  - The key held when the minigame ends: sneak stops, and it does nothing more until it is let go and pressed again.
  - Without a rod, with a screen open or on another server the key does nothing.
- **On the same button as the use key** (right mouse; 1.21.11 lets two bindings share a key, the controls screen shows them red): the game keeps making the right clicks and the mod makes none. In the minigame the mod drops the game's right click (`MinecraftClientMixin.doItemUse`, `FishKey.blockUse`) so the button is only sneak, and keeps dropping it until the button is let go once, so holding through the end of the minigame does not cast through the game's hold repeat.
- **Knowing the minigame is on screen** (the only game state read): every action bar message goes through `InGameHudOverlayMixin.setOverlayMessage` to `FishKey.onActionBar`. A message is a minigame frame when a part of it is in a font whose name starts with `fishing_` (`fishing_1/2/3`, research section 2) or it holds the minigame box `U+FAA0`, the bar `U+EDD0` or a progress glyph `U+EC00-ECC0`.
- **Back to right click**, at the first of:
  1. the catch title (one glyph in `U+E000-E999`, `CatchWatch.isCatchTitle`); a late frame within 500 ms after it does not start the minigame again,
  2. an empty action bar,
  3. an action bar line that is not a frame (the `+66xp` line that follows a catch),
  4. no new frame for a while: 500 ms until three frames were seen, then twice the longest wait between frames in this minigame plus 50 ms, between 150 and 1500 ms. At a frame per tick that is 150 ms.
  The `successful` / `failure` glyphs are not used: nobody has seen yet whether they mark the end or every tick of the minigame.
- The key is read at the start of the client tick (`ClientTickEvents.START_CLIENT_TICK`), before the game handles its own keys and the player's input, so a press acts in the same tick.
- Tests: unit `input/FishKeyTest`, integration `FishKeyIntegrationTest` (two rounds on the key tick by tick, a minigame that just stops, the key on right mouse).
- Not run in game yet. To check:
  - the action bar shows the fishing font only during the minigame. If it also shows it while waiting for a bite, the key would stay sneak and could not reel in.
  - which end signal comes first after a catch and after an escape, and that the next cast feels instant.
  - sneak through the key moves the bar like the sneak key, held and tapped.
  - what holding the key from the reel click into the minigame does to the bar.
  - on right mouse: no right click reaches the server during the minigame, and letting go after it gives the button back.
  - with foecollab installed: the game still starts (new mixin into `KeyboardInput.tick`, cancel in `doItemUse`).
