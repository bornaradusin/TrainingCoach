import json
SEAT = dict(hx=64, hy=60, torso=-82, th=180, sh=90)            # seated, facing the front (left)
SEATF = dict(hx=50, hy=61, torso=-90, th=90, ths=0.25, sh=90)  # seated, seen from the front
STAND_IN = dict(hx=44, hy=63, torso=-90, th=90, sh=90)         # standing in front, facing the machine
STANDF = dict(hx=50, hy=63, torso=-90, th=92, sh=90)           # standing, seen from the front
def P(base, **k): d = dict(base); d.update(k); return d
X = []
def ex(id, name, station, attachment, muscles, secondary, setup, steps, tips, reps, anim, bodyweight=False, unilateral=False, yt=None):
    X.append(dict(id=id, name=name, station=station, attachment=attachment, muscles=muscles, secondary=secondary,
                  setup=setup, steps=steps, tips=tips, reps=reps, bodyweight=bodyweight, unilateral=unilateral,
                  youtube=yt or (name + " cable machine how to"), anim=anim))

ex("chest_press", "Chest press", "press", "Press arms, handles", ["Chest"], ["Front shoulders", "Triceps"],
   ["Sit with your back flat against the backrest.", "Set the seat so the handles are level with the middle of your chest.", "Hook your feet behind the lower rollers or keep them flat."],
   ["Grip the handles, elbows slightly below your shoulders.", "Push forward until your arms are almost straight.", "Pause, then let the handles come back slowly until you feel a stretch in your chest."],
   ["Don't lock your elbows at the front.", "Keep your shoulder blades back against the pad; don't let your shoulders roll forward."],
   "8-12", dict(view="side", start=P(SEAT, ua=15, fa=180), end=P(SEAT, ua=176, fa=178), lever="PRESS", attach="hands"))

ex("butterfly", "Butterfly (pec deck)", "press", "Press arms, butterfly pads", ["Chest"], ["Front shoulders"],
   ["Sit with your back against the backrest.", "Set the seat so your upper arms are level with your shoulders when your forearms are on the pads.", "Elbows bent about 90°, forearms flat against the pads."],
   ["Squeeze the pads together in front of your chest, leading with your elbows.", "Hold for a moment with your chest tight.", "Let the pads open slowly until your elbows are just behind your body."],
   ["Push with your forearms and elbows, not your hands.", "Keep your back on the pad and your chest up."],
   "10-15", dict(view="front", start=P(SEATF, ua=0, fa=-90, uas=1.0), end=P(SEATF, ua=0, fa=-90, uas=0.18), pads=True))

ex("reverse_butterfly", "Reverse butterfly", "press", "Press arms, butterfly pads", ["Rear shoulders"], ["Upper back"],
   ["Sit facing the backrest, chest against it.", "Reach forward and place the back of your upper arms against the pads, elbows bent."],
   ["Push the pads back and out with your elbows until your arms are in line with your shoulders.", "Squeeze your shoulder blades together.", "Return slowly."],
   ["Use a light weight; the rear shoulders are small muscles.", "Don't shrug your shoulders up."],
   "12-15", dict(view="front", start=P(SEATF, ua=0, fa=-90, uas=0.18), end=P(SEATF, ua=0, fa=-90, uas=1.0), pads=True))

ex("lat_pulldown", "Lat pulldown", "high", "Lat bar, wide grip", ["Back (lats)"], ["Biceps", "Rear shoulders"],
   ["Sit facing the front with your knees under the upper rollers.", "Reach up and take the lat bar with a wide overhand grip."],
   ["Pull the bar down in front of you to the top of your chest.", "Drive your elbows down and back, chest up.", "Let the bar rise slowly until your arms are straight."],
   ["Don't pull the bar behind your neck.", "Lean back only a little; don't swing your body to move the weight."],
   "8-12", dict(view="front", start=P(SEATF, ua=-58, fa=-82), end=P(SEATF, ua=28, fa=-96), cable="HP", bar=True))

ex("close_grip_pulldown", "Close-grip pulldown", "high", "Lat bar, close underhand grip", ["Back (lats)"], ["Biceps"],
   ["Sit facing the front with your knees under the upper rollers.", "Take the middle of the lat bar with an underhand grip, hands shoulder-width or closer."],
   ["Pull the bar down to your upper chest, elbows tight to your sides.", "Squeeze your back at the bottom.", "Return slowly to straight arms."],
   ["Keep your chest up and your elbows in front of your body."],
   "8-12", dict(view="side", start=P(SEAT, torso=-95, ua=-118, fa=-115), end=P(SEAT, torso=-84, ua=62, fa=-115), cable="HP", attach="hands"))

