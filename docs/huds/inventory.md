# Inventory hud

Your inventory on the hud without opening it (luna, 2026-11-15). Off by default (`inventoryHud.on`).

- `hud/InventoryHud`: the 27 slots above the hotbar (`PlayerInventory` 9 to 35) as three rows of nine, 18 px cells, each item with its count and durability bar (`drawItem` + `drawStackOverlay`). Display only, nothing can be clicked.
- `inventoryHudCells` (on): a faint dark square behind every slot, so empty slots can be told apart.
- Hidden while a `HandledScreen` (chest, inventory) is open, which shows the inventory anyway.
- Place, size: a `HudSpot` (see `timers.md`), box in `/tacklebox move`, scroll resizes. Settings page "huds", group "inventory".
- Not here: rarity circles behind the items (the `rarityCircles` setting only draws in slots and the hotbar).
- Tests: `hud/InventoryHudTest` (cell layout), `InventoryHudIntegrationTest` (settings through a save, old settings get it off).

Not run in game yet: the look, the size next to the hotbar, that it hides under chest screens.
