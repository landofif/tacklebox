# Feature list

One line per tacklebox feature: what it does, the setting, and its topic doc. **(no doc)** means no topic doc covers it yet. Built 2026-10-29 from the docs and `src/`; keep it in step when a feature is added or removed.

## Screens and look

- Settings screen: pages, search, alt+number page switch, hover help. `/tacklebox` (alias `/dofi`), right shift, Mod Menu. `ui/look-and-settings.md`
- Accent gradient: two colours for focused tiles, hovered buttons, toggles, hotbar and menu fills. `accent1`, `accent2`. `ui/look-and-settings.md`
- Flat buttons and rounded text fields everywhere. `flatButtons`, off. `ui/look-and-settings.md`
- No toast popups: every toast (recipes, advancements, hints) is blocked. Always on. **(no doc)**, `mixin/ToastManagerMixin`
- Old foecollab title logo, from the built-in pack `tacklebox:foelogo`. Always on. **(no doc)**, one line in `dev/rename-tacklebox.md`

## Menus

- Simple menus: chests and inventory as bare items, optional background colour and picture. `flatMenus`, off. `ui/look-and-settings.md`
- Fishonmc menu as a big-icon tile screen (/menu, /stats, /crew, quests). `fishMenu`, off. `menus/fish-menu-and-presets.md`
- Travel screen: location tiles with level rings, best place to fish, frenzy tile green, fabled tile red. `fishMenu`, off. `menus/fish-menu-and-presets.md`
- /travel chest marks: a place in a frenzy gets a green slot, the fabled event's place a red one. Always on. `menus/fish-menu-and-presets.md`
- Presets wardrobe: armour sets on player models, rod parts as big items, click to equip. `presetsMenu`, off. `menus/fish-menu-and-presets.md`
- Vault overview: all seven /pv vaults on one screen with search. `vaultMenu`, off. `menus/vaults.md`
- Bait picker: fastest quest and best bait for a location. `/tacklebox bait`. `menus/bait-picker.md`
- Power screen: power by source, cheapest next level, location table. `/tacklebox power`. `menus/power-and-compendium.md`

## Items and tooltips

- Short fish and pet tooltips. `shortFishTooltips`, `shortPetTooltips`, off. `display/item-tooltips.md`
- Pet profit marks in the auction house (common and rare). `petProfitMarks`, off. `display/pet-profit.md`
- Rarity circles behind items in slots and the hotbar. `rarityCircles`, off. **(no doc)**, `mixin/ItemMarkerHandlerMixin`

## Bobber and world

- Still bobber. `stillBobber`, off. `display/world-and-totem.md`
- Smooth cast on a laggy connection. `smoothCast`, off. `display/world-and-totem.md`
- Hide other players' bobbers, lines and foecollab's bait under them. `hideOtherBobbers`, off. `display/world-and-totem.md`
- Bobber skins (ten), optionally on everyone's bobbers. `bobberSkin`, `skinEveryone`. **(no doc)**, `display/BobberLooks`
- Own fishing line colour. `customLineColor`, `lineColor`, off. **(no doc)**, `display/BobberLooks`
- Hide your own or every nametag. `hideOwnNametag`, `hideAllNametags`, off. `display/world-and-totem.md`
- Hide xp popups (action bar line and holograms). `hideXpPopups`, off. `display/world-and-totem.md`
- Flat leaves. `flatLeaves`, off. `display/world-and-totem.md`
- Black sky and fog. `blackSky`, off. `display/world-and-totem.md`
- Hide far server displays for fps. `hideFarDisplays`, `farDisplayRange`, off. `display/world-and-totem.md`
- Chummer range ring. `chummerRing`, `chummerRingColor`, off. `huds/chummer-range.md`
- Screenshot preview: F2 copies to the clipboard and slides in at the top right. `screenshotPreview`, off. `display/screenshots.md`
- Word swaps: replace any phrase wherever the game draws text. `/tacklebox words` (list empty by default). **(no doc)**, `display/WordSwap`
- Held item viewmodel: size, position, rotation, no equip dip. `/tacklebox viewmodel`, off. **(no doc)**, `screen/ViewmodelScreen`
- Swing style and speed. `swingStyle`, `swingSlowness`, `swingTurn`. **(no doc)**, `mixin/HeldItemRendererMixin`
- Sneak camera height. `sneakHeight` (1.5 = vanilla). `display/world-and-totem.md`

## Triggers

- Chat triggers: a matching chat line shows an animated title and plays sounds, can hide the line. `/triggers`. `display/triggers.md`
- Trigger presets (quest completed, pet drop, rare catch, fabled, storm and more). `+ new trigger`. `display/triggers.md`
- Totem pop: a trigger can pop an item (pet head, shard head, the real item). Trigger totem tab. `display/world-and-totem.md`
- Trigger share codes: copy and import a trigger as a short code. **(no doc)**, `trigger/TriggerStore`
- New in compendium alert, from foecollab's catch hook. Trigger preset. `menus/power-and-compendium.md`

