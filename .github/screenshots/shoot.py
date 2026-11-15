"""
Installs Training Coach on the emulator with the made-up history from seed.py and takes the
wiki's screenshots.

    python3 shoot.py <app.apk> <seed folder> <output folder>
"""
import os
import re
import subprocess
import sys
import time
import traceback
import xml.etree.ElementTree as ET

APK, SEED, OUT = sys.argv[1], sys.argv[2], sys.argv[3]
PACKAGE = "io.github.bornaradusin.trainingcoach"
os.makedirs(OUT, exist_ok=True)
failed = []


def adb(*args, check=True):
    return subprocess.run(["adb", *args], capture_output=True, check=check).stdout


def shell(*args):
    return adb("shell", *args).decode(errors="replace")


def nodes():
    """Everything on screen, as (text, description, class, centre) from a UI dump."""
    for _ in range(5):
        raw = adb("exec-out", "uiautomator", "dump", "/dev/tty", check=False).decode(errors="replace")
        start, end = raw.find("<?xml"), raw.rfind("</hierarchy>")
        if start >= 0 and end > start:
            break
        time.sleep(1)
    else:
        return []
    root = ET.fromstring(raw[start:end + len("</hierarchy>")])
    found = []
    for n in root.iter("node"):
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds", "[0,0][0,0]")))
        found.append((n.get("text", ""), n.get("content-desc", ""), n.get("class", ""), ((x1 + x2) // 2, (y1 + y2) // 2)))
    return found


def find(label, exact=True, timeout=20):
    deadline = time.time() + timeout
    while time.time() < deadline:
        for text, desc, _, centre in nodes():
            for value in (text, desc):
                if value and (value == label if exact else label in value):
                    return centre
        time.sleep(1)
    raise SystemExit(f"Not on screen: {label}")


def tap(label, exact=True, timeout=20):
    x, y = find(label, exact, timeout)
    shell("input", "tap", str(x), str(y))
    time.sleep(1.5)


def tap_at(x, y):
    shell("input", "tap", str(x), str(y))
    time.sleep(1.5)


def type_text(value):
    shell("input", "text", value.replace(" ", "%s"))
    time.sleep(0.5)


def hide_keyboard():
    """Back closes the keyboard, but only press it while the keyboard is up, or it leaves the app."""
    for _ in range(3):
        if "mInputShown=true" not in shell("dumpsys", "input_method"):
            return
        shell("input", "keyevent", "4")
        time.sleep(1.5)


def edit_boxes():
    return sorted(((text, centre) for text, _, cls, centre in nodes() if cls.endswith("EditText")), key=lambda b: b[1][1])


def fill(index, value, secret=False):
    """Types into the index-th text box, a few characters at a time, and retypes it if keys got lost."""
    for _ in range(3):
        text, centre = edit_boxes()[index]
        tap_at(*centre)
        shell("input", "keyevent", "123")  # to the end
        for _ in range(len(text) + 2):
            shell("input", "keyevent", "67")  # delete
        for i in range(0, len(value), 4):
            shell("input", "text", value[i:i + 4])
            time.sleep(0.3)
        time.sleep(1.5)
        typed = edit_boxes()[index][0]
        if (len(typed) == len(value)) if secret else (typed == value):
            return
        print(f"retyping box {index}: got {typed!r}")
    raise SystemExit(f"Couldn't type into box {index}")


def swipe(x1, y1, x2, y2, ms=400):
    shell("input", "swipe", str(x1), str(y1), str(x2), str(y2), str(ms))
    time.sleep(1.5)


def scroll_to(label, exact=True):
    for _ in range(8):
        try:
            return find(label, exact, timeout=2)
        except SystemExit:
            swipe(540, 1700, 540, 900)
    raise SystemExit(f"Couldn't scroll to: {label}")


def shot(name, settle=1.5):
    time.sleep(settle)
    with open(os.path.join(OUT, f"{name}.png"), "wb") as f:
        f.write(adb("exec-out", "screencap", "-p"))
    print("screenshot", name)


def demo_status_bar():
    shell("settings", "put", "global", "sysui_demo_allowed", "1")
    for extras in (
        ["-e", "command", "enter"],
        ["-e", "command", "clock", "-e", "hhmm", "0941"],
        ["-e", "command", "battery", "-e", "level", "100", "-e", "plugged", "false"],
        ["-e", "command", "network", "-e", "wifi", "show", "-e", "level", "4"],
        ["-e", "command", "network", "-e", "mobile", "show", "-e", "datatype", "none", "-e", "level", "4"],
        ["-e", "command", "notifications", "-e", "visible", "false"],
    ):
        shell("am", "broadcast", "-a", "com.android.systemui.demo", *extras)


def back(times=1):
    for _ in range(times):
        shell("input", "keyevent", "4")
        time.sleep(1.5)


def section(name, steps):
    """One part of the tour; if it goes wrong the rest still runs."""
    try:
        steps()
    except BaseException:
        traceback.print_exc()
        failed.append(name)
        shot(f"failed-{name}", settle=0)
        shell("am", "force-stop", PACKAGE)
        shell("am", "start", "-n", f"{PACKAGE}/.MainActivity")
        time.sleep(4)


def seed():
    with open(os.path.join(SEED, "training.json"), "rb") as f:
        subprocess.run(["adb", "exec-in", "run-as", PACKAGE, "sh", "-c", "mkdir -p files && cat > files/training.json"], stdin=f, check=True)


def tab(label):
    tap(label, exact=True)
    time.sleep(1.5)


def train():
    shell("am", "start", "-n", f"{PACKAGE}/.MainActivity")
    find("Training Coach", timeout=60)
    shot("train", settle=3)


def workout():
    tap("Start Workout B")
    find("Log set")
    shot("workout", settle=2)
    tap("Log set")
    find("Rest")
    shot("rest", settle=2)
    back()
    find("Workout under way")
    tap("Discard")
    tap("Discard", exact=True)
    time.sleep(1)


def exercises():
    tab("Exercises")
    shot("exercises", settle=3)
    tap("Lat pulldown")
    find("Setting up")
    shot("exercise", settle=3)
    swipe(540, 1900, 540, 700)
    shot("exercise-2")
    back()


def plans():
    tab("Plans")
    shot("plans", settle=2)
    tap("Upper / Lower")
    shot("plan", settle=2)
    back()
    tap("Make your own plan")
    find("Plan name")
    shot("plan-editor", settle=2)
    back()


def progress():
    tab("Progress")
    shot("progress", settle=2)
    tap("Chest press")
    find("Weight (best set)")
    shot("exercise-progress", settle=2)
    back()


def settings():
    tab("Settings")
    shot("settings", settle=2)
    tap("Change weights")
    swipe(540, 1700, 540, 900)
    shot("settings-stack", settle=2)


def main():
    adb("install", "-r", "-g", APK)
    demo_status_bar()
    seed()
    section("train", train)
    section("workout", workout)
    section("exercises", exercises)
    section("plans", plans)
    section("progress", progress)
    section("settings", settings)
    if failed:
        print("Failed parts:", ", ".join(failed))
        with open(os.path.join(OUT, "failed-here.png"), "wb") as f:
            f.write(adb("exec-out", "screencap", "-p"))


main()
