# dofis addons

client side fabric mod for **fishonmc** (minecraft 1.21.11).

display only: the mod reacts to what the server sends. it never sends chat, clicks, casts, reels or presses keys for you.

needs: fabric api and [foecollab](https://github.com/Kimberlyow/FOECollab) (with its own requirements, cloth config and mod menu). dofis addons is made to run next to foecollab: it leaves the trackers to foecollab, draws foecollabs huds and screens in the system24 look, and puts foecollabs settings in its own settings screen.

## getting around

- `/dofi` opens the home screen with everything below
- `/dofi triggers`, `/dofi huds`, `/dofi chat`, `/dofi stats`, `/dofi move` jump straight to one part
- right shift opens `/dofi` too
- `/dofi debug item` prints the held items data, `/dofi debug tooltip` prints the next tooltip you hover
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

## chat

`/dofi chat`, or chat on the home screen.

- **hide rules**: lines that match never show up in chat. type part of the line (`*` matches anything), the whole line, or a regex, and pick fishonmc only or every server. paste a line into **test with** to check which rule hides it
- **move chat**: drag the chat box anywhere in the move screen. right click it there to put it back
- **copy**: with chat open, right click a message to copy it. shift + right click a message to make a hide rule for it
- **chat boxes**: separate chat panels, each with its own place on screen. two come set up: **dms** (`✉ from …` and `✉ to …` lines) and **crew** (`[crew chat] …` lines). their lines leave the main chat (or stay there too with **also in chat**). each box has rules like the hide rules, a name in its top border, and can fade out a while after its last line. with chat open you can scroll a box, hover and click its text like normal chat, and right click a line to copy it. move them in the move screen: drag a box, scroll on it for more or fewer lines, shift + scroll for its width. **+ box** makes a new one for anything else

## hotkey

right shift opens `/dofi` from anywhere in game. change it in options → controls → dofis addons.

## stats

counts what you catch, all-time (per account, kept since you installed the mod, survives relaunches) and for the current session.

- fish, fish per hour, time spent fishing, total value, xp
- every rarity, size and variant, with percentages
- pets, shards, lightning in a bottle, infusion capsules, quests completed, money from selling
- fish since your last mythical, pet, shard and so on (dry streaks)
- most caught species, heaviest fish, top locations

a catch is counted when fishonmc shows the caught fish title and a new fish with your name as catcher lands in your inventory (or on the ground next to you when your inventory is full). every fish has its own id, so nothing is counted twice. the fishing clock pauses when you stop catching, so afk time does not count.

## custom huds

custom huds are foecollabs: `/dofi huds` (or the custom huds button) opens foecollabs own editor. dofis addons only restyles them. **move huds** is one screen for everything: foecollabs trackers and custom huds, the chat and the chat boxes.

## level

on fishonmc the vanilla xp bar shows your fishing level. dofis addons hides it (and its number) because foecollabs bar and level hud show it better. turn the vanilla bar back on in settings.

## bobber timer

shows how long your line has been in the water, floating above your bobber or under your crosshair. it changes colour as the wait gets long, and says bite! when something bites. in the bobber tab of the settings you can pick all four colours (normal, warn, slow, bite) and when they kick in, the size, how many decimals, bold, the height above the bobber, the box behind it, whether it shows through walls and how far under the crosshair it sits.

## bite

the bite is read from your own bobber, so someone fishing next to you can never set it off. it turns the timer to the bite colour, and with **bite message** on it also shows a short message (fish on! by default) in the middle of the screen.

## bobber and world tweaks

these change only what you see. bobber options are under settings → bobber, the rest under settings → world.

- **still bobber** (on by default): the bobber sits still on the water instead of bobbing. it still dips when a fish bites
- **smooth cast** (on by default): on a laggy connection your own cast flies smoothly instead of jumping back. where it lands stays exact
- **hide other bobbers**: other players bobbers and their lines disappear
- **hide my nametag**: hides the rank, name and power tag the server puts over your head
- **hide all nametags**: hides every players nametag, the servers and the vanilla one
- **hide xp popups**: hides the +66xp line above the hotbar after a catch
- **flat leaves**: draws leaves flat like vanilla instead of the fishonmc packs 3d leaves (reloads resources when changed)
- **short fish tooltips**: leaves out the latin name, value, ecosystem and the kg / cm part of fish tooltips
- **short pet tooltips**: shows maxed stats once and leaves out empty skin, item and trail slots

## bobber looks

- **bobber skin**: swap your bobber for one of 10 skins (ocean, gold, emerald, midnight, candy, duck, ember, pearl, star, rainbow). only you see it.
- **skin every bobber**: use your skin for everyone elses bobbers too, on your screen only.
- **line colour**: pick any colour for your own fishing line. other lines stay normal.

## viewmodel

changes how your held item looks in first person. only you see it.

- size, left / right, up / down, forward / back, tilt, turn and roll
- no switch dip stops the item from dipping when you switch slots
- swing style: normal, smooth, snappy or no swing (no swing keeps the item in place and only turns it around its own centre)
- swing turn sets how far no swing turns the item, from the full vanilla turn down to none
- swing length makes the swing animation slower or faster. it only changes how it looks, not how fast you can do anything.

all of this is in settings under bobber and viewmodel.

## files

everything lives in `config/dofisaddons/`: `triggers.json`, `settings.json` and `stats/<your uuid>.json`.

## look

screens and huds are styled after the system24 discord theme: flat dark panels, thin borders that light up purple on hover, labels set into the panel border. by default the whole game gets the same look (settings → look turns each part off):

- **flavor**: the colour set, from the flavors of the system24 discord theme: system24, catppuccin mocha, catppuccin macchiato, nord, tokyo night, rose pine, rose pine moon, everforest and vencord. it changes the mod, the restyled screens and the huds right away
- **restyle all screens**: buttons, sliders, text fields, checkboxes, tabs, lists, scrollbars, screen backgrounds and tooltips in every screen, vanilla and other mods included
- **restyle menus**: chests, your inventory and fishonmc menus become flat panels with the menu name in the border and flat slots. the art from the fishonmc pack is left out
- **restyle hotbar**: flat hotbar boxes with an accent border on the selected slot
- **mono font everywhere**: all text in the game in dm mono. fishonmc pack icons and animated text effects keep the server font
- **restyle foecollab huds**: foecollabs hud boxes become system24 panels in your flavor. pick any clean theme in foecollab (settings → foecollab → change theme) and the titles sit in the top border like the dofis huds. foecollabs own screens get system24 buttons with restyle all screens
- **title screen**: dofis addons (a system24 title screen with a **join fishonmc** button that shows how many players are on and your ping), foecollab, or minecraft

## foecollab

settings → foecollab has every foecollab option as a row in the same screen, grouped like foecollabs own config, with a search box on top. opening foecollabs config from its menu or from mod menu lands here too. **foecollab menu** opens foecollabs own screens (custom huds, custom buttons, pet calculator), and **move huds** its hud mover.

## font

all mod text uses dm mono light, with dm mono medium for bold (sil open font license 1.1, see `assets/dofisaddons/font/ofl-dmmono.txt`). it is shipped at several resolutions (`font/mono_1.json` … `mono_8.json`, `mono_bold_1.json` … `mono_bold_8.json`) and drawn at the one matching your gui scale, without the vanilla shadow, so it stays crisp.

## building

```bash
./gradlew build
```

java 21, gradle 9.5 (wrapper included). the jar ends up in `build/libs/`.
