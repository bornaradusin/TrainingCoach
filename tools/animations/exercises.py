"""assets/exercises.json: the exercises, their setup and steps, muscles and captions (run: python3 exercises.py).
The 3D animations themselves are in 3d/exercises3d.py. Poses here are in centimetres on the measured IM-309 (machine.py).
Angles: 0° points to the back of the machine, 90° up, 180° to the front, 270° down."""
import json, os
ASSET = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../app/src/main/assets/exercises.json')
old = {e['id']: e for e in json.load(open(ASSET, encoding='utf-8'))['exercises']}
SEAT = dict(hx=85, hy=65, torso=76.5, th=185, sh=270, face=-1)            # seated, back on the backrest
SEATF = dict(hx=0, hy=65, torso=90, ths=0.15, shs=1.0, kx=4)              # seated, seen from the front
STANDF = dict(hx=0, hy=93, torso=90, ths=1.0, shs=1.0, kx=1)              # standing, seen from the front
def P(b, **k): d = dict(b); d.update(k); return d
A = {}
A['chest_press'] = dict(view='side', start=P(SEAT, ua=-35, fa=185), end=P(SEAT, ua=184, fa=181), parts=['press'],
    work=['torso', 'ua'], travel=28, captions=dict(lift='Push the handles forward', lower='Let them come back slowly'))
A['butterfly'] = dict(view='front', start=P(SEATF, ua=0, fa=90, uas=1.0), end=P(SEATF, ua=0, fa=90, uas=0.22), parts=['pads'],
    work=['torso', 'ua'], travel=24, attach='hands', captions=dict(lift='Squeeze the pads together', lower='Open slowly'))
# The press arms only swing forward (towards the legs of someone on the seat), so the rear
# shoulders are done on the top pulley instead of a reverse butterfly
A['face_pull'] = dict(view='side', start=P(dict(hx=-10, hy=93, torso=84, th=268, sh=270, face=1), ua=40, fa=40),
    end=P(dict(hx=-10, hy=93, torso=84, th=268, sh=270, face=1), ua=10, fa=160), cable='HP', parts=['rope'],
    travel=25, captions=dict(lift='Pull the rope to your face', lower='Let your arms go back out slowly'))
FACE_PULL = dict(id='face_pull', name='Face pull', station='high', attachment='Rope',
    muscles=['Rear shoulders'], secondary=['Upper back', 'Traps'],
    setup=["Clip the rope to the top pulley's cable.", "Stand in front of the machine, facing it, a step back from the front post.",
           "Take the rope ends with your palms facing in, arms stretched out towards the pulley."],
    steps=["Pull the rope towards your face, elbows high and out to the sides.", "Pull the ends apart beside your ears and squeeze your shoulder blades together.", "Let your arms go back out slowly."],
    tips=["Use a light weight; the rear shoulders are small muscles.", "Keep your elbows at shoulder height or higher, and don't lean back."],
    reps='12-15', bodyweight=False, unilateral=False, youtube='Cable face pull rope how to')
A['lat_pulldown'] = dict(view='front', start=P(SEATF, ua=52, fa=86), end=P(SEATF, ua=-28, fa=96), cable='HP', parts=['bar'], barw=118,
    work=['torso', 'ua'], travel=40, captions=dict(lift='Pull the bar down to your chest', lower='Let it rise slowly'))
A['close_grip_pulldown'] = dict(view='side', start=P(SEAT, torso=80, ua=113, fa=110), end=P(SEAT, torso=76, ua=262, fa=112), cable='HP', parts=['bar'],
    work=['torso', 'ua'], travel=40, captions=dict(lift='Pull the bar to your upper chest', lower='Arms up slowly'))
STAND_IN = dict(hx=-10, hy=93, torso=84, th=268, sh=270, face=1)          # standing in front, facing the machine
A['straight_arm_pulldown'] = dict(view='side', start=P(STAND_IN, ua=30, fa=30), end=P(STAND_IN, ua=-80, fa=-80), cable='HP', parts=['bar'],
    work=['torso', 'ua'], travel=30, captions=dict(lift='Sweep the bar down to your thighs', lower='Back up to eye level'))
A['triceps_pushdown'] = dict(view='side', start=P(STAND_IN, ua=-82, fa=25), end=P(STAND_IN, ua=-82, fa=-80), cable='HP', parts=['bar'],
    work=['ua', 'fa'], travel=22, captions=dict(lift='Push the bar down', lower='Let it come up to chest height'))
