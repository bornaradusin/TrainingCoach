"""A 3D person built from smooth shapes (bones as tapered capsules, muscles as ellipsoids blended
into them), turned into a mesh with marching cubes. Units cm; x towards the back of the machine,
y up, z to the person's right side as seen in the side view (the near side, +z)."""
import math
import numpy as np
from skimage.measure import marching_cubes

L = dict(torso=52, neck=8, head=11, ua=30, fa=26, th=43, sh=42)
SHOULDER_W, HIP_W = 17.5, 9.5


def v(*a): return np.array(a, dtype=float)
def norm(a): n = np.linalg.norm(a); return a / n if n > 1e-9 else a
def d2(a): r = math.radians(a); return v(math.cos(r), math.sin(r), 0)


def dir3(a, b, side):
    """Sagittal angle a (0 back, 90 up, 180 front) and abduction b (out to that side)."""
    ra, rb = math.radians(a), math.radians(b)
    return v(math.cos(rb) * math.cos(ra), math.cos(rb) * math.sin(ra), math.sin(rb) * side)


def joints(p):
    """Pose → 3D joints. Limbs: ua/fa/th/sh (+z side), ua2/fa2/th2/sh2 (-z side); *_b abductions."""
    face = p.get('face', -1)
    hip = v(p['hx'], p['hy'], 0)
    up = d2(p['torso'])
    front = d2(p['torso'] - 90 * face)
    sho = hip + up * L['torso'] + v(0, p.get('sy', 0), 0)
    head_ang = p['torso'] + (90 - p['torso']) * 0.6 if 55 <= p['torso'] <= 125 else p['torso']
    head = sho + d2(head_ang + p.get('nk', 0)) * (L['neck'] + L['head'])
    J = dict(hip=hip, sho=sho, head=head, up=up, front=front, face=face, head_up=d2(head_ang + p.get('nk', 0)))
    for side, sfx in ((1, ''), (-1, '2')):
        g = lambda k, dflt=None: p.get(k + sfx, p.get(k, dflt))

        def dirof(k, b_default):
            vec = g(k + '_v')
            if vec is not None:   # (forward, up, out) in the body's own terms
                return norm(v(face, 0, 0) * vec[0] + v(0, 1, 0) * vec[1] + v(0, 0, side) * vec[2])
            return dir3(g(k), g(k + '_b', b_default), side)
        S = sho + v(0, -3, SHOULDER_W * side) - up * 3
        H = hip + v(0, 0, HIP_W * side)
        tgt = g('ha_t')
        if tgt is not None:
            # two-bone reach: the hand to a point (forward, up, out from the shoulders' middle),
            # the elbow bending towards a hint direction
            body = lambda q: v(face, 0, 0) * q[0] + v(0, 1, 0) * q[1] + v(0, 0, side) * q[2]
            H_ = sho + body(tgt)
            hint = norm(body(g('el_hint', (-0.3, 0.3, 1))))
            dvec = H_ - S; dist = min(np.linalg.norm(dvec), L['ua'] + L['fa'] - 0.5)
            dn = norm(dvec)
            a = (L['ua'] ** 2 - L['fa'] ** 2 + dist ** 2) / (2 * dist)
            h = math.sqrt(max(L['ua'] ** 2 - a * a, 0))
            perp = norm(hint - (hint @ dn) * dn)
            el = S + dn * a + perp * h
            ha = S + dn * dist
        else:
            el = S + dirof('ua', 6) * L['ua']
            ha = el + dirof('fa', 4) * L['fa']
        kn = H + dirof('th', 3) * L['th']
        an = kn + dirof('sh', 0) * L['sh']
        key = 'R' if side == 1 else 'L'
        J.update({'S' + key: S, 'H' + key: H, 'el' + key: el, 'ha' + key: ha, 'kn' + key: kn, 'an' + key: an})
    return J


# ---------- signed distances ----------
def sd_round_cone(P, a, b, r1, r2):
    ba = b - a; l2 = ba @ ba
    pa = P - a
    t = np.clip((pa @ ba) / l2, 0, 1)
    r = r1 + (r2 - r1) * t
    q = pa - t[:, None] * ba
    return np.linalg.norm(q, axis=1) - r


def sd_ellipsoid(P, c, axes, radii):
    q = (P - c) @ np.stack(axes, 1)       # local coords
    k0 = np.linalg.norm(q / radii, axis=1)
    k1 = np.linalg.norm(q / (radii * radii), axis=1)
    return k0 * (k0 - 1.0) / np.maximum(k1, 1e-6)


def smin(a, b, k):
    h = np.clip(0.5 + 0.5 * (b - a) / k, 0, 1)
    return b + (a - b) * h - k * h * (1 - h)


def frame(u, front_hint, side):
    """Axes for a bone: along, front, outward."""
    u = norm(u)
    n = front_hint - (front_hint @ u) * u
    if np.linalg.norm(n) < 1e-3: n = v(0, 1, 0) - u[1] * u
    n = norm(n)
    o = v(0, 0, side); o = o - (o @ u) * u - (o @ n) * n
    if np.linalg.norm(o) < 1e-3: o = np.cross(u, n) * side
    return u, n, norm(o)


