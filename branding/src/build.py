import os, sys, math
from geom import *
import letters

OUT = sys.argv[1]
os.makedirs(f"{OUT}/svg", exist_ok=True)
def w(name, text): open(f"{OUT}/svg/{name}", "w").write(text)

INK, PAPER = DARK, "#eef3f6"
TILE_ON_DARK = "#1a222b"
WW, WD = letters.wordmark()          # width, path (ascender height 100, baseline y=100)

# ---- symbols
w("tacklebox-symbol.svg", symbol_color())
w("tacklebox-symbol-flat.svg", symbol_color(flat=True))
w("tacklebox-symbol-small.svg", symbol_color(small=True))
w("tacklebox-symbol-negative.svg", symbol_negative())
w("tacklebox-symbol-negative-small.svg", symbol_negative(small=True))
# tile edge to edge (mod menu icon, favicon, avatar): crop the viewBox to the tile
w("tacklebox-icon.svg", symbol_color(vb="24 24 208 208"))
w("tacklebox-icon-small.svg", symbol_color(small=True, vb="24 24 208 208"))

# ---- wordmark
def wordmark_svg(fill, title="tacklebox"):
    return svg(f'  <path fill="{fill}" d="{WD}"/>\n', f"-6 -6 {math.ceil(WW+12)} 112", title)
w("tacklebox-wordmark.svg", wordmark_svg(INK))
w("tacklebox-wordmark-light.svg", wordmark_svg(PAPER))
w("tacklebox-wordmark-black.svg", wordmark_svg("#000"))
w("tacklebox-wordmark-white.svg", wordmark_svg("#fff"))

# ---- lockups
K = 1.0                       # wordmark scale beside a 208 tile (ascender height 100, x-height 72)
WM_MID = 62                   # y of the wordmark's optical middle (between the x-height band and the ascenders)
GAP = 56
def sym_group(kind, tile_fill=DARK, ink="#000"):
    """the symbol on its own 24..232 tile, as a <g> to paste into a lockup (shifted so the tile starts at 0,0)"""
    if kind == "color":
        d = (f'<rect x="24" y="24" width="208" height="208" rx="48" fill="{tile_fill}"/>'
             f'<path fill="url(#g)" fill-rule="evenodd" d="{HOOK_FULL}{HOLE_FULL}"/>')
    else:
        d = f'<path fill="{ink}" fill-rule="evenodd" d="{tile()} {HOOK_FULL}{HOLE_FULL}"/>'
    return f'  <g transform="translate(-24 -24)">{d}</g>\n'

def horizontal(kind, word_fill, tile_fill=DARK, title="tacklebox"):
    width = 208 + GAP + WW*K
    body = (sym_group(kind, tile_fill, word_fill) +
            f'  <path fill="{word_fill}" transform="translate({n(208+GAP)} {n(104-WM_MID*K)}) scale({K})" d="{WD}"/>\n')
    return svg(body, f"0 0 {math.ceil(width)} 208", title, grad() if kind == "color" else "")

def stacked(kind, word_fill, tile_fill=DARK, title="tacklebox"):
    k = 0.84; ww = WW*k
    width = ww
    body = (f'  <g transform="translate({n((ww-208)/2)} 0)">\n{sym_group(kind, tile_fill, word_fill)}  </g>\n' +
            f'  <path fill="{word_fill}" transform="translate(0 {n(208+44)}) scale({k})" d="{WD}"/>\n')
    return svg(body, f"0 0 {math.ceil(width)} {math.ceil(208+44+100*k)}", title, grad() if kind == "color" else "")

w("tacklebox-horizontal.svg", horizontal("color", INK))
w("tacklebox-horizontal-dark.svg", horizontal("color", PAPER, TILE_ON_DARK))
w("tacklebox-horizontal-black.svg", horizontal("neg", "#000"))
w("tacklebox-horizontal-white.svg", horizontal("neg", "#fff"))
w("tacklebox-stacked.svg", stacked("color", INK))
w("tacklebox-stacked-dark.svg", stacked("color", PAPER, TILE_ON_DARK))
w("tacklebox-stacked-black.svg", stacked("neg", "#000"))
w("tacklebox-stacked-white.svg", stacked("neg", "#fff"))

# ---- maskable / apple-touch: full-bleed dark square, hook scaled into the safe zone
mask = (f'  <rect width="256" height="256" fill="{DARK}"/>\n'
        f'  <g transform="translate(128 126) scale(0.9) translate(-128 -126)"><path fill="url(#g)" fill-rule="evenodd" d="{HOOK_SMALL}"/></g>\n')
w("tacklebox-icon-maskable.svg", svg(mask, "0 0 256 256", "tacklebox icon", grad()))

# ---- GitHub social preview 1280x640: dark ground, light lockup centred
kk = 1.0; lw = 208 + GAP + WW*K
sc = 1000/lw
soc = (f'  <rect width="1280" height="640" fill="{DARK}"/>\n'
       f'  <g transform="translate({n((1280-lw*sc)/2)} {n((640-208*sc)/2)}) scale({n(sc)})">\n'
       + sym_group("color", TILE_ON_DARK) +
       f'  <path fill="{PAPER}" transform="translate({n(208+GAP)} {n(104-WM_MID*K)}) scale({K})" d="{WD}"/>\n  </g>\n')
w("tacklebox-social-preview.svg", svg(soc, "0 0 1280 640", "tacklebox", grad()))
print("wordmark width", WW)
