# Review test playlist

`test-playlist.m3u` is the playlist for Google Play reviewers (Play Console › App content › **App access**) and for QA before each release.

It contains only public **test streams published for video-player developers** and **open movies** (Blender Foundation, CC BY 3.0). No TV channels, no copyrighted programming. 15 entries in 5 groups:

| Group | Contents | Format |
|---|---|---|
| Live test streams | Unified Streaming Live Demo, Shaka Player Live Test | HLS live |
| Open movies (HLS) | Big Buck Bunny, Tears of Steel (with subtitles), Tears of Steel (Unified Streaming) | HLS VOD |
| Open movies (DASH) | Big Buck Bunny, Tears of Steel, Sintel | DASH |
| Open movies (MP4) | Sintel trailer, Big Buck Bunny clip, Sintel clip | MP4 |
| Player test patterns | Apple BipBop (fMP4/16:9/4:3), Envivio DASH | HLS, DASH |

Last checked 2026-10-06: all 15 played in the app (emulator). Removed: Akamai Live Test (`cph-p2p-msl.akamaized.net`). Its variant playlist returns 404.

## Publish

Published at **[iptvstore779/iptv_review](https://github.com/iptvstore779/iptv_review)** (public):

`https://raw.githubusercontent.com/iptvstore779/iptv_review/refs/heads/main/test-playlist.m3u`

Verified 2026-10-06: the raw file is identical to `docs/review/test-playlist.m3u`; a fresh install imports it as **15 channels, 5 groups**, and one channel from each group played.

When you edit the playlist, update both copies (this file and the GitHub repo).

## Text for Play Console › App access

Play Console › App content › App access › **All or some functionality is restricted** › **Add new instructions** (up to 5 entries).
Use **three** entries: one for the Xtream login (the only part that needs credentials) and one each for the
playlist and the single stream (no login). Only **Name** is mandatory; leave Username and Password empty for 2 and 3.
The first release (1.0.0) was rejected with "we could not find the information you entered" because the Xtream screen
had no test credentials. Server source: [`xtream-demo/`](xtream-demo/).

### 1. Xtream login (487 chars in the last box)

| Field | Value |
|---|---|
| Name | `Xtream demo account` |
| Username | `reviewer` |
| Password | `review2026` |

**Any other instructions**
```
Server URL: https://iptv-review-xtream.xtream-review-demo.workers.dev

Open the app, tap + (bottom centre) and choose Xtream Codes. Enter the Server URL, then the username and password above, and tap "Log in & sync". Tap "Watch now" when it finishes.

The Movies, Series and Live tabs all contain playable items (HLS, DASH and MP4). The app is a media player and contains no content; this demo server only serves public test streams and open movies by the Blender Foundation (CC BY 3.0).
```

### 2. Playlist URL — no login (344 chars)

| Field | Value |
|---|---|
| Name | `Playlist URL (no login)` |
| Username / Password | leave empty |

**Any other instructions**
```
Open the app, tap + (bottom centre) > Playlist URL. Paste this link and tap "Add playlist":

https://github.com/iptvstore779/iptv_review/raw/main/test-playlist.m3u

Then tap "View channels" and pick any channel. The playlist has live test streams and open movies in HLS, DASH and MP4. No login is needed, and it contains no copyrighted content.
```

### 3. Single stream — no login (247 chars)

| Field | Value |
|---|---|
| Name | `Single stream (no login)` |
| Username / Password | leave empty |

**Any other instructions**
```
Open the app, tap + (bottom centre) > Single stream. Paste this link and tap "Play now":

https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8

No login is needed. This is a public test stream published by Mux for player developers, not a TV channel.
```

## Before each release

Re-import the playlist and play one channel from each group. Public test servers sometimes change URLs; replace any entry that stops working with another developer test stream or open movie.
