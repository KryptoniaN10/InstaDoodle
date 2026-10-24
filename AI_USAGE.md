# AI usage record

If maintaining this record, briefly document the actual contributions of whichever AI tools you use at Devin Hack Kochi. Add a row after each meaningful task or milestone. Keep summaries concise and omit secrets, private data, and full chat transcripts.

## Event build log

| Date and time with timezone | Tool / model, if known | Task | Assistance and affected files | Human review / corrections | Validation and result |
| --- | --- | --- | --- | --- | --- |
| 2026-10-24T21:39+05:30 | Devin (Cognition AI) | Milestones 0–2: Gradle project, CI, accessibility overlay + gesture player, image-to-strokes engine, Compose UI, storage, sample images, unit tests | Wrote all files under `app/`, `.github/workflows/`, Gradle files | Pending | `assembleDebug` and `testDebugUnitTest` pass; APK launched on API 34 emulator |
| 2026-10-24T21:46+05:30 | Devin (Cognition AI) | Milestones 3–4: emulator verification, two overlay fixes, instrumented tests, README/PROGRESS/checklist/screenshots | `overlay/GesturePlayer.kt`, `overlay/OverlayController.kt`, `app/src/androidTest/`, docs | Pending | `connectedDebugAndroidTest` 4/4 pass; test-canvas replay with pause/stop/calibrate verified manually on emulator; Instagram not tested |
| 2026-10-24T23:23+05:30 | Devin (Cognition AI) | Sketch accuracy: Canny PHOTO edges, speck/spur cleanup, smoothing, accuracy test | `engine/ImagePreprocessor.kt`, `engine/Cleanup.kt`, `engine/StrokeExtractor.kt`, `engine/Stroke.kt`, `ui/MainViewModel.kt`, `AccuracyTest.kt` | Pending | Unit + instrumented tests pass; portrait preview checked on emulator |
| 2026-10-24T23:35+05:30 | Devin (Cognition AI) | More detail: stroke limit up to 1000, 1024 px working size, Detail-driven Canny thresholds | `engine/Stroke.kt`, `engine/ImagePreprocessor.kt`, `ui/MainViewModel.kt`, `ui/Screens.kt`, `README.md` | Pending | Unit + instrumented tests pass; portrait 180 → 256 strokes at defaults on emulator |

## Template preparation

This historical preparation note refers to the older template. It does not define Devin Hack Kochi's event rules or AI tool requirements.

On 10 October 2026, Codex prepared this documentation template from the organizer's supplied rules and checked the linked Luma page for the event name and dates. This covered `README.md`, `RULES.md`, `AGENTS.md`, `CHANGELOG.md`, `AI_USAGE.md`, `SUBMISSION.md`, `.gitignore`, and the pull request checklist. Agent validation covered local links, event consistency, required documents, and diff whitespace; organizer review is pending.

This preparation is separate from participant project implementation and participant AI usage during the event.

## Event documentation correction — 2026-10-24T21:53:05+05:30

- Tool: Codex.
- Task: correct the organizer-shared template to Devin Hack Kochi and clarify
  that the event has no major AI usage constraints.
- Affected files: `.github/pull_request_template.md`, `AGENTS.md`, `AI_USAGE.md`, `CHANGELOG.md`, `README.md`, `RULES.md`, `SUBMISSION.md`.
- Human review: pending maintainer review of this pull request.
- Validation: documentation diff, whitespace, and local Markdown link checks;
  no application code changed and no runtime checks were needed.
