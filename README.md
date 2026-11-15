# Training Coach

An Android training app for the **Ironman IM-309** single-station home multi gym.

- Every exercise the machine can do, by station, with setup, steps, tips and a built-in animation of it on the machine (and a small link to YouTube)
- Ready-made plans for the IM-309, and your own
- Workout mode: the pin to use and its weight, a rest timer, one-tap logging
- Progress per exercise with charts and personal bests; suggests the next pin when you hit your reps
- The weight stack as it is on the IM-309 (3.5 kg top plate + 11 × 5.7 kg), changeable, in kg or lb
- Updates itself from GitHub releases

## The exercises and animations

`app/src/main/assets/exercises.json` is made by `tools/animations/exercises.py`. `tools/animations/render.py` draws every animation's start, middle and end on the machine for checking:

```
cd tools/animations
python3 exercises.py && cp exercises.json ../../app/src/main/assets/
python3 render.py exercises.json /tmp/sheet.png
```

## Building

GitHub Actions builds a signed APK on every push to `main` and puts it in a draft release. The signing key comes from the repository secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD`; without them it makes a debug build.
