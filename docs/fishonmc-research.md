# FishOnMC server data — research for a stats + cosmetics mod

Compiled 2026-10-02 from the two FishOnMC repos in this project:

- **landofif/FOECollab** (`master` @ 47af3e6) — "FishonMC Extras Collab", a mature Fabric QoL mod (v2.6.2). Most server knowledge lives here. Paths below are relative to `src/client/java/io/github/foecollab/`.
- **landofif/fishonmc-autofish** (@ 6a72173) — used **only as a data source** (resource-pack font map, menu layouts, chat lines). None of its automation (`auto/`, `control/`, `MenuRobot`, `SteerController`) is carried forward.

Other repos visible to this account (`mccisland-autofish`, `hypixel-autofish`) are not FishOnMC and were skipped. FOECollab credits two upstreams worth knowing about but not cloned here: `DannyPX/FishOnMC-Extras-R` (GPL-3.0) and the public `FOECollab/FOE-Collab`.

> **Scope guard for the new mod:** read-only observation of chat, titles, scoreboard, tab, boss bar, item NBT and container screens; render-only cosmetics. No input simulation, no auto-clicking menus, no auto-sent commands (FOECollab itself auto-sends `/contest` and `/pay` in places — see §12 — don't copy those).

> **Style requirement (from luna, 2026-10-02):** all mod-rendered text uses a monospaced, typewriter-style font. Ship it as a font provider under `assets/<modid>/font/` (a TTF with a permissive license, e.g. Courier Prime (OFL) or Special Elite (Apache 2.0)) and apply it with `Style.withFont(...)` on every mod-drawn `Text` (HUDs, screens, title popups, mod chat lines). Leave server glyph text (rarity/rank tags, `U+E000`+ icons) in its original font so the private-use glyphs still render.

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

- Fonts `fishing_1/2/3` (ascent 140/155/170), drawn in the **action bar** (confirmed by luna 2026-10-17: Noxesium's `Action Bar Scale` slider resizes the minigame; Title Scale and Boss Bar Scale are not it).
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
- `RARE CATCH! You pulled a <N>x ... Shard ...`: shards (count between `a ` and `x `; >1 with Prospecting Amulet). One shard per climate (luna, 2026-10-06): arid, semi-arid, subtropical, oceanic, arctic, savanna, subarctic, continental, monsoon, rainforest, e.g. `Subarctic Shard`.
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
- Fabled (luna's logs 2026-10-10, the old `FABLED » ...` format was wrong): separate chat lines. Start: `FABLED FISH EVENT`, `Visit the Cypress Lake to`, `try and catch it!`. End: `FABLED FISH EVENT`, `Caught by <player>`, then the fish name. No length.
- Fish Frenzy (luna's logs 2026-10-10, Kenai River both times, random start, not every 3 hours: ended 18:17:54, next alert 19:43:05): start `FISH FRENZY ALERT`, `Fish at Kenai River for 1 hour of buffed rates`, two bonus lines (+100 Bite Speed +50 Prospect +25% Pet Luck +100% Money +250 Luck +250 Scale). Reminders `FISH FRENZY » A Fish Frenzy is active at Kenai River (for 16m 56s)` give the time left (also on joining). End `FRENZY » The Fish Frenzy has ended at Kenai River`.
- Fish Frenzy cycles (luna's sheet 2026-10-18, Middle European summer time): cycle starts 13, 16, 19, 22, 01, 04, 07, 10 o'clock; a frenzy can spawn until start +2h ("spawn window end"), lasts 1h from its spawn, the cycle ends at start +3h and the next window opens then. The 2026-10-10 logs fit it (ended 18:17:54, next alert 19:43:05). Sheet only, not seen to repeat in logs yet.

**Boosters and chummers** (from luna's logs, checked 2026-10-09)
- `/active` (seen 2026-10-29) answers with one plain line per running booster, no header: `Global <type> Booster » 2x activated by <name> with 45m 47s remaining. Use /tipall to gain an extra 500 XP and $500!` (types seen: Money, XP, Luck, Scale, Event). The reply with no booster running is not known yet.
- `BOOSTER » <name> has activated a Global <Luck|Scale|Money|XP> Booster » 2x for 30m! (/tipall)`; lengths seen: 30m, 60m. One player can activate several at once and several players can run the same type. Ends with `BOOSTER » Global <type> Booster from <name> has expired!`, exactly the announced length after the activation line (20:09:02 to 20:39:02). Global: nothing repeats them when you join.
- `CHUMMER » <name> activated a <rarity glyph> Chummer. (+75 Bite Speed)` (+75 common, +130 rare, +180 epic, +250 legendary, +375 mythical seen); no length in the line, the item says `Lasts for 15 minutes` (`timer:15.0f` for every rarity). Ends with `CHUMMER » Chummer has run out of stock.`, 15:14 to 15:23 after the activation line in the 2026-10-09 logs, 15:00 to 15:06 (mostly exactly 15:00) in the logs from 2026-10-10 on. Range: new chummers reach 20 blocks, about year-old ones 10 (only in the item lore, not in `custom_data`); see `huds/chummer-range.md`. Other lines: `CHUMMER > Place the Chummer on a block.` (using the item needs a block click) and `CHUMMER > There is already a Chummer activated nearby.` Activations often come seconds after a run out line, so they seem to queue.
- Event lengths measured from start to end lines (single digits of samples): Rain Shower ~9 min, Thunderstorm ~11 min, Gold Rush ~15 min, Blooming Oasis ~15.5 min. The end lines say it, so the mod shows no countdown for them.
- Moon lines carry the bonus: `BLUE MOON » A Blue Moon has risen. (5x Fishing XP)`, `SUPER MOON » ... (+150 Reel Speed)`, `FULL MOON » ... (+120 Bite Speed)`, `BLOOD MOON » ... (+120 Bite Speed)`; ends `The <X> Moon has set.`

**Player chat, dms, crew chat** (from luna's logs, 2026-09)
- Player chat is a server line: `<glyphs>! [<level>] <rank glyph> <tag glyph> <name> » <message>`; after `Matcher.normalize` it reads `! [113] Jukoneko > is crc easy...`.
- Direct messages: `✉ From <name> » <message>` (incoming, seen in logs); outgoing is presumably `✉ To <name> » <message>`.
- Crew chat: `[Crew Chat] <name> » <message>` (luna's memory, not in the logs yet). Other crews lines seen 2026-10-09: `CREWS » FoE |  <name> joined the server` / `left the server`, `CREWS » Mission completed! Catch 3x Gigantic → + 50Kxp`, `CREWS » Teleporting to your crew island...`. A server-wide broadcast `! [368]   <name> » CREWS  Mission completed! …` has a different shape. Toggled with `/crew chat`: `CREWS » Crew Chat has been enabled/disabled`.
- Others' catches broadcast: `WR! <name> caught a record <fish> <lb> (+<lb>) <in> (+<in>)`, `TROPHY! <name> caught a <glyph> <fish>`. Yours: `RARE CATCH! You pulled a 1x Subtropical Shard`.
- More prefixes seen: `REACTIONS`, `BOOSTER`, `VOTE`, `TIP` (`You tipped <name>! + 500xp | $500`), `LOCATION` (`Lake Biwa gained 4.17K xp`), `TP`, `PURCHASE!`, `CHUMMER` (`<name> activated a <glyph> Chummer. (+75 Bite Speed)`), `CONTESTS` (`0.2lb → 0.43lb (+0.24lb)`), `NOTIFICATION`, `TRAILS`, `HATS`, `ROD PARTS`, `ARTISAN`, `MANUFACTURER`, `PAY`, `TRADE`, `TPA`, `AUCTION HOUSE`, `<LOCATION> MINI EVENT` (message starts with a newline), and FOECollab's own `FOE » You went <n> fish dry before catching a ...`.
- Gameplay logs to grep for more: `/home/luna/.local/share/PrismLauncher/instances/fishonmc-dev/minecraft/logs/` (luna offered them, 2026-10-03).

**Other**
- `SELL » You just earned $38.48K from selling your fish!` (money earned per sell; good stat)
- `SELL » You just earned $824.69 from selling pets!` (pet store, an npc). Worked out from 12 of luna's sells (2026-10-16): payout = base × 2^((rating − 50) / 10), base $1,250 for common and $2,500 for rare (the payout of a 50% pet), exact to the cent. Rating is the pet's rating percent from its lore. Not known: the bases of uncommon, epic and legendary, and whether pet level changes it (all samples may have been level 0). See `display/pet-profit.md`.
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

Catcher **name** is taken from lore line 15's last word (fragile; prefer UUID). A real fish also has `conservation` (IUCN code, `VU`) and `water` (`saltwater`); `native` holds `Native`.

**Fish tooltip layout** (luna's dump of a common Camouflage Grouper, 2026-10-16, `/tacklebox debug item`; 20 tooltip lines, 19 of them lore). Every lore line is a `LORE` Text whose first segment is the window prefix `U+EEE4 U+EEE1 ␣ <body glyph> ␣ U+EEE8 U+EEE7 U+EEE5 U+EEE2` plus spaces, white; the rest are the server's own segments, all in the default font. Body glyph: `U+D003` common (the first line has the header glyph `U+D001`, the last the end cap `U+D004`). Order: 0 the plain item name (vanilla line, no prefix); 1 name again with the header glyph; 2 rarity mark (` ` plus an empty segment; foecollab rebuilds it as `` in font `foecollab:tags` and a space); 3 scientific name (small caps, dark gray); 4 separator (47 spaces, dark gray, strikethrough); 5 value (`$37.56`, green); 6 nature; 7 location; 8 size (`ᴀᴅᴜʟᴛ ` and the sex sign); 9 weight and 10 length (`0.17lb ` in `#E7FFF5` then `(0.08kg)` dark gray, own segment each; same for `7.11in ` and `(18.06cm)`); 11 separator; 12 fish group; 13 lifestyle; 14 ecosystem; 15 separator; 16 Angler, 17 Date, 18 Rod (these three labels are plain mixed case, the others small caps; labels are `#CBCED3`, the fish group, lifestyle and ecosystem labels `#F6CF6D`); 19 end cap (prefix only). The rod name segments are one colour per letter. Vanilla wraps the lore lines in dark purple, not italic.

**Pet tooltip layout** (luna's dump of a common Duck Pet, 2026-10-16, `/tacklebox debug item`; 31 tooltip lines, 30 of them lore). Same window prefix as the fish (body glyph `U+D003`, header `U+D001`, end cap `U+D004`). Order: 0 the plain item name (per-letter gradient from `custom_name`); 1 name again with the header glyph; 2 rarity mark (`U+F033` as a plain segment, foecollab re-fonts it to `foecollab:tags`); 3 kind (`ꜱᴜʙᴛʀᴏᴘɪᴄᴀʟ ᴘᴇᴛ`, small caps, dark gray); 4 separator; 5 pet level (`[0]`); 6 xp bar (20 bold `#677982` strikethrough spaces, a space, `(0/1K)`); 7 separator; 8 `While in any <Climate>:`; 9 luck and 10 scale (indented four spaces, `(+0/+8) (32%)`); 11 blank line (prefix and one space); 12 `While at <Location>:` (the lore says `Toledo Bend Reservoir:`, foecollab shortens the tooltip to `Toledo:`); 13 luck; 14 scale; 15 blank; 16 rating (`ɢᴏᴏᴅ [53%]`); 17 separator; 18 `Equipped Skin: Default`; 19 and 20 two separators; 21 `Equipped Item: Empty`; 22 and 23 two separators; 24 `Trail Item: Empty`; 25 separator; 26 Discoverer; 27 Date (both indented two spaces, labels `#CBCED3`); 28 separator; 29 the hint `ʀɪɢʜᴛ ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ ᴘᴇᴛ ᴍᴇɴᴜ`; 30 end cap. The three equipment labels are plain mixed case, gray, their value `#666960`. Not sampled: what the equipment lines say with something equipped, other rarities, and the auction house layout (it adds `Seller:`, `Price:`, `Time:` before the end).

**Other `type` values:** `pet`, `armor`, `armorShard`, `bait`, `lure`, `line`, `pole`, `reel`, `craftingComponent`, `package` (bait package), `chummer`, `petItem`. Fishing rod is a vanilla `FISHING_ROD` with custom data.

- **Pet:** `id`, `pet` (bullfrog, bear, fox, kangaroo, marshrabbit, seaturtle, duck, eagle, wolf, pelican, capybara, lynx, shark, dolphin, sheep, koala), `rarity`, `level`, `xp_cur`, `xp_need`, `climate`, `location`, `cbase`/`lbase` lists of `{cur, cur_max, percent_max}` (index 0 = luck, 1 = scale; the lore's rating is the average of the four `percent_max`, checked on 4 pets 2026-10-16, foecollab's `Pet.getPercentPetRating` does the same). On an auction house listing (pet page, title art `U+EEE4 U+D543`, slots are listings) the stack also has `renderInfo[0]` with the whole item again plus `money` (the exact price as a float; the lore only shows `$8.5K`), `player` (seller) and `unix`; the lore ends with the window piece `Seller:`, `Price:`, `Time:`, a blank line and the confirm hint, `username`+`uuid` (discoverer), `date`, `item` (pet item list), `petItem`, **`skin`**, **`trail`** (server cosmetics present on pets).
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
| nile | Nile River | arid (from the hippo pet) | fresh | |
| crewisland | Crew Island | — | — | |

Spawn hub bounding box: x 95..145, z −58..38 (treated as Cypress Lake).

## 8. Menus (screen titles are glyph strings)

| Screen | Title string |
|---|---|
| **Stats** (`/stats`) | `U+EEE4 픲` |
| Main menu | `U+EEE4 할` (contains `END_CRYSTAL` named "Daily Missions"; lore lines `| <goal> (<p>/<n>)`) |
| Quests (`/quests`, 45 slots) | `U+EEE4 픹` |
| Crew / Leveling bonuses; also the collections overview, location leveling pages and the instance list (told apart by their items, `FishMenu.crewTitleMenu`, `FishMenu.isInstanceList`) | `U+EEE4 핒` (U+D552) |
| A collection's level pages | `U+EEE4 학` (U+D559) |
| Pet menu (gone: luna 2026-10-29 says there is none; pets are taken out of /pv, put in the hotbar and right-clicked to equip. Foecollab still checks this title.) | `Pet Menu` + `U+EEE6 U+EEE5 U+EEE3 핑` |
| Tackle shop | `Tackle Shop` + `U+EEE7 U+EEE3 합` |
| Armor menu | `Armor Menu` + `U+EEE7 U+EEE2 핈` |
| Fish Merchant (`/sell`, 54 slots) | contains "Fish Merchant" |
| Personal vault | `Personal Vault <n>` |

**Stats page slots** (luna's dump 2026-10-30, title `U+EEE4 픲`, 24 items, the same layout for `/stats <player>`): 1 book "Fishing Menu (Click)"; 2-6 the profile display showcase (bread "Empty Profile Display" or a chosen item, such as gunpowder "Christmas Event 2025" with `Placement: #23`, `Points: 23145pts`); 7 the pet (`custom_data type:"pet"`, lore `ᴘᴇᴛ ʟᴇᴠᴇʟ: [100]`, `(246.69M/MAX)`, `Equipped Item: Wade's Amulet`); 10 the hat (a leather boot with `type:"hat"`; no helmet slot in this dump); 16 the bait (`type:"bait"`, `Stock: 433x`); 19, 28, 37 chest, legs, boots (`type:"armor"`, `piece:"leather chestplate"` etc., `quality`, `tier`, `rgb`); 21 Collections; 22 the profile head (`Profile of <name>`, `Fishing Level [287]`, `(3.02M/4.22M)`, `Power 425.18K`, bite speed, line strength, reel speed, luck, scale, prospect, wait time, bonuses); 23 General Statistics (`ꜰɪꜱʜ ᴄᴀᴜɢʜᴛ: 210,542`, rarity, size and variant counts); 25, 34, 43 reel, pole, line (`type:"reel"|"pole"|"line"`, the line with `curDurability:146`, `maxDurability:400`); 31 Power Breakdown; 32 Runes Breakdown (`Total Runes Discovered: 202`); 46 Return; 48 "Level 300 Colors"; 49 Close; 50 Admiral Colors. The buttons have no custom_data.

**Stats menu = authoritative all-time numbers.** Your page has a `PLAYER_HEAD` whose profile UUID is you; a `KNOWLEDGE_BOOK` whose lore lines hold `<rarity/size/variant tag> <count>` and `ꜰɪꜱʜ ᴄᴀᴜɢʜᴛ: <n>` (numbers use `,` separators and `K` suffix). FOECollab's "Import Stats" button seeds all-time stats from this (`handler/StatsImportHandler.java`). The new mod can read this passively whenever the player opens `/stats`, with no automation needed.

**Quest menu:** 8 shulkers; white = not started, red/orange/yellow = in progress (lore `Catch <n>x <goal> fish`, `ɪɴ ᴘʀᴏɢʀᴇss (<p>/<n>)`), green = complete (moves around; find by item). Difficulty menu: slots 11/13/15 yellow/orange/red shulker = Easy/Medium/Hard. Tier menu: 11 blue terracotta Adult, 12 orange terracotta Large, 13 light-blue concrete Rare, 14 lime concrete Epic, 15 orange concrete Legendary. Dumps 2026-10-29: list, difficulty and tier menu share the title `U+EEE4 U+D539`; difficulty buttons are named `EASY`/`MEDIUM`/`HARD` with lore `Click to start an easy quest.`, tier buttons lore `Click to start.`, both menus have `Return` (37) and `Close` (40). A quest item is named `Fishing Quest #n` (slots 12-14, 20-24) and its lore has the difficulty in small caps (`ʜᴀʀᴅ ꞯᴜᴇsᴛ`, q is U+A7AF), `Catch 71x Rare fish`, `at Murray River`, progress, then the reward ranges.

### Fishing menu (wiki.fishonmc.net/doku.php?id=fishing_menu, checked 2026-10-06)

Opened with the book in the 9th hotbar slot or `/menu`. Layout in the wiki screenshot: profile head on top, two rows of five icons, then help, close, settings and daily missions along the bottom. Buttons: daily cosmetic store, recipes, fish compendium (personal codex, fish data and **location leveling info**), quests (also `/quests`), turbo travel (needs captain rank, otherwise the location NPC), cosmetics, crews, shop (crew island blocks), crafting, player profile (stats, equipped items, collections, achievements), help (`/help`), settings (scoreboard and ambient sounds), daily missions.

Location wiki pages are `wiki.fishonmc.net/doku.php?id=location:<page>`; the example picture is `lib/exe/fetch.php?media=cinematic_<name>.png|jpg`. Pages and pictures: cypress_lake/cypresslake, kenai_river/kenairiver, lake_biwa/lakebiwa, murray_river/murrayriver, everglades/everglades, key_west/keywest, toledo_bend_reservoir/toledobend, great_lakes/greatlakes, danube_river/danuberiver, oil_rig/oilrig (jpg), amazon_river/amazon, mediterranean_sea/mediterranean, cape_cod/capecod, hawaii/hawaii, lofoten_islands/lofoten (jpg), cairns/cairns. Not bundled any more: the menu draws the item the server ships. 17 locations since nile river was added.

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

Rule change 2026-10-16 (`rules/text-and-automation.md`): only automating fishing, selling and questing is barred. The autofish and `MenuRobot` items above stay out. Tipping, `/contest` and `/find` are no longer barred outright, but ask luna before adding any.

## 13. Gaps to fill with an in-game session

1. Exact full text and JSON structure of catch title/subtitle and the `RARE CATCH!`/`PET DROP!` lines (only prefixes are known).
2. Whether the minigame `successful`/`failure` glyphs arrive via title or action bar (gives escape counting).
3. Achievement / collection menus, if any (§10).
4. Full species list per location (`fish` ids, `group` ids).
5. Current pack version: re-extract fonts from `downloads/` after joining, since codepoints can change when the server updates the pack.

---

## 14. Feature spec: chat triggers → custom titles & sounds (requested by luna, 2026-10-02)

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

2026-10-28: the title queue was replaced. A title of the same trigger stacks, a different trigger replaces it (`display/triggers.md`).
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

## presets menu dump, 2026-10-06

- title `Presets핲` (menuName `presets`). 54 slots, bread filler. Each preset is one row: slot 2, 11, 20, 29, 38 is a gunpowder equip button (`custom_data {type:"equip", preset:n, filter:"armor"}`, lore "Preset #n"), followed by its leather chestplate, leggings and boots (`custom_data` has `name`, `piece`, `rarity`, `tier`, `quality`, `infusion`, `fish_bonus`).
- slot 48 flint `{navigation:"filter"}` switches armor and part presets, slot 52 "Next" `{navigation:"next", page}` (page 2 has `{navigation:"back"}` in slot 46, a gunpowder named Back, instead of a Next), slot 49 Close. Rod part presets (dump 2026-10-06, `filter:"parts"`): same layout, a gunpowder equip button then up to three slots in the order reel (flint), pole (blaze_rod), line (feather), each with `custom_model_data`. An empty part slot is a gunpowder named "Reel Slot"/"Pole Slot"/"Line Slot" with `custom_data {type:"reel"|"pole"|"line"}`. Slot 48 is a leather chestplate "Switch to Armor Presets" (`navigation:"filter"`), slot 52 "Next" (`navigation:"next", page:2`). Page 2 has the back button (slot 46, `navigation:"back"`).

## compendium location item and foecollab's catch hook, 2026-10-09

- Compendium location item lore (luna's screenshot of Key West): `Level: [11] (929.25/432K)` (432K is 250 x 12^3, the next level), `Catches: 7,955`, `Runes Discovered: (15/15)`, then `normal -> (28/28)`, `gigantic`, `albino`, `melanistic`, `trophy`, `fabled` in that order (a green check in front of a finished one, counts green when complete, yellow in between, red at 0), last line `CLICK TO VIEW THE COMPENDIUM`. The species page behind that click has not been dumped yet. No chat line is sent when a fish is new for the compendium (only `RUNES » You discovered a rune! (n/m)`), checked in the logs of 2026-09 to 10.
- Foecollab (reference only, AGPL): `FishCatchHandler` sees a catch by the one glyph title, then the subtitle with a rarity tag, then looks for the new fish stack of yours in the inventory for 2 s (or the dropped item entity when full). It then calls `ProfileDataHandler.updateStatsOnCatch(Fish)` and `onFishCaughtSendDryStreak(Fish)`; `Fish` carries `fishId` (species), `variant`, `size`, `rarity`, `location`, `groupId`, `weight`, `length`, `catcher`. It saves only totals (`variantCounts`, `rarityCounts`, `fishSizeCounts`, dry streaks), no species or location, so it cannot give a compendium baseline. Its chat line per catch is the dry streak message (`FOE » You went N fish dry before catching a ...`), which only shows for the rarities, sizes and variants its config switches on, so hook the method, not the chat.

- **Compendium species page** (luna's dump 2026-10-09, Mediterranean Sea, filter normal, page 1; title `U+EEE4 U+D502` plus art, `menuName` "menu"): slots 1-6 are bread with the header lore (`<Location> Compendium`, `Normal Progression: (40/40) [100%]` in small caps, the completion rewards), slot 7 glowstone dust is the location (`Current Level: [9]`, `(223.96K/250K)`, `Progress to Level 10`, so the progress is into the current level out of the next level's cost). Species are `minecraft:cod` items named by the species in slots 10-16, 19-25, 28-34, 37-43 (28 a page, 40 species here so a second page behind "Next" in slot 52). Lore: rarity glyph (U+F033 common, F034 rare, F035 epic), scientific name in small caps, `TOTAL: n`, then `U+F041` to `U+F044: n` (probably albino, melanistic, trophy, fabled counts), weight and length personal best, world record weight and length with holder and date, IUCN status, fish group, lifestyle, ecosystem, migration, waters. Slot 46 Return, 48 Leveling Bonuses, 49 Close, 50 hopper "Filter" (`custom_data {filter:"normal"}`, lore lists normal, gigantic, albino, melanistic, trophy, fabled), 52 Next. `U+F041` to `U+F044` are the albino, melanistic, trophy and fabled counts (foecollab's variant glyph tags).
  Albino filter dump (2/40, Bluespotted Sea Bream and Common Stingray caught): header lore `albino progression: (2/40) [5%]`, a missing species is a `minecraft:gray_dye` named `\u2716 <Species>` with the same lore (TOTAL and the rest still filled in), a caught one keeps its variant item (`white_dye` for albino, `cod` for normal) with the plain name. Page 2 (12 species, slots 10-16 and 19-23) has `Back` in slot 46 instead of Return and nothing in 52. The gigantic filter works the same (19/40 in luna's dump). Species are named in English; the fish item's `fish` id is probably the same name in snake case (not seen yet, `Compendium` matches on letters only and logs `compendium: no species` for a miss).

## power, 2026-10-08

- **Where it shows:** `/stats`, the treble hook item named "Power Breakdown" (wiki page `power`), and `Power 345.35K` in the lore of the profile head in `/menu`. Lore of the breakdown (from luna's screenshot): `Total Power: 345.35K`, `Location Leveling: 151.28K`, `Compendium Completion: 39K`, `Collections: 83.33K`, `Total Unique Fish Caught 71.75K`, then one line per variant, `<name> <count> (<amount> per)`. The names of albino, melanistic, trophy and fabled are pack glyph tags (dropped by `Glyphs.plain`), so a line is told apart by its amount. Every number is followed by the power icon `U+F064`. The four sources add up to the total (151.28 + 39 + 83.33 + 71.75 = 345.36 with rounding).
- **Unique fish, per new unique fish:** normal 50, gigantic 150, albino 250, melanistic 500, trophy 750, fabled 1000 (355 x 50 + 37 x 250 + 16 x 500 + 8 x 750 + 3 x 1000 + 185 x 150 = 71,750, which is the line above). "Unique" is by species: the compendium location pages count a fish once per location (the 2026-10-06 dump had 396 normal fish caught over the 17 pages, the breakdown says 355 unique), so summing the location pages overcounts.
- **Location leveling:** every level gives power, the same amounts at every location (luna confirmed). Levels 1 to 20: 50, 75, 100, 125, 150, 175, 250, 325, 500, 750, 1.25K, 1.75K, 2.5K, 3.75K, 4K, 6K, 7.75K, 9.25K, 11.25K, 14K, which is 64,000 at level 20. 17 locations make 1,088,000. Level 20 is the cap (progress keeps counting past it, Lake Biwa showed 11.85M of 2.32M). The levels saved on 2026-10-08 give 142,275 against the 151.28K shown, the difference is level-ups since (Murray 16, Key West 11 and Toledo Bend 12 would be exactly 9,000).
- **Location leveling page** (title `U+EEE4 핒`, shared with the crew menu and the collections overview; luna's dump 2026-10-08): slot 4 gunpowder "Location Leveling Data", lore `Information for Lake Biwa`, `Level: [20]`, `(11.85M/2.32M)`. Slots 10-16, 19-25 and 28-33 are green concrete (done) "Level n" with lore `(Level n) Lake Biwa`, `Rewards:`, `(+1) Location <Species|Generic|Sell/XP> Bonus Roll`, `<power> U+F064`, sometimes a rod title, hat or rod skin, and `<n>x <Standard|Advanced> Bait Package`. Slot 46 Return, slot 48 "Leveling Bonuses" (`Bonuses Available: (20/20)`).
- **Not known yet:** what a completed compendium location is worth (39K today) and what each collection gives (83.33K), so `/tacklebox power` only shows those two as read. Needs F8 dumps of the collections menu (`/collections`), a compendium species page (the "View Species" click on a location) and whatever the compendium shows for a finished location.
- **Collections menu** (a collection's level pages, title `U+EEE4 학` U+D559, `menuName` "menu"; luna's dumps 2026-10-08, five categories seen: albino, all, common, epic, fabled, each its own page of 10 levels; gigantic also has 10, level 9 needs 5,000 and 10 needs 10K, luna's screenshot 2026-11-01): every level is concrete (green done, yellow in progress, gray locked) named "Level n", lore `(Level n)`, `<category> collection` in small caps, `Requirement: (have/need)` or `Unlocked! (n/n)`, `Rewards:` with money, `<power> U+F064`, bait packages and sometimes a recipe or rod skin. Power per level differs per category, e.g. albino 100, 200, 300, 500, 1K, 2.5K, 5K, 7.5K, 10K, 20K (47,100 in all; needs 1, 2, 4, 8, 16, 32, 50, 80, 150, 500 fish), all 25, 100, 200, 500, 1K, 2K, 4K, 7.5K ... 50K (100 to 5M-ish catches), fabled 200, 500, 700, 1K, 2.4K, 5.3K, 7.8K, 10.2K, 20.5K, 50K (needs 1, 2, 3, 6, 8, 12, 16, 20, 30, 50). The first page of each category has Level 1-7 in slots 1-7 and 8-10 in slots 10-12. Not read by the mod yet.
- **Species collections** (luna's dumps 2026-10-08 and 2026-10-10): 35 categories in the same menu and format, one per fish group, 7 levels each (the first 13 were basses, billfishes, carps, catfishes, cods, flatfishes, gars, jacks, pikes, rays, salmons, sharks, trouts; 22 more came with the update of 2026-10-10). Power per level is the same for all of them: 50, 100, 250, 400, 1K, 5K, 10K (16,800 per collection, 588,000 for all 35). The new ones do NOT pay more per level. Catches needed for levels 1 to 7:
  - basses: 100,500,2K,5K,10K,20K,50K
  - billfishes: 10,100,500,1.5K,5K,10K,20K
  - bonytongues: 25,125,600,2K,6K,12K,25K
  - carps: 100,500,2K,5K,10K,20K,50K
  - catfishes: 75,250,1.5K,3K,7.5K,15K,30K
  - characins: 50,200,1K,2.5K,5K,10K,30K
  - cichlids: 100,500,2K,5K,10K,20K,40K
  - cods: 75,250,1.5K,3K,7.5K,15K,30K
  - drums: 50,200,1K,2.5K,5K,10K,30K
  - eels: 25,125,600,2K,6K,12K,25K
  - eurasian minnows: 100,500,2K,5K,10K,20K,40K
  - flatfishes: 75,250,1.5K,3K,7.5K,10K,20K
  - gars: 50,200,1K,2.5K,5K,9K,15K
  - groupers: 50,200,1K,2.5K,5K,10K,30K
  - jacks: 50,200,1K,2.5K,5K,15K,30K
  - mackerels: 75,250,1.5K,3K,7.5K,15K,30K
  - panfishes: 100,500,2K,5K,10K,20K,50K
  - perches: 100,500,2K,5K,10K,20K,40K
  - pikes: 50,200,1K,2.5K,5K,10K,20K
  - rays: 20,150,700,1.8K,6K,10K,20K
  - reeffishes: 100,500,2K,5K,10K,20K,40K
  - reptiles: 25,125,600,2K,4K,7.5K,15K
  - salmons: 100,500,2K,5K,10K,20K,50K
  - seabasses: 50,200,1K,2.5K,5K,10K,30K
  - seabreams: 75,250,1.5K,3K,7.5K,15K,30K
  - sharks: 25,125,600,2K,7.5K,15K,30K
  - snappers: 75,250,1.5K,3K,7.5K,15K,30K
  - sturgeons: 25,125,600,2K,6K,12K,25K
  - suckers: 50,200,1K,2.5K,5K,10K,30K
  - temperate perches: 75,250,1.5K,3K,7.5K,15K,30K
  - trevallies: 50,200,1K,2.5K,5K,10K,30K
  - trouts: 100,500,2K,5K,10K,20K,50K
  - tunas: 50,200,1K,2.5K,5K,10K,30K
  - whitefishes: 75,250,1.5K,3K,7.5K,15K,30K
  - wrasses: 50,200,1K,2.5K,5K,10K,30K
  The overview page (title `U+EEE4 핒` U+D552, the crew title, so `FishMenu.crewTitleMenu` names it "collections" by its items; slots 1 to 35, then Return and Close) shows each as an item named `<Group> Collection` with lore `Current Level - [n]` and `(have/need)` for the next level, no power, so `Power.overview` reads all 35 from that page (next level = n + 1, power from the table above). One /collections visit is enough. A finished one (luna's screenshot 2026-11-01, catfishes) shows `Current Level - [7]` and `(30,039/MAX)` (what you have, then MAX), no "Progress to Level" line, then `Catch at:`; the menu panel and `Power.overview` take that MAX as finished. The page also has a Gigantic Collection (red block icon, level 8 of 10 in the same screenshots; the 2026-10-10 dump had only the 35 species), so a level number cannot tell a finished collection. Its overview lore is not dumped yet; the tests assume the species format.

## fishing stats formulas (wiki "Fishing Stats" page, pasted by luna 2026-10-18)

The wiki is behind a Cloudflare check (403 for curl, WebFetch and headless Chromium), so luna pastes pages.

- **Bite Speed:** wait from the bobber hitting the water to the fish particles. Max wait = 20 s - 0.02 s x Bite Speed, floor 3 s (at 850). Min wait = max / 4 = 5 s - 0.005 s x Bite Speed, floor 1.5 s (at 700). The max wait is shown in the stats profile. How the wait is drawn between min and max is not stated.
- **Reel Speed:** progress per successful minigame tick, base 0.6 units, 1 Reel Speed = 0.001 units (+600 doubles it).
- **Line Strength:** lowers the total progress the bar needs (no formula given).
- **Luck / Scale:** the tables in `docs/menus/bait-picker.md` and the bait picker page (`odds`, `sizeOdds`) match the wiki.
- **Prospect:** shard chance = 1 / (50 / (1 + Prospect x 0.0008)), base 1/50.
- **Pet Luck:** per rarity 1 / (N / (1 + PetLuck / 100)) with N = 750, 4000, 10000, 30000, 80000 (common to mythic).
- **Bonus XP / Bonus Money:** multipliers on top of the 2x boosters; money bonus applies at catch, not at sale.
