# Instagram DM Draw test checklist (for a human tester)

InstaDoodle has **only been verified on the in-app test canvas on an Android emulator**.
Nobody has run it against the real Instagram app yet. This checklist is for the first person
who does. Please record the results at the bottom and open an issue or PR with them.

## 1. Setup

- [ ] Install the debug APK from the latest GitHub Actions run (artifact `instadoodle-debug-apk`).
- [ ] Open InstaDoodle and follow onboarding.
- [ ] Android 13+: if the accessibility toggle is greyed out ("Restricted setting"), open
      **Settings → Apps → InstaDoodle → ⋮ → Allow restricted settings**, then enable the service in
      **Settings → Accessibility → InstaDoodle**.
- [ ] Back in the app, the Home screen shows "Accessibility service: ON".

## 2. Sanity check on the test canvas first

- [ ] Pick the **Logo** sample, LINE_ART, Generate sketch.
- [ ] Tap **Test canvas**, then **Start** on the overlay. Confirm the logo is drawn inside the
      dashed rectangle, Pause/Resume and Stop work, and **Calibrate** draws a cross through the
      rectangle centre. If this fails, stop here: Instagram will not work either.

## 3. Open Instagram DM Draw

- [ ] In InstaDoodle Preview, tap **Send to Instagram** (opens Instagram with the overlay showing).
- [ ] Open any DM thread (a chat with yourself or a test account is ideal).
- [ ] Open the camera / photo editor → **Draw** (the scribble tool). Pick a pen and colour.
- [ ] Note your device model, Android version and Instagram version below.

## 4. Place the rectangle

- [ ] Drag the ✥ handle to move the dashed rectangle over the drawing area.
- [ ] Drag the ⤡ handle to resize it. Keep it away from Instagram's toolbars (pen picker, colour
      bar, Done button) so strokes never land on a button.
- [ ] Tap **Calibrate**. The cross should be centred in the rectangle on Instagram's canvas.
      If it is off, use X−/X+/Y−/Y+ (offsets are saved) and Calibrate again.

## 5. Start with defaults

Defaults: speed 1200 px/s, min stroke 60 ms, max stroke 4000 ms, delay between strokes 150 ms,
dot 40 ms.

- [ ] Tap **Start** with the Logo sample. Watch the overlay progress counter.
- [ ] Repeat with Portrait (PHOTO) and Handwriting.
- [ ] Try Pause → Resume and Stop in the middle of a drawing.

## 6. Tuning guide

All values live in InstaDoodle **Settings** and apply the next time you tap Start.

| Symptom | Try |
| --- | --- |
| Strokes missing / dropped, "skipped" count grows | Increase **Delay between strokes** (250–400 ms), lower **Drawing speed** (600–900 px/s) |
| Lines look jagged or cut short | Lower **Drawing speed**, raise **Min stroke duration** (100–150 ms) |
| Instagram lags or stutters | Increase **Delay between strokes**; reduce **Max strokes** on Home to 100–150 |
| Drawing shifted from the rectangle | Calibrate, then X/Y offset nudges until the cross is centred |
| Dots do not appear | Raise **Dot duration** (60–100 ms) |
| Very long strokes get truncated | Lower **Max stroke duration** so they are drawn faster, or lower Detail |
| Taps on Instagram buttons by accident | Shrink the rectangle away from the toolbars |

## 7. Results (fill in)

| Field | Value |
| --- | --- |
| Device / Android version | |
| Instagram version | |
| Sample(s) tested | |
| Settings used | |
| Strokes drawn / skipped | |
| Pause / Resume / Stop OK? | |
| Alignment OK after calibration? | |
| Notes / screenshots | |
