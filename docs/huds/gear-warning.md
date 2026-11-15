# Wrong pet and armor warning

A red warning on the hud while your equipped pet is not the pet of the place you are at, or your armor is for another climate, and one chat line a moment after you arrive there (luna, 2026-11-15). Off by default (`gearHud.on`); the chat line has its own switch (`gearChat`, on) and only comes while the hud is on. Nothing at the Cypress Lake spawn.

Credit: the pet per location table and the idea come from FOE+ (`itshaxr/FOE-Plus`, FEATURES.md, `PetLocationHandler`, its gear warning), which builds on FOE-R (DannyPX) and FOECollab (Kimberlyow). No code was copied. FOECollab has its own wrong pet and armor warnings; luna wanted tacklebox's own and switches FOECollab's off.

## Key files

- `hud/GearCheck`: pure (the tests run it). The places with climate and pet, the PETS chat lines, the rows, the chat line, the arrival timer.
- `hud/GearWarning`: chat hook (`equippedPet`), tick (twice a second, the arrival chat line), reads the worn armor and the inventory, draws through `TextHud` (title "warning"), a sample in the mover.
- Hooks: `Tacklebox` chat, tick, `custom_huds` render, `HudMover`. Settings page "huds", group "wrong gear".

## How it works

| Location | Pet | Climate |
|---|---|---|
| Cypress Lake | Bullfrog | subtropical |
| Kenai River | Bear | subarctic |
| Lake Biwa | Fox | subtropical |
| Murray River | Kangaroo | semi-arid |
| Everglades | Marsh Rabbit | savanna |
| Key West | Sea Turtle | savanna |
| Toledo Bend | Duck | subtropical |
| Great Lakes | Eagle | continental |
| Danube River | Wolf | continental |
| Oil Rig | Pelican | savanna |
| Amazon River | Capybara | rainforest |
| Mediterranean Sea | Lynx | mediterranean |
| Cape Cod | Shark | oceanic |
| Hawaii | Dolphin | savanna |
| Cairns | Koala | monsoon |
| Lofoten Islands | Sheep | subarctic |
| Nile River | Hippo | arid |

- **Where you are**: `ServerInfo.locationId` (the boss bar). At Cypress Lake inside the spawn box (x 95..145, z -58..38), on the crew island and in unknown places there is no warning.
- **Equipped pet**: from the server's own lines, saved as `equippedPet` (the pet's id): `PETS » Equipped your <pet> Pet` sets it, `PETS » Your <pet> Pet is now level ...` too (only the equipped pet levels, so a missed equip line is caught up), `PETS » Pet unequipped!` clears it. Until one of these came the pet is not known and there is no pet warning. No pet equipped is no warning either.
- **Right pet in the inventory**: any item whose custom_data `pet` is the place's pet.
- **Armor**: the worn chestplate, leggings and boots; the custom_data `name` holds the climate id (fact from FOE+/FOECollab). A piece of another climate than the place's is named ("savanna chestplate"); a piece without a known climate is ignored.
- **Hud rows**: `wrong pet | use wolf · in inventory` and `wrong armor | savanna chestplate · need continental`, labels red. Shows only while something is wrong.
- **Chat line**: 3 s after the place changed (the boss bar and items settle), once per arrival: `tacklebox » wrong pet at danube river: use a wolf pet (in inventory) · wrong armor: ...`.

## Gotchas

- The level line of a pet that is not equipped does not exist, but a quoted line from a player has `name »` in front and does not match.
- The equipped pet stays saved over a restart; if you swap pets while the mod is off it is stale until the next PETS line.

## Tests

`hud/GearCheckTest` (the table against `ServerInfo.LOCATIONS`, chat lines, rows, armor pieces, spawn hub, arrival timing), `GearWarningIntegrationTest` (log lines through the normalizer to the saved pet and the rows, a save, old settings).

Not run in game yet: the hud, the chat line, that armor's `name` really is the climate id, that the pet is still in the inventory while equipped (so "in inventory" can be told), the spawn box.
