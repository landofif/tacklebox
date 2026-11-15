# Inventory hud

Your inventory on the hud without opening it (luna, 2026-11-15). Off by default (`inventoryHud.on`).

- `hud/InventoryHud`: the 27 slots above the hotbar (`PlayerInventory` 9 to 35) as three rows of nine, 18 px cells, each item with its count and durability bar (`drawItem` + `drawStackOverlay`). Display only, nothing can be clicked.
- `inventoryHudCells` (on): a faint dark square behind every slot, so empty slots can be told apart.
- Hidden while a `HandledScreen` (chest, inventory) is open, which shows the inventory anyway.
- Place, size: a `HudSpot` (see `timers.md`), box in `/tacklebox move`, scroll resizes. Settings page "huds", group "inventory".
- Not here: rarity circles behind the items (the `rarityCircles` setting only draws in slots and the hotbar).
- Tests: `hud/InventoryHudTest` (cell layout), `InventoryHudIntegrationTest` (settings through a save, old settings get it off).

## Slots left hud

- `hud/SlotsLeft` (luna, 2026-11-15; off by default, `slotsLeftHud.on`): one row "slots left n" under the title "inventory", drawn through `TextHud`. It counts the empty ones of the 36 slots (hotbar 0 to 8 and inventory 9 to 35; armour and offhand are not counted) and shows once that is at or under `slotsLeftAt` (slider 0 to 36, default 5; 36 shows it always, 0 only when full). The number is red at 0. In the mover it always shows so it can be placed.
- FOECollab has its own inventory space warning; this one is tacklebox's and works without it.
- Tests: `hud/SlotsLeftTest` (threshold, always, full, preview), `SlotsLeftIntegrationTest` (settings through a save, an out of range number is pulled back).

Not run in game yet: the look, the size next to the hotbar, that it hides under chest screens.
