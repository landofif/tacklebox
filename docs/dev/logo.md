# Logo (2026-10-12)

The mod's logo kit lives in `branding/`, outside `src/`. Usage rules, colours, minimum sizes and file picker: `branding/GUIDELINES.md`.

## What it is

A fish hook, with its eye, cut into a rounded tile ("hook in a box"). The hook carries the accent gradient `#33ccff` to `#00ff99`, the tile is `#0e1318`. Wordmark is `tacklebox` in custom lowercase letters (l asked for lowercase, not capitals), drawn as paths (no font). Chosen over a T-shaped hook and a dot-matrix hook.

## Key files

- `branding/svg/`: symbol (colour, flat, small-size cut, one-colour negative), edge-to-edge `tacklebox-icon*`, wordmark, horizontal and stacked lockups (light, dark, black, white), maskable icon, GitHub social preview.
- `branding/png/`, `branding/web/`: rendered icons (16 to 1024 px), `favicon.ico`, apple-touch, 192/512, maskable.
- `branding/presentation/`: board with mockups (`board.html`, `slides/`).
- `branding/src/`: generator. `geom.py` (hook, tile), `letters.py` (wordmark), `build.py` (all SVGs), `render_all.sh` (PNG/ICO set).

## How it works

SVGs are generated, not hand-edited: change the geometry in `src/`, run `cd branding/src && python3 build.py .. && ./render_all.sh ..`. The hook uses exact angles (outer edge 30 degrees, inner edge 75 degrees, barb 30 and 60 degrees). Below 48 px use the small-size cut (heavier hook, no eye hole, no barb).

## Gotchas

- One-colour conversion tools turn the colour symbol into a solid block. The one-colour files are built from `tacklebox-symbol-negative`, a single path with the hook cut out (`fill-rule="evenodd"`).
- The tile (`#0e1318`) vanishes on dark surfaces of the same colour; the `-dark` files use `#1a222b`.
- `render_all.sh` needs the logo-design skill's `render_png.py` (`LOGO_SCRIPTS` overrides the path).
- No CMYK or Pantone values: screen use only. No trademark search was done.

## Mod Menu icon

`src/main/resources/assets/tacklebox/icon.png` is a copy of `branding/png/tacklebox-icon-128.png`, referenced by the `icon` entry in `fabric.mod.json`. After changing the logo, rebuild the kit and copy the 128 px PNG over it. In the built jar (checked). Not run in game: how the icon looks in the Mod Menu list at its real size.

## Tests

None. The kit was checked by rendering and looking: `svg_audit.py` on every SVG, the preview test sheet (size ladder, pixel test at 16/32/48, one-colour, reversed, mirrored, rotated, backgrounds) and the 16/24/32/48 px renders.
