# Bait sort key

- Asked for by luna 2026-10-30 as tacklebox's own (so it works without foecollab): "just the keybind and just make it sort from common to legendary". `input/BaitSort`, key `key.tacklebox.sort_bait` ("sort bait, common to legendary"), unbound by default, pressed in a screen (`ScreenKeyboardEvents.afterKeyPress` in `Tacklebox`).
- Only in your own inventory (`InventoryScreen`), only items whose custom_data `type` is `bait` in your main inventory and hotbar (inventory index below 36). Lures are not sorted. The baits stay in the slots baits already take, in rarity order common, rare, epic, legendary, then mythical, special, unknown; equal rarities keep their order.
- Each swap is three pickup clicks (pick one up, put it in the other's slot, put that one back), one click every 2 ticks. Stops if the inventory closes; does nothing if something is on the cursor or a sort is still running. Allowed by the automation rule (sorting items, `rules/text-and-automation.md`).
- Foecollab's own bait sorter (its key, merging, chests) is separate and untouched.
- Tests: `input/BaitSortTest` (rank, swaps, equal ranks), `input/BaitSortIntegrationTest` (the clicks played on an inventory with a cursor). Not seen in game yet: whether the server accepts the clicks at this speed.