# Muscles on each bone: name → (bone, t along, front offset, outward offset, radii along/front/out)
MUSCLES = {
    'chest':      [('torso', 0.79, 6.8, 7.2, (7.8, 3.4, 9.0))],
    'abs':        [('torso', 0.38, 8.2, 0, (13, 2.6, 7.5))],
    'obliques':   [('torso', 0.4, 3, 12.5, (10, 4.5, 2.8))],
    'lats':       [('torso', 0.6, -3.5, 11.5, (14, 5, 4.2))],
    'upper_back': [('torso', 0.8, -9.5, 5.5, (8, 2.6, 5))],
    'traps':      [('torso', 0.98, -4, 6.5, (6, 4, 8))],
    'lower_back': [('torso', 0.25, -8.6, 3.8, (9, 2.6, 3.6))],
    'glutes':     [('torso', 0.0, -7.5, 7, (8.5, 5.5, 8))],
    'hip_flexors': [('torso', 0.06, 8.5, 7, (5, 3, 4))],
    'delts':      [('ua', 0.1, 0, 1.6, (8.5, 6.6, 6.4))],
    'front_delt': [('ua', 0.1, 3, 1, (7.5, 4.2, 5))],
    'rear_delt':  [('ua', 0.1, -3, 1, (7.5, 4.2, 5))],
    'biceps':     [('ua', 0.56, 2.4, 0, (10, 3.6, 3.6))],
    'triceps':    [('ua', 0.45, -2.6, 0.4, (11, 3.9, 4))],
    'forearms':   [('fa', 0.28, 1, 0.5, (8.5, 3.2, 3.6))],
    'quads':      [('th', 0.5, 2.8, 0.6, (17, 5.2, 6.2))],
    'hamstrings': [('th', 0.52, -2.8, 0, (16, 5, 5.4))],
    'calves':     [('sh', 0.3, -2.6, 0, (9.5, 4, 4.2))],
}
# Muscles that always shape the body (whether they're worked or not)
SHAPE = ['chest', 'abs', 'lats', 'traps', 'glutes', 'delts', 'biceps', 'triceps', 'forearms', 'quads', 'hamstrings', 'calves', 'obliques', 'upper_back']


def bones(J):
    """Every bone: name → (start, end, frame)."""
    B = {}
    B['torso'] = {s: (J['hip'], J['sho'], frame(J['up'], J['front'], s)) for s in (1, -1)}
    for s, k in ((1, 'R'), (-1, 'L')):
        for name, a, b in (('ua', 'S', 'el'), ('fa', 'el', 'ha'), ('th', 'H', 'kn'), ('sh', 'kn', 'an')):
            A, Bp = J[a + k], J[b + k]
            B.setdefault(name, {})[s] = (A, Bp, frame(Bp - A, J['front'], s))
    return B


def muscle_ellipsoids(J, name):
    out = []
    Bs = bones(J)
    for (bone, t, fo, oo, radii) in MUSCLES[name]:
        for s in (1, -1):
            A, Bp, (u, n, o) = Bs[bone][s]
            c = A + (Bp - A) * t + n * fo + o * oo
            out.append((c, (u, n, o), np.array(radii, float)))
    return out