ex("straight_arm_pulldown", "Straight-arm pulldown", "high", "Lat bar or rope", ["Back (lats)"], ["Triceps (long head)", "Core"],
   ["Stand facing the machine under the lat bar.", "Take the bar shoulder-width, arms straight and in front of you at eye level.", "Hinge slightly at the hips."],
   ["Keeping your arms straight, sweep the bar down to your thighs.", "Squeeze your back at the bottom.", "Let it rise slowly back to eye level."],
   ["Only a soft bend in the elbows; the movement is at the shoulders."],
   "12-15", dict(view="side", start=P(STAND_IN, torso=-80, ua=-25, fa=-25), end=P(STAND_IN, torso=-80, ua=80, fa=80), cable="HP", attach="hands"))

ex("triceps_pushdown", "Triceps pushdown", "high", "Lat bar or short bar", ["Triceps"], [],
   ["Stand facing the machine under the high pulley.", "Grip the bar overhand, hands about shoulder-width.", "Tuck your elbows to your sides."],
   ["Push the bar down until your arms are straight.", "Squeeze the backs of your arms.", "Let it come up slowly until your forearms pass horizontal."],
   ["Your upper arms stay still at your sides; only your forearms move."],
   "10-15", dict(view="side", start=P(STAND_IN, torso=-86, ua=85, fa=-15), end=P(STAND_IN, torso=-86, ua=85, fa=88), cable="HP", attach="hands"))

ex("rope_pushdown", "Rope pushdown", "high", "Triceps rope", ["Triceps"], [],
   ["Clip the rope to the high pulley.", "Stand facing the machine and hold the rope ends, thumbs up, elbows at your sides."],
   ["Push down and spread the rope ends apart at the bottom.", "Return slowly to the start."],
   ["Spreading the rope at the bottom gives a stronger squeeze."],
   "10-15", dict(view="side", start=P(STAND_IN, torso=-86, ua=85, fa=-15), end=P(STAND_IN, torso=-86, ua=85, fa=92), cable="HP", attach="hands"))

ex("kneeling_crunch", "Kneeling cable crunch", "high", "Triceps rope or ab harness", ["Abs"], ["Obliques"],
   ["Clip the rope (or ab harness) to the high pulley.", "Kneel facing the machine and hold the rope beside your head."],
   ["Curl your chest down towards your knees, rounding your back.", "Squeeze your abs at the bottom.", "Come up slowly, keeping tension."],
   ["Move with your abs, not your arms or hips: your hips stay still."],
   "12-20", dict(view="side", start=dict(hx=32, hy=79, torso=-78, th=90, sh=180, ua=-115, fa=55), end=dict(hx=32, hy=79, torso=6, th=90, sh=180, ua=-45, fa=110), cable="HP", attach="neck"))

ex("seated_crunch", "Seated cable crunch", "mid", "Triceps rope or ab harness", ["Abs"], [],
   ["Clip the rope (or ab harness) to the pulley at the top of the backrest.", "Sit with your back to the backrest and hold the rope ends over your shoulders."],
   ["Curl your chest down and forward towards your knees.", "Squeeze your abs.", "Uncurl slowly back to the pad."],
   ["Breathe out as you crunch.", "Keep your hips on the seat."],
   "12-20", dict(view="side", start=P(SEAT, ua=150, fa=-30), end=P(SEAT, torso=-140, ua=200, fa=-60), cable="MP", attach="hands"))

ex("preacher_curl", "Preacher curl", "low", "Short bar", ["Biceps"], ["Forearms"],
   ["Clip the short bar to the low pulley at the front.", "Sit facing the front with your upper arms resting down the preacher pad.", "Take the bar underhand, arms almost straight."],
   ["Curl the bar up towards your shoulders.", "Squeeze at the top.", "Lower slowly until your arms are nearly straight."],
   ["Keep the backs of your arms on the pad all the way.", "Don't fully lock out at the bottom."],
   "8-12", dict(view="side", start=P(SEAT, torso=-100, ua=135, fa=145), end=P(SEAT, torso=-100, ua=135, fa=-95), cable="LP", attach="hands"))

ex("standing_curl", "Standing cable curl", "low", "Short bar", ["Biceps"], ["Forearms"],
   ["Clip the short bar to the low pulley.", "Stand facing the machine, feet hip-width, bar underhand at your thighs."],
   ["Curl the bar up to your shoulders, elbows fixed at your sides.", "Lower it slowly."],
   ["Don't swing your back to lift the weight."],
   "8-12", dict(view="side", start=P(STAND_IN, hx=26, ua=88, fa=82), end=P(STAND_IN, hx=26, ua=95, fa=-65), cable="LP", attach="hands"))

