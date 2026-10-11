"""tacklebox logo geometry: hook, tile, letters. All output is plain path data (no strokes, no text)."""
import math

def pt(cx, cy, r, deg):
    a = math.radians(deg); return (cx + r*math.cos(a), cy + r*math.sin(a))
def n(v): 
    s = f"{v:.2f}".rstrip('0').rstrip('.')
    return s if s not in ('-0','') else '0'
def P(p): return f"{n(p[0])} {n(p[1])}"

def intersect(p, d, q, e):
    """p + s*d == q + t*e -> point"""
    det = d[0]*(-e[1]) - d[1]*(-e[0])
    s = ((q[0]-p[0])*(-e[1]) - (q[1]-p[1])*(-e[0])) / det
    return (p[0]+s*d[0], p[1]+s*d[1]), s

# ---------- hook ----------
# shank on the east, bend clockwise through south; outer edge leaves the bend along a 45 deg tangent,
# inner edge along a 75 deg tangent, so the point is exactly 30 deg and every straight edge is a multiple of 15.
def hook(cx, cy, R, w, eye=None, barb=True, a_out=240, a_in=195, barb_at=15, barb_len=13, ytop=None):
    ri, ro = R - w/2, R + w/2
    I = pt(cx, cy, ri, a_in); O = pt(cx, cy, ro, a_out)
    t_in  = (-math.sin(math.radians(a_in)),  math.cos(math.radians(a_in)))
    t_out = (-math.sin(math.radians(a_out)), math.cos(math.radians(a_out)))
    tip, _ = intersect(O, t_out, I, t_in)
    d = []
    if eye:
        er, ye = eye
        dy = math.sqrt(er*er - (w/2)**2)
        start = (cx + ri, ye + dy)
    else:
        start = (cx + ri, ytop)
    d.append(f"M{P(start)}")
    d.append(f"V{n(cy)}")
    if barb:
        B0 = (I[0] + barb_at*t_in[0], I[1] + barb_at*t_in[1])
        # inner arc stops at I, then climbs the straight inner edge to the barb
        d.append(f"A{n(ri)} {n(ri)} 0 0 1 {P(I)}")
        d.append(f"L{P(B0)}")
        S = (B0[0] + barb_len*math.cos(math.radians(30)), B0[1] + barb_len*math.sin(math.radians(30)))
        up = (math.cos(math.radians(240)), math.sin(math.radians(240)))   # 60 deg above horizontal, leaning left
        B1, _ = intersect(S, up, I, t_in)
        d.append(f"L{P(S)}")
        d.append(f"L{P(B1)}")
    else:
        d.append(f"A{n(ri)} {n(ri)} 0 0 1 {P(I)}")
    d.append(f"L{P(tip)}")
    d.append(f"L{P(O)}")
    d.append(f"A{n(ro)} {n(ro)} 0 1 0 {n(cx+ro)} {n(cy)}")
    if eye:
        d.append(f"V{n(ye+dy)}")
        d.append(f"A{n(er)} {n(er)} 0 1 0 {P(start)}")
    else:
        d.append(f"V{n(ytop)}")
    d.append("Z")
    return " ".join(d), tip

def circle(cx, cy, r):
    return f"M{n(cx-r)} {n(cy)}A{n(r)} {n(r)} 0 1 0 {n(cx+r)} {n(cy)}A{n(r)} {n(r)} 0 1 0 {n(cx-r)} {n(cy)}Z"

def tile(x0=24, y0=24, s=208, r=48):
    x1, y1 = x0+s, y0+s
    return (f"M{x0+r} {y0}H{x1-r}A{r} {r} 0 0 1 {x1} {y0+r}V{y1-r}A{r} {r} 0 0 1 {x1-r} {y1}"
            f"H{x0+r}A{r} {r} 0 0 1 {x0} {y1-r}V{y0+r}A{r} {r} 0 0 1 {x0+r} {y0}Z")

# full drawing
FULL = dict(cx=128, cy=152, R=40, w=26, eye=(16, 64))
HOOK_FULL, TIP_FULL = hook(**FULL)
HOLE_FULL = circle(FULL['cx']+FULL['R'], FULL['eye'][1], 6)
# small-size cut: heavier hook, solid eye, no barb
SMALL = dict(cx=128, cy=150, R=38, w=36, eye=(22, 66), barb=False)
HOOK_SMALL, TIP_SMALL = hook(**SMALL)

DARK = "#0e1318"
CYAN, MINT = "#33ccff", "#00ff99"

def svg(body, vb="0 0 256 256", title="tacklebox logo", defs=""):
    w, h = vb.split()[2:]
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{vb}" width="{w}" height="{h}" role="img" aria-labelledby="t">\n'
            f'  <title id="t">{title}</title>\n{defs}{body}</svg>\n')

def grad(id_="g", x1=76, y1=48, x2=181, y2=205):
    return (f'  <defs>\n    <linearGradient id="{id_}" gradientUnits="userSpaceOnUse" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}">\n'
            f'      <stop offset="0" stop-color="{CYAN}"/>\n      <stop offset="1" stop-color="{MINT}"/>\n    </linearGradient>\n  </defs>\n')

def symbol_color(small=False, vb="0 0 256 256", flat=False):
    hook_d = HOOK_SMALL if small else HOOK_FULL + HOLE_FULL
    fill = CYAN if flat else "url(#g)"
    body = (f'  <rect x="24" y="24" width="208" height="208" rx="48" fill="{DARK}"/>\n'
            f'  <path fill="{fill}" fill-rule="evenodd" d="{hook_d}"/>\n')
    return svg(body, vb, "tacklebox symbol", "" if flat else grad())

def symbol_negative(small=False, fill="#000", vb="0 0 256 256", extra=""):
    hook_d = HOOK_SMALL if small else HOOK_FULL + HOLE_FULL
    return svg(f'  <path fill="{fill}" fill-rule="evenodd" d="{tile()} {hook_d}"/>\n', vb, "tacklebox symbol")

if __name__ == "__main__":
    import sys
    out = sys.argv[1]
    open(f"{out}/symbol.svg","w").write(symbol_color())
    open(f"{out}/symbol-small.svg","w").write(symbol_color(small=True))
    open(f"{out}/negative.svg","w").write(symbol_negative())
    open(f"{out}/negative-small.svg","w").write(symbol_negative(small=True))
    print("tip", TIP_FULL, TIP_SMALL)
