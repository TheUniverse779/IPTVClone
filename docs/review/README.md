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

Choose "All or some functionality is restricted", then add instructions:

> **Name:** Test playlist
>
> **Instructions:** The app is a media player and contains no content. To test it, add this sample playlist of public test streams and open movies:
> `https://raw.githubusercontent.com/iptvstore779/iptv_review/refs/heads/main/test-playlist.m3u`
> 1. Open the app and tap **+** (bottom centre) › **Playlist URL**.
> 2. Paste the link above and tap **Add playlist**.
> 3. Tap **View channels** and pick any channel to play.
>
> No login is needed. To test a single stream, tap **+** › **Single stream** and paste
> `https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8`.
>
> **Xtream Codes** accounts are not included: that feature logs in to the IPTV provider the user
> already subscribes to, so it can only be tested with provider credentials. The playlist above
> covers the player, the channel list, categories, favourites, search and continuation. The Xtream
> screens are the same player and lists, with Movies / Series / Live tabs.

## Before each release

Re-import the playlist and play one channel from each group. Public test servers sometimes change URLs; replace any entry that stops working with another developer test stream or open movie.