## Huds

- Quests left until the next fishing and location level. `questsHud`, off. `huds/quests-left.md`
- Level progress bars. `levelBars`, off. `huds/level-bars.md`
- Active boosts: boosters, chummers, weather, moon, frenzy, fabled. `boostsHud`, off. `huds/boosts-and-now-playing.md`
- Event hud: points, placement, missions. `eventHud`, off. `huds/event-hud.md`
- Now playing (playerctl). `nowPlaying`, off. `huds/boosts-and-now-playing.md`
- Server hud: sidebar-style lines with picture; includes compendium lines, frenzy schedule and community goal lines. `serverHud`, off. `huds/server-hud.md`
- Sends /communityevent once per game start, 5 s after joining. Always on. `huds/server-hud.md`
- Bobber timer, on the hud or over the bobber. `/tacklebox bobber`, on. `huds/bobber-timer.md`
- Bite countdown style for the timer. `timerStyle`. `huds/bobber-timer.md`
- Bite notification replacing the server's BITE! title. `biteOn`, on. `huds/bobber-timer.md`
- Focus the game window on a bite (Hyprland). `biteFocus`, off. `huds/bobber-timer.md`
- Hotbar look, spacing, size, offhand box, gliding selection. Settings > huds > hotbar. `ui/look-and-settings.md`
- Own armour and rod parts row above the hotbar. `equipHud`, off. `ui/look-and-settings.md`
- Huds, hotbar, chat and chat boxes in foecollab's hud mover. `/tacklebox move`. `foecollab/integration.md`, `huds/positions.md`
- Hide hud parts behind chat, chat boxes and the player list. `hideBehindChat`, off. `chat/chat.md`

## Chat

- Hide rules (contains, whole line, regex); shift+right-click a line to start one. `/tacklebox chat`. `chat/chat.md`
- Hide login welcome lines. `hideLoginChat`, off. **(no doc)**, `chat/ChatTweaks`
- Hide empty chat lines. `hideEmptyChat`, off. **(no doc)**, named in `chat/index.md` only
- Chat boxes: DMs and crew chat (or any rule) in their own movable, fading box. `chatBoxes`, on. `chat/chat.md`
- Right-click copy with hover highlight. `chatCopy`, on. `chat/chat.md`
- Stack a repeat of the newest line as (xN). `chatStack`, on. `chat/chat.md`
- Timestamps on hover, optional prefix. `chatTimeMode`. `chat/chat.md`
- Ctrl+F chat search, click a result to jump there. Always on. `chat/chat.md`
- Smooth chat scrolling and a draggable scroll bar. Always on. `chat/chat.md`
- Hold a key to peek at chat like it is open, wheel scrolls it. Key, unbound. `chat/chat.md`
- One Escape closes chat even with command suggestions showing. Always on. `chat/chat.md`
- Type "here" and press Tab to put in your location and instance. Always on. `chat/chat.md`
- Up arrow history keeps plain messages too, after a restart or reconnect. Always on. `chat/chat.md`
- Your name and chosen words in a colour inside chat lines. `chatHighlightName`, on; `chatHighlights`. `chat/chat.md`
- 2000 lines of chat history instead of 100. Always on. **(no doc)**, `mixin/ChatHudMixin`
- No `!` before player levels. Always on. `chat/chat.md`
- Move and resize vanilla chat in the hud mover. `huds/positions.md`

## Keys and commands

- Key commands: a key or mouse button sends a command once per press. `/tacklebox keys`. **(no doc)**, `input/KeyCommands`, one line in `rules/text-and-automation.md`
- Command aliases, like `/sb 5` for `/sellall bait 5`. `aliases`. `chat/command-aliases.md`

## Foecollab tweaks

- Lure matching in foecollab's bait sorting helper. Always on. `foecollab/tweaks.md`
- Bait sorting key works as a toggle. Always on. `foecollab/tweaks.md`
- `{instance}` in foecollab's custom inventory buttons. Always on. `foecollab/tweaks.md`
- Pet hud remembers the equipped pet when foecollab's scan misses it. Always on. `foecollab/tweaks.md`
- Daily missions keep updating while tacklebox's menu screen is open. `foecollab/tweaks.md`
- Hud mover boxes as thin outlines, timer `s` in small caps. Always on. **(no doc)**, `mixin/MovableBoxWidgetMixin`

## Dev

- F8 gui dump to a file and the clipboard. `dev/build-and-debug.md`
- `/tacklebox debug item | entities | count | displays`. `dev/build-and-debug.md`
