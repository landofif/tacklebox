# dofis addons

client side fabric mod for **fishonmc** (minecraft 1.21.11).

display only: the mod reacts to what the server sends. it never sends chat, clicks, casts, reels or presses keys for you.

needs: fabric api. mod menu is optional.

## getting around

- `/dofi` opens the home screen with everything below
- `/dofi triggers`, `/dofi huds`, `/dofi stats`, `/dofi move` jump straight to one part
- with mod menu: mods → dofis addons → configure

## chat triggers

show your own on-screen title and play a sound when a chat message appears.

1. **+ new trigger**: start blank or from a fishonmc preset (quest completed, pet drop, rare catch, fabled event, storm, moon, fish sold).
2. **when**: type the chat text to react to.
   - `*` matches anything
   - `{name}` grabs that part so you can show it in the title, for example `quests > quest #{n} has been completed!`
   - `>` also matches `»`, and fishonmc icon glyphs and extra spaces are ignored
   - paste a real chat line into **test with** to see right away if it fires and what `{n}` becomes
3. **title**: text, subtitle, colours, size, height, timing and animation (fade, pop, slide, typewriter, shake).
   - colours: `&a`, `&l` (bold), `&o`, `&#ff8800`
   - placeholders: your `{name}` captures, `{player}`, `{count}` (times fired this session), `{message}`
4. **sound**: click through popular sounds or type any sound id (server pack sounds work too), volume, pitch, repeats.
5. **▶ preview** any time, then **save**.

switch triggers on and off with the dot, reorder with the arrows, **share** copies a code others can **import**, and a cooldown stops repeated lines from spamming.

## stats

counts what you catch, all-time (per account, kept since you installed the mod, survives relaunches) and for the current session.

- fish, fish per hour, time spent fishing, total value, xp
- every rarity, size and variant, with percentages
- pets, shards, lightning in a bottle, infusion capsules, quests completed, money from selling
- fish since your last mythical, pet, shard and so on (dry streaks)
- most caught species, heaviest fish, top locations

a catch is counted when fishonmc shows the caught fish title and a new fish with your name as catcher lands in your inventory (or on the ground next to you when your inventory is full). every fish has its own id, so nothing is counted twice. the fishing clock pauses when you stop catching, so afk time does not count.

## custom huds

your own info boxes on screen, built from stats and server info.

- start from a preset: all-time stats (the default), session stats, dry streaks, where and when, wallet, level, bobber timer
- each line is text with colours and live values, for example `&7fish%tab%&f%alltime.fish%`
- `%tab%` splits a line into a label on the left and a value lined up on the right
- huds are drawn as panels in the style of the system24 discord theme. a bold first line with no values (like `&6&lall-time`) becomes the title in the top border. set the background to 0 for no panel at all
- **+ insert value** lists every value with what it shows right now, so you never have to remember names
- a line whose value is not available right now hides itself
- logic and maths work too: `%condition.(<session.fish_raw>>100)%`, `%round.(<expression.(<a>\/<b>)>,1)%`
- **move huds** lets you drag every box into place and scroll to resize
- share codes from foecollab and fishonmc extras import as well

## level

on fishonmc the vanilla xp bar shows your fishing level. dofis addons hides it (and its number) and shows a level hud instead: your level in its level colour, a progress bar and the percent to the next level. it is a normal custom hud, so you can move it, resize it or rebuild it with `%player.level_colored%`, `%player.level_bar%` and `%player.xp_progress%`. turn the vanilla bar back on in settings.

## bobber timer

shows how long your line has been in the water, floating above your bobber or under your crosshair. it turns yellow and then red as the wait gets long, and says bite! when something bites. change it in settings.

## bobber looks

- **bobber skin**: swap your bobber for one of 10 skins (ocean, gold, emerald, midnight, candy, duck, ember, pearl, star, rainbow). only you see it.
- **skin every bobber**: use your skin for everyone elses bobbers too, on your screen only.
- **line colour**: pick any colour for your own fishing line. other lines stay normal.

## viewmodel

changes how your held item looks in first person. only you see it.

- size, left / right, up / down, forward / back, tilt, turn and roll
- no switch dip stops the item from dipping when you switch slots
- swing style: normal, smooth, snappy or no swing
- swing length makes the swing animation slower or faster. it only changes how it looks, not how fast you can do anything.

all of this is in settings under bobber and viewmodel.

## files

everything lives in `config/dofisaddons/`: `triggers.json`, `huds.json`, `settings.json` and `stats/<your uuid>.json`.

## look

screens and huds are styled after the system24 discord theme: flat dark panels, thin borders that light up purple on hover, labels set into the panel border.

## font

all mod text uses dm mono light, with dm mono medium for bold (sil open font license 1.1, see `assets/dofisaddons/font/ofl-dmmono.txt`). it is shipped at several resolutions (`font/mono_1.json` … `mono_8.json`, `mono_bold_1.json` … `mono_bold_8.json`) and drawn at the one matching your gui scale, without the vanilla shadow, so it stays crisp.

## building

```bash
./gradlew build
```

java 21, gradle 9.5 (wrapper included). the jar ends up in `build/libs/`.
