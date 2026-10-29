# tacklebox: logo guidelines

## 1. The logo

- **Idea:** a fish hook, with its eye, cut into a rounded tile. The tackle box is the tile.
- **The tile** is the same rounded square the mod's own screens use. **The hook** carries the accent gradient (cyan to mint). The tile stays dark and quiet.
- **Versions:** symbol (the tile with the hook), small-size cut, horizontal lockup, stacked lockup, wordmark only.
- **Wordmark:** `tacklebox` in custom lowercase letters, drawn as paths. Always lowercase, never capitalised. Nothing to install, nothing to type.

## 2. Clear space

Keep **1 × x** free on every side, where **x = the corner radius of the tile** (about a quarter of the tile's height). It scales with the logo, never a fixed number of pixels.

## 3. Minimum size

| Version | Smallest on screen | Use |
|---|---|---|
| Symbol, full | 48 px | `tacklebox-symbol` / `tacklebox-icon` |
| Symbol, small-size cut | 16 px | `tacklebox-symbol-small` / `tacklebox-icon-small` (heavier hook, no eye hole, no barb) |
| Horizontal lockup | 160 px wide | `tacklebox-horizontal*` |
| Stacked lockup | 100 px wide | `tacklebox-stacked*` |

Below 48 px always switch to the small-size cut. Above 64 px the full symbol.

## 4. Colour

Screen only, so HEX and RGB. No print colours were chosen (no CMYK, no Pantone).

| Name | HEX | RGB | Where |
|---|---|---|---|
| Accent cyan | `#33ccff` | 51, 204, 255 | gradient start (top left of the hook), flat fallback |
| Accent mint | `#00ff99` | 0, 255, 153 | gradient end (bottom right of the hook) |
| Tile / ink | `#0e1318` | 14, 19, 24 | the tile, the wordmark on light backgrounds |
| Tile on dark | `#1a222b` | 26, 34, 43 | the tile on dark backgrounds, so it still shows |
| Paper | `#eef3f6` | 238, 243, 246 | the wordmark on dark backgrounds |

The gradient runs at 45 degrees, cyan top left to mint bottom right, inside the hook only. `tacklebox-symbol-flat` is the flat fallback (hook in `#33ccff`).

**Approved pairs**
- Colour symbol or lockup on white, light grey or photos (the tile is its own container).
- `-dark` lockups on dark backgrounds.
- `-black` on white, `-white` on dark or on a colour: one colour, tile with the hook cut out.

On `#0e1318`-dark surfaces the plain tile blends into the background and only the hook shows. That is fine. Use the `-dark` files when you want the tile visible.

## 5. Which file for what

| Need | File |
|---|---|
| Mod Menu icon (128 px) | `png/tacklebox-icon-128.png` |
| Browser tab | `web/favicon.ico`, `web/favicon.svg` |
| iOS / PWA / maskable | `web/apple-touch-icon.png`, `web/icon-192.png`, `web/icon-512.png`, `web/maskable-512.png` |
| GitHub social preview | `png/tacklebox-social-preview-1280x640.png` |
| README header | `png/tacklebox-horizontal-1200.png` (light) or `…-dark-1200.png` |
| One colour only | `svg/*-black.svg`, `svg/*-white.svg`, `svg/tacklebox-symbol-negative*.svg` |
| Everything else | `svg/` |

## 6. Don'ts

- Don't use the one-colour tools on the colour symbol. They turn it into a solid block. Use the `-negative`, `-black` or `-white` files.
- Don't fill the tile with the gradient, add an outline or a shadow.
- Don't use the full symbol under 48 px.
- Don't stretch, rotate or recolour outside the palette.
- Don't rearrange or resize the parts of a lockup.
- Don't type the wordmark in a font. Use the wordmark files.

## 7. Limits and open items

- No trademark or reverse-image search was done. A fish hook is a common symbol; do a search before relying on the name or mark commercially.
- Reversed (white on dark) versions are not thinned by hand; the hook is bold enough that it looks fine, but check it at small sizes.
- The 128 px icon is wired into the mod as its Mod Menu icon (`assets/tacklebox/icon.png`). After changing the logo, copy the new `png/tacklebox-icon-128.png` over it. See `docs/dev/logo.md`.
- The mockups on the board come from a generic software template, so text like the terminal line is placeholder.

## 8. Rebuild

The SVGs are generated. Geometry is in `src/geom.py` (hook, tile) and `src/letters.py` (wordmark), assembled by `src/build.py`:

```bash
cd branding/src && python3 build.py .. && ./render_all.sh ..
```

`render_all.sh` needs the logo-design skill's `render_png.py` (set `LOGO_SCRIPTS` if it lives elsewhere).
