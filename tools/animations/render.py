"""Draws exercises the way the app does (anim/ExerciseAnimation.kt), for checking by eye:
start, middle and end of each, on the machine to scale."""
import json, math, sys
from PIL import Image, ImageDraw, ImageFont
from rig import joints, pose_at, L, W, WF, d
from machine import SIDE, FRONT

COL = dict(frame=(70, 72, 78), pad=(196, 178, 150), roller=(40, 40, 44), pulley=(110, 110, 118), chrome=(150, 150, 160),
           stackbox=(55, 56, 60), plate=(35, 35, 38), plateup=(230, 120, 40), body=(52, 110, 190), far=(140, 170, 215),
           work=(235, 110, 40), cable=(20, 160, 90), arrow=(220, 40, 40), active=(20, 160, 90))
S = 3      # pixels per cm
BOX = dict(side=(-85, 200, 0, 220), front=(-80, 80, 0, 220))   # x0, x1, y0, y1 in cm

class Pen:
    def __init__(self, dr, box): self.dr, self.box = dr, box
    def p(self, pt): return ((pt[0] - self.box[0]) * S, (self.box[3] - pt[1]) * S)
    def line(self, a, b, w, c):
        A, B = self.p(a), self.p(b); r = w * S / 2
        self.dr.line([A, B], fill=c, width=max(1, int(w * S)))
        for q in (A, B): self.dr.ellipse([q[0] - r, q[1] - r, q[0] + r, q[1] + r], fill=c)
    def circle(self, c0, r, c, outline=None):
        q = self.p(c0); self.dr.ellipse([q[0] - r * S, q[1] - r * S, q[0] + r * S, q[1] + r * S], fill=c, outline=outline)
    def rect(self, x, y, w, h, c, r=0):
        a = self.p((x, y + h)); b = self.p((x + w, y))
        self.dr.rounded_rectangle([a, b], radius=r * S, fill=c)
    def poly(self, pts, c): self.dr.polygon([self.p(q) for q in pts], fill=c)

def shape(pen, s):
    c = COL[s['c']]
    if s['t'] == 'rect': pen.rect(s['x'], s['y'], s['w'], s['h'], c)
    elif s['t'] == 'rrect': pen.rect(s['x'], s['y'], s['w'], s['h'], c, s.get('r', 2))
    elif s['t'] == 'line': pen.line(s['a'], s['b'], s['w'], c)
    elif s['t'] == 'circle': pen.circle((s['x'], s['y']), s['r'], c)

def stack(pen, st, lift, travel):
    if not st: return
    y = st['y']; up = 5   # plates above the pin rise
    for i in range(st['plates']):
        rising = i >= st['plates'] - up
        dy = lift * travel if rising else 0
        pen.rect(st['x'], y + dy, st['w'], st['h'], COL['plateup' if rising and lift > 0 else 'plate'], 0.6)
        y += st['h'] + st['gap']
    return (st['x'] + st['w'] / 2, y + lift * travel)

def effector(J, a, view):
    att = a.get('attach', 'hands')
    if att == 'neck': return J['head'][0] + 6 * J['face'], J['head'][1] - 10
    if att == 'ankle': return J['an'] if view == 'side' else ((J['anR'][0] + J['anL'][0]) / 2, J['anR'][1])
    if view == 'side': return J['ha']
    return ((J['haR'][0] + J['haL'][0]) / 2, (J['haR'][1] + J['haL'][1]) / 2)

def figure_side(pen, J, work, near=True):
    w = lambda k: COL['work'] if k in work else COL['body']
    # far limbs
    pen.line(J['hip'], J['kn2'], W['th'], COL['far']); pen.line(J['kn2'], J['an2'], W['sh'], COL['far'])
    pen.line(J['an2'], (J['an2'][0] + L['foot'] * J['face'] * 0.8, J['an2'][1] - 3), W['foot'], COL['far'])
    pen.line(J['sho'], J['el2'], W['ua'], COL['far']); pen.line(J['el2'], J['ha2'], W['fa'], COL['far'])
    pen.circle(J['ha2'], L['hand'], COL['far'])
    # body
    pen.line(J['hip'], J['sho'], W['torso'], w('torso'))
    pen.line(J['sho'], J['head'], W['neck'], COL['body'])
    pen.circle(J['head'], L['head'], COL['body'])
    # near limbs
    pen.line(J['hip'], J['kn'], W['th'], w('th')); pen.line(J['kn'], J['an'], W['sh'], w('sh'))
    pen.line(J['an'], (J['an'][0] + L['foot'] * J['face'] * 0.8, J['an'][1] - 3), W['foot'], COL['body'])
    pen.line(J['sho'], J['el'], W['ua'], w('ua')); pen.line(J['el'], J['ha'], W['fa'], w('fa'))
    pen.circle(J['ha'], L['hand'], COL['body'])

def figure_front(pen, J, work):
    w = lambda k: COL['work'] if k in work else COL['body']
    for s in 'RL':
        pen.line(J['hp' + s], J['kn' + s], WF['th'], w('th')); pen.line(J['kn' + s], J['an' + s], WF['sh'], w('sh'))
    pen.poly([J['shL'], J['shR'], J['hpR'], J['hpL']], w('torso'))
    pen.line(J['shL'], J['shR'], 10, w('torso')); pen.line(J['hpL'], J['hpR'], 12, COL['body'])
    pen.line(J['sho'], J['head'], WF['neck'], COL['body']); pen.circle(J['head'], L['head'], COL['body'])
    for s in 'RL':
        pen.line(J['sh' + s], J['el' + s], WF['ua'], w('ua')); pen.line(J['el' + s], J['ha' + s], WF['fa'], w('fa'))
        pen.circle(J['ha' + s], L['hand'], COL['body'])

