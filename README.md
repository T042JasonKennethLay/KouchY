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

Neon ラーメン sign, swaying noren and lanterns, steaming bowls, a glowing vending machine and a purple neon skyline behind the shop. The sky follows the time of day and the shop closes while the owner sleeps.

## Weather

The weather shuffles every 3 hours:

| Weather | Scene |
|---|---|
| Hujan | Slanted rain, wet asphalt reflections, puddle ripples, dripping eaves |
| Salju | Falling snow, snow caps on the shop, snowy street with tire tracks |
| Gugur | Drifting maple leaves, leaf piles, big orange moon |
| Musim panas | Fireworks at night, wind chime, big summer clouds |
| Cerah | Clear night with a vivid purple neon skyline |

When the owner is lonely the sky turns grey and it starts to drizzle.

## Guests

Invite up to 3 friends into the shop. Each guest has a name, hair style, hair color, outfit color and something to do (eating ramen, sipping tea or playing on their phone). They sit on the red stools in the widget with a pixel name plate under their seat. Guests stay home when the shop is lonely or closed.

## App

The companion app is fully pixel styled: starry backdrop, neon pixel font headers, chunky 3D pixel buttons, a mood panel with segmented screen time bars, guest cards with pixel portraits and a guest editor with a live preview of the shop.

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
