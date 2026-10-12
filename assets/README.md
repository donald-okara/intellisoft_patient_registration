# Screen recording

## Recording limit disclaimer

Android caps a single screen recording at **3 minutes (180 seconds)** — this applies both to
the on-device Quick Settings "Screen Record" tile and to `adb shell screenrecord` (which
refuses any `--time-limit` above 180s outright). The full user journey (sign up, both sign-up
and sign-in error cases, two patients through both BMI-routed assessment forms, the listing
date filter, sign-out) runs longer than that in a single continuous take, so it doesn't fit in
one clip. The recording is therefore split across multiple files, each one picking up where the
previous one was cut off by the OS, not by choice.

## Current status

- `part_1.mp4` — ≈ 1:35. Covers the start of the journey up to where the OS cut the recording.
- `part_2.mp4` — ≈ 2:59, right at the device's 3-minute cutoff. Picks up the walkthrough where
  `part_1.mp4` left off.
- Combined: ≈ 4:33, under the assignment's 5-minute limit. Watch in order (`part_1.mp4` then
  `part_2.mp4`) for the full flow.
