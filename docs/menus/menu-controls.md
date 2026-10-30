# Menu controls and rarity marks

Small additions to server menus and items, asked for by luna on 2026-10-29. All off by default. None of it is run in game yet.

## Rarity marks (`rarityCircles`, `rarityShape`)

- A see-through mark in the item's rarity colour (the item's custom_data `rarity`, fishing rods left out like foecollab does, `display/Rarity`; with foecollab the slot marks are drawn in its slot markers, without it in `HandledScreenMixin`) behind items in menu slots and the hotbar. The first and last hotbar slot and the helmet slot never get one.
- "rarity shape" (look page) picks a circle (16x16, the old look) or a square that covers the whole 18x18 slot, so neighbouring squares touch (`Rarity.mark`, `slotSquare`).
- Drawn from `mixin/ItemMarkerHandlerMixin` (foecollab's item marker background) and `mixin/InGameHudHotbarMixin`. The vault overview always draws circles; the equipment row has its own `equipCircle`.

## Page wheel (`pageWheel`)

- `menu/MenuPages`: in a paged server menu, wheel down clicks the Next item, wheel up the Back item (one raw `ClickSlotC2SPacket` pickup, the click the player would make). Works anywhere on the screen, shift does nothing special. One page per 250 ms, so a fast spin does not skip pages before the next one loads.
- A page item is a slot whose plain name, or first lore line, is `Next` / `Next Page` or `Back` / `Previous` / `Previous Page`. The auction house keeps the plain item name "Gunpowder" and says `Next` only in the lore. "Return" (back to the menu before) is never clicked.
- Seen in dumps: compendium species pages (52 Next, 46 Back), auction house (51 Next), personal vaults and presets (52 Next, 46 Back).
- Works in the chest, in the mods menu screen (compendium pages open there when `fishMenu` is on) and the presets screen. Quest menus (menu name contains "quest") are left out. Only on fishonmc.
- **Mouse Tweaks** (luna has it) handles the wheel in fabric's `afterMouseScroll` and moves the item under the mouse. The page wheel cancels the scroll in `ScreenMouseEvents.allowMouseScroll`, so neither the screen nor Mouse Tweaks sees a turn that clicked a page; in other menus nothing changes.

## Tall tooltips (`tooltipScroll`)

- `display/TooltipScroll` + `mixin/DrawContextTooltipMixin` (wraps the positioner call in `DrawContext.drawTooltipImmediately`). Vanilla puts a tooltip taller than the screen with its bottom on screen and cuts the top (the name). With the setting on, it starts with its top 4 px from the screen edge and the wheel moves it 12 px a turn, down to 3 px above the bottom.
- The scroll starts over when a tooltip of another size shows or none was drawn for 150 ms. A tall tooltip takes the wheel before the page wheel (registered first).

## Chest menu scale (`chestScale`)

- `menu/ChestScale`: while a server chest menu is open (generic chest, 3x3, hopper, shulker screen) the window uses this gui scale; 0 keeps the games. Whole steps only, since the games scale factor is an int; too big a number is cut to the biggest that fits, like the game's own option. The hud behind the chest (hotbar, vanilla, fabric and foecollab huds) keeps the games scale: `mixin/GameRendererMixin` wraps `InGameHud.render` and `renderDeferredSubtitles` (Fabric draws hud elements added last, which is where tacklebox's huds and trigger titles are, with the subtitles after the screen; wrapping only `render` left them big), puts the window back on the games scale for the huds layout and shrinks it by games / chest scale (`ChestScale.drawHud`), so hud pixels stay whole screen pixels. The F3 screen and toasts are drawn outside it and still change size.
- `mixin/MinecraftClientMixin`: set before a new screen reads the scaled size in `setScreen`, put back at the end of `setScreen` (closing has no layout step), and kept on a window resize by wrapping `calculateScaleFactor` in `onResolutionChanged`. The mods own menu, presets and vault screens are not chests, so they keep the games scale and their own size settings.

## Presets number keys

- `screen/PresetsScreen.keyPressed`: hotbar key n clicks the equip button of the nth card on the page that is showing (key 7 on a page of five does nothing). Uses the hotbar key bindings, so rebound keys work. Only in tacklebox's presets screen; vanilla's hotbar swap in chests is untouched.

## Tests

`display/RarityTest`, `screen/PresetsKeysTest`, `menu/MenuPagesTest` (names and lore from luna's dumps, Return and quests left out, cooldown), `display/TooltipScrollTest`, `menu/ChestScaleTest`, `MenuItemsIntegrationTest` (old settings keep everything off, saved values round-trip, bad values mended).

## Not checked in game

All of it. Things to look at: the square at hotbar spacings other than 20; that Mouse Tweaks still works in menus without page items; a compendium page turn in the mods menu screen; the hud keeping its size behind a chest at another scale, hotbar item icons looking sharp there (they are drawn at the chest scale and shrunk), and foecollab's buttons.
