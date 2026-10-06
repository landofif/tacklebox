# FishOnMC server data — research for a stats + cosmetics mod

Compiled 2026-10-02 from the two FishOnMC repos in this project:

- **landofif/FOECollab** (`master` @ 47af3e6) — "FishonMC Extras Collab", a mature Fabric QoL mod (v2.6.2). Most server knowledge lives here. Paths below are relative to `src/client/java/io/github/foecollab/`.
- **landofif/fishonmc-autofish** (@ 6a72173) — used **only as a data source** (resource-pack font map, menu layouts, chat lines). None of its automation (`auto/`, `control/`, `MenuRobot`, `SteerController`) is carried forward.

Other repos visible to this account (`mccisland-autofish`, `hypixel-autofish`) are not FishOnMC and were skipped. FOECollab credits two upstreams worth knowing about but not cloned here: `DannyPX/FishOnMC-Extras-R` (GPL-3.0) and the public `FOECollab/FOE-Collab`.

> **Scope guard for the new mod:** read-only observation of chat, titles, scoreboard, tab, boss bar, item NBT and container screens; render-only cosmetics. No input simulation, no auto-clicking menus, no auto-sent commands (FOECollab itself auto-sends `/contest` and `/pay` in places — see §12 — don't copy those).

> **Style requirement (from l, 2026-10-02):** all mod-rendered text uses a monospaced, typewriter-style font. Ship it as a font provider under `assets/<modid>/font/` (a TTF with a permissive license, e.g. Courier Prime (OFL) or Special Elite (Apache 2.0)) and apply it with `Style.withFont(...)` on every mod-drawn `Text` (HUDs, screens, title popups, mod chat lines). Leave server glyph text (rarity/rank tags, `U+E000`+ icons) in its original font so the private-use glyphs still render.

---

## 1. Platform / toolchain

| | FOECollab | autofish |
|---|---|---|
| Minecraft | 1.21.11 | 1.21.11 |
| Loader | Fabric 0.19.2 | Fabric 0.19.5 |
| Fabric API | 0.141.3+1.21.11 | 0.141.6+1.21.11 |
| Mappings | Yarn 1.21.11+build.3 | Yarn 1.21.11+build.6 |
| Loom | 1.14.7 | 1.17.20 (needs Gradle 9.5+) |
| Java | 21 | 21 |
| Config UI | Cloth Config 21.11.153 + ModMenu 17.0.0 | ModMenu optional |
| Extra | DiscordIPC (rich presence), ProxLib 0.2.1 (vendored, relaxed MC pin) | — |
| License | AGPL-3.0-only (+ GPL-3.0 parts from FOE-R) | MIT |

- Server address check: `getCurrentServerEntry().address` contains `fishonmc.net` (`handler/LoadingHandler.java`). Hostname is `play.fishonmc.net`.
- "Loading done" heuristic: hotbar slot 0 holds a `FISHING_ROD` with `CUSTOM_DATA` (the server rod). Gate all parsing on this.
- `HudRenderCallback` is deprecated in 0.141.6; use `HudElementRegistry` (FOECollab attaches before `VanillaHudElements.PLAYER_LIST`).
- **Licensing:** copying FOECollab code makes the new mod AGPL-3.0. Facts (strings, glyph codepoints, NBT keys) are not copyrightable, so re-implementing from this document keeps the license choice open.

## 2. Server resource pack & private-use glyphs

> Glyphs are written as `U+XXXX` codepoints (the raw characters were lost when this doc was pasted over from the claude.ai project; values re-checked against `~/IdeaProjects/FOECollab` @ 47af3e6). Spaces between codepoints in a sequence are for readability only.

The server pushes a resource pack; most UI is bitmap-font glyphs in the Unicode private-use area. A mod must match on these codepoints, not on visible text.

- Vanilla caches pushed packs at `<game>/downloads/<uuid>/<sha1>` with journal `downloads/log.json`; FOECollab mirrors them into `resourcepacks/foecollab_*.zip` (`handler/ServerPackHandler.java`). Easiest way to get the full pack for analysis.
- Small-caps Unicode (e.g. `ᴄᴀᴛᴄʜᴇꜱ`, `ʟᴜᴄᴋ`) is used heavily in scoreboard, lore and menus. Note mixed `ꜱ`/`s` variants: match the exact string from the code.

- Pack `731da466fdc8be483990b59b11e0ca5c6533dc06` ("© FishOnMC Shader Pack - Handcrafted By Zebra", formats 75–85) has shaders only, with no fonts or textures. It overrides `rendertype_text.{vsh,fsh}`, `rendertype_text_intensity.vsh`, `rendertype_text_see_through.*`, `position_tex.*`, `entity.*`, `stars.*` and the armor cutout vsh.
  - Text effects (`include/text_effects_config.glsl`) are keyed on vertex colour / 4: `(240,240,0..64)` in steps of 4 (shake, wave, outline, gradient, rainbow, shimmer, …), `(255,255,248)` and `(94,171,136)`. They only run in `rendertype_text`, so text drawn in a TTF font (which goes through `rendertype_text_intensity`) loses them.
  - NoShadow (GUI only): text colour `#4E5C24` is drawn in the lightmap colour, and its shadow `#131709` is discarded.
  - The menu and glyph art (fonts, textures) must be in a different pack the server sends.

### Tag glyphs (`FOMC/Constant.java`)

| Glyph | Meaning | Colour |
|---|---|---|
| `U+F033` | Common | FFFFFF |
| `U+F034` | Rare | 2B85C4 |
| `U+F035` | Epic | 1CD832 |
| `U+F036` | Legendary | D98103 |
| `U+F037` | Mythical | C93832 |
| `U+F092` | Special (event rarity) | DD7ACF |
| `U+F041` | Albino | C4C19F |
| `U+F042` | Melanistic | 1C1C1C |
| `U+F043` | Trophy | D5BF3B |
| `U+F044` | Fabled | 82171E |
| `U+F098` | Alternate (event variant) | 9CB4FC |
| `U+F102` | Spooky (event variant) | 2E2E8F |
| `U+F179` | Frozen (event variant) | 7FB0E7 |
| `U+F032` / `U+F031` / `U+F030` / `U+F029` / `U+F028` | Ranks: Angler / Sailor / Mariner / Captain / Admiral | 20BBD7 / 96F564 / 66F8AE / FCA307 / AE5AF6 |
| `U+F024` Staff, `U+F026` Designer, `U+F027` Builder, `U+F023` Manager, `U+F022` Admin, `U+F021` Owner, `U+F088` Community Manager | Staff ranks | |
| `U+F039` | Marker in the time/weather boss bar | |
| `U+F00C` | Credits icon in scoreboard | |
| `U+F060` / `U+F061` / `U+F062` | Contest 1st / 2nd / 3rd place | |
| `U+E000`–`U+E999` | A single glyph here as the **title** = the caught fish's icon | |
| `U+EEE1`–`U+EEE8` (seen) | Spacing / negative-space helpers used in titles and menu names | |

Lore "rarity window" prefixes (`TEXTCOMMON` etc.): `U+EEE4 U+EEE1 퀃 U+EEE8 U+EEE7 U+EEE5 U+EEE2 ` with `퀃/퀇/퀑/퀕/퀙` for Common→Mythical.

Size words are plain small caps with colour: `ʙᴀʙʏ` 468CE7, `ᴊᴜᴠᴇɴɪʟᴇ` 22EA08, `ᴀᴅᴜʟᴛ` 1C7DA0, `ʟᴀʀɢᴇ` FF9000, `ɢɪɢᴀɴᴛɪᴄ` AF3333.

### Fishing minigame font (autofish `reference/fishing_font_map.json`)

Useful only for *display* (e.g. knowing a minigame is on screen, or which themed backdrop is shown); never for steering.

- Fonts `fishing_1/2/3` (ascent 140/155/170), drawn in the **action bar**.
- Progress glyphs `p00..p100` in `U+EC00–U+ECC0` (lookup table in `decode/Glyphs.java`).
- Named glyphs: `minigame_box U+FAA0`, `newbar U+EDD0`, `successful U+EFF4`, `failure U+EFF1`, `negative_space U+E994`.
- Themed backdrops (event/location skins): `cobweb U+ED00`, `fallleaves U+ED03`, `frozen_preset U+ED06`, `christmas_preset U+ED09`, `spring U+ED12`, `shark U+ED13`, `volcano U+ED14`, `koipond U+ED15`, `bigfoot U+ED16`, `santa U+ED17`, `lofoten_shark U+ED18`, `sharkweek2026 U+ED20`.
- Seeing `successful`/`failure` would give a clean catch-success vs. escape counter (not yet observed in a trace; may arrive as a title).

## 3. Detecting a catch (core of session stats)

Flow in `handler/FishCatchHandler.java` + `mixin/InGameHudMixin.java`:

1. `InGameHud.setTitle` → title is exactly one char in `0xE000–0xE999` ⇒ a fish was caught (that char is the fish's icon).
2. `InGameHud.setSubtitle` → contains a rarity glyph (`U+F033`–`U+F037` or `U+F092`); subtitle also contains the fish's display name.
3. Within 2 s, scan inventory for a fish stack whose NBT `catcher` UUID = you, whose `id` UUID wasn't in the inventory snapshot taken before, and whose display name appears in the subtitle. If the inventory is full, scan dropped `ItemEntity`s instead.
4. Parse NBT (§5) → increment stats.

Snapshot the list of fish `id`s on join and after every catch so already-held fish aren't counted again.

Other catch-related titles: `"BITE!"` title fires on a bite and is re-sent while hooked. Bite is also visible as `FishingBobberEntity` tracked data `CAUGHT_FISH` (server-synced). Fine for a display-only bite indicator; don't act on it.

## 4. Chat messages (match on `Text.getString()`)

Hook via Fabric `ClientReceiveMessageEvents.ALLOW_GAME` (observe/suppress) and `MODIFY_GAME` (rewrite). Prefixes use `»`.

**Rare drops**
- `PET DROP! You pulled ...`: pet drop.
- `RARE CATCH! You pulled a <N>x ... Shard ...`: shards (count between `a ` and `x `; >1 with Prospecting Amulet).
- `RARE CATCH! You pulled ... Lightning in a Bottle ...`: only during Thunderstorm.
- `RARE CATCH! You pulled ... Infusion Capsule ...`: only during Blood Moon.
- Other rare catch names known from constants: Prospecting Amulet, Bigfoot Fur, Bigfoot Tooth (Witching Hour, Cypress Lake, 12–3 AM in-game, "1 in 300 chance for bigfoot to steal your fish (and leave a gift)").

**Quests**
- `QUEST Complete [...` and then the next ~5 lines are rewards: `+ <N>x ... Shard`, `+ <rarityGlyph> ... Pet`.
- `QUESTS » Quest #6 has been completed! Type /quests to claim it.`
- `QUEST » Started a new Hard fishing quest to catch 29x Epic at Cypress Lake`
- `DAILY MISSIONS » Contest Participation ...`

**Contest** (`handler/ContestHandler.java`), sent as a block in response to `/contest`:
- Header `FISHING CONTEST RANKINGS (<time>)` such as `(10m)`/`(30s)`, or `(ENDED)`.
- `Type: Heaviest <Fish group>`, `Location: <Location name>`, `Level: <lo>-<hi>`
- `U+F060 <name> → <stat>` / `U+F061 ...` / `U+F062 ...` (top 3)
- `You → #<n> (<weight>lb) ... (out of <N>)` or `You → Unranked`

**Weather / world events** (`handler/EventHandler.java`, exact strings)
- `RAINBOW » A Rainbow has appeared!` (+500 Luck)
- `STORM » A thunderstorm has formed!` (+100 Bite Speed)
- `STORM » A supercell has formed!` (+150 Bite Speed)
- `GOLD RUSH » A Gold Rush has begun!` (base shard chance 1/25)
- `RAIN » A rain shower has begun!` (+50 Bite Speed)
- `BLOOMING OASIS » A Blooming Oasis has appeared!` (+100% Pet Luck)
- `FULL MOON » A Full Moon has risen!` (+120 Bite Speed)
- `BLUE MOON » A Blue Moon has risen.` (+120 Bite Speed, 5x XP)
- `SUPER MOON » A Super Moon has risen.` (+120 Bite Speed, +150 Reel Speed)
- `BLOOD MOON » A Blood Moon has risen!` (+120 Bite Speed)
- Moon set: contains `» The ` … ` has set.`
- Fabled: `FABLED » A Fabled Event is active at <Location>` or `Visit the <Location> to ...`; ends with `Caught by <player>`.

**Player chat, dms, crew chat** (from l's logs, 2026-09)
- Player chat is a server line: `<glyphs>! [<level>] <rank glyph> <tag glyph> <name> » <message>`; after `Matcher.normalize` it reads `! [113] Jukoneko > is crc easy...`.
- Direct messages: `✉ From <name> » <message>` (incoming, seen in logs); outgoing is presumably `✉ To <name> » <message>`.
- Crew chat: `[Crew Chat] <name> » <message>` (l's memory, not in the logs yet). Toggled with `/crew chat`: `CREWS » Crew Chat has been enabled/disabled`.
- Others' catches broadcast: `WR! <name> caught a record <fish> <lb> (+<lb>) <in> (+<in>)`, `TROPHY! <name> caught a <glyph> <fish>`. Yours: `RARE CATCH! You pulled a 1x Subtropical Shard`.
- More prefixes seen: `REACTIONS`, `BOOSTER`, `VOTE`, `TIP` (`You tipped <name>! + 500xp | $500`), `LOCATION` (`Lake Biwa gained 4.17K xp`), `TP`, `PURCHASE!`, `CHUMMER` (`<name> activated a <glyph> Chummer. (+75 Bite Speed)`), `CONTESTS` (`0.2lb → 0.43lb (+0.24lb)`), `NOTIFICATION`, `TRAILS`, `HATS`, `ROD PARTS`, `ARTISAN`, `MANUFACTURER`, `PAY`, `TRADE`, `TPA`, `AUCTION HOUSE`, `<LOCATION> MINI EVENT` (message starts with a newline), and FOECollab's own `FOE » You went <n> fish dry before catching a ...`.
- Gameplay logs to grep for more: `/home/luna/.local/share/PrismLauncher/instances/fishonmc-dev/minecraft/logs/` (l offered them, 2026-10-03).

**Other**
- `SELL » You just earned $38.48K from selling your fish!` (money earned per sell; good stat)
- `TACKLE SHOP » ... Tackle items restocked!`
- `PETS » Equipped your <pet>.` / `PETS » Pet unequipped!`
- `CREWS » You left the crew`, `CREWS » You have joined ...`, `CREWS » Crew Chat has been enabled/disabled`
- `<name> activated|has placed a <rarity> chummer` (regex `(\w{1,16}) (?:activated|has placed) a(.*?)chummer`)
- `REACTIONS » <player> ...` (chat reaction games)
- `LOCATION` / `LOCATIONS` prefixed lines (location lists)
- Spam prefixes: `DESIGN TEAM »`, `HELP »`, `WIKI »`, `RULES »`, `BUILD TEAM »`, `COSMETICS »`, `STORE »`, `DISCORD »`
- Staff: `You are now invisible!` / `You are no longer invisible!`

## 5. Item NBT (`DataComponentTypes.CUSTOM_DATA`)

Skip any stack with `shopitem: true` (shop previews). Most items carry `type`; fish don't.

**Fish** (no `type`; item is one of `COD`, `WHITE_DYE`, `BLACK_DYE`, `GOLD_INGOT`, `PRISMARINE_SHARD`, `DRIED_KELP`, `BONE`, `LIGHT_BLUE_DYE`, `ROTTEN_FLESH`, with `LORE`; the item kind likely encodes the variant)

| key | type | notes |
|---|---|---|
| `id` | int[4] UUID | unique per fish |
| `fish` | string | species id |
| `scientific` | string | Latin name |
| `rarity` | string | common/rare/epic/legendary/mythical/special |
| `size` | string | baby/juvenile/adult/large/gigantic |
| `variant` | string | normal/albino/melanistic/trophy/fabled/alternate/spooky/frozen |
| `weight` | float | lb |
| `length` | float | in |
| `value` | float | $ |
| `xp` | float | |
| `location` | string | location id (§7) |
| `group` | string | fish group (contest "Heaviest <group>") |
| `nature`, `lifestyle`, `native`, `migration`, `sex` | string | flavour/biology |
| `catcher` | int[4] UUID | who caught it |
| `date` | string | `MM/dd/yyyy` |
| `rod` | string | rod used |

Catcher **name** is taken from lore line 15's last word (fragile; prefer UUID).

**Other `type` values:** `pet`, `armor`, `armorShard`, `bait`, `lure`, `line`, `pole`, `reel`, `craftingComponent`, `package` (bait package), `chummer`, `petItem`. Fishing rod is a vanilla `FISHING_ROD` with custom data.

- **Pet:** `id`, `pet` (bullfrog, bear, fox, kangaroo, marshrabbit, seaturtle, duck, eagle, wolf, pelican, capybara, lynx, shark, dolphin, sheep, koala), `rarity`, `level`, `xp_cur`, `xp_need`, `climate`, `location`, `cbase`/`lbase` lists of `{cur, cur_max, percent_max}` (index 0 = luck, 1 = scale), `username`+`uuid` (discoverer), `date`, `item` (pet item list), `petItem`, **`skin`**, **`trail`** (server cosmetics present on pets).
- **Fishing rod:** `uuid` (owner), **`skin`**, `soulbound_rod`, `disableBait`, `tacklebox` (list of serialized stacks), `activeBait` (equipped bait/lure), `line`/`pole`/`reel` (lists with one serialized stack). Rod name = display name.
- **Armor:** `id`, `name`, `piece`, `rarity`, `tier`, `quality`, `rolls`, `identified`, `rolled`, `unlocked`, `rgb`, `uuid` (crafter), stat entries `{cur,max}`.
- **Bait / Lure / Line / Pole / Reel:** `id`, `name`, `rarity`, `water`, `base` (stat list), `cur`/`counter`/`totalUses`, `intricacy`; lures add `color`, `size`; poles/reels add `calibration`.
- **Chummer:** `rarity`, `bitespeed`, `timer`. **Bait package:** `id`, `intricacy`, `location`, `rarity`.

Tooltip stat names (small caps): `ʟᴜᴄᴋ`, `sᴄᴀʟᴇ`, `ᴘʀᴏsᴘᴇᴄᴛ`, `ʀᴇᴇʟ sᴘᴇᴇᴅ`, `ʟɪɴᴇ sᴛʀᴇɴɢᴛʜ`, `ʙɪᴛᴇ sᴘᴇᴇᴅ`, `ᴀʟʙɪɴᴏ ᴄʜᴀɴᴄᴇ`, `ᴍᴇʟᴀɴɪsᴛɪᴄ ᴄʜᴀɴᴄᴇ`, `ᴛʀᴏᴘʜʏ ᴄʜᴀɴᴄᴇ`.

## 6. Scoreboard, boss bar, tab list

**Sidebar** (`handler/ScoreboardHandler.java`): lines are team **prefixes** of the sidebar objective's teams (read at ~2 Hz).

| contains | value |
|---|---|
| `ᴡᴀʟʟᴇᴛ` | after `$` → balance |
| `ᴄʀᴇᴅɪᴛꜱ` | after `U+F00C` → credits |
| `ᴄᴀᴛᴄʜᴇꜱ` | after `: ` → server's catch count |
| `ᴄᴀᴛᴄʜ ʀᴀᴛᴇ` | after `: ` → catch rate |
| `ᴄʀᴇᴡ:` | `[name]` |
| `┠ ʟᴇᴠᴇʟ` | `[crew level]` |
| `ᴄʀᴇᴡ ɴᴇᴀʀʙʏ` | `✔` if crew nearby |
| `┠ ʟᴏᴄᴀᴛɪᴏɴ` | `min / max` (skip if `---`) |

Player level and progress = vanilla `experienceLevel` / `experienceProgress` (FishOnMC uses the XP bar for fishing level). Level colour bands are in `FOMC/LevelColors.java` (0–4 … 300+).

**Boss bar** (`handler/BossBarHandler.java`): the bar whose name contains `U+F039` holds weather icon, `hh:mm`, AM/PM, temperature (`… <n>°`) and the location name. Weather icons: `☂` rain, `☀` sun, `⚡` thunderstorm, `♣` blooming oasis, `⭐` fabled, `⚠` gold rush, `○` moon.

**Tab list** (`handler/TabHandler.java`): your entry's display name contains your rank glyph; footer contains `ɪɴꜱᴛᴀɴᴄᴇ <name> (` when you're in an instance.

**Dimension:** world key containing `crew` ⇒ Crew Island.

## 7. Locations

| id (NBT/bossbar) | Name | Climate | Water | Colour |
|---|---|---|---|---|
| spawn / spawnhub | Cypress Lake | subtropical | fresh | 5CAE65 |
| kenai | Kenai River | subarctic | fresh | 68D499 |
| biwa | Lake Biwa | subtropical | fresh | FBC0FA |
| murray | Murray River | semi-arid | fresh | CD5916 |
| everglades | Everglades | savanna | fresh | 2EBB8D |
| keywest | Key West | savanna | salt | FBF17C |
| toledobend | Toledo Bend Reservoir | subtropical | fresh | 99A7D0 |
| greatlakes | Great Lakes | continental | fresh | 3CABF3 |
| danube | Danube River | continental | fresh | FBC598 |
| oilrig | Oil Rig | savanna | salt | FCEB47 |
| amazon | Amazon River | rainforest | fresh | 3EA729 |
| mediterranean | Mediterranean Sea | mediterranean | salt | F0FB37 |
| capecod | Cape Cod | oceanic | salt | BBF5FB |
| hawaii | Hawaii | savanna | salt | gradient |
| lofotenislands | Lofoten Islands | subarctic | salt | gradient |
| cairns | Cairns | monsoon | salt | A1C2FB |
| crewisland | Crew Island | — | — | |

Spawn hub bounding box: x 95..145, z −58..38 (treated as Cypress Lake).

## 8. Menus (screen titles are glyph strings)

| Screen | Title string |
|---|---|
| **Stats** (`/stats`) | `U+EEE4 픲` |
| Main menu | `U+EEE4 할` (contains `END_CRYSTAL` named "Daily Missions"; lore lines `| <goal> (<p>/<n>)`) |
| Quests (`/quests`, 45 slots) | `U+EEE4 픹` |
| Crew / Leveling bonuses | `U+EEE4 핒` |
| Pet menu | `Pet Menu` + `U+EEE6 U+EEE5 U+EEE3 핑` |
| Tackle shop | `Tackle Shop` + `U+EEE7 U+EEE3 합` |
| Armor menu | `Armor Menu` + `U+EEE7 U+EEE2 핈` |
| Fish Merchant (`/sell`, 54 slots) | contains "Fish Merchant" |
| Personal vault | `Personal Vault <n>` |

**Stats menu = authoritative all-time numbers.** Your page has a `PLAYER_HEAD` whose profile UUID is you; a `KNOWLEDGE_BOOK` whose lore lines hold `<rarity/size/variant tag> <count>` and `ꜰɪꜱʜ ᴄᴀᴜɢʜᴛ: <n>` (numbers use `,` separators and `K` suffix). FOECollab's "Import Stats" button seeds all-time stats from this (`handler/StatsImportHandler.java`). The new mod can read this passively whenever the player opens `/stats`, with no automation needed.

**Quest menu:** 8 shulkers; white = not started, red/orange/yellow = in progress (lore `Catch <n>x <goal> fish`, `ɪɴ ᴘʀᴏɢʀᴇss (<p>/<n>)`), green = complete (moves around; find by item). Difficulty menu: slots 11/13/15 yellow/orange/red shulker = Easy/Medium/Hard. Tier menu: 11 blue terracotta Adult, 12 orange terracotta Large, 13 light-blue concrete Rare, 14 lime concrete Epic, 15 orange concrete Legendary.

### Fishing menu (wiki.fishonmc.net/doku.php?id=fishing_menu, checked 2026-10-06)

Opened with the book in the 9th hotbar slot or `/menu`. Layout in the wiki screenshot: profile head on top, two rows of five icons, then help, close, settings and daily missions along the bottom. Buttons: daily cosmetic store, recipes, fish compendium (personal codex, fish data and **location leveling info**), quests (also `/quests`), turbo travel (needs captain rank, otherwise the location NPC), cosmetics, crews, shop (crew island blocks), crafting, player profile (stats, equipped items, collections, achievements), help (`/help`), settings (scoreboard and ambient sounds), daily missions.

Location wiki pages are `wiki.fishonmc.net/doku.php?id=location:<page>`; the example picture is `lib/exe/fetch.php?media=cinematic_<name>.png|jpg`. Pages and pictures: cypress_lake/cypresslake, kenai_river/kenairiver, lake_biwa/lakebiwa, murray_river/murrayriver, everglades/everglades, key_west/keywest, toledo_bend_reservoir/toledobend, great_lakes/greatlakes, danube_river/danuberiver, oil_rig/oilrig (jpg), amazon_river/amazon, mediterranean_sea/mediterranean, cape_cod/capecod, hawaii/hawaii, lofoten_islands/lofoten (jpg), cairns/cairns. Already bundled as circles in `textures/gui/location`.

Still unknown (get an F8 dump, see CLAUDE.md): the `/travel` tooltip format (level and progress bar), the titles of the travel, quest difficulty and tier menus, and where the location level lives in the compendium.

## 9. Stats model already proven in FOECollab

`handler/ProfileDataHandler.java` persists per-UUID JSON at `config/foe/stats/<uuid>.json` (backups in `backups/`). Fields worth reusing as a schema:

- Session + all-time: fish caught, total XP, total value, counts by rarity / size / variant, quests completed, pets, shards, lightning bottles, infusion capsules, pets/shards from quests.
- Active-time timer: accumulates only while catching; auto-pauses after N seconds without a catch, giving fish/hour.
- **Dry streaks:** stores `allFishCaughtCount` at the last catch of each rarity/size/variant/pet/shard; dry = total − last − 1. Lightning bottle only counts during thunderstorms, infusion capsule only during Blood Moon, Fabled only while a Fabled event is active at your location.
- Contest PB: heaviest fish of the contest group at the contest location.

Additions that the data supports: money from `SELL »`, per-location and per-species counts (`fish`, `location`), heaviest/longest per species (personal records), rod used, weather/moon at catch time, catch rate from scoreboard.

## 10. "Achievements" — what can be detected

No explicit server achievement messages or menu were found in either repo. Milestones the mod can detect reliably and turn into cosmetic unlocks:

- Rarity/size/variant firsts and counts (incl. event variants Spooky, Frozen, Alternate and event rarity Special)
- Rare drops: pet, shard (incl. multi-shard), Lightning in a Bottle, Infusion Capsule, Bigfoot items, Prospecting Amulet
- Fabled fish caught during a Fabled event
- Contest placements from the `You → #n` line and `(ENDED)` header
- Level thresholds (XP bar; the colour bands give natural tiers), rank glyph from tab
- Quests completed, daily missions progress
- Species/location collection completion (needs a species list; gather from caught NBT over time or the wiki)
- Pet rating (Sickly … Perfect, `FOMC/Constant.java`) and pet level

**Gap:** check in-game whether FishOnMC has an achievements/collections GUI or broadcast lines (e.g. via `ScreenInspector`-style dumps). If it exists, its title glyph and lore format should be captured next.

## 11. Cosmetics: hooks and constraints

- Everything a client mod renders is **client-side only**. Others see your cosmetic only if they also run the mod and the mod tells their client what you've unlocked.
- FOECollab already shares data between mod users through **ProxLib** (vendor id `219`, packet id `2` = contest PB string `contest_pb:<group>:<name>:<lb>:<level>`). The same channel could broadcast "player X has cosmetic Y"; a small web API is the alternative.
- Render hooks proven on 1.21.11: `ArmorFeatureRenderer.renderArmor` (cancel/replace armour layers), `Entity.isGlowing` (outline), `Entity.isCustomNameVisible`, `TextDisplayEntity` nameplates (server stamps FoE-relevant nameplates with a private-use marker; FOECollab re-tags them), tab name rewrite via `PlayerListHud`, chat rewrite in `MODIFY_GAME`, custom font providers under `assets/<mod>/font/*.json` (FOECollab ships `foecollab:tags` bitmap glyphs for re-styled rank/rarity tags; `tools/TagGlyphGen.java` generates them).
- Server already has cosmetics: rod `skin`, pet `skin` and `trail` NBT, and a `COSMETICS »` chat channel. Avoid visuals that could be mistaken for paid server cosmetics, and check FishOnMC's rules on client mods before shipping.
- Ideas the data supports: achievement badges next to names in chat/tab/nameplate, title-popup frames by rarity, rod/bobber particle trails, catch-celebration effects for Mythical/Fabled, profile card screen.

## 12. Things in the source repos to NOT carry over

- All of `fishonmc-autofish/auto/*`, `control/*` (minigame steering, `MenuRobot` /sell and /quests clicking).
- FOECollab `AutoTippingHandler` (auto `/pay` on REACTIONS), auto `/contest` refreshes on PB packets, `/find` sends. Show a button or hint instead and let the player type the command.
- FOECollab `QuestMenuSquareHandler` highlights are fine (visual), but don't click for the player.

## 13. Gaps to fill with an in-game session

1. Exact full text and JSON structure of catch title/subtitle and the `RARE CATCH!`/`PET DROP!` lines (only prefixes are known).
2. Whether the minigame `successful`/`failure` glyphs arrive via title or action bar (gives escape counting).
3. Achievement / collection menus, if any (§10).
4. Full species list per location (`fish` ids, `group` ids).
5. Current pack version: re-extract fonts from `downloads/` after joining, since codepoints can change when the server updates the pack.

---

## 14. Feature spec: chat triggers → custom titles & sounds (requested by l, 2026-10-02)

Goal: when a chat line matches a rule, show a custom on-screen title and/or play a sound, with deep customization. Display-only: a trigger never sends chat, clicks, or presses keys.

**Hook:** `ClientReceiveMessageEvents.ALLOW_GAME` (lets a rule hide the original line) + `GAME` for overlay/action-bar if wanted. Match against `Text.getString()` with server glyphs stripped optionally, so `QUESTS » Quest #6 has been completed!` matches plain text.

### Rule model (one JSON file, `config/<modid>/triggers.json`, editable in-game)

| Field | Options |
|---|---|
| `name`, `enabled` | |
| `match.mode` | `contains`, `startsWith`, `exact`, `regex` (named/numbered groups) |
| `match.pattern` | e.g. regex `^QUESTS » Quest #(?<n>[1-8]) has been completed!` |
| `match.ignoreCase`, `match.stripFormatting` | booleans |
| `conditions` | only at location(s), only during weather/moon/event, only when level ≥ N, only if a session counter ≥ N |
| `cooldownMs` | debounce repeats (server re-sends some lines) |
| `hideOriginal` | suppress the chat line |
| `title.text` / `subtitle.text` | supports placeholders `{n}`, `{1}`, `{player}`, `{location}`, `{session.fish}`, `{alltime.mythical}`, etc. and colour codes `&a` / `&#RRGGBB` / gradients |
| `title.style` | font (default: mod's typewriter mono), scale, bold/italic, shadow, colour or gradient, x/y anchor + offset |
| `title.timing` | fade-in, stay, fade-out (ms); animation: none / slide / pop / typewriter-reveal / shake |
| `title.queue` | replace current or queue behind it |
| `sound` | any vanilla sound id or a custom `.ogg` dropped into the mod's sound folder; volume, pitch, repeat count + interval |
| `extra` (optional) | toast, action-bar text, screen flash colour, chat echo with the mod's prefix |

Ship presets (all off by default, easy to clone): quest completed (`QUESTS » Quest #1-8 has been completed!`), `PET DROP!`, `RARE CATCH!` (shard / Lightning in a Bottle / Infusion Capsule), each weather/moon event, Fabled event start, contest ending, `SELL »` earnings.

### Editor screen
List of rules → editor with a **"test against text"** box (paste a chat line, see whether it matches and what the groups capture), live preview of the title, and a play-sound button. Import/export a rule as a share string (base64 JSON), like FOECollab's custom HUD/button sharing.

### Reuse from FOECollab (as reference, not copied: AGPL)
`handler/TitleHandler` + `screens/hud/TitleHud` (custom title renderer that blanks the vanilla title), `handler/NotificationSoundHandler` (note-block sound presets), `customhud/PlaceholderEngine` (placeholder/expression language).

## gui dumps, 2026-10-06 (main menu, /travel, fish compendium)

- every menu pads empty slots with `minecraft:bread` named "Bread" (the mod hides them). real buttons are gunpowder, books, player heads and so on.
- main menu: profile head slot 4, buttons in rows at slots 20-24 and 29-33, bottom row 46, 48-50, 52. lore lines all start with ` 퀃 ` (art glyphs).
- /travel (title `픁`): 16 location items (glowstone dust) in slots 1-7, 10-16, 19-21, lore has climate, temperature, `fastTravel:1b` custom_data. no level on them.
- fish compendium location page (title `픂`): same slots, lore `Level: [17] (354.17K/1.46M)`, `Catches: n`, `Runes Discovered: (a/b)`, then small-caps rarity lines `normal -> (14/14)`. progress can exceed the max (lake biwa `11.85M/2.32M`), so progress is clamped.
- profile head lore: `Fishing Level [274]`, `(1.02M/3.73M)`, power, stats, luck and so on.
