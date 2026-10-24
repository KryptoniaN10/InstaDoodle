# Change log

Record each meaningful project change during the event, newest first. Update this file in the same commit as the change. Use a real timestamp with timezone, for example `YYYY-MM-DDTHH:MM:SS+05:30`.

## Event build log

## 2026-10-24T21:39:26+05:30 — Android skeleton, overlay gesture replay, image-to-strokes engine

- Changed: Gradle project (`app/`), GitHub Actions CI (`.github/workflows/android.yml`, `release.yml`), accessibility service + overlay (`overlay/`), engine (`engine/`), Compose UI (`ui/`), settings/library storage (`data/`), bundled sample images (`app/src/main/assets/samples/`), JVM unit tests.
- Why: Milestones 0–2 of the InstaDoodle plan: risky gesture-dispatch path first, then the stroke engine and UI.
- Validation: `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` pass locally (JDK 17, AGP 8.5.2). APK installed and launched on an API 34 x86_64 emulator.
- Limitations: Not tested inside the real Instagram app. JVM tests run the pure-Kotlin half of the engine; the OpenCV half is covered by the instrumented test.

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
