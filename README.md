# IPTV Player

An Android IPTV player: M3U/M3U8/JSON playlists, Xtream Codes, single streams, a sports schedule, and an in-app guide for finding public playlists. The app ships with no built-in content; users add their own sources.

- Kotlin, Activity/Fragment, XML + ViewBinding (no Compose), Hilt, Room + Paging 3, DataStore, WorkManager
- Media3 ExoPlayer (HLS/DASH/RTSP/progressive), MediaSession, PiP
- Package `com.iptvplayer.app`, minSdk 24, target/compileSdk 35

## Build

```bash
./gradlew assembleDebug        # or: bash build.sh :app:assembleDebug (Git Bash, uses Android Studio's JBR)
./gradlew testDebugUnitTest
```

## Layout

| Path | What |
|---|---|
| `app/` | Android app |
| `design/prototype.html` | Approved clickable HTML prototype (serve: `python -m http.server 5173 --directory design`) |
| `design/_gen_*.py`, `_svg2vd.py` | Generators for XML layouts / vector drawables from the prototype |
| `design/_mock_xtream.py` | Mock Xtream server for device testing (`adb reverse tcp:8766 tcp:8766`, user `demo`/`demo`) |
| `design/_adb.sh` | Device test helpers (`shot`, `tapid`, `ui`, `crash`) |
| `PLAN.md`, `docs/ANALYSIS.md` | Plan and analysis |

## Before publishing

- App name / icon / applicationId
- `app/google-services.json` (Community, Remote Config)
- Privacy / terms URLs and support e-mail in `ui/settings/Settings.kt` (currently `example.com`)
- Release signing config
