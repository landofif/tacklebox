# Travel map

`/tt`, `/travel` and `/turbotravel` as a world map (luna, 2026-11-15): the continents drawn in code, every place of the server's travel menu at its spot in the world. Off by default (`travelMap`, settings > menus > "travel map").

Credit: the travel menu as a world map is FOE+'s idea (`itshaxr/FOE-Plus`, `TravelMapHandler`), which builds on FOE-R (DannyPX) and FOECollab (Kimberlyow). The outlines, the positions and the code are tacklebox's own; nothing was copied, and there is no picture file.

## Key files

- `menu/TravelMap`: pure (the tests run it). The outlines (`LAND`, `WATER` for the black and caspian seas) as longitude/latitude pairs, a pin per location (`PINS`, ids as in `Locations.ALL`), the view (`View`: zoom 1 to 8, pan, clamped to the map's edges), the row crossings the land is filled with, pushing crowded pins apart, the hit test.
- `screen/TravelMapScreen`: draws it over the server's chest like `FishMenuScreen` does (it reads the real slots), sends the clicks.
- `menu/FishMenu.wantsMap` + `mixin/MinecraftClientMixin`: the swap, asked before the tile menu's `wants`.

## How it works

- **When**: the chest that opens within 4 s after you typed `/tt`, `/travel` or `/turbotravel`, on FishOnMC, with the setting on. A travel menu opened another way (the travel npc) stays what it was, like the tile menu. `/tt` was added to the commands the tile menu takes too.
- **Places**: every menu item named after a location (`Locations.byName`) is drawn as that item at its pin, so it shows the picture the server pack gives it (the pack has one per location under `customitems/locations/`; the items are glowstone dust). Under it the name, where no other pin or name is in the way. Hover shows the server's own tooltip.
- **Clicks**: a left click on a place is your click on that menu item (travel), a right click too (the server opens its species). Sent as a slot click, as in the tile menu; nothing is sent by itself.
- **Marks**: a place in a frenzy has a green disc and name, the fabled event's place a red one (`Locations.mark`, as in the tiles), the place you are at one in the accent, the hovered one a faint white one.
- **Zoom and pan**: the wheel zooms around the mouse (1.25 a notch, 1x to 8x), a left drag on the sea or land pans.
- **Crowded pins**: pins closer than 19 px are pushed apart (`TravelMap.spread`), so the five places around the Gulf of Mexico can all be clicked with the whole world in view. Cypress Lake sits at Caddo Lake (the server's tooltip says so), right next to Toledo Bend. Zoomed in they are at their real spots.
- **Places the map does not know** (a location added later): menu items with `fastTravel` in their custom_data and no pin sit in a row along the bottom of the map and work the same.
- **Tile menu button**: bottom left, opens the same menu as tiles (`FishMenuScreen`). Escape and E close.
- The map is a plain longitude/latitude grid, 180 west to 180 east, 78 north to 58 south (no Antarctica). The land is filled one pixel row at a time from the outline's crossings.

## Gotchas

- From the map, a right click's species menu opens in the fishonmc menu screen when `fishMenu` is on (`FishMenu.wants` treats the map like the tile menu for chaining), otherwise as the chest.
- The outlines are rough on purpose (about 30 polygons).

## Tests

`menu/TravelMapTest` (a pin per location, land and sea by sample points, land pins on land and sea pins at sea, crossings, zoom around the mouse, drag limits, spreading, all pins clickable at 1x), `TravelMapIntegrationTest` (the names of luna's /travel dump onto the map, the commands, the setting).

Not run in game yet: the whole screen. To check: that `/tt` is a real server command and the map opens for it, that the pins show the location pictures and not glowstone dust, the names, the tooltip, that left click travels and right click opens the species, zoom and drag feel, the marks, the tile menu button.
