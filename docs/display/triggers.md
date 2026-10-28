# Chat triggers

A trigger watches chat lines and, when one matches, shows a title, plays a sound and can pop an item in the totem animation. It never sends anything. Rule model and first spec: `fishonmc-research.md` section 14.

## Key files

- `trigger/TriggerEngine`: runs each chat line (`ALLOW_GAME`) through the triggers, merges lines, fills placeholders, reads chat colours.
- `trigger/Matcher` (normalize, SIMPLE/EXACT/REGEX, capture spans), `Trigger` (saved model), `TriggerPresets`, `TriggerStore` (`config/tacklebox/triggers.json`).
- `display/TitleOverlay` (titles on screen), `TotemPop` (the pop and its item lookup), `ItemLooks` (remembered item looks), `PetHeads` (pet and shard heads), `SoundQueue`.
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

## Presets

- **pet drop**: regex anchored with `^`. The rarity can be a glyph, a word or a word in brackets. Title "pet drop!", subtitle `{pet.lower}`, shake, totem `pet:{pet}`, level-up sound twice.
- **rare catch**: `^rare catch! you (pulled|caught) (a )?<n>x <item>`, own lines only (shards, Lightning in a Bottle, Infusion Capsule, Prospecting Amulet). Title "rare catch!", subtitle `{item.lower}`, pop, totem `{item}`, amethyst chime three times.
- "pet totem", "climate shard" and "shard totem" are gone. Triggers saved from them stay as they are.

## Gotchas

- Fabric runs `ALLOW_GAME` (ours) before `MODIFY_GAME` (foecollab's), so triggers see the server's own text and colours. The game log shows the line after foecollab, with `[COMMON]` where the server sends a rarity glyph.
- Lines hidden by a trigger ("hide chat line") are not in the game log.
- Other players' drops come as `! [n] name » PET DROP! ...` or `[n] name » RARE CATCH! ...`, and whispers as `✉ To name » ...`. Hence the `^`.
- Tests can't make ItemStacks (registries need Fabric's loader), so the lookup order is tested with names (`TotemPop.source`, `fallback`).

## Tests

`TriggerEngineTest` (fires at once, merge in place, cooldown, hide), `TitleOverlayTest` (stacking, own timers, slide, replace, update, entrance values), `TotemPopTest` (name keys, lookup order, shard fallback), `TriggerColorsTest` (colour mapping, bold after the value, `.lower`, defaults), `TriggerPresetsTest` (real log lines, other players' lines), `TriggerIntegrationTest` (log lines through the engine with the presets).

Not run in game yet: titles come with the chat line, the pop shows the real shard, bottle or capsule, how a stack looks, the server's colours in the subtitle.
