# Training Coach

An Android training app for the **Ironman IM-309** single-station home multi gym.

- Every exercise the machine can do, by station, with setup, steps, tips and a built-in 3D animation of it on the machine (and a small link to YouTube)
- Ready-made plans for the IM-309, and your own
- Workout mode: the pin to use and its weight, a rest timer, one-tap logging
- Progress per exercise with charts and personal bests; suggests the next pin when you hit your reps
- The weight stack as it is on the IM-309 (3.5 kg top plate + 11 × 5.7 kg), changeable, in kg or lb
- Updates itself from GitHub releases

## The exercises and animations

`app/src/main/assets/exercises.json` (setup, steps, muscles, captions) is made by `tools/animations/exercises.py`.

The animations are a 3D person on a 3D IM-309, to scale, rendered in `tools/animations/3d` (numpy, scikit-image, OpenCV): the body is smooth shapes with its muscles, the worked ones in red (main) and orange (also). Each exercise is 13 frames from start to end, in `app/src/main/assets/anim3d/<id>/`, which the app plays forwards and back:

```
cd tools/animations/3d
python3 exercises3d.py --preview chest_press   # start, middle, end in preview/
python3 exercises3d.py                         # every frame of every exercise in out/
python3 pack.py                                # out/ → the app's assets as WebP
```

## Building

GitHub Actions builds a signed APK on every push to `main` and puts it in a draft release. The signing key comes from the repository secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD`; without them it makes a debug build.
