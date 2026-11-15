"""
Makes a made-up training history for the wiki's screenshots: three weeks of the Full body A / B
plan, with the weights going up. Nothing personal is ever shown.

    python3 seed.py <output folder>
"""
import json, os, sys, time, uuid

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)
STACK = [round(3.5 + 5.7 * i, 1) for i in range(12)]
A = [("chest_press", 4, 8, 10, 120, 5), ("lat_pulldown", 4, 8, 10, 120, 5), ("leg_extension", 3, 10, 12, 90, 4),
     ("leg_curl", 3, 10, 12, 60, 3), ("upright_row", 3, 10, 12, 90, 3), ("triceps_pushdown", 3, 10, 12, 60, 3),
     ("kneeling_crunch", 3, 12, 15, 60, 4)]
B = [("butterfly", 3, 10, 12, 90, 3), ("close_grip_pulldown", 4, 8, 10, 120, 5), ("seated_row", 3, 8, 10, 120, 4),
     ("leg_extension", 3, 10, 12, 90, 4), ("front_raise", 3, 10, 12, 60, 2), ("preacher_curl", 3, 10, 12, 60, 3),
     ("sit_up", 3, 12, 20, 60, 0)]
day = 86400 * 1000
now = int(time.time() * 1000)
sessions = []
for n in range(9):
    plan = A if n % 2 == 0 else B
    start = now - (9 - n) * 2 * day - 3 * 3600 * 1000
    logs = []
    for ex, sets, lo, hi, rest, pin in plan:
        p = pin + n // 3 if pin else 0
        reps = [hi, hi - 1, lo + 1, lo][:sets] if n % 3 else [lo + 1] * sets
        logs.append({"exercise": ex, "sets": [{"pin": p, "kg": STACK[p - 1] if p else 0.0, "reps": r} for r in reps]})
    sessions.append({"id": str(uuid.uuid4()), "start": start, "end": start + 52 * 60 * 1000, "planId": "fullbody_ab",
                     "planName": "Full body A / B", "dayIndex": n % 2, "dayName": "Workout A" if n % 2 == 0 else "Workout B",
                     "items": [{"exercise": e, "sets": s, "repMin": lo, "repMax": hi, "restSec": r} for e, s, lo, hi, r, _ in plan],
                     "logs": logs})
data = {"settings": {"units": "KG"}, "activePlan": "fullbody_ab", "nextDay": 1, "sessions": sessions}
json.dump(data, open(os.path.join(OUT, "training.json"), "w"))
print("seeded", len(sessions), "workouts")
