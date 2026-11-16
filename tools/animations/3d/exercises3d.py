"""The exercises in 3D: poses (start → end), the machine's moving parts and the camera.
Run: python3 exercises3d.py [ids] [--preview]  → frames in out/<id>/NN.png (and a check sheet)."""
import sys, os, json, math
import numpy as np, cv2
import body3d as B, render3d as R

def v(*a): return np.array(a, float)

ANG = {'torso', 'ua', 'fa', 'th', 'sh', 'ua2', 'fa2', 'th2', 'sh2'}

def lerp(s, e, t):
    out = {}
    for k in set(s) | set(e):
        a, b = s.get(k, e.get(k)), e.get(k, s.get(k))
        if isinstance(a, (tuple, list)): out[k] = tuple(x + (y - x) * t for x, y in zip(a, b)); continue
        if k in ANG: b = a + ((b - a + 540) % 360 - 180)
        out[k] = a + (b - a) * t
    return out

P = lambda base, **kw: {**base, **kw}
SEAT = dict(hx=85, hy=65, torso=76.5, th=185, sh=270, face=-1, th_b=8, sh_b=-2)
STAND_IN = dict(hx=-10, hy=93, torso=84, th=268, sh=270, face=1, th_b=4)
KNEEL = dict(hx=-25, hy=48, th=270, sh=180, face=1, th_b=6)
PREACH = dict(hx=72, hy=65, torso=120, th=200, sh=270, face=-1, th_b=10)
STAND_LP = dict(hx=-22, hy=93, torso=88, th=270, sh=270, face=1, th_b=4)
FLOOR = dict(hx=-52, hy=12, th=4, sh=354, face=1, th_b=10)
STAND_OUT = dict(hx=-14, hy=93, torso=92, th=270, sh=270, face=-1, th_b=5)
CURL = dict(hx=19, hy=93, torso=96, th=272, sh=270, th2=270, sh2=270, face=1, ua=-62, fa=-55, ua_b=14, fa_b=-6)

HP, LP, MP, BEAM = v(73, 186, 0), v(8, 9, 0), v(121, 138, 0), v(176, 158, 0)
FRONT, SIDE, BACK, FACING = -35, -20, -70, 35   # camera from: front-quarter, side, more from the front (sees the back of someone facing the machine)

E = {}
E['chest_press'] = dict(s=P(SEAT, ua=-35, fa=185, ua_b=28, fa_b=-8), e=P(SEAT, ua=184, fa=181, ua_b=10, fa_b=-4), press=True, travel=28)
E['butterfly'] = dict(s=P(SEAT, ua_v=(-0.25, 0.02, 1), fa_v=(0, 1, 0)), e=P(SEAT, ua_v=(1, 0.02, 0.18), fa_v=(0, 1, 0)), pads=True, travel=24, cam=-55)
E['face_pull'] = dict(s=P(STAND_IN, ha_t=(50, 16, 8), el_hint=(0, -0.4, 1)), e=P(STAND_IN, torso=86, ha_t=(7, 13, 19), el_hint=(-0.9, 0.15, 1)), cable=HP, rope=True, travel=25, cam=-50)
E['lat_pulldown'] = dict(s=P(SEAT, ua_v=(0.35, 1, 0.55), fa_v=(0.2, 1, 0.25)), e=P(SEAT, ua_v=(-0.35, -1, 0.8), fa_v=(0.3, 1, -0.15)), cable=HP, bar=118, travel=40, cam=-55)
E['close_grip_pulldown'] = dict(s=P(SEAT, torso=80, ua=113, fa=110, ua_b=12, fa_b=-12), e=P(SEAT, torso=76, ua=262, fa=112, ua_b=14, fa_b=-14), cable=HP, bar=118, travel=40)
E['straight_arm_pulldown'] = dict(s=P(STAND_IN, ua=30, fa=30, ua_b=14, fa_b=0), e=P(STAND_IN, ua=-80, fa=-80, ua_b=10, fa_b=0), cable=HP, bar=60, travel=30, cam=FACING)
E['triceps_pushdown'] = dict(s=P(STAND_IN, ua=-82, fa=25, ua_b=6, fa_b=-8), e=P(STAND_IN, ua=-82, fa=-80, ua_b=6, fa_b=-6), cable=HP, bar=60, travel=30, cam=FACING)
E['rope_pushdown'] = dict(s=P(STAND_IN, ua=-82, fa=25, ua_b=6, fa_b=-6), e=P(STAND_IN, ua=-84, fa=-84, ua_b=6, fa_b=14), cable=HP, rope=True, travel=30, cam=FACING)
E['kneeling_crunch'] = dict(s=P(KNEEL, torso=82, ua=32, fa=173, ua_b=30, fa_b=-30), e=P(KNEEL, torso=40, ua=-10, fa=131, ua_b=30, fa_b=-30), cable=HP, rope=True, attach='neck', travel=25, cam=FACING)
E['seated_crunch'] = dict(s=P(SEAT, ua=225, fa=70, ua_b=25, fa_b=-30), e=P(SEAT, torso=118, ua=255, fa=100, ua_b=25, fa_b=-30), cable=MP, rope=True, travel=25)
E['preacher_curl'] = dict(s=P(PREACH, ua=212, fa=222, ua_b=10, fa_b=-4), e=P(PREACH, ua=212, fa=78, ua_b=10, fa_b=-4), cable=LP, bar=44, travel=22)
E['standing_curl'] = dict(s=P(STAND_LP, ua=268, fa=282, ua_b=8, fa_b=-2), e=P(STAND_LP, ua=266, fa=100, ua_b=8, fa_b=-2), cable=LP, bar=44, travel=22, cam=FACING)
E['upright_row'] = dict(s=P(STAND_IN, hx=-12, ha_t=(10, -64, 13), el_hint=(-0.6, 0.1, 1)), e=P(STAND_IN, hx=-12, ha_t=(19, -19, 8), el_hint=(-0.8, 0.9, 0.9)), cable=LP, bar=44, travel=22, cam=BACK)
E['shrug'] = dict(s=P(STAND_IN, hx=-12, ua_v=(0.08, -1, 0.1), fa_v=(0.1, -1, 0)), e=P(STAND_IN, hx=-12, sy=7, ua_v=(0.08, -1, 0.1), fa_v=(0.1, -1, 0)), cable=LP, bar=56, travel=7, cam=BACK)
E['seated_row'] = dict(s=P(FLOOR, torso=68, ua=8, fa=6, ua_b=10, fa_b=-6), e=P(FLOOR, torso=98, ua=255, fa=8, ua_b=14, fa_b=-14), cable=LP, bar=44, travel=28, cam=SIDE)
E['front_raise'] = dict(s=P(STAND_OUT, ua=264, fa=264, ua_b=8, fa_b=-4), e=P(STAND_OUT, ua=180, fa=180, ua_b=8, fa_b=-4), cable=LP, bar=44, travel=15)
E['leg_extension'] = dict(s=P(SEAT, ua=262, fa=230, ua_b=18), e=P(SEAT, ua=262, fa=230, ua_b=18, sh=184), lever=True, travel=26)
E['leg_curl'] = dict(s=P(CURL), e=P(CURL, th=262, sh=150), heel=True, travel=18, cam=FACING)
E['sit_up'] = dict(s=dict(hx=-5, hy=12, torso=180, th=52, sh=-60, face=1, ua=200, fa=20, ua_b=12, fa_b=-45, th_b=5),
                   e=dict(hx=-5, hy=12, torso=62, th=52, sh=-60, face=1, ua=-80, fa=150, ua_b=12, fa_b=-45, th_b=5), travel=0, cam=SIDE)


