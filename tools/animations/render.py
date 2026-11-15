# Draws an exercise's start / middle / end poses on the machine, to check them by eye.
import json, sys
from PIL import Image, ImageDraw
from rig import joints, pose_at, L
S = 4  # pixels per unit

# Machine anchors (side view), the same numbers as anim/Machine.kt
SIDE = dict(HP=(54, 12), LP=(34, 95), MP=(71, 34), PRESS=(72, 24))
FRONT = dict(HP=(50, 4), LP=(50, 95), MP=(50, 30))

def draw_machine(dr, view):
    g = (150, 150, 150)
    def ln(a, b, w=2): dr.line([a[0]*S, a[1]*S, b[0]*S, b[1]*S], fill=g, width=w*S//2)
    if view == 'side':
        dr.rectangle([86*S, 14*S, 96*S, 96*S], outline=g, width=2)
        ln((30, 96), (98, 96), 3); ln((72, 96), (72, 10)); ln((52, 10), (96, 12))
        ln((52, 63), (67, 63), 3); ln((60, 63), (60, 96))
        ln((68, 63), (72, 33), 3)
        ln((47, 56), (55, 47), 3)  # preacher pad
        ln((49, 58), (49, 96))     # leg developer post
        for c in ((49, 66), (48, 82)):
            dr.ellipse([(c[0]-1.8)*S, (c[1]-1.8)*S, (c[0]+1.8)*S, (c[1]+1.8)*S], outline=g, width=2)
        for k, v in SIDE.items():
            dr.ellipse([(v[0]-1.2)*S, (v[1]-1.2)*S, (v[0]+1.2)*S, (v[1]+1.2)*S], outline=(90, 90, 200), width=2)
    else:
        dr.rectangle([42*S, 8*S, 58*S, 96*S], outline=g, width=2)
        ln((20, 96), (80, 96), 3); ln((40, 63), (60, 63), 3)
        ln((22, 6), (78, 6))
        ln((32, 18), (68, 18))

def draw_pose(dr, p, view, col):
    J = joints(p, view)
    def ln(a, b, w=3): dr.line([a[0]*S, a[1]*S, b[0]*S, b[1]*S], fill=col, width=w)
    hd = J['head']; r = L['head'] * S
    dr.ellipse([hd[0]*S - r, hd[1]*S - r, hd[0]*S + r, hd[1]*S + r], outline=col, width=3)
    ln(J['hip'], J['sho'])
    if view == 'side':
        for a, b in (('sho', 'el2'), ('el2', 'ha2'), ('hip', 'kn2'), ('kn2', 'an2')): ln(J[a], J[b], 2)
        for a, b in (('sho', 'el'), ('el', 'ha'), ('hip', 'kn'), ('kn', 'an')): ln(J[a], J[b], 4)
    else:
        ln(J['shL'], J['shR']); ln(J['hpL'], J['hpR'])
        for s in 'LR':
            ln(J['sh'+s], J['el'+s]); ln(J['el'+s], J['ha'+s]); ln(J['hp'+s], J['kn'+s]); ln(J['kn'+s], J['an'+s])
    return J

def cable_end(J, a, view):
    att = a.get('attach', 'hands')
    if att == 'ankle': return J['an'] if view == 'side' else J['anR']
    if att == 'neck': return J['neck']
    if view == 'side': return J['ha']
    return ((J['haL'][0] + J['haR'][0]) / 2, (J['haL'][1] + J['haR'][1]) / 2)

def render(ex, out):
    a = ex['anim']; view = a['view']
    im = Image.new('RGB', (3 * 100 * S, 100 * S), 'white')
    for i, t in enumerate((0, 0.5, 1)):
        tile = Image.new('RGB', (100 * S, 100 * S), 'white'); dr = ImageDraw.Draw(tile)
        draw_machine(dr, view)
        p = pose_at(a['start'], a['end'], t)
        J = draw_pose(dr, p, view, (30, 90, 200) if t < 1 else (200, 60, 60))
        c = a.get('cable')
        if c:
            anc = (SIDE if view == 'side' else FRONT)[c]
            e = cable_end(J, a, view)
            dr.line([anc[0]*S, anc[1]*S, e[0]*S, e[1]*S], fill=(0, 160, 0), width=2)
        dr.text((6, 6), f"{ex['name']}  t={t}", fill='black')
        im.paste(tile, (i * 100 * S, 0))
    im.save(out)

if __name__ == '__main__':
    data = json.load(open(sys.argv[1]))
    ids = sys.argv[3:] or [e['id'] for e in data['exercises']]
    from PIL import Image as I
    tiles = []
    for e in data['exercises']:
        if e['id'] in ids:
            render(e, f"/tmp/{e['id']}.png"); tiles.append(f"/tmp/{e['id']}.png")
    w = 3 * 100 * S // 2
    sheet = I.new('RGB', (w * 2, 100 * S // 2 * ((len(tiles) + 1) // 2)), 'white')
    for i, f in enumerate(tiles):
        t = I.open(f).resize((w, 100 * S // 2))
        sheet.paste(t, ((i % 2) * w, (i // 2) * 100 * S // 2))
    sheet.save(sys.argv[2])
