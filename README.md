# KouchY

A tiny late-night Tokyo ramen shop that lives on your Android home screen. The shop owner reacts to how you use your phone today.

## Moods

| Mood | When |
|---|---|
| Senang | Normal phone usage |
| Kesepian | Phone left alone for 3+ hours, or barely used today |
| Capek | More than 5 hours of screen time today, or 1.5 hours non-stop |
| Ngantuk | Still on the phone after 11 PM |
| Tidur | Phone untouched at night |

It is always raining on this little street: neon ラーメン sign, swaying noren and lanterns, steaming bowls, a glowing vending machine, wet asphalt reflections and puddle ripples. The sky follows the time of day, the rain gets heavier when the owner is lonely, and the shop closes while they sleep.

## Tech

- Kotlin, no third-party dependencies
- `UsageStatsManager` for screen time and unlock count
- Pixel art rendered at runtime on a 96x96 canvas
- `AppWidgetProvider` with a `ViewFlipper` playing 16 frames at 8 fps
- Frames served as PNGs through a read-only `ContentProvider`, avoiding the RemoteViews bitmap size limit
- Inexact repeating alarm for refreshes every ~15 minutes

## Build

```
./gradlew assembleDebug
```

Requirements: Android Studio with SDK 37, min Android 10 (API 29).

## Setup on device

1. Install the app and open **Rumahku**.
2. Tap **Kasih izin baca screen time** and enable usage access.
3. Tap **Pasang widget ke Home Screen**.