ex("upright_row", "Upright row", "low", "Short bar", ["Shoulders", "Traps"], ["Biceps"],
   ["Clip the short bar to the low pulley.", "Stand facing the machine, holding the bar overhand at your thighs, hands a bit narrower than shoulder-width."],
   ["Pull the bar straight up close to your body until your elbows are at shoulder height.", "Lower slowly."],
   ["Lead with your elbows; stop at shoulder height if your shoulders complain."],
   "10-12", dict(view="front", start=P(STANDF, ua=100, fa=95), end=P(STANDF, ua=-12, fa=186), cable="LP", bar=True, barw=10))

ex("shrug", "Cable shrug", "low", "Short bar", ["Traps"], [],
   ["Clip the short bar to the low pulley.", "Stand facing the machine with the bar at your thighs, arms straight."],
   ["Lift your shoulders straight up towards your ears.", "Hold for a second, then lower slowly."],
   ["Don't roll your shoulders; just up and down.", "Keep your arms straight."],
   "12-15", dict(view="front", start=P(STANDF, ua=96, fa=92), end=P(STANDF, ua=96, fa=92, sy=-3.5), cable="LP", bar=True, barw=16))

ex("seated_row", "Seated low row", "low", "Short bar", ["Back (mid)"], ["Lats", "Biceps", "Rear shoulders"],
   ["Clip the short bar to the low pulley at the front.", "Sit on the seat facing the front and take the bar with arms straight."],
   ["Pull the bar to your stomach, driving your elbows back.", "Squeeze your shoulder blades together.", "Let your arms straighten slowly."],
   ["Keep your back straight; don't rock back and forth."],
   "8-12", dict(view="side", start=P(SEAT, torso=-105, ua=150, fa=150), end=P(SEAT, torso=-88, ua=65, fa=180), cable="LP", attach="hands"))

ex("front_raise", "Front raise", "low", "Short bar", ["Front shoulders"], [],
   ["Clip the short bar to the low pulley.", "Stand with your back to the machine, the cable between your feet, bar at your thighs."],
   ["Raise the bar in front of you with straight arms to shoulder height.", "Lower it slowly."],
   ["A slight bend in the elbows is fine; don't lean back."],
   "10-15", dict(view="side", start=dict(hx=22, hy=63, torso=-90, th=90, sh=90, ua=92, fa=92), end=dict(hx=22, hy=63, torso=-90, th=90, sh=90, ua=180, fa=180), cable="LP", attach="hands"))

ex("leg_extension", "Leg extension", "leg", "Leg rollers", ["Quads (front of thighs)"], [],
   ["Sit on the seat with your back against the backrest.", "Hook your ankles behind the lower roller, knees at the edge of the seat."],
   ["Straighten your legs until they're level.", "Squeeze your thighs at the top.", "Lower slowly."],
   ["Hold the seat edges or the handles.", "Don't swing; control the way down."],
   "10-15", dict(view="side", start=P(SEAT, ua=90, fa=150), end=P(SEAT, ua=90, fa=150, sh=176), roller="ankle", pivot="knee"))

ex("leg_curl", "Standing leg curl", "leg", "Leg rollers", ["Hamstrings"], ["Calves"],
   ["Stand facing the seat, holding the backrest for balance.", "Put one leg behind the lower roller so it sits just above your heel."],
   ["Curl your heel up towards your bottom.", "Squeeze, then lower slowly.", "Do all reps, then switch legs."],
   ["Keep your hips still and your standing knee soft."],
   "10-15", dict(view="side", start=dict(hx=38, hy=63, torso=-88, th=92, sh=90, th2=90, sh2=90, ua=60, fa=10), end=dict(hx=38, hy=63, torso=-88, th=100, sh=200, th2=90, sh2=90, ua=60, fa=10), roller="ankle", pivot="knee"), unilateral=True)

ex("sit_up", "Sit-up", "bench", "Lower leg roller (bodyweight)", ["Abs"], ["Hip flexors"],
   ["Lie on your back on the floor in front of the machine.", "Bend your knees and hook your feet under the lower leg roller.", "Cross your arms on your chest."],
   ["Curl up until your chest is close to your knees.", "Lower yourself back down slowly, one vertebra at a time."],
   ["Breathe out on the way up.", "Don't pull on your neck; for an easier version, only lift your shoulders (crunch)."],
   "10-20", dict(view="side", start=dict(hx=30, hy=92, torso=180, th=-56, sh=41, ua=150, fa=-40), end=dict(hx=30, hy=92, torso=-62, th=-56, sh=41, ua=40, fa=-110)), bodyweight=True)

json.dump(dict(machine="Ironman IM-309", exercises=X), open('exercises.json', 'w'), indent=1, ensure_ascii=False)
print(len(X), "exercises")
