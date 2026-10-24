# InstaDoodle progress

Status of each milestone from the project brief. "Verified" means checked on an Android 14
(API 34, x86_64) emulator or by automated tests in this repo. Nothing has been tested in the
real Instagram app; see [docs/INSTAGRAM_TEST_CHECKLIST.md](docs/INSTAGRAM_TEST_CHECKLIST.md).

| Milestone | Status | Evidence |
| --- | --- | --- |
| M0 — Gradle skeleton, CI, accessibility overlay, demo shapes, test canvas | Done | `./gradlew assembleDebug`, `.github/workflows/android.yml`, screenshots 03–06 |
| M1 — Image → strokes engine (PHOTO / LINE_ART, thinning, tracing, simplify, cap, ordering) | Done | JVM tests in `app/src/test`, instrumented tests in `app/src/androidTest`, screenshots 02/09/10 |
| M2 — Compose UI: Home, Preview + replay, Library, Settings, Send to Instagram | Done | screenshots 01, 02, 09–12 |
| M3 — Gesture mapping, timing, pause/resume/stop, calibration + offsets | Done on emulator test canvas | screenshots 04–08, `GestureTest` |
| M4 — Onboarding, restricted-settings help, README, checklist, release workflow | Done | `README.md`, `docs/INSTAGRAM_TEST_CHECKLIST.md`, `.github/workflows/release.yml` |
| Real Instagram DM Draw test | **Not done** | Needs a human with a real device and account |

## Definition of done

1. CI-built debug APK installs on a device — debug APK built locally and installed on the emulator;
   CI workflow uploads `instadoodle-debug-apk` (first CI run happens once the code is pushed).
2. Image → preview → replay for three sample images — verified (logo, portrait, handwriting).
3. Overlay draws on the in-app test canvas with pause/stop — verified on the emulator.
4. README, PROGRESS.md, Instagram test checklist — written.

## Emulator findings fixed during verification

- Taps on the overlay's Pause/Stop could be lost while a gesture was in flight: a real touch
  cancels the injected gesture and the immediate retry cancelled the touch in turn. Retries now
  wait 400 ms (`GesturePlayer.cancelBackoffMs`) so the user's tap goes through.
- The system can remove overlay windows while a UiAutomation client is connected (e.g.
  `uiautomator dump`). `OverlayController.show()` now re-adds them instead of assuming they exist.

## Accuracy update (v1.0.2)

PHOTO mode switched from thresholded DoG to Canny edges, plus speck removal, spur pruning and stroke smoothing. The portrait sample previously lost the face (DoG blobs thinned to dots); it now keeps eyes, nose, mouth and chin (`docs/screenshots/09-preview-portrait.png`). `AccuracyTest` checks strokes stay within 2 px of the cleaned source lines.

## Detail update (v1.0.3)

Max strokes now goes up to 1000 (default 400), default Detail is 70%, and images are processed at up to 1024 px. Portrait sample: 256 strokes at defaults, 478 at Detail 100% / Max 1000.

## UI update (v1.0.4)

Animated splash, gradient brand theme with dark mode, redesigned Home/Preview/Library/Settings, and an estimated drawing time on Preview. Overlay + test canvas re-checked after the redesign.

## Known limitations

- Instagram compatibility is unknown. Instagram may change its UI, ignore injected gestures, or
  smooth/drop fast strokes; tune with the checklist.
- One stroke = one gesture. Strokes longer than the platform maximum gesture duration are drawn
  faster rather than split.
- Skeleton tracing leaves small spurs at sharp corners on some images.
- Tested on one emulator configuration only.
