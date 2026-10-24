# Project submission

Complete this file during the hackathon and use it to prepare your submission through the method announced by the organizers. Do not submit your project to the template repository.

## Project

- **Project name:** InstaDoodle
- **Short description:** Android app that turns an image into a simplified hand-drawn stroke set and replays it as touch gestures (AccessibilityService overlay) on drawing canvases such as Instagram DM Draw. On-device only, no network.
- **Code repository URL:** https://github.com/KryptoniaN10/InstaDoodle
- **Final submitted implementation commit SHA:** `142779d` (later commits only update docs)
- **Working demo URL or local demo steps:** Install the debug APK, enable the accessibility service, pick the Logo sample → Generate sketch → Test canvas → Start. See README "First run" and `docs/screenshots/`.
- **Demo access requirements:** Android 8.0+ device or emulator. No accounts needed for the test canvas; Instagram account only for the (untested) Instagram flow.

## Participants

List participants according to the organizers' current participation format. Remove unused rows.

| Participant name | GitHub handle | Contribution |
| --- | --- | --- |
| Rinjan | KryptoniaN10 | Project idea and specification |

## Run and demonstrate

- **Prerequisites and versions:** JDK 17, Android SDK platform 34; Gradle 8.7 via wrapper
- **Install command:** `./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk`
- **Required environment variable names:** None (release signing secrets are optional, see `.github/workflows/release.yml`)
- **Start command:** Launch InstaDoodle from the device launcher
- **Demo walkthrough:** Onboarding → enable service → sample image → PHOTO/LINE_ART + sliders → Generate → Replay/Save → Test canvas → overlay Start/Pause/Stop/Calibrate → Library → Settings
- **Checks performed and results:** `./gradlew testDebugUnitTest` pass, `./gradlew connectedDebugAndroidTest` 4/4 pass (API 34 emulator), manual emulator verification (see PROGRESS.md)
- **Known limitations or incomplete features:** Not tested in real Instagram (checklist in `docs/INSTAGRAM_TEST_CHECKLIST.md`); single emulator configuration verified
- **Libraries, APIs, and public datasets used:** [OpenCV 4.9.0](https://opencv.org/) (Apache 2.0), Jetpack Compose / AndroidX, Kotlin coroutines. Sample images generated for this repo (portrait derived from scikit-image's public-domain `astronaut` image).
- **AI usage record:** [AI_USAGE.md](AI_USAGE.md)
- **Development change log:** [CHANGELOG.md](CHANGELOG.md)

## Submission checklist

- [ ] The project follows the organizers' current project-start and eligibility instructions.
- [ ] The participation format follows the organizers' current instructions.
- [ ] Any AI usage record included accurately identifies the tools used; this template requires no particular AI tool or provider.
- [ ] The code repository URL, short description, and working demo are provided.
- [ ] README setup and demo instructions match the submitted code.
- [ ] Actual checks and known limitations are documented.
- [ ] Meaningful changes are documented and committed throughout development.
- [ ] The final implementation is committed and pushed, and its SHA is recorded above.
- [ ] No credentials or private data are included in the repository or demo instructions.
- [ ] The organizers' announced submission method and timing have been followed.

Event: **Devin Hack Kochi**. Follow the organizers' current submission method and deadline. The event has no major AI usage constraints.
