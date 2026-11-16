"""The IM-309 in 3D, from the measured side view (../machine.py) given widths (z, cm).
Everything becomes small triangles with a colour, for the renderer."""
import math
import numpy as np

FRAME = (58, 60, 66); PAD = (34, 34, 38); ROLLER = (30, 30, 34); CHROME = (176, 178, 186)
PLATE = (40, 40, 44); PLATE_UP = (226, 112, 36); PULLEY = (120, 120, 128); CABLE = (30, 30, 30)
ACTIVE = (24, 150, 86)


def v(*a): return np.array(a, float)


class Mesh:
    def __init__(self): self.V, self.F, self.C = [], [], []

    def tri(self, a, b, c, col):
        i = len(self.V); self.V += [a, b, c]; self.F.append((i, i + 1, i + 2)); self.C.append(col)

    def quad(self, a, b, c, d, col, n=1):
        # split into n x n so the painter's sort stays right
        for i in range(n):
            for j in range(n):
                s0, s1, t0, t1 = i / n, (i + 1) / n, j / n, (j + 1) / n
                P = lambda s, t: a + (b - a) * s + (d - a) * t + (a - b + c - d) * s * t
                self.tri(P(s0, t0), P(s1, t0), P(s1, t1), col); self.tri(P(s0, t0), P(s1, t1), P(s0, t1), col)

    def box(self, lo, hi, col, cell=8):
        lo, hi = v(*lo), v(*hi)
        x0, y0, z0 = lo; x1, y1, z1 = hi
        P = [v(x0, y0, z0), v(x1, y0, z0), v(x1, y1, z0), v(x0, y1, z0), v(x0, y0, z1), v(x1, y0, z1), v(x1, y1, z1), v(x0, y1, z1)]
        size = max(hi - lo)
        n = max(1, int(math.ceil(size / cell)))
        for f in ((0, 3, 2, 1), (4, 5, 6, 7), (0, 1, 5, 4), (2, 3, 7, 6), (1, 2, 6, 5), (0, 4, 7, 3)):
            self.quad(*[P[i] for i in f], col, n)

    def bar(self, a, b, w, depth, col, zc=0.0, cell=8):
        """A beam along a→b (in x,y), w thick, depth wide in z."""
        a, b = v(a[0], a[1], 0), v(b[0], b[1], 0)
        u = b - a; L = np.linalg.norm(u); u /= L; n = v(-u[1], u[0], 0) * w / 2
        z0, z1 = v(0, 0, zc - depth / 2), v(0, 0, zc + depth / 2)
        P = [a - n + z0, b - n + z0, b + n + z0, a + n + z0, a - n + z1, b - n + z1, b + n + z1, a + n + z1]
        k = max(1, int(math.ceil(L / cell)))
        for f in ((0, 3, 2, 1), (4, 5, 6, 7), (0, 1, 5, 4), (2, 3, 7, 6), (1, 2, 6, 5), (0, 4, 7, 3)):
            self.quad(*[P[i] for i in f], col, k)

    def tube(self, a, b, r, col, seg=12, cell=6):
        a, b = v(*a), v(*b); u = b - a; L = np.linalg.norm(u)
        if L < 1e-6: return
        u /= L; t = v(1, 0, 0) if abs(u[0]) < 0.9 else v(0, 1, 0)
        p = np.cross(u, t); p /= np.linalg.norm(p); q = np.cross(u, p)
        k = max(1, int(math.ceil(L / cell)))
        ring = lambda c, i: c + (p * math.cos(2 * math.pi * i / seg) + q * math.sin(2 * math.pi * i / seg)) * r
        for j in range(k):
            c0, c1 = a + (b - a) * j / k, a + (b - a) * (j + 1) / k
            for i in range(seg):
                self.quad(ring(c0, i), ring(c0, i + 1), ring(c1, i + 1), ring(c1, i), col)
        for c in (a, b):
            for i in range(seg): self.tri(c, ring(c, i), ring(c, i + 1), col)

    def arrays(self):
        return np.array(self.V), np.array(self.F), np.array(self.C, float)


