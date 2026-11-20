# tacklebox

client side fabric mod for **fishonmc** (minecraft 1.21.11).

needs: [fabric api](https://modrinth.com/mod/fabric-api). [mod menu](https://modrinth.com/mod/modmenu) is optional.

works with or without [foecollab](https://github.com/Kimberlyow/FOECollab) (and next to foe-r). tacklebox reads catches and game data itself; when foecollab is installed it adds a few small tweaks to it (see [foecollab tweaks](#foecollab-tweaks)). things foecollab already does, tacklebox leaves out.

almost everything is off by default. turn on what you want in the settings.

## getting around

- `/tacklebox` (or `/dofi`, or right shift) opens the settings: pages on the left, their groups in the middle, the settings of a group on the right, a search over everything, hover for help
- `/tacklebox move` moves every hud, the hotbar, chat and the chat boxes. drag to move, right click to put back
- `/tacklebox triggers`, `chat`, `keys`, `bobber`, `viewmodel`, `words` jump straight to one part
- with mod menu: mods → tacklebox → configure

## screens

- **bait picker** (`/tacklebox bait`): the fastest quest and best bait for a place
- **power** (`/tacklebox power`): your power by source, the cheapest next level, all places in a table
- **pet prices** (`/tacklebox prices`): what pets sell for between players by rating and rarity, from the community's pet price sheet
- **travel map**: `/tt`, `/travel` and `/turbotravel` as a world map. left click a place to travel, right click for its species
- **fishonmc menu**: travel, stats, crew, event and quests as one dark panel with place tiles and bars
- **vault overview**: all seven `/pv` vaults on one screen, with search
- **presets wardrobe**: your armour sets on player models, click to equip

## menus and items

- **simple menus**: chests and your inventory as bare items, no boxes
- **/travel and /quests marks**: frenzy place green, fabled place red; after a quest, its difficulty and tier get a green slot to start it again
- **page wheel**: the mouse wheel turns pages in the compendium, auction house, vaults and presets
- **chest scale**: its own gui scale for server menus
- **short tooltips** for fish and pets, **pet profit marks** in the auction house, **rarity circles** behind items, and **tooltip scroll** for tooltips taller than the screen

## huds

all huds move in `/tacklebox move`, and each has an optional title.

- **bobber timer**: on the hud or over the bobber, with a bite countdown and its own bite title
- **catch title**: the fish's picture, rarity, name, weight and length in place of the server's catch title
- **quests left** until your next fishing and place level, and **level bars**
- **active boosts**: boosters, chummers, weather, moon, frenzy, fabled
- **event** and **mini event**: points, placement and missions. the mini event hud quietly sends `/minievent` every 10 minutes and hides the answer
- **cryptids**: where to fish for what during moons, rainbows, storms and fabled, with popups
- **inventory** (your 27 slots on screen) and **slots left** (shows when your inventory is almost full)
- **timers**: tackle shop restock, cosmetic store reset, today's votes
- **wrong gear warning**: when your pet or armor doesn't fit the place you're at, and which one does
- **server hud**: sidebar-style lines, compendium, frenzy schedule, community goal
- **equipment row** above the hotbar, and a **hotbar** with its own look, size and spacing

## fishing and catches

- **rare drops**: a title, totem pop and sound for your own pet drops and rare catches. pet titles end with the pet's rating ("+ capybara pet 93.4%")
- **water rings** at your bobber while a fish swims in, a bigger one on the bite, and one in the rarity colour where you caught it
- **big catch moment**: an edge glow, a light sweep and a sound for fabled, trophy, mythical and gigantic fish
- **dry streaks**: a chat line saying how many catches a pet, shard, capsule or gigantic took
- **rod not cast** alarm, **chummer range** ring, and **bite focus** (hyprland) to bring the game to the front on a bite

## chat triggers

`/tacklebox triggers` shows your own title, pops a totem and plays a sound when a chat line matches.

- start blank or from a fishonmc preset (quest completed, pet max level, fabled, storm, moon, new in compendium and more)
- `*` matches anything and `{name}` grabs a part to show in the title, like `quests > quest #{n} has been completed!`. paste a real line into **test with** to check it
- titles take colours (`&a`, `&l`, `&#ff8800`), animations, and any sound id
- **share** copies a trigger as a code others can **import**

## chat

`/tacklebox chat`.

- **hide rules**: lines that match never show up. shift + right click a line to make one
- **chat boxes**: dms and crew chat (or any rule) in their own movable box that fades out
- **right click** a line to copy it, **ctrl + f** to search chat
- **stacking**: repeated lines become (xN), even with other lines in between
- **timestamps** on hover, **highlights** for your name and chosen words
- chat comes back after a reconnect or restart, keeps 2000 lines, and the up arrow remembers plain messages too
- type `here` and press tab for your place and instance
- a key to peek at chat without opening it, smooth scrolling and a scroll bar

## keys

- **key commands**: a key or mouse button sends a command
- **fish key**: one key for fishing as a rebind. a press is one right click with the rod
- **bait sort key**: sorts your bait and lures common to legendary
- **command aliases**, like `/sb 5` for `/sellall bait 5`

## world and bobber

these only change what you see.

- **still bobber**, **smooth cast** on a laggy connection
- **bobber skins** (ten), on your bobber or everyone's, and your own **line colour**
- hide **other bobbers**, **other players near you** while fishing, **other players' fish**, **nametags**, **xp popups**, the **block outline** and **far server displays** (for fps)
- **particle switches**: turn off any particle type, like rain splashes
- **flat leaves**, **black sky**, **sneak camera height**
- **word swaps**: replace any phrase wherever the game draws text
- **screenshot preview**: f2 copies to the clipboard and slides in at the top right
- no toast popups, and the pause menu asks before you disconnect

## viewmodel

`/tacklebox viewmodel` changes how your held item looks in first person: size, position, tilt, turn, roll, no switch dip, and swing style, turn and speed. it only changes how it looks.

## foecollab tweaks

only when foecollab is installed:

- lure matching in its bait sorting helper, and its bait sorting key works as a toggle
- `{instance}` in its custom inventory buttons
- its pet hud remembers your pet when its scan misses it
- its own dry streak lines are hidden (tacklebox has its own)
- its hud mover boxes as thin outlines

## files

everything lives in `config/tacklebox/`, with a daily backup in `config/tacklebox/backups/` (last 7 kept).

## building

```bash
./gradlew build
```

java 21, gradle wrapper included. the jar ends up in `build/libs/`.