A['rope_pushdown'] = dict(view='side', start=P(STAND_IN, ua=-82, fa=25), end=P(STAND_IN, ua=-84, fa=-84), cable='HP', parts=['rope'],
    work=['ua', 'fa'], travel=22, captions=dict(lift='Push down, spread the rope', lower='Back up slowly'))
KNEEL = dict(hx=-25, hy=48, th=270, sh=180, face=1)
A['kneeling_crunch'] = dict(view='side', start=P(KNEEL, torso=82, ua=32, fa=173), end=P(KNEEL, torso=40, ua=-10, fa=131), cable='HP', parts=['rope'], attach='neck',
    work=['torso'], travel=26, captions=dict(lift='Curl down towards your knees', lower='Uncurl slowly'))
A['seated_crunch'] = dict(view='side', start=P(SEAT, ua=225, fa=70), end=P(SEAT, torso=118, ua=255, fa=100), cable='MP', parts=['rope'],
    work=['torso'], travel=20, captions=dict(lift='Curl forward', lower='Back to the pad slowly'))
PREACH = dict(hx=72, hy=65, torso=120, th=200, sh=270, face=-1)
A['preacher_curl'] = dict(view='side', start=P(PREACH, ua=212, fa=222), end=P(PREACH, ua=212, fa=78), cable='LP', parts=['bar'],
    work=['ua'], travel=26, captions=dict(lift='Curl the bar up', lower='Lower it slowly'))
STAND_LP = dict(hx=-22, hy=93, torso=88, th=270, sh=270, face=1)
A['standing_curl'] = dict(view='side', start=P(STAND_LP, ua=268, fa=282), end=P(STAND_LP, ua=266, fa=104), cable='LP', parts=['bar'],
    work=['ua'], travel=26, captions=dict(lift='Curl the bar up', lower='Lower it slowly'))
A['upright_row'] = dict(view='front', start=P(STANDF, ua=-95, fa=-100), end=P(STANDF, ua=15, fa=200), cable='LP', parts=['bar'], barw=44, infront=True,
    work=['ua', 'torso'], travel=34, captions=dict(lift='Pull the bar up, elbows high', lower='Lower slowly'))
A['shrug'] = dict(view='front', start=P(STANDF, ua=-92, fa=-92), end=P(STANDF, ua=-92, fa=-92, sy=7), cable='LP', parts=['bar'], barw=56, infront=True,
    work=['torso'], travel=7, captions=dict(lift='Shoulders up to your ears', lower='Down slowly'))
FLOOR = dict(hx=-52, hy=12, th=4, sh=354, face=1)
A['seated_row'] = dict(view='side', start=P(FLOOR, torso=68, ua=8, fa=6), end=P(FLOOR, torso=98, ua=255, fa=8), cable='LP', parts=['bar'],
    work=['torso', 'ua'], travel=40, captions=dict(lift='Pull the bar to your stomach', lower='Arms forward slowly'))
STAND_OUT = dict(hx=-14, hy=93, torso=92, th=270, sh=270, face=-1)
A['front_raise'] = dict(view='side', start=P(STAND_OUT, ua=264, fa=264), end=P(STAND_OUT, ua=180, fa=180), cable='LP', parts=['bar'],
    work=['ua'], travel=30, captions=dict(lift='Raise the bar to shoulder height', lower='Lower slowly'))
A['leg_extension'] = dict(view='side', start=P(SEAT, ua=262, fa=230), end=P(SEAT, ua=262, fa=230, sh=184), parts=['lever'], attach='ankle',
    work=['th'], travel=26, captions=dict(lift='Straighten your legs', lower='Lower slowly'))
CURL = dict(hx=19, hy=93, torso=96, th=272, sh=270, th2=270, sh2=270, face=1, ua=-62, fa=-55)
A['leg_curl'] = dict(view='side', start=P(CURL), end=P(CURL, th=262, sh=150), parts=['heel'], attach='ankle',
    work=['th'], travel=22, captions=dict(lift='Curl your heel up', lower='Lower slowly'))
A['sit_up'] = dict(view='side', start=dict(hx=-5, hy=12, torso=180, th=52, sh=-60, face=1, ua=200, fa=20),
    end=dict(hx=-5, hy=12, torso=62, th=52, sh=-60, face=1, ua=-80, fa=150), attach='hands',
    work=['torso'], travel=0, captions=dict(lift='Curl up', lower='Lower yourself slowly'))