def body_sdf(P, J):
    """Distance to the skin; also returns the shorts and shoe masks' distances."""
    up, fr = J['up'], J['front']
    side = norm(np.cross(up, fr)) * (1 if J['face'] < 0 else -1)
    if side[2] < 0: side = -side
    tor = lambda t: J['hip'] + (J['sho'] - J['hip']) * t
    ax = (up, fr, side)
    d = sd_ellipsoid(P, tor(0.68) - fr * 0.5, ax, np.array([16, 10.5, 15.5]))
    d = smin(d, sd_ellipsoid(P, tor(0.36) + fr * 0.5, ax, np.array([13, 9.5, 13])), 6)
    d = smin(d, sd_ellipsoid(P, tor(0.06) - fr * 0.5, ax, np.array([11, 10.5, 16.5])), 6)
    # neck, head, face
    d = smin(d, sd_round_cone(P, J['sho'] - up * 2, J['head'] - J['head_up'] * 6, 6, 5), 3)
    hu = J['head_up']; hf = norm(fr - (fr @ hu) * hu)
    hax = (hu, hf, norm(np.cross(hu, hf)))
    d = smin(d, sd_ellipsoid(P, J['head'], hax, np.array([11.5, 9.8, 8.2])), 2.5)
    d = smin(d, sd_ellipsoid(P, J['head'] - hu * 6.5 + hf * 2.5, hax, np.array([5.5, 6.5, 6])), 2.5)     # jaw
    d = smin(d, sd_ellipsoid(P, J['head'] + hf * 9.3 - hu * 1.5, hax, np.array([2.4, 1.6, 1.2])), 1)     # nose
    for s in (1, -1):
        d = smin(d, sd_ellipsoid(P, J['head'] + hax[2] * 8.2 * s - hf * 0.5, hax, np.array([3, 1.4, 1])), 0.8)  # ears
    shorts = sd_ellipsoid(P, tor(0.05) - fr * 0.5, ax, np.array([13.5, 12.5, 18.5]))
    shoes = None
    for s, k in ((1, 'R'), (-1, 'L')):
        d = smin(d, sd_round_cone(P, J['S' + k], J['el' + k], 5.2, 4.2), 2)
        d = smin(d, sd_round_cone(P, J['el' + k], J['ha' + k], 4.2, 2.7), 1.5)
        d = smin(d, sd_round_cone(P, J['H' + k], J['kn' + k], 8, 5.2), 3)
        d = smin(d, sd_round_cone(P, J['kn' + k], J['an' + k], 4.9, 3.1), 2)
        # hand: a mitten pointing along the forearm
        fu = norm(J['ha' + k] - J['el' + k]); u, n, o = frame(fu, fr, s)
        d = smin(d, sd_ellipsoid(P, J['ha' + k] + fu * 5.5, (u, n, o), np.array([6.5, 2.4, 4.4])), 1.5)
        # foot / shoe
        sd_ = norm(J['an' + k] - J['kn' + k]); _, ff, _ = frame(sd_, fr, s)
        ff = norm(ff - (ff @ sd_) * sd_ * 0.0)
        fax = frame(ff, -sd_, s)
        cen = J['an' + k] + sd_ * 5 + ff * 6
        foot = sd_ellipsoid(P, cen, fax, np.array([13, 4.6, 5]))
        d = smin(d, foot, 2)
        shoes = foot if shoes is None else np.minimum(shoes, foot)
        th = sd_round_cone(P, J['H' + k], J['H' + k] + (J['kn' + k] - J['H' + k]) * 0.45, 9.5, 8.5)
        shorts = np.minimum(shorts, th)
    for (c, axes, radii) in muscle_ellipsoids(J, 'glutes'):
        shorts = np.minimum(shorts, sd_ellipsoid(P, c, axes, radii * 1.25))
    for m in SHAPE:
        for (c, axes, radii) in muscle_ellipsoids(J, m):
            d = smin(d, sd_ellipsoid(P, c, axes, radii), 3.0 if m in ('chest', 'lats', 'traps', 'glutes', 'abs', 'upper_back', 'obliques') else 1.8)
    return d, shorts, shoes


def mesh(J, step=1.0, pad=14):
    pts = np.array([J[k] for k in J if isinstance(J[k], np.ndarray) and J[k].shape == (3,) and k not in ('up', 'front', 'head_up')])
    lo = pts.min(0) - pad; hi = pts.max(0) + pad
    xs = [np.arange(lo[i], hi[i] + step, step) for i in range(3)]
    G = np.stack(np.meshgrid(*xs, indexing='ij'), -1).reshape(-1, 3)
    d, _, _ = body_sdf(G, J)
    vol = d.reshape(len(xs[0]), len(xs[1]), len(xs[2]))
    V, F, _, _ = marching_cubes(vol, 0, spacing=(step, step, step))
    V = V + lo
    # normals from the distance field itself (smooth and pointing out)
    e = 0.5; N = np.zeros_like(V)
    for i in range(3):
        o = np.zeros(3); o[i] = e
        N[:, i] = body_sdf(V + o, J)[0] - body_sdf(V - o, J)[0]
    N /= np.maximum(np.linalg.norm(N, axis=1, keepdims=True), 1e-9)
    return V, F, N


def colours(V, N, J, mus):
    """Skin, shorts, shoes, hair, and the worked muscles in red (main) / orange (also)."""
    SKIN = np.array([228, 184, 150.]); SHORTS = np.array([40, 56, 88.]); SHOE = np.array([36, 36, 42.])
    HAIR = np.array([58, 40, 30.]); MAIN = np.array([212, 48, 30.]); ALSO = np.array([240, 132, 66.])
    C = np.tile(SKIN, (len(V), 1))
    _, shorts, shoes = body_sdf(V, J)
    ws = np.clip((1.0 - shorts) / 0.8, 0, 1)[:, None]; C = C * (1 - ws) + SHORTS * ws
    wf = np.clip((1.2 - shoes) / 0.8, 0, 1)[:, None]; C = C * (1 - wf) + SHOE * wf
    hu = J['head_up']; fr = J['front']; hf = norm(fr - (fr @ hu) * hu)
    rel = V - J['head']
    hair = (rel @ hu > 3.0 - 6.5 * np.clip(-(rel @ hf) / 9, 0, 1)) & (np.linalg.norm(rel, axis=1) < 15) & (rel @ hf < 6.5)
    C[hair] = HAIR
    for names, col in ((mus.get('also', []), ALSO), (mus.get('main', []), MAIN)):
        for m in names:
            if m not in MUSCLES: continue
            for (c, axes, radii) in muscle_ellipsoids(J, m):
                q = (V - c) @ np.stack(axes, 1)
                e = np.linalg.norm(q / (radii * 1.18), axis=1)
                w = np.clip((1.0 - e) / 0.18, 0, 1)[:, None] * 0.92
                ok = (shorts >= 0.6)[:, None]
                C = np.where(ok, C * (1 - w) + col * w, C)
    return C
