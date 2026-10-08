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

Play Console › App content › App access › **All or some functionality is restricted** › **Add new instructions**.

Only the Xtream login needs credentials; the playlist and single stream are not logins, so they go in the
**"Any other instructions"** box of the same entry (500-character limit — the text below is 416).

**Name** (max 60)
```
Xtream demo account
```

**Username**
```
reviewer
```

**Password**
```
review2026
```

**Any other instructions** (max 500). The Server URL has no field of its own in this form, so it must appear here.
```
Server URL: https://iptv-review-xtream.xtream-review-demo.workers.dev (enter it at + > Xtream Codes, then tap Log in & sync)
No login needed for the rest:
- Playlist URL: + > Playlist URL, paste https://github.com/iptvstore779/iptv_review/raw/main/test-playlist.m3u
- Single stream: + > Single stream, paste https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8
Content: public test streams and Blender open movies only.
```

The first release (1.0.0) was rejected with "we could not find the information you entered" because the Xtream
screen had no test credentials. Server source: [`xtream-demo/`](xtream-demo/).

## Before each release

Re-import the playlist and play one channel from each group. Public test servers sometimes change URLs; replace any entry that stops working with another developer test stream or open movie.
