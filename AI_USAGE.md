# AI usage record

Use Codex during the hackathon and briefly document its actual contribution here. Add a row after each meaningful task or milestone. Keep summaries concise and omit secrets, private data, and full chat transcripts.

## Event build log

| Date and time with timezone | Tool / model, if known | Task | Assistance and affected files | Human review / corrections | Validation and result |
| --- | --- | --- | --- | --- | --- |
| 2026-10-24T21:39+05:30 | Devin (Cognition AI) | Milestones 0–2: Gradle project, CI, accessibility overlay + gesture player, image-to-strokes engine, Compose UI, storage, sample images, unit tests | Wrote all files under `app/`, `.github/workflows/`, Gradle files | Pending | `assembleDebug` and `testDebugUnitTest` pass; APK launched on API 34 emulator |
| 2026-10-24T21:46+05:30 | Devin (Cognition AI) | Milestones 3–4: emulator verification, two overlay fixes, instrumented tests, README/PROGRESS/checklist/screenshots | `overlay/GesturePlayer.kt`, `overlay/OverlayController.kt`, `app/src/androidTest/`, docs | Pending | `connectedDebugAndroidTest` 4/4 pass; test-canvas replay with pause/stop/calibrate verified manually on emulator; Instagram not tested |

## Template preparation

On 10 October 2026, Codex prepared this documentation template from the organizer's supplied rules and checked the linked Luma page for the event name and dates. This covered `README.md`, `RULES.md`, `AGENTS.md`, `CHANGELOG.md`, `AI_USAGE.md`, `SUBMISSION.md`, `.gitignore`, and the pull request checklist. Agent validation covered local links, event consistency, required documents, and diff whitespace; organizer review is pending.

This preparation is separate from participant project implementation and participant AI usage during the event.
