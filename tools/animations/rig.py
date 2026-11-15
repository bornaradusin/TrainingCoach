"""The person, to scale (about 1.75 m), and the pose maths. The app's app/src/main/java/.../anim/Rig.kt is the same."""
import math
L = dict(torso=52, neck=8, head=11, ua=30, fa=26, th=43, sh=42, foot=22, hand=4.5)
W = dict(torso=24, ua=10, fa=8, th=15, sh=11, foot=7, neck=9)      # thickness, side view
WF = dict(torso=34, ua=10, fa=8, th=15, sh=11, foot=8, neck=11)    # thickness, front view
SHOULDERS, HIPS = 19, 10                                           # half widths, front view
ANGLES = ('torso', 'ua', 'fa', 'th', 'sh', 'ua2', 'fa2', 'th2', 'sh2')

def d(a):   # angle in degrees, 0 = towards the back (+x), 90 = up
    return math.cos(math.radians(a)), math.sin(math.radians(a))

def lerp_angle(a, b, t):
    diff = (b - a + 540) % 360 - 180
    return a + diff * t

def pose_at(s, e, t):
    out = {}
    for k in set(s) | set(e):
        a, b = s.get(k, e.get(k)), e.get(k, s.get(k))
        if isinstance(a, str): out[k] = a; continue
        out[k] = lerp_angle(a, b, t) if k in ANGLES else a + (b - a) * t
    return out

def add(p, v, n): return (p[0] + v[0] * n, p[1] + v[1] * n)

def joints(p, view):
    hip = (p['hx'], p['hy'])
    sho = add(hip, d(p['torso']), L['torso']); sho = (sho[0], sho[1] + p.get('sy', 0))
    head = add(sho, d(p['torso'] + p.get('nk', 0)), L['neck'] + L['head'])
    J = dict(hip=hip, sho=sho, head=head, face=p.get('face', -1))
    def limb(a, ang1, l1, ang2, l2, s1=1.0, mirror=False):
        u, f = d(ang1), d(ang2)
        if mirror: u, f = (-u[0], u[1]), (-f[0], f[1])
        m = (a[0] + u[0] * l1 * s1, a[1] + u[1] * l1)
        return m, add(m, f, l2)
    if view == 'side':
        J['el'], J['ha'] = limb(sho, p['ua'], L['ua'], p['fa'], L['fa'])
        J['el2'], J['ha2'] = limb(sho, p.get('ua2', p['ua']), L['ua'], p.get('fa2', p['fa']), L['fa'])
        J['kn'], J['an'] = limb(hip, p['th'], L['th'], p['sh'], L['sh'])
        J['kn2'], J['an2'] = limb(hip, p.get('th2', p['th']), L['th'], p.get('sh2', p['sh']), L['sh'])
    else:
        for s, sg in (('R', 1), ('L', -1)):
            J['sh' + s] = (sho[0] + sg * SHOULDERS, sho[1]); J['hp' + s] = (hip[0] + sg * HIPS, hip[1])
            J['el' + s], J['ha' + s] = limb(J['sh' + s], p['ua'], L['ua'], p['fa'], L['fa'], p.get('uas', 1.0), s == 'L')
            kn = (J['hp' + s][0] + sg * p.get('kx', 2), J['hp' + s][1] - L['th'] * p.get('ths', 1.0))
            J['kn' + s] = kn; J['an' + s] = (kn[0], kn[1] - L['sh'] * p.get('shs', 1.0))
    return J
