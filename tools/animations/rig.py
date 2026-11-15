# Same rig as the app (anim/Rig.kt): forward kinematics of a stick figure on the IM-309 drawing.
import math
L = dict(neck=2.5, head=3.6, torso=18.0, ua=11.0, fa=10.0, th=15.0, sh=15.0, foot=4.5)

def d(a): return math.cos(math.radians(a)), math.sin(math.radians(a))

def lerp_angle(a, b, t):
    diff = (b - a + 540) % 360 - 180
    return a + diff * t

def pose_at(s, e, t):
    out = {}
    for k in set(s) | set(e):
        a, b = s.get(k, e.get(k)), e.get(k, s.get(k))
        if isinstance(a, str): out[k] = a; continue
        angle = k in ('torso', 'ua', 'fa', 'th', 'sh', 'ua2', 'fa2', 'th2', 'sh2')
        out[k] = lerp_angle(a, b, t) if angle else a + (b - a) * t
    return out

def joints(p, view):
    """Returns dict of joint points; for front view, mirrored limbs are '..L'."""
    hip = (p['hx'], p['hy'])
    tx, ty = d(p['torso'])
    sho = (hip[0] + tx * L['torso'], hip[1] + ty * L['torso'] + p.get('sy', 0))
    neck = (sho[0] + tx * L['neck'], sho[1] + ty * L['neck'])
    head = (neck[0] + tx * L['head'], neck[1] + ty * L['head'])
    J = dict(hip=hip, sho=sho, neck=neck, head=head)
    def arm(sx, ua, fa, sc=1.0, mirror=False):
        ux, uy = d(ua); fx, fy = d(fa)
        if mirror: ux, fx = -ux, -fx
        el = (sx[0] + ux * L['ua'] * sc, sx[1] + uy * L['ua'])
        ha = (el[0] + fx * L['fa'] * p.get('fas', 1.0), el[1] + fy * L['fa'])
        return el, ha
    def leg(hp, th, sh, sc=1.0, mirror=False):
        ux, uy = d(th); fx, fy = d(sh)
        if mirror: ux, fx = -ux, -fx
        kn = (hp[0] + ux * L['th'] * sc, hp[1] + uy * L['th'] * sc if view == 'side' else hp[1] + uy * L['th'] * sc)
        an = (kn[0] + fx * L['sh'], kn[1] + fy * L['sh'])
        return kn, an
    if view == 'side':
        J['el'], J['ha'] = arm(sho, p['ua'], p['fa'])
        J['el2'], J['ha2'] = arm(sho, p.get('ua2', p['ua']), p.get('fa2', p['fa']))
        J['kn'], J['an'] = leg(hip, p['th'], p['sh'])
        J['kn2'], J['an2'] = leg(hip, p.get('th2', p['th']), p.get('sh2', p['sh']))
    else:
        sw, hw = 7.0, 4.5
        shR = (sho[0] + sw, sho[1]); shL = (sho[0] - sw, sho[1])
        hpR = (hip[0] + hw, hip[1]); hpL = (hip[0] - hw, hip[1])
        J['shR'], J['shL'], J['hpR'], J['hpL'] = shR, shL, hpR, hpL
        J['elR'], J['haR'] = arm(shR, p['ua'], p['fa'], p.get('uas', 1.0))
        J['elL'], J['haL'] = arm(shL, p['ua'], p['fa'], p.get('uas', 1.0), mirror=True)
        J['knR'], J['anR'] = leg(hpR, p['th'], p['sh'], p.get('ths', 1.0))
        J['knL'], J['anL'] = leg(hpL, p['th'], p['sh'], p.get('ths', 1.0), mirror=True)
    return J
