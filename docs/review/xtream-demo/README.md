# Xtream demo server for Google Play review

Google rejected the first release because the **Xtream login screen had no test credentials**
(Play Console › App access). This folder is a tiny Xtream Codes API server that reviewers can log in to.

- **Content:** only public test streams for video-player developers and Blender Foundation open movies
  (CC BY 3.0), the same sources as [`../test-playlist.m3u`](../test-playlist.m3u). No TV channels, no copyrighted programming.
- **Login:** username `reviewer`, password `review2026` (change both in `worker.js` if you like).
- **Data:** 5 live channels (2 groups), 7 movies (HLS, DASH, MP4), 2 series (3 + 3 episodes).
- **Hosting:** Cloudflare Workers free plan (100,000 requests/day, always on, no sleeping).
- Tested 2026-10-07 in the app (Android 15 emulator, run locally): login + sync OK, and an HLS movie, a DASH movie,
  an MP4, a series episode and a live channel all played.

## Deploy (dashboard, no install) — about 10 minutes

1. Sign up / log in at <https://dash.cloudflare.com> (free).
2. Left menu **Compute (Workers)** › **Workers & Pages** › **Create** › **Create Worker** (template "Hello World").
3. Name it, e.g. `iptv-review-xtream`, then **Deploy**.
4. Click **Edit code**, delete everything in the editor, paste the whole content of [`worker.js`](worker.js), then **Deploy** again.
5. Copy the Worker address shown at the top, e.g. `https://iptv-review-xtream.<your-subdomain>.workers.dev`.
6. Open that address in a browser: you should see "Xtream Codes API demo server for app review".
7. Send the address to Claude to test it in the app.

## Deploy (command line, optional)

```bash
cd docs/review/xtream-demo
npx wrangler login
npx wrangler deploy
```

## Test locally

```bash
node docs/review/xtream-demo/local.mjs
```

Server URL `http://127.0.0.1:8787` (on an emulator: `adb reverse tcp:8787 tcp:8787` first).

## Keep it running

The server must stay online while the app is in review **and** for later updates (every new release is reviewed again).
If a test stream stops working, replace its URL in `worker.js` with another developer test stream or open movie and deploy again.
