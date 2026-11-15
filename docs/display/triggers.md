# Chat triggers

A trigger watches chat lines and, when one matches, shows a title, plays a sound and can pop an item in the totem animation. It never sends anything. Rule model and first spec: `fishonmc-research.md` section 14.

## Key files

- `trigger/TriggerEngine`: runs each chat line (`ALLOW_GAME`) through the triggers, merges lines, fills placeholders, reads chat colours.
- `trigger/Matcher` (normalize, SIMPLE/EXACT/REGEX, capture spans), `Trigger` (saved model), `TriggerPresets`, `TriggerStore` (`config/tacklebox/triggers.json`).
- `display/TitleOverlay` (titles on screen), `TotemPop` (the pop, its item lookup and the shorter leaving spin, see `world-and-totem.md`), `ItemLooks` (remembered item looks), `PetHeads` (pet and shard heads), `SoundQueue`.
- `trigger/RareDrops`: the built-in pet drop and rare catch (2026-11-01, see Rare drops).
- `screen/TriggerEditScreen`: tabs when, title, totem, sound, and the preview.

## Timing (2026-10-28)

luna found triggers late and plain next to skyblock mods. Before, every trigger waited 250 ms (plus up to a tick), then faded in from invisible over 0.3 s, and a second title waited for the first to end.

- **Fires on the line.** Title, sound and totem start inside the chat callback for the first matching line. The cooldown works as before.
- **Merge in place.** More lines of the same trigger within 250 ms of that first line change the title already on screen ("quests 3 & 5", `{matches}`, `{s}`). No second sound or pop, and the title's timer keeps running. `{count}` counts firings.
- **Same trigger stacks.** The newest title sits at the trigger's height, older ones slide up one place (title, subtitle and a 4 px gap; 150 ms ease-out). Each keeps its own timer, at most 3.
- **Different trigger replaces.** It clears the screen and shows alone (luna: "if its two different ones then just show the later one"). There is no queue.
- **Entrance.** Every animation starts at 40% opacity and reaches full over the fade-in. POP starts at 70% size and settles with a small overshoot. Total time is still in + stay + out.

## Totem item lookup

`TotemPop.show(spec)` gets the totem field after placeholders are filled:

1. An item id gives that item (nothing typed gives the totem). `pet:<type>` and `shard:<climate>` give a head from `PetHeads`.
2. Anything else is an item name, like `{item}` from the rare catch preset. Name key: `Matcher.normalize`, lowercase, letters and digits only. Then:
   1. a stack with that name in the inventory: a copy (count 1), so the server pack draws the real thing (rare items are vanilla items with `custom_model_data`; Bigfoot Tooth is a stick with cmd 2.0);
   2. else a remembered look (`ItemLooks`, setting `itemLooks`: the item plus `item_model`, `custom_model_data`, `profile`, `dyed_color`, `enchantment_glint_override`, as ItemStack json). Learned in the `PetHeads` slot pass every 20 ticks from items whose custom_data has a `type` other than `pet` (fish have none). The first stack of a name in a pass wins, so two looks of one name don't save every second;
   3. else wait up to 2 s, checking the inventory every tick (`TotemPop.tick`), and pop when it lands. A newer pop replaces a waiting one;
   4. else a name ending in " shard" gives that shard's head, anything else the totem.

## Chat colours

`title.chatColors` (toggle "chat colours" on the title tab): captured words keep the colour the chat line gave them. Off for triggers saved before it, on in every preset.

