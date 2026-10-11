"""Custom lowercase letters for the tacklebox wordmark. Ascender height 100, x-height 72, y down, baseline y=100.
Positive shapes are clockwise, holes counter-clockwise, one path per letter (nonzero fill)."""
import math
from geom import n, P, intersect

H, S, HZ = 100, 18, 16          # cap height, vertical stem, horizontal bar
def area(pts): return sum(pts[i][0]*pts[(i+1)%len(pts)][1]-pts[(i+1)%len(pts)][0]*pts[i][1] for i in range(len(pts)))/2
def poly(pts):
    if area(pts) < 0: pts = pts[::-1]                 # y-down: positive area = clockwise on screen
    return "M" + "L".join(P(p) for p in pts) + "Z"
def rect(x0, y0, x1, y1): return poly([(x0,y0),(x1,y0),(x1,y1),(x0,y1)])
def band(p0, p1, th):
    """parallelogram: centre line p0->p1, horizontal thickness th, horizontal cuts at both ends"""
    return poly([(p0[0]-th/2,p0[1]),(p0[0]+th/2,p0[1]),(p1[0]+th/2,p1[1]),(p1[0]-th/2,p1[1])])
def ell(cx, cy, rx, ry, cw=True):
    sw = 1 if cw else 0
    return f"M{n(cx-rx)} {n(cy)}A{n(rx)} {n(ry)} 0 1 {sw} {n(cx+rx)} {n(cy)}A{n(rx)} {n(ry)} 0 1 {sw} {n(cx-rx)} {n(cy)}Z"

XH = 28                          # top of the x-height band
RX, RY, CY = 36, 37.5, 64        # round letters: outer radii and centre (1.5 overshoot top and bottom)

def ring(cx): return ell(cx, CY, RX, RY) + ell(cx, CY, RX-S, RY-HZ, cw=False)
def t():
    x0, Ro, y0 = 12, 30, 10
    foot = (f"M{x0} {y0}H{x0+S}V{100-Ro}A{Ro-S} {Ro-HZ} 0 0 0 {x0+Ro} {100-HZ}H{x0+Ro+12}V100H{x0+Ro}"
            f"A{Ro} {Ro} 0 0 1 {x0} {100-Ro}Z")
    return x0+Ro+12, foot + rect(0, XH, x0+S+16, XH+HZ)
def a():
    return 2*RX, ring(RX) + rect(2*RX-S, XH, 2*RX, 100)
def b():
    return 2*RX, ring(RX) + rect(0, 0, S, 100)
def o():
    return 2*RX, ring(RX)
def c():
    cx = RX; rxi, ryi = RX-S, RY-HZ; k = math.radians(45)
    po = (cx+RX*math.cos(k), CY-RY*math.sin(k)); qo = (cx+RX*math.cos(k), CY+RY*math.sin(k))
    pi_ = (cx+rxi*math.cos(k), CY-ryi*math.sin(k)); qi = (cx+rxi*math.cos(k), CY+ryi*math.sin(k))
    return 2*RX, f"M{P(po)}A{n(RX)} {n(RY)} 0 1 0 {P(qo)}L{P(qi)}A{n(rxi)} {n(ryi)} 0 1 1 {P(pi_)}Z"
def e():
    cx = RX; rxi, ryi = RX-S, RY-HZ; k = math.radians(45)
    yb = CY + HZ/2                                  # underside of the bar; the ring is cut off here on the right
    qo = (cx+RX*math.cos(k), CY+RY*math.sin(k)); qi = (cx+rxi*math.cos(k), CY+ryi*math.sin(k))
    ao = (cx+RX*math.sqrt(1-((yb-CY)/RY)**2), yb); ai = (cx+rxi*math.sqrt(1-((yb-CY)/ryi)**2), yb)
    sector = (f"M{P(qo)}A{n(RX)} {n(RY)} 0 1 1 {P(ao)}L{P(ai)}A{n(rxi)} {n(ryi)} 0 1 0 {P(qi)}Z")
    return 2*RX, sector + rect(S-8, CY-HZ/2, cx+rxi+4, yb)
def k():
    ang = math.radians(55); th = 18.5/math.sin(ang)
    J = (S+7, 68)
    top = (J[0] + (J[1]-XH)/math.tan(ang), XH)
    bot = (J[0] + (100-J[1])/math.tan(ang), 100)
    return max(top[0], bot[0]) + th/2, rect(0, 0, S, 100) + band(J, top, th) + band(J, bot, th)
def l():
    return S, rect(0, 0, S, 100)
def x():
    ang = math.radians(60); dx = (100-XH)/math.tan(ang); th = 18.5/math.sin(ang)
    return dx+th, band((th/2, XH), (th/2+dx, 100), th) + band((th/2+dx, XH), (th/2, 100), th)

LETTERS = {"t": t, "a": a, "c": c, "k": k, "l": l, "e": e, "b": b, "o": o, "x": x}

def shift(d, dx, dy=0):
    """translate a path made only of absolute M/L/H/V/A/Z commands"""
    import re
    out=[]; toks = re.findall(r"[MLHVAZ]|-?\d*\.?\d+", d); i=0; cmd=None
    while i < len(toks):
        t = toks[i]
        if t in "MLHVAZ":
            cmd=t; out.append(t); i+=1
            if t=="Z": continue
        if cmd in "ML":   out.append(f"{n(float(toks[i])+dx)} {n(float(toks[i+1])+dy)}"); i+=2
        elif cmd=="H":    out.append(n(float(toks[i])+dx)); i+=1
        elif cmd=="V":    out.append(n(float(toks[i])+dy)); i+=1
        elif cmd=="A":
            v=toks[i:i+7]; out.append(f"{v[0]} {v[1]} {v[2]} {v[3]} {v[4]} {n(float(v[5])+dx)} {n(float(v[6])+dy)}"); i+=7
    # re-join with separators that are unambiguous
    s=""
    for o in out:
        if o in "MLHVAZ": s+=o
        else: s+=o+" "
    return s.strip()

TRACK = 18
PAIR = {("t","a"):-2, ("c","k"):-6, ("k","l"):-4, ("b","o"):-2, ("o","x"):-2}
def wordmark(word="tacklebox"):
    x = 0; parts = []; prev = None
    for ch in word:
        w, d = LETTERS[ch]()
        if prev: x += TRACK + PAIR.get((prev,ch), 0)
        parts.append(shift(d, x))
        x += w; prev = ch
    return x, "".join(parts)

if __name__ == "__main__":
    import sys
    w, d = wordmark()
    open(sys.argv[1],"w").write(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="-20 -20 {n(w+40)} 140" width="{n(w+40)*2}" height="280"><rect x="-20" y="-20" width="{n(w+40)}" height="140" fill="#fff"/><path fill="#0e1318" d="{d}"/></svg>')
    print("width", w)
