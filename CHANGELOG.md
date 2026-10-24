# Change log

## 2026-10-24T21:53:05+05:30 — Correct Devin Hack Kochi event guidance

- Changed: corrected the current event name and AI tool guidance in `.github/pull_request_template.md`, `AGENTS.md`, `AI_USAGE.md`, `CHANGELOG.md`, `README.md`, `RULES.md`, `SUBMISSION.md`.
- Why: the organizer shared an older event template; Devin Hack Kochi has no
  major AI usage constraints and the template must not require a particular AI provider.
- Scope: preserved project implementation and historical assistance records.
- Removed obsolete template logistics, participation limits, and credit promises; current organizer instructions remain authoritative.
- Validation: reviewed documentation diffs, checked whitespace with `git diff --check`, and checked local Markdown links for new breakage.
- Limitations: maintainer review is pending; application tests were not run for this documentation-only correction.

Record each meaningful project change during the event, newest first. Update this file in the same commit as the change. Use a real timestamp with timezone, for example `YYYY-MM-DDTHH:MM:SS+05:30`.

## Event build log

## 2026-10-24T23:23:03+05:30 — More accurate sketches (PHOTO edges, noise and spur cleanup)

- Changed: PHOTO mode in `ImagePreprocessor` now uses Canny edges (bilateral + Gaussian smoothing, median-based thresholds) instead of thresholded DoG; new `Cleanup` removes ink specks before thinning, prunes short skeleton spurs, and smooths pixel staircase before simplification (`StrokeExtractor`, `EngineConfig`); simplify epsilon 1.2 → 0.8 px; minimum stroke length at 50% detail 12 → 9 px; added `AccuracyTest`; refreshed preview screenshots; version 1.0.2.
- Why: The DoG output was filled blobs that thinning collapsed to dots, so faces (eyes, nose, mouth, chin) disappeared from portrait sketches.
- Validation: `./gradlew testDebugUnitTest` passes (AccuracyTest: recall ≥ 0.95, precision 1.0 on all three samples); `./gradlew connectedDebugAndroidTest` 4/4 pass; on the emulator the portrait preview now shows the face, logo and handwriting unchanged (`docs/screenshots/02`, `09`, `10`).
- Limitations: Canny draws both sides of thick lines in photos; very small details (e.g. eyes in a small face) can still be dropped by the minimum stroke length. Instagram replay accuracy not tested.

## 2026-10-24T22:55:00+05:30 — Fix release workflow signing fallback

- Changed: `.github/workflows/release.yml` builds the debug variant first when no release keystore secret is set, so the debug keystore exists for the fallback signing config; version bumped to 1.0.1.
- Why: The `v1.0.0` release run failed in `validateSigningRelease` (debug keystore not found on the fresh runner), so no APK was attached to a GitHub Release.
- Validation: `./gradlew assembleRelease` passes locally; CI result recorded in the `v1.0.1` release run.
- Limitations: Without the `RELEASE_KEYSTORE_*` secrets each release is signed with a different throwaway key.

## 2026-10-24T21:46:00+05:30 — Emulator verification fixes, instrumented tests, docs

- Changed: `GesturePlayer` waits 400 ms before retrying a cancelled stroke so overlay taps (Pause/Stop) are not swallowed; `OverlayController.show()` re-adds overlay windows dropped by the system; added `SketchEngineInstrumentedTest`; rewrote `README.md`; added `PROGRESS.md`, `docs/INSTAGRAM_TEST_CHECKLIST.md`, `docs/screenshots/`; filled in `AI_USAGE.md` and `SUBMISSION.md`.
- Why: Milestones 3–4: verify replay on the test canvas and document the app.
- Validation: `./gradlew testDebugUnitTest assembleDebug` pass; `./gradlew connectedDebugAndroidTest` 4/4 pass on the API 34 emulator. Manually on the emulator: generated logo/portrait/handwriting sketches, saved them to the library, drew the logo on the test canvas via the overlay, Pause, Resume, Stop ("Stopped at 9/43") and Calibrate all worked (screenshots in `docs/screenshots/`).
- Limitations: Not tested in the real Instagram app. Single emulator configuration.

## 2026-10-24T21:39:26+05:30 — Android skeleton, overlay gesture replay, image-to-strokes engine

- Changed: Gradle project (`app/`), GitHub Actions CI (`.github/workflows/android.yml`, `release.yml`), accessibility service + overlay (`overlay/`), engine (`engine/`), Compose UI (`ui/`), settings/library storage (`data/`), bundled sample images (`app/src/main/assets/samples/`), JVM unit tests.
- Why: Milestones 0–2 of the InstaDoodle plan: risky gesture-dispatch path first, then the stroke engine and UI.
- Validation: `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` pass locally (JDK 17, AGP 8.5.2). APK installed and launched on an API 34 x86_64 emulator.
- Limitations: Not tested inside the real Instagram app. JVM tests run the pure-Kotlin half of the engine; the OpenCV half is covered by instrumented tests added in the next entry.

Entry template:

```text
## <actual timestamp> — <short change title>

- Changed: <feature or files changed>
- Why: <reason for the change>
- Validation: <commands or manual checks actually run, with results>
- Limitations: <known issues or "None identified in these checks">
```

## Template setup — 10 October 2026

- Changed: Added the event README, rules, agent guide, change log, AI usage record, submission checklist, Git ignore rules, and pull request checklist.
- Why: Give participants a consistent starting point for event rules, basic coding patterns, documented changes, and regular commits.
- Validation: Checked local Markdown links, event detail consistency, required documents, and diff whitespace before publishing.
- Limitations: Documentation scaffolding only; no participant project, application, or working demo is included. GitHub publication and template status are verified separately after creation.
