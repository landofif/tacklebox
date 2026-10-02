# dofis addons

client side fabric mod for **fishonmc** (minecraft 1.21.11).

display only: the mod reacts to what the server sends. it never sends chat, clicks, casts, reels or presses keys for you.

needs: fabric api and [yacl](https://modrinth.com/mod/yacl). mod menu is optional.

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

counts what you catch, this session and all-time (per account, since you installed the mod).

- fish, fish per hour, time spent fishing, total value, xp
- every rarity, size and variant, with percentages
- pets, shards, lightning in a bottle, infusion capsules, quests completed, money from selling
- fish since your last mythical, pet, shard and so on (dry streaks)
- most caught species, heaviest fish, top locations

a catch is counted when fishonmc shows the caught fish title and a new fish with your name as catcher lands in your inventory (or on the ground next to you when your inventory is full). every fish has its own id, so nothing is counted twice. the fishing clock pauses when you stop catching, so afk time does not count.

## custom huds

your own info boxes on screen, built from stats and server info.

- start from a preset: session stats, all-time stats, dry streaks, where and when, wallet, bobber timer
- each line is text with colours and live values, for example `&7fish &f%session.fish%`
- **+ insert value** lists every value with what it shows right now, so you never have to remember names
- a line whose value is not available right now hides itself
- logic and maths work too: `%condition.(<session.fish_raw>>100)%`, `%round.(<expression.(<a>\/<b>)>,1)%`
- **move huds** lets you drag every box into place and scroll to resize
- share codes from foecollab and fishonmc extras import as well

## bobber timer

shows how long your line has been in the water, floating above your bobber or under your crosshair. it turns yellow and then red as the wait gets long, and says bite! when something bites. change it in settings.

## files

everything lives in `config/dofisaddons/`: `triggers.json`, `huds.json`, `settings.json` and `stats/<your uuid>.json`.

## font

all mod text uses courier prime, a monospaced typewriter style font (sil open font license 1.1).

## building

```bash
./gradlew build
```

java 21, gradle 9.5 (wrapper included). the jar ends up in `build/libs/`.