def arrow(pen, a, b):
    dx, dy = b[0] - a[0], b[1] - a[1]; n = math.hypot(dx, dy)
    if n < 1: return
    ux, uy = dx / n, dy / n; tip = (a[0] + ux * 18, a[1] + uy * 18)
    pen.line(a, tip, 2.2, COL['arrow'])
    left = (tip[0] - ux * 6 - uy * 4, tip[1] - uy * 6 + ux * 4); right = (tip[0] - ux * 6 + uy * 4, tip[1] - uy * 6 - ux * 4)
    pen.poly([tip, left, right], COL['arrow'])

def draw(ex, t, lift_phase=True):
    a = ex['anim']; view = a['view']; M = SIDE if view == 'side' else FRONT; box = BOX[view]
    im = Image.new('RGB', (int((box[1] - box[0]) * S), int((box[3] - box[2]) * S)), (250, 250, 250))
    pen = Pen(ImageDraw.Draw(im), box)
    p = pose_at(a['start'], a['end'], t); J = joints(p, view)
    travel = a.get('travel', 25) if not ex.get('bodyweight') else 0
    behind = a.get('infront', False)
    for s in M['shapes']:
        if s['l'] == 'back' or behind: shape(pen, s)
    if view == 'side' and not ({'lever', 'heel'} & set(a.get('parts', []))):
        pen.line(SIDE['pivots']['LEG'], SIDE['rest']['LEG'], 3, COL['frame']); pen.circle((SIDE['rest']['LEG'][0] - 6, SIDE['rest']['LEG'][1]), 5, COL['roller'])
    if view == 'side' and not ({'press'} & set(a.get('parts', []))):
        pen.line(SIDE['pivots']['PRESS'], (88, 100), 4, COL['frame']); pen.rect(84, 106, 7, 30, COL['pad'], 2)
    top = stack(pen, M.get('stack'), t, travel)
    # cable: pulley → the stack (side view), and pulley → what it's clipped to
    if a.get('cable'):
        pul = M['pulleys'][a['cable']]; e = effector(J, a, view)
        if view == 'side' and top: pen.line(M['pulleys']['BEAM'], top, 0.8, COL['chrome'])
        pen.line(pul, e, 1.0, COL['cable'])
    # far moving parts
    parts = a.get('parts', [])
    if 'press' in parts and view == 'side':
        pen.line(M['pivots']['PRESS'], J['ha2'], 4, COL['frame'])
    (figure_side if view == 'side' else figure_front)(pen, J, set(a.get('work', [])))
    if 'press' in parts and view == 'side':
        piv = M['pivots']['PRESS']; pen.line(piv, J['ha'], 4.5, COL['active']); pen.circle(J['ha'], 3, COL['active'])
    if 'pads' in parts and view == 'front':
        for s in 'RL':
            el, ha = J['el' + s], J['ha' + s]
            pen.line(M['pivots']['PRESS_' + s], (ha[0], ha[1] + 8), 4, COL['active'])
            pen.line(el, ha, 9, COL['pad'])
    if 'bar' in parts:
        e = effector(J, a, view); half = a.get('barw', 60) / 2
        if view == 'front': pen.line((e[0] - half, e[1]), (e[0] + half, e[1]), 3, COL['active'])
        else: pen.circle(e, 3.2, COL['active'])
    if 'rope' in parts:
        e = effector(J, a, view); pen.circle(e, 3, COL['active'])
    if 'lever' in parts:
        piv = SIDE['pivots']['LEG']; an = J['an']
        pen.line(piv, an, 3, COL['active']); pen.circle((an[0] - 6, an[1] + 1), 5, COL['roller'])
    if 'heel' in parts:
        an = J['an']; pen.circle((an[0] - 6 * J['face'], an[1] + 4), 5, COL['roller'])
    for s in M['shapes']:
        if s['l'] == 'front' and not behind: shape(pen, s)
    # arrow: direction of the movement at this moment
    if 0 < t < 1:
        e0 = effector(joints(pose_at(a['start'], a['end'], t - 0.05), view), a, view)
        e1 = effector(joints(pose_at(a['start'], a['end'], t + 0.05), view), a, view)
        off = a.get('arrow_off', [0, 0]); e = effector(J, a, view)
        arrow(pen, (e[0] + off[0], e[1] + off[1]), (e[0] + off[0] + (e1[0] - e0[0]), e[1] + off[1] + (e1[1] - e0[1])))
    return im

def sheet(exs, out, cols=3):
    tiles = []
    for ex in exs:
        row = [draw(ex, t) for t in (0, 0.5, 1)]
        w = sum(i.width for i in row); h = max(i.height for i in row)
        im = Image.new('RGB', (w, h + 30), 'white'); x = 0
        for i in row: im.paste(i, (x, 30)); x += i.width
        ImageDraw.Draw(im).text((8, 8), ex['name'], fill='black', font=ImageFont.load_default(size=22))
        tiles.append(im.resize((im.width // 2, im.height // 2)))
    W_ = max(t.width for t in tiles); H = max(t.height for t in tiles)
    cols = min(cols, len(tiles)); rows = (len(tiles) + cols - 1) // cols
    sh = Image.new('RGB', (W_ * cols, H * rows), 'white')
    for i, t in enumerate(tiles): sh.paste(t, ((i % cols) * W_, (i // cols) * H))
    sh.save(out)

if __name__ == '__main__':
    data = json.load(open(sys.argv[1])); ids = sys.argv[3:]
    exs = [e for e in data['exercises'] if not ids or e['id'] in ids]
    sheet(exs, sys.argv[2], cols=int(__import__('os').environ.get('COLS', 2)))