out = []
for id_, e in old.items():
    if id_ == 'reverse_butterfly': id_, e = 'face_pull', FACE_PULL
    e = dict(e); e['anim'] = A[id_]; out.append(e)
# Setup text that now matches the machine
T = {o['id']: o for o in out}
T['lat_pulldown']['setup'] = ["Stand and take the lat bar with a wide overhand grip, then sit on the seat facing the front with the bar.", "Feet flat on the floor, chest up."]
T['close_grip_pulldown']['setup'] = ["Take the middle of the lat bar underhand, hands shoulder-width or closer, and sit on the seat facing the front.", "Feet flat on the floor."]
T['straight_arm_pulldown']['setup'] = ["Stand in front of the machine, facing it, a step back from the front post.", "Take the lat bar shoulder-width with straight arms at about eye level; lean forward slightly."]
T['triceps_pushdown']['setup'] = ["Stand in front of the machine, facing it, a step back from the front post.", "Take the lat bar overhand, hands about shoulder-width, elbows tucked to your sides."]
T['rope_pushdown']['setup'] = ["Clip the rope to the top pulley's cable.", "Stand in front of the machine, facing it, and hold the rope ends with your thumbs up, elbows at your sides."]
T['kneeling_crunch']['setup'] = ["Clip the rope or the ab-crunch strap to the top pulley's cable.", "Kneel in front of the machine, facing it, and hold the rope beside your head."]
T['seated_crunch']['setup'] = ["Clip the rope or the ab-crunch strap to the pulley at the top of the backrest.", "Sit with your back on the backrest and hold the ends over your shoulders."]
T['preacher_curl']['setup'] = ["Clip the short bar to the low pulley's chain at the front of the machine.", "Sit near the front of the seat facing the front, and lean your chest against the top of the preacher pad.", "Rest the backs of your upper arms down the pad and take the bar underhand."]
T['standing_curl']['setup'] = ["Clip the short bar to the low pulley's chain at the front.", "Stand in front of the machine, facing it, close to the pulley; bar underhand at your thighs."]
T['upright_row']['setup'] = ["Clip the short bar to the low pulley's chain at the front.", "Stand in front of the machine, facing it, bar overhand at your thighs, hands a bit narrower than shoulder-width."]
T['shrug']['setup'] = ["Clip the short bar to the low pulley's chain at the front.", "Stand in front of the machine, facing it, bar at your thighs, arms straight."]
T['seated_row']['setup'] = ["Clip the short bar to the low pulley's chain at the front.", "Sit on the floor in front of the machine, facing it, feet against the front of the base and knees soft.", "Take the bar with straight arms."]
T['front_raise']['setup'] = ["Clip the short bar to the low pulley's chain at the front.", "Stand with your back to the machine, the cable between your feet, bar at your thighs."]
T['leg_extension']['setup'] = ["Sit with your back against the backrest.", "Hook your ankles behind the lower rollers of the leg lever, knees at the front edge of the seat."]
T['leg_curl']['setup'] = ["Stand in front of the machine, facing the seat, with the front of your thighs against the upper rollers.", "Hold the preacher pad for balance and put one heel behind the lower roller."]
T['sit_up']['setup'] = ["Lie on your back on the floor in front of the machine.", "Bend your knees and hook your feet under the lower rollers of the leg lever.", "Cross your arms on your chest."]
T['chest_press']['setup'] = ["Sit with your back flat against the backrest.", "Take the hook handles at the bottom of the press arms; they should be level with the middle of your chest.", "Feet flat on the floor."]
T['butterfly']['setup'] = ["Sit with your back against the backrest.", "Put your forearms flat against the upright pads of the press arms, elbows bent about 90° and level with your shoulders."]
from machine import SIDE, FRONT
from muscles import NAMES
# The muscles drawn on the person: the exercise's own list (main) and its 'secondary' list (also)
SEEN_FROM_BEHIND = {'upright_row', 'shrug'}
for o in out:
    a = o['anim']; a.pop('work', None)
    a['muscles'] = [NAMES[m] for m in o['muscles'] if m in NAMES]
    a['also'] = [NAMES[m] for m in o['secondary'] if m in NAMES and NAMES[m] not in a['muscles']]
    if o['id'] in SEEN_FROM_BEHIND: a['seen'] = 'back'
json.dump(dict(machine="Ironman IM-309", side=SIDE, front=FRONT, exercises=out), open(ASSET, 'w', encoding='utf-8'), indent=1, ensure_ascii=False)
print(len(out))
