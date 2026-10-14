# tacklebox

client side fabric mod for **fishonmc** (minecraft 1.21.11).

needs: fabric api and [foecollab](https://github.com/Kimberlyow/FOECollab) (with its own requirements, cloth config and mod menu). tacklebox is an addon for foecollab: it leaves the trackers and the look to foecollab, draws its own screens and huds with foecollabs boxes and the normal game font, puts its huds in foecollabs hud mover, and its settings next to foecollabs in one settings screen.

## getting around

- `/tacklebox` opens the settings screen, with chat triggers, chat and key commands in its list on the left
- `/tacklebox triggers`, `/tacklebox huds`, `/tacklebox chat`, `/tacklebox move` jump straight to one part
- right shift opens `/tacklebox` too
- `/tacklebox debug item` prints the held items data, f8 in any screen dumps it to a file
- with mod menu: mods → tacklebox → configure

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

`/tacklebox chat`, or chat in the screens list of the settings.

- **hide rules**: lines that match never show up in chat. type part of the line (`*` matches anything), the whole line, or a regex, and pick fishonmc only or every server. paste a line into **test with** to check which rule hides it
- **move chat**: drag the chat box anywhere in foecollabs hud mover. right click it there to put it back
- **copy**: with chat open, right click a message to copy it. shift + right click a message to make a hide rule for it
- **chat boxes**: separate chat panels, each with its own place on screen. two come set up: **dms** (`✉ from …` and `✉ to …` lines) and **crew** (`[crew chat] …` lines). their lines leave the main chat (or stay there too with **also in chat**). each box has rules like the hide rules, a name in its top border, and can fade out a while after its last line. with chat open you can scroll a box, hover and click its text like normal chat, and right click a line to copy it. move them in the move screen: drag a box, scroll on it for more or fewer lines, shift + scroll for its width. **+ box** makes a new one for anything else

## hotkey

right shift opens `/tacklebox` from anywhere in game. change it in options → controls → tacklebox.

## custom huds

custom huds are foecollabs: `/tacklebox huds` (or the custom huds button) opens foecollabs own editor. **move huds** (`/tacklebox move`) is foecollabs hud mover, with the tacklebox huds in it too: the quests left hud, the chat and the chat boxes. drag one to move it, right click to put it back.

## bobber and world tweaks

these change only what you see. bobber options are under settings → bobber, the rest under settings → world.

- **still bobber** (on by default): the bobber sits still on the water instead of bobbing. it still dips when a fish bites
- **smooth cast** (on by default): on a laggy connection your own cast flies smoothly instead of jumping back. where it lands stays exact
- **hide other bobbers**: other players bobbers and their lines disappear
- **hide my nametag**: hides the rank, name and power tag the server puts over your head
- **hide all nametags**: hides every players nametag, the servers and the vanilla one
- **hide xp popups**: hides the +66xp line above the hotbar after a catch
- **flat leaves**: draws leaves flat like vanilla instead of the fishonmc packs 3d leaves (reloads resources when changed)

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

everything lives in `config/tacklebox/`: `triggers.json` and `settings.json`.

## look

tacklebox has no look of its own. text is small capitals like foecollabs labels (tooltips keep the normal font). its screens, chests and huds use foecollabs box textures and the normal game font, widgets stay vanilla. settings → general has **simple menus**: chests, your inventory and fishonmc menus lose their box and slot boxes, you see just the items and the menu name (the art from the fishonmc pack is left out). the huds (quests left, chat boxes) have no box either, only text. **flat buttons** draws every button, the game menus too, as a flat see-through fill instead of the bevelled texture, lighter when hovered. the mods own screens have no frame either, only the title in the corner.

## settings

one settings screen for both mods: foecollabs categories first, then tacklebox (menus, huds, bobber, world, viewmodel), each a plain list of label and control with a hover tooltip, and a search box over everything. a long foecollab category (trackers, other, cosmetics) opens a dropdown of its sections under it when picked, and a click on one shows only that section. a grey heading folds the rows under it when clicked. opening foecollabs config from its menu or from mod menu lands here too. **foecollab menu** opens foecollabs own screens (custom huds, custom buttons, pet calculator), and **move huds** its hud mover.

## building

```bash
./gradlew build
```

java 21, gradle 9.5 (wrapper included). the jar ends up in `build/libs/`.