def mid(a, b): return (a + b) / 2


def state_for(x, J, t):
    st = dict(lift=t, travel=x.get('travel', 25), cables=[], bars=[], ropes=[])
    hands = mid(J['haR'], J['haL'])
    if x.get('press'): st['press'] = (J['haR'], J['haL'])
    if x.get('pads'): st['pads'] = ((J['elR'], J['haR']), (J['elL'], J['haL']))
    if x.get('lever'): st['lever'] = mid(J['anR'], J['anR'])
    if x.get('heel'):
        a = J['anR']; st['roller'] = v(a[0] - 7, a[1] + 2, 0)
    if x.get('cable') is not None:
        if x.get('attach') == 'neck':
            end = J['head'] - J['head_up'] * 14 + J['front'] * 4
        else:
            end = hands + (v(0, 6, 0) if x.get('rope') else 0)
        st['cables'].append((x['cable'], end))
        st['cables'].append((BEAM, v(171, 6 + 12 * 3.55 + t * st['travel'], 0)))
        if x.get('bar'):
            w = x['bar'] / 2
            st['bars'].append((v(hands[0], hands[1], -w), v(hands[0], hands[1], w), 1.3))
        if x.get('rope'):
            for k in ('R', 'L'): st['ropes'].append((end, J['ha' + k]))
    return st


def camera_for(x, Js):
    pts = np.concatenate([[J[k] for k in J if isinstance(J[k], np.ndarray) and J[k].shape == (3,) and k not in ('up', 'front', 'head_up')] for J in Js])
    c = (pts.min(0) + pts.max(0)) / 2
    target = (c[0] * 0.6 + 80 * 0.4, max(80, c[1] * 0.7 + 95 * 0.3), 0)
    return R.Camera(target, x.get('cam', FRONT), 9, 440)


def mus_of(eid):
    d = json.load(open(os.path.join(os.path.dirname(__file__), '../../../app/src/main/assets/exercises.json')))
    a = next(e for e in d['exercises'] if e['id'] == eid)['anim']
    return dict(main=a.get('muscles', []), also=a.get('also', []))


def run(eid, frames, outdir):
    x = E[eid]
    Js = [B.joints(lerp(x['s'], x['e'], t)) for t in (0, 0.5, 1)]
    cam = camera_for(x, Js)
    mus = mus_of(eid)
    os.makedirs(outdir, exist_ok=True)
    imgs = []
    for i, t in enumerate(frames):
        J = B.joints(lerp(x['s'], x['e'], t))
        img = R.render(J, mus, state_for(x, J, t), cam)
        cv2.imwrite(os.path.join(outdir, '%02d.png' % i), cv2.cvtColor(img, cv2.COLOR_RGB2BGR))
        imgs.append(img)
    return imgs


if __name__ == '__main__':
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    ids = args or list(E)
    preview = '--preview' in sys.argv
    frames = [0, 0.5, 1] if preview else [i / 12 for i in range(13)]
    for eid in ids:
        imgs = run(eid, frames, os.path.join('out' if not preview else 'preview', eid))
        if preview:
            cv2.imwrite('preview/%s.png' % eid, cv2.cvtColor(np.concatenate(imgs, 1), cv2.COLOR_RGB2BGR))
        print(eid, flush=True)