- `TriggerEngine.lineColors` reads each character's colour from the Text and maps it through `Matcher.normalize` (glyphs dropped, spaces collapsed). `Matcher.Result.spans` says where each capture sits.
- `fill` writes a coloured capture as `&#RRGGBB` runs, then puts back the colour and formatting from before the placeholder (`RichText.codesAtEnd`). This is needed because a colour code resets bold in `RichText`, like vanilla. `{x.lower}` keeps the colours.
- Only colour is taken, not bold. The totem item and everything outside the title get plain values. Lines without colours (the Compendium's own line) stay plain.

## Rare drops (2026-11-01, asked for by luna)

Pet drops and rare catches are no longer presets in /triggers. `RareDrops` holds two built-in triggers (ids `builtin:pet-drop`, `builtin:rare-catch`, made from `TriggerPresets.petDrop()/rareCatch()`) that `TriggerEngine.onGameMessage` runs before the saved triggers, so timing, stacking, merging and chat colours are the same.

- **Title** (2026-11-01): one line, no subtitle (pets get their rating under it, see Pet rating): `+ {item}` ("+ subtropical shard") and `+ {pet} pet` ("+ capybara pet"). The "+" is the trigger colour (orange, pink for pets), the name keeps its chat colour. A count above 1 shows: "3x arctic shard" reads "+3 arctic shard" (set per drop in `RareDrops.apply`, since placeholders have no "only if more than 1").
- **Pet rating** (2026-11-13, asked for by luna; `display/PetRating`): a pet title gets a second line, the pet's rating as the lore says it, word and percent with one decimal in the word's colour ("amazing 93.4%"). The chat line has no stats, so `TriggerEngine` hands the title to `PetRating.await`, and `PetRating.tick` (every client tick) looks through the inventory for a pet (`type:"pet"` head, rating from `PetSale.rating`) of that kind that is new: first seen after the line, or up to 3 s before it, and not used for another title. Pets are told apart by `custom_data.id` (the whole data when there is none); the ones there on the first look after joining are not new. No pet within 3 s (full inventory) leaves the title as one line. The words and their steps: sickly ≤20, bad <30, below average <40, average <50, good <60, great <80, excellent <90, amazing <100, perfect.
- **Quest pets** (2026-11-13): a quest's pet reward, the line ` + <rarity glyph> Lynx Pet` under the quest's money, xp and shards, is a third built-in (`builtin:quest-pet`, `RareDrops.QUEST_PET_PATTERN`): the same title, pop, sound and rating as a fished pet, under the pet row's switches (no row of its own). Its title replaces a quest completed title on screen, since that is another trigger.
- **Rows.** `RareDrops.row` sorts a drop: pet, shards (any climate), lightning in a bottle, infusion capsule, prospecting amulet, bigfoot items (by item name, a guess: no bigfoot log line yet), other rare catch.
- **Switches.** Settings > fishing > "rare drops" group: a main switch (`rareDrops`) and per row a title, totem pop and sound toggle, stored as off-sets `dropTitleOff`, `dropPopOff`, `dropSoundOff` (all on by default). `RareDrops.apply` fires a copy with those parts off, and nothing at all (no cooldown used) when all three are off.
- **Migration.** Once (`dropsMigrated`, `RareDrops.migrate()` from `Tacklebox`), saved triggers whose pattern is exactly the old pet drop or rare catch regex are switched off, not deleted; edited ones stay.
- Test hook: `TriggerEngine.assumeFishOnMc` (built-ins are FishOnMC only). `TriggerEngine.forgetFires()` clears cooldowns, since Gradle reruns failed classes first and a real-clock fire from `TriggerIntegrationTest` blocked `RareDropsTest`'s fake clock.
- Tests: `RareDropsTest` (also the pet line through the engine to a title with its rating, and the quest pet line), `PetRatingTest` (words, the line, new and old pets, kinds, two drops, the 3 s window), plus `TriggerIntegrationTest` (log lines through the built-ins). Not run in game yet: the rating line and the quest pet title (whether the pet is in the inventory within 3 s, and that `id` tells two pets apart).

## Presets

- **pet drop** (built in now, see Rare drops; `TriggerPresets.petDrop()` still makes it): regex anchored with `^`. The rarity can be a glyph, a word or a word in brackets. Title "pet drop!", subtitle `{pet.lower}`, shake, totem `pet:{pet}`, level-up sound twice.
- **rare catch** (built in now): `^rare catch! you (pulled|caught) (a )?<n>x <item>`, own lines only (shards, Lightning in a Bottle, Infusion Capsule, Prospecting Amulet). Title "rare catch!", subtitle `{item.lower}`, pop, totem `{item}`, amethyst chime three times.
- **pet max level** (2026-10-29, from luna's own trigger "petg"): `^pets > your (?<pet>.+?) pet is now level \d+ → 100\b`, so a jump like `97 → 100 (+3)` fires too. 100 is the top: no log line goes past it. Title "{pet} max level!", subtitle "level 100", totem `pet:{pet}`, bell.
- **rod not cast** (2026-10-29): matches `tacklebox > rod not cast for {seconds}s`, a line the mod hands to the triggers itself (not to chat), like the compendium line. Made by `display/RodIdle`, see `huds/bobber-timer.md`. Title "cast your rod", note block bell twice.
- "pet totem", "climate shard" and "shard totem" are gone. Triggers saved from them stay as they are.

## Gotchas

- Fabric runs `ALLOW_GAME` (ours) before `MODIFY_GAME` (foecollab's), so triggers see the server's own text and colours. The game log shows the line after foecollab, with `[COMMON]` where the server sends a rarity glyph.
- Lines hidden by a trigger ("hide chat line") are not in the game log.
- Other players' drops come as `! [n] name » PET DROP! ...` or `[n] name » RARE CATCH! ...`, and whispers as `✉ To name » ...`. Hence the `^`.
- Tests can't make ItemStacks (registries need Fabric's loader), so the lookup order is tested with names (`TotemPop.source`, `fallback`).

## Tests

`TriggerEngineTest` (fires at once, merge in place, cooldown, hide), `TitleOverlayTest` (stacking, own timers, slide, replace, update, entrance values), `TotemPopTest` (name keys, lookup order, shard fallback, leaving spin), `TotemPopSpinIntegrationTest` (a whole pop frame by frame), `TriggerColorsTest` (colour mapping, bold after the value, `.lower`, defaults), `TriggerPresetsTest` (real log lines, other players' lines, pet max level, rod not cast), `TriggerIntegrationTest` (log lines through the engine with the presets, pet max level pop).

Not run in game yet: the pet max level and rod not cast presets; titles come with the chat line, the pop shows the real shard, bottle or capsule, how a stack looks, the server's colours in the subtitle, the pop leaving with a 300° spin.