def machine(state):
    """state: lift (0..1 of travel), travel cm, press handles (two 3D points or None), lever ankle
    point or None, cables [(a, b)], bar/rope attachments."""
    m = Mesh()
    # base
    m.box((-4, 0, -48), (8, 6, 48), FRAME)
    m.box((180, 0, -48), (196, 6, 48), FRAME)
    m.box((-2, 0, -4), (196, 4, 4), FRAME)
    # weight stack: frame, rods, plates
    for x in (152, 186.5):
        m.box((x, 4, -22), (x + 3.5, 152, -18), FRAME); m.box((x, 4, 18), (x + 3.5, 152, 22), FRAME)
    m.box((152, 148, -22), (190, 152, 22), FRAME)
    for z in (-12, 12): m.tube((171, 8, z), (171, 150, z), 0.9, CHROME, seg=8, cell=20)
    y = 6.0; lift = state.get('lift', 0) * state.get('travel', 0)
    top = None
    for i in range(12):
        rising = i >= 7
        dy = lift if rising else 0
        m.box((156, y + dy, -17), (186, y + dy + 3.2, 17), PLATE_UP if rising and lift > 0 else PLATE)
        y += 3.55
    top = v(171, y + lift, 0)
    m.tube((171, y + lift - 6, 0), (171, y + lift + 2, 0), 1.2, CHROME, seg=8)
    # main frame
    m.bar((90, 4), (131, 172), 6, 6, FRAME)
    m.bar((60, 201), (190, 152), 6, 6, FRAME)
    m.bar((97, 150), (97, 191), 5, 5, FRAME)
    m.bar((73, 196), (73, 190), 2, 3, FRAME)
    m.bar((35, 4), (35, 82), 5, 5, FRAME)
    m.bar((35, 42), (86, 42), 4, 5, FRAME)
    m.bar((70, 42), (80, 4), 4, 5, FRAME)
    m.bar((43, 60), (52, 42), 3, 4, FRAME)
    # seat, backrest, preacher pad
    m.box((52, 50, -15), (89, 58, 15), PAD)
    m.bar((100, 58), (118, 133), 10, 28, PAD)
    m.bar((22, 75), (48, 91), 8, 34, PAD)
    # upper rollers of the leg station
    m.tube((34, 63, -24), (34, 63, -3), 5, ROLLER); m.tube((34, 63, 3), (34, 63, 24), 5, ROLLER)
    m.tube((34, 63, -3), (34, 63, 3), 1.2, CHROME)
    # pulleys
    for (x, yy, r) in ((73, 186, 4), (126, 176, 3.5), (176, 158, 3.5), (121, 138, 3), (8, 9, 3.5)):
        m.tube((x, yy, -1.2), (x, yy, 1.2), r, PULLEY, seg=14)
    # leg lever (down at rest unless moving)
    # roller: where the lower rollers are (at rest, at the ankle, or behind the heel)
    piv = v(43, 60, 0)
    if state.get('roller') is not None:
        r = state['roller']; col = ACTIVE
    elif state.get('lever') is not None:
        a = state['lever']; r = v(a[0] - 6, a[1], 0); col = ACTIVE
    else:
        r = v(37, 18, 0); col = FRAME
    m.tube(piv + v(0, 0, -3), piv + v(0, 0, 3), 2.2, CHROME)
    m.tube(piv, v(r[0] + 6 * (1 if r[0] < piv[0] else -1) * 0, r[1], 0), 1.6, col, seg=8)
    m.tube((r[0], r[1], -24), (r[0], r[1], -3), 5, ROLLER)
    m.tube((r[0], r[1], 3), (r[0], r[1], 24), 5, ROLLER)
    m.tube((r[0], r[1], -3), (r[0], r[1], 3), 1.2, CHROME)
    # press arms: from their pivots down to the handles
    hs = state.get('press'); pads = state.get('pads')
    for s, z in ((1, 9), (-1, -9)):
        piv = v(97, 152, z)
        if pads is not None:
            el, ha = pads[0] if s == 1 else pads[1]
            mid = (el + ha) / 2
            m.tube(piv, mid + v(3, 0, 0), 1.8, ACTIVE, seg=10)
            m.tube(el + v(0, -2, 0), ha + v(0, 4, 0), 4.2, PAD, seg=12)
        elif hs is not None:
            h = hs[0] if s == 1 else hs[1]
            m.tube(piv, h + v(0, 6, 0), 1.8, ACTIVE, seg=10)
            m.tube(h + v(0, 6, 0), h + v(0, -6, 0), 1.6, ACTIVE, seg=10)
        else:
            rest = v(88, 100, 24 * s)
            m.tube(piv, rest, 1.8, FRAME, seg=10)
            m.box((84, 106, 24 * s - 4), (91, 136, 24 * s + 4), PAD)
    for (a, b) in state.get('cables', []):
        m.tube(a, b, 0.45, CABLE, seg=6, cell=15)
    for (a, b) in state.get('ropes', []):
        m.tube(a, b, 1.1, ACTIVE, seg=8)
    for (a, b, r) in state.get('bars', []):
        m.tube(a, b, r, ACTIVE, seg=10)
    return m.arrays(), top
