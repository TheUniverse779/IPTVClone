# Ads (Google AdMob via the EZTech Ads SDK)

Integrated 2026-10-08 from `eztech-ads-com.cp.livetv.iptvplayer-1.0.8.zip` (see `Implement Ads SDK.docx` in that zip). Formats live so far: **banner** and **interstitial**. Rewarded, native and app-open are not used yet.

## Where things live

| Piece | File |
|---|---|
| SDK (vendored Maven repo) | `libs/maven/com/ezt/ads/…-1.0.8.aar`, wired in `settings.gradle.kts` and `app/build.gradle.kts` |
| Host config (app ID, events, revenue) | `App.kt` → `configureAds()` |
| One `AdsKit` for the app + consent + banner/interstitial helpers | `app/src/main/java/com/iptvplayer/app/ads/AppAds.kt` |
| Placement keys | `app/src/main/assets/ads/placements.json` (release), `app/src/debug/assets/ads/app-placements.json` (debug) |
| Banner strip | `res/layout/layout_ad_banner.xml` |
| Where the banner is attached | `BaseActivity` (see below), plus `activity_main.xml` / `activity_xtream_home.xml`, which keep their own copy above the tab bar |
| Call sites | `MainActivity`, `XtreamHomeActivity` (banner), `ui/common/SourceActions.kt` (interstitial before opening a source) |
| Kill switch | `Features.ADS` in `Features.kt` |

The SDK is package-restricted: it only serves ads when the applicationId is `com.cp.livetv.iptvplayer`.

## How the flow works

1. `App.onCreate` → `AdsSdk.configure(AdsHostConfig(...))`. Must run once, before anything else.
2. `SplashActivity` → `AppAds.initializeFromSplash()` loads and applies the placements JSON (Firebase Remote Config → cache → bundled asset), then `AppAds.requestConsent()` runs the UMP consent form and initialises Google Ads.
3. Consent finishes → `AppAds.ready` turns true. The consent form belongs to the Splash window, so the splash waits (up to 10 s) before navigating.
4. `AppAds.showBanner()` waits for `ready` (up to 10 s) and then shows `main_banner`. Requesting earlier fails: the Google Ads runtime finishes its own init a few seconds after consent and rejects requests until then.

### Where the banner sits

`BaseActivity` wraps every screen in a vertical column — the screen's own layout on top (weight 1) and
`layout_ad_banner` at the very bottom — so the banner sits at the bottom edge of the activity and the
content is pushed up rather than covered. On the two tab screens the order is content › tab bar › banner.
`PlayerActivity` overrides `showAdBanner = false`: no ads while watching, they would cover the controls.
The player is the only screen without a banner.

Insets: the column takes the system-bar padding, so the banner clears the gesture/navigation bar. Screens
that draw edge-to-edge themselves (`MovieDetailActivity`, `SeriesDetailActivity`, `applyInsets = false`)
keep their own top handling and the column only gets the bottom padding.
5. `AppAds.showInterstitial()` is called from `SourceActions.launchSource` before opening a playlist, Xtream profile or single stream; navigation runs in its callback. The SDK applies its own cooldown (`fullscreenIntervalMs`, 60 s here).

## Configuration

`placements.json` shipped in the app:

```json
{ "key": "main_banner",   "format": "banner", "enabled": true, "adUnitId": "…/9214589741", "refreshIntervalMs": 60000 }
{ "key": "next_screen",   "format": "inter",  "enabled": true, "adUnitId": "…/1033173712" }
```

The same file can be pushed without an app update through **Firebase Remote Config**, parameter `ad_placements`, containing the whole JSON. The SDK prefers Remote Config, then the cache, then the asset.

Cooldowns are per placement, so keep the interstitial rare: a full-screen ad every time a source is opened is a common reason for bad reviews.

## Before the production release

- [ ] Replace the **app ID** in `app/src/main/res/values/strings.xml` (`admob_app_id`). It is currently `ca-app-pub-3940256099942544~3347511713`, Google's public test app ID.
- [ ] Replace the **ad unit IDs** in `app/src/main/assets/ads/placements.json` with the real ones from AdMob. The debug build can keep the test IDs.
- [ ] Create the AdMob **app** for `com.cp.livetv.iptvplayer` and link the app to AdMob in Play Console.
- [ ] Buy/confirm a **mediation** account if the publisher intends to add networks — not needed for AdMob-only.
- [ ] Play Console › Data safety and **Advertising ID: Yes** (the Ads SDK declares `com.google.android.gms.permission.AD_ID`; this entry was "No" before ads were integrated).
- [ ] Privacy policy must mention personalised advertising and the ad SDK.
- [ ] **Consent**: keep the UMP form; the SDK shows it where the region requires it. If the app is ever published in the EEA/UK, add the consent message in the AdMob console (Privacy & messaging).
- [ ] Decide about `isAdFree`: it is hard-coded `false` (no purchase removes ads). Wire it to a real purchase if one is added.

## Notes and gotchas

- Core library desugaring is **required** by the SDK (`isCoreLibraryDesugaringEnabled = true`, `desugar_jdk_libs`).
- The SDK uses the **next-generation** Google Mobile Ads SDK (`com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.4.0`), not the classic `play-services-ads`.
- It pulls in Facebook's SDK for bidding; harmless, but it logs its own warnings.
- Test ad units still apply in debug builds only because the debug asset file has them. Never ship test IDs to release.
- `Features.ADS = false` disables everything (no configure, no consent, no banner, no interstitial) — useful for the review builds if a reviewer complains about ads.
