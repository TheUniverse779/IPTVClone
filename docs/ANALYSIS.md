# Phân tích kỹ thuật app gốc: Smart IPTV Player, Online TV

| | |
|---|---|
| Package | `com.iptv.smartplayer.onlinetv` |
| Phiên bản | 2.1.8 (versionCode 20108) |
| SDK | minSdk 32, targetSdk 37 |
| Nhà phát triển | betasoftmobile |
| Nguồn phân tích | APK kéo từ Pixel 6a (`apk/smartplayer/`), dịch ngược bằng jadx 1.5.3 (`decompiled/smartplayer/`), ảnh chụp app thật (`screenshots/`) |

Code bị R8 làm rối tên nên logic nằm trong `decompiled/smartplayer/sources/p000/*.java`. Tên class obfuscated được ghi kèm để tra lại. Chỉ các model trong `com/iptv/smartplayer/onlinetv/**` còn giữ nguyên tên.

---

## 1. Tổng quan kiến trúc

**Kiến trúc:** 1 Activity (`MainActivity`, có PiP), toàn bộ UI viết bằng Jetpack Compose với Navigation Compose. Mẫu MVVM: ViewModel + Repository + Flow, quản lý trạng thái bằng lớp `Resource` (Success / Loading / Error).

**Thư viện:**

| Nhóm | Thư viện |
|---|---|
| UI | Compose 1.12, Material3 1.4, Navigation-compose 2.10 |
| DI & dữ liệu | Hilt 2.60, Room 2.8.4 (DB `Smart_Iptv`, version 6), Paging 3.3, WorkManager 2.9.1, Coroutines 1.10 |
| Cài đặt | Orhanobut Hawk (SharedPreferences file `Hawk2`, serialize bằng Gson). App **không** dùng DataStore |
| Mạng | Retrofit + Gson + OkHttp 5.0.0-alpha.7 |
| Player | Media3 ExoPlayer 1.9.0 |
| Ảnh | Coil (cùng với Glide, Picasso) |
| Firebase | Remote Config, Analytics, Realtime Database, FCM, Crashlytics |
| Thanh toán | Play Billing 9.1, gói qua lib nội bộ `com.mono.beta_jsc_lib` |
| Quảng cáo | Google Mobile Ads Next-Gen SDK, mediation AppLovin / Meta / InMobi / Mintegral / Pangle / Unity / Vungle; thêm AppsFlyer, TikTok Business SDK, Facebook SDK, UMP consent |

**Khác:**
- Điện thoại khóa dọc, tablet được xoay. Không hỗ trợ Android TV/leanback.
- Shortcut launcher: Unlock Unlimited, Add Playlist, Add Xtream Profile, Play Channels.

---

## 2. Điều hướng & màn hình

NavHost nằm ở `p000/C12382px.java`, màn khởi đầu là `splash`.

### 2.1 Luồng khởi động
1. **`splash`:** ảnh + nhạc splash, dòng chữ "This action may contain ads", tải interstitial (timeout 40 s).
   - Người đã mua gói: vào thẳng `main`.
2. **Mở lần đầu:**
   - `choose_language` → `onboarding` (3 trang):
     - "Welcome to Smart IPTV"
     - "Smart Player with Personalize Watching Tools"
     - "Streaming To Bigger Screen"
   - Sau onboarding: `rate_screen` ("I love it!" / "I'll do later!"), rồi paywall onboarding, rồi `disclaimer_screen` (10 đoạn miễn trừ, checkbox "I confirm License Agreement", nút Accept), rồi `main`.
3. **Mở lại:** hiện paywall (`iap_main` hoặc `iap_trial`), rồi `main`.

### 2.2 Màn chính: `main`
**Bottom bar:** cao 64 dp, nút **+** màu cam nổi ở giữa.

| Tab | Nội dung |
|---|---|
| Home | Banner "Get Pro"; chip lọc Playlist / Recent / Favourite; lưới Playlist; mục Recent; nút chatbot nổi |
| Channels | Danh sách playlist; mở một playlist thì có 2 tab Category / Channels, tìm kiếm, sắp xếp 0-9 và A-Z, chuyển list/grid |
| Xtream | "Who's Watching?": lưới profile, Add Profile, nút sửa |
| Sport | "Sport Matches" (Remote Config `show_sport` bật/tắt) |

**Nút + mở sheet "Add Playlist"** gồm:
- Import Your Playlist URL
- Import Your Xtream Code API
- Play Your Single Stream
- Upload M3U File (tối đa 5 file .m3u/.m3u8)
- Load From Device (chỉ bản premium)

**Form import** có 3 tab: URL / Xtream / Single Stream.
- **URL:** Playlist URL, toggle Playlist Passcode, toggle Auto Update, "How to add playlist?", Add Playlist.
- **Xtream:** Server URL, User Name, Password, "Choose one from our Community playlist".
- **Single Stream:** Stream Link, Play Stream.

**Playlist item:** tim favourite + menu ⋮ gồm Edit (đổi tên, passcode, auto update) và Delete (có xác nhận).

### 2.3 Danh sách route

| Route | Màn |
|---|---|
| `splash`, `choose_language`, `onboarding`, `rate_screen`, `disclaimer_screen` | Khởi động |
| `onboard_tier1` → `add_screen_tier1` → `scan_tier1` → `list_tier1` | Onboarding thay thế (A/B) |
| `main` | Pager 4 tab |
| `add_edit_profile?id_args=` | Thêm/sửa profile Xtream |
| `xtream_home`, `xtream_list_cate?id_cate&key_search`, `xtream_info?id_mov`, `xtream_recent` | Xtream |
| `play_chanel?id_args&url_play_single&is_xtream&media_type&is_device&play_from_iap` | Player chung (single / Xtream / file máy / trận đấu) |
| `play_chanelNew?id_args&play_from_iap` | Player kênh M3U |
| `search_screen` | Tìm kênh |
| `setting_screen`, `language_screen`, `feedback_screen?check_from` | Cài đặt |
| `FAQCommon`, `faq1_Screen`…`faq4_Screen`, `how_to_add_screen`, `how_to_add_xtream` | Hướng dẫn |
| `get_link`, `share_screen`, `favorite_screen` | Community |
| `sport`, `my_match`, `WC`, `WCNOTI` | Thể thao |
| `iap_main`, `iap_feature`, `iap_expired`, `iap_onboard`, `iap_trial`, `iap_trial_new`, `Iap_ob_7`, `Iap_ob_8`, `iap_christmas`, `IAPADS` | Các biến thể paywall (`?show_iap_from=` ghi lại nguồn mở) |

### 2.4 Settings
Thứ tự các mục:
1. Banner Get Pro
2. Restore Purchase
3. Language
4. Quảng cáo chéo app Remote TV
5. Privacy Policy
6. Term of Service
7. FAQ
8. License Agreement
9. Contact for support (trang Facebook)
10. Hai nút cạnh nhau: Share App và Rate App (dialog chấm sao + feedback)

Không có mục chọn theme: enum `ChangeTheme` có tồn tại nhưng app ép dùng giao diện tối.

### 2.5 Passcode (khóa riêng tư)
- **Một passcode chung cho cả app**, lưu **dạng chữ thường (plaintext)** ở pref `create_password`, tối thiểu 4 ký tự.
- Lần đầu: "Create your Passcode" → "Confirm Passcode". Các lần sau: "Enter your Passcode for <tên>".
- Khóa theo từng mục: `isLock` trên playlist/kênh, `isPassword` trên profile Xtream.

---

## 3. Nguồn nội dung

### 3.1 Playlist M3U/M3U8
Logic nằm ở `p000/C12681xy.java` (AppViewModel) và `p000/C12607vy.java` (import job).

**Kiểm tra URL:**
- Không được rỗng.
- Phải bắt đầu bằng `http`, `https`, `mms`, `rtmp` hoặc `rtp`, hoặc kết thúc bằng `.m3u8` hoặc `.ts`.
- Nếu thiếu scheme, app thử `http://` trước, rồi đến `https://`.

**Tên mặc định:** "My Playlist" hoặc "My Playlist N" (đếm bằng pref `COUNT_PLAYLIST`).

**Import từ file:** copy qua SAF vào `cacheDir/tempM3u*.m3u`, parse xong thì xóa file tạm.

**Tải file:** OkHttp GET, **không gửi header nào** (User-Agent mặc định của okhttp), timeout 30 s, **bỏ qua kiểm tra SSL**.

**Thuật toán parse:**
```
insert PlayList → đọc lại theo linkUrl để lấy id
đếm tổng số dòng (lần tải thứ 1) để tính progress
đọc từng dòng (lần tải thứ 2):
  "#EXTINF..." → current = Channel(name, logo, group ?: "Unknown")
      logo  = (?:tvg-logo|logo)="([^"]*)
      name  = tvg-name="([^"]*)"   nếu không có thì lấy chữ sau dấu phẩy ĐẦU TIÊN
      group = group-title="([^"]*)
  dòng URL (điều kiện giống lúc kiểm tra URL) → current.urlLink = line; batch.add
  cứ đủ 800 kênh → insert batch
  mọi dòng khác (#EXTVLCOPT, #KODIPROP, #EXTGRP, ...) → bỏ qua
xong → cập nhật totalChannel; state = SUCCESS / FAILED / CANCEL
nếu 0 kênh → xóa playlist
```

**Group:** không lưu bảng riêng, mà `GROUP BY groupName` lúc truy vấn, cộng thêm nhóm ảo "All".

**Nhiều link cho 1 kênh:** `urlLink` có dấu `;` thì tách thành nhiều MediaItem.

**Không hỗ trợ:** playlist dạng JSON (không tìm thấy dù app quảng cáo có), EPG (`url-tvg`), `tvg-id`, catchup.

### 3.2 Xtream Codes
Retrofit interface ở `p000/InterfaceC12528tt.java`. Mọi call là `GET {server}/player_api.php?username=&password=&action=`:

| action | Tham số thêm | Trả về |
|---|---|---|
| `get_live_categories` / `get_vod_categories` / `get_series_categories` | | `[ {category_id, category_name} ]` |
| `get_live_streams` | | `[ {name, stream_icon, stream_id, category_id} ]` |
| `get_vod_streams` | | `[ {stream_id, name, stream_icon, rating_5based, container_extension, category_id, added} ]` |
| `get_series` | | `[ {series_id, name, cover, plot, cast, director, genre, releaseDate, rating_5based, episode_run_time, youtube_trailer, category_id} ]` |
| `get_vod_info` | `vod_id` | `{info{plot, cast, director, genre, rating, releasedate, duration_secs, backdrop_path[], youtube_trailer}, movie_data{container_extension, name}}` |
| `get_series_info` | `series_id` | `{seasons[{id, season_number, name, episode_count, cover, air_date}], episodes{"<season>": [{id, episode_num, title, container_extension, season, info{movie_image, duration_secs, releasedate}}]}}` |

Không có `get_short_epg`, `xmltv.php`, thông tin tài khoản (`user_info`), hay timeshift.

**Luồng import Xtream:**
1. Tạo profile với id là UUID; id này cũng là khóa `idUser` cho mọi dữ liệu cache.
2. Chạy song song 6 API (3 danh mục + live + VOD + series). Insert từng khúc 500 dòng. Thanh progress giả: 0.2 → 0.65 → 0.99.
3. **Thành công chỉ khi live, VOD và series đều có dữ liệu.** Thiếu bất kỳ loại nào là FAILED và xóa luôn profile. Đây là lỗi: tài khoản chỉ có live sẽ không import được.
4. Chi tiết series (mùa, tập) được tải khi mở và cache lại. Chi tiết phim không cache.

**URL phát** (`p000/eb6.java`):
```
Live   : {server}/{user}/{pass}/{stream_id}                 (không có /live/, không đuôi file)
Movie  : {server}/movie/{user}/{pass}/{stream_id}.{ext}
Episode: {server}/series/{user}/{pass}/{episode_id}.{ext}
```

**Retrofit:** tạo mới cho mỗi lần gọi, trust-all SSL, timeout 30 phút.

### 3.3 Single Stream
Người dùng dán 1 link rồi phát ngay qua route `play_chanel?url_play_single=`. App có lưu lại vào danh sách (giới hạn bản free tính bằng `COUNT_SINGLE`).

### 3.4 Load From Device (premium)
Mở trình chọn ảnh/video của hệ thống để phát file trong máy.

---

## 4. Cơ sở dữ liệu (Room `Smart_Iptv`, version 6)

| Bảng | Cột chính |
|---|---|
| `LIST_CHANNEL` | idPlayList PK, nameList, totalChannel, logo, isFavourite, linkUrl, isLock, lastUpdated |
| `CHANNEL` | idChannel PK, idPlayList, name, logo, urlLink, groupName, isFavourite, isRecent, isLock, lastUpdated; index (idChannel, name) |
| `xtreamProfileEntity` | id (UUID) PK, ProfileName, ServerURL, UserName, Password, isPassword, imgUri |
| `CategoryXtream` | id, idUser, idCategory, name, count, type (LIVE / MOVIE / SERIES) |
| `LiveTV` | id, idStream, idUser, name, icon, idCategory, isFavorite, timeUpdate, totalTime |
| `SeriesMovie` | id, idStream, type, idUser, name, icon, idCategory, rating, duration, cast, plot, director, genre, youtubeTrailer, releaseDate, extension, isFavorite, isRecent, timeUpdate, totalTime, timeViewed |
| `Season` | id, idUser, idSeries, idSeason, name, countSs, seasonNumber, icon; UNIQUE (idUser, idSeason) |
| `Episode` | id, idUser, idSeason, idSeries, idEpisode, extension, name, icon, releaseDate, episodeNum, seasonNumber, isFav, isRecent, timeUpdate, totalTime, timeViewed; UNIQUE (idUser, idEpisode) |
| `History` | id, idUser, name (lịch sử từ khóa tìm kiếm); UNIQUE (idUser, name) |
| `communityIPTV` / `communityXtream` / `communitySingleStream` | Bản sao local của link community, có thêm isFavorite, isShare, isAutoUpdate |
| `MATCH_NOTIFY` | Nhắc lịch trận World Cup |
| `favourite_match` | id, leagueSlug, sportSlug |

**Truy vấn quan trọng:**
- **Danh sách kênh:** `WHERE idPlayList=? AND (groupName=? OR ?='All') AND name LIKE %q%`, sắp xếp theo `Filter` gồm AZ, ZA, DATE_AZ (theo idChannel) và DATE_ZA.
- **Nhóm:** `SELECT groupName, count(*) … GROUP BY groupName`, sắp theo tên hoặc số lượng.
- **Favourites trang Home:** UNION giữa kênh và playlist có isFavourite=1.
- **Continue watching Xtream:** UNION giữa episode và seriesmovie có isRecent=1, mới nhất trước.

---

## 5. Player

**Engine:**
- Media3 ExoPlayer 1.9.0 **để mặc định hoàn toàn**: không tùy chỉnh LoadControl, DataSource hay User-Agent.
- Định dạng thực sự phát được: HLS, DASH, progressive (TS, MP4, MKV, FLV…), UDP, file/content.
- **Không có** RTSP, RTMP hay FFmpeg, dù chuỗi hiển thị trong app ghi là có hỗ trợ.

**Giao diện điều khiển:** tự viết bằng Compose, `PlayerView` đặt `useController=false`.

| Vị trí | Thành phần |
|---|---|
| Thanh trên | Back, tên kênh chạy chữ, Share (link app trên Store), hẹn giờ ngủ, Favourite |
| Giữa | Tua −10 s, Play/Pause (hoặc vòng loading), tua +10 s |
| Dưới | Thanh tua (cập nhật mỗi 1 s, hiện HH:MM:SS), Fullscreen |
| Thanh trượt | Độ sáng (chỉnh window `screenBrightness`), âm lượng (`STREAM_MUSIC`) |
| Nút chức năng | Channel list, Subtitle (chọn ngôn ngữ text track hoặc None), Popup Play (PiP), Cast, Lock |

**Hành vi:**
- Tự ẩn điều khiển sau 6 s.
- Fullscreen: ép xoay ngang, ẩn thanh hệ thống.
- **Màn dọc:** dưới video có tên playlist, thẻ "Unlock Unlimited" và lưới kênh chia trang.
- **Lỗi:**
  - Màn dọc: overlay "Channel currently unavailable. Please retry later." có mũi tên sang kênh trước/sau.
  - Fullscreen: dialog "Oops! “X” is not available now" với nút Retry.
  - Không tự thử lại.
- **PiP:** tỉ lệ 16:9, có 1 RemoteAction play/pause (broadcast `action_play_pause`). Chỉ vào PiP khi bấm nút, không tự vào khi rời app.
- **Hẹn giờ ngủ:** 15 / 30 / 45 / 60 phút; hết giờ thì pause player. Không lưu lại và không có đếm ngược.
- **Cast:** không có Google Cast SDK. Chỉ mở Cài đặt hệ thống (`WIFI_DISPLAY_SETTINGS` hoặc `CAST_SETTINGS`). Chỉ premium hoặc sau khi xem quảng cáo thưởng.
- **Resume (phim/tập Xtream):** lúc thoát lưu `timeViewed` và `totalTime`; còn dưới 4 s thì coi như đã xem xong. Lần sau mở thì tua tới vị trí đã lưu.
- **Không có:** chọn tỉ lệ khung hình, chọn audio track, tốc độ phát, vuốt chỉnh sáng/âm lượng, MediaSession, phát nền.

---

## 6. Hướng dẫn tìm nguồn (Guide)

**Cách mở trang gợi ý:**
- Sheet **WebView trong app** (`p000/lk0.java` → `C11876gp.java`): tiêu đề "Playlist", bật JavaScript, DOM storage, zoom, có thanh tiến trình, nút Cancel và "Maybe later".
- **Chrome Custom Tabs** (`C12620wa.java`) cho nút "Search in browser".

**Trang được gợi ý:**
- IPTV: `github.com/locnvbetasoft/iptv-playlist`
- Xtream: `github.com/locnvbetasoft/xtream-playlist`
- Single stream: `github.com/locnvbetasoft/single_stream`
- Google search `newest free global iptv`
- `theipfire.com/iptv-m3u-playlist-links/`, `tonkiang.us`, `ip-tv.app`

**Các màn hướng dẫn:**
- "How to add" cho playlist và Xtream: các bước A–E (Import URL, Upload M3U, Import from Device, Single Stream, Xtream), kèm video hướng dẫn trong `res/raw`.
- FAQ: 4 trang và mục "Finding a playlist?".

**Chatbot:** chạy **offline hoàn toàn**, so khớp từ khóa.

| Từ khóa | Nhóm câu trả lời |
|---|---|
| m3u / playlist | Hướng dẫn playlist |
| single / stream link | Hướng dẫn single stream |
| xtream / username / server url | Hướng dẫn Xtream |
| Không khớp | "I'm sorry, I couldn't understand that." |

- Câu trả lời là hướng dẫn từng bước, kèm các nút Watch Tutorial / Add Playlist / Contact Support / Check FAQs.
- 4 câu hỏi gợi ý sẵn.

Remote Config: `url_iptv` điền sẵn URL M3U, `xtream_account` điền sẵn form Xtream.

---

## 7. Thể thao

**API ESPN không chính thức:** base `https://site.api.espn.com/apis/site/v2/`
- `sports/{sport}/{league}/scoreboard?dates=yyyyMMdd[-yyyyMMdd]`
- `sports/{sport}/{league}/summary?event={id}`

**Giải đấu:**
- Danh sách lấy từ Remote Config `sport_data`: `Map<sportType, List<League{id, name, logo, slug, isHot}>>`.
- Giải người dùng chọn lưu ở pref `selected_leagues`.
- Ghi cứng 3 giải bóng đá: `esp.1`, `eng.1`, `ita.1`.

**Màn chính:** Top Event / My Match / Live / Upcoming.
- Lọc ngày: hôm nay, mai, hôm qua, 7 ngày, tháng này, năm nay, chọn ngày.
- Live polling: đã đá thì 30 s; còn dưới 1 giờ thì 60 s; dưới 24 giờ thì 5 phút; xa hơn thì 30 phút.

**Nhắc lịch** (WorkManager one-time):
- Trước giờ đá 15 phút (World Cup: 15 và 30 phút).
- Kênh thông báo `sport_notify` / `match_notify`, nội dung "Live now: Home vs Away".

**Nút xem trận:**
- App gốc lấy link live / full / highlight từ Remote Config `sport_play` và `wc_replay`.
- Bản free xem quảng cáo thưởng được 15 phút.
- Có màn "Add your iPTV source to unlock live match channels".

---

## 8. Community (Firebase Realtime Database)

- **Node:** `iptv{channelName, url, date}`, `xtream{userName, serverUrl, password}`, `singleStream{channelName, url}`.
- **Ghi dữ liệu:** `push().setValue()`.
- **Màn hình:** Get Link (Copy / Edit / Delete / Save / Report), Share ("Start Sharing", "Add new share"), Favorite.
- Có quảng cáo thưởng chặn trước.

---

## 9. Kiếm tiền

### 9.1 In-app purchase

| Gói | SKU | Loại |
|---|---|---|
| Weekly | `sub_week_iptv`, `week_sale_iptv` | subs |
| Monthly | `sub_month_iptv`, `sub_month_offer` (trial 3 ngày), `sub_month_iptv_pro` | subs |
| 6 tháng trả góp | `sub_iptv_instalment` | subs |
| Yearly | `sub_year_iptv`, `sub_year_sale` | subs |
| Lifetime | `sub_lifetime_iptv`, `lifetime_iptv_premium` | inapp |
| Chỉ gỡ quảng cáo | `remove_ads_week`, `remove_ads_week2` | subs |

- **Paywall thấy trên máy (VN):** Lifetime 990.000 ₫ (gạch giá 4.950.000 ₫, nhãn "Best Offer / Save 80%"), Monthly 199.000 ₫, Yearly 499.000 ₫.
- **Quyền lợi được liệt kê:** Unlimited Playlist & Channels, Private Channels with Passcode, Unlimited Xtream, Cast to TV, Remove Ads.
- **Trạng thái mua** lưu ở pref: `is_purchase`, `is_remove_ads`, `is_lifetime_purchase`. Được kiểm tra lại mỗi lần app resume.

**Giới hạn của bản free:**

| Tính năng | Giới hạn |
|---|---|
| Playlist | 1 |
| Profile Xtream | 1 |
| Single stream | 1 |
| Favourite | 4 |
| Phát kênh | Xem quảng cáo thưởng trước (1 lần, pref `reward_cn`) |
| Cast | Xem quảng cáo thưởng (`r_cast`) |
| Xem trận | Quảng cáo thưởng, mở khóa 15 phút |
| Load From Device | Chỉ premium |

### 9.2 Quảng cáo
Mọi ad unit lấy từ Remote Config, không ghi cứng. Mỗi vị trí có 3 tầng high / medium / default.

| Loại | Vị trí |
|---|---|
| App open | Khi app trở lại foreground |
| Interstitial | Splash; mở kênh; thoát player; chuyển tab. Giãn cách `time_show_ads`, mặc định 15 s |
| Native | Language, onboarding, import, channels, xtream, home, back, splash… |
| Banner | home, add_playlist, language, player (ẩn khi đang PiP) |
| Rewarded | Phát kênh, cast, xem trận, community |

- Có UMP consent.
- Thông báo nhắc hằng ngày "Have you watch "X" today?" chỉ gửi cho user free.

### 9.3 Remote Config
Remote Config là "bảng điều khiển" của toàn app. Các nhóm khóa chính:

| Nhóm | Khóa |
|---|---|
| Tính năng | `show_sport`, `show_wc`, `check_rate`, `check_disclaimer` |
| Paywall | `check_iap_main`, `check_iap_onboard`, `iap_sale`, `iap_exit`, `iap_remove_ads` |
| Giao diện & A/B | `ui_all`, `ui_language`, `country_scripts` |
| Điền sẵn nguồn | `url_iptv`, `xtream_account` |
| Thể thao | `sport_data`, `sport_play`, `schedule_wc` |
| Quảng cáo | Ad unit, các mốc thời gian (timeout), cấu hình JSON cho native |

---

## 10. Thiết kế

- **Giao diện tối.** Màu đều ghi cứng trong Compose.

| Vai trò | Màu |
|---|---|
| Accent | `#F56F10` (cam) |
| Nền | `#151419`, `#1B1B1E`, `#262626`, `#313136`, `#333338`; gradient thẻ `#151419 → #262626` |
| Chữ | Trắng / `#FBFBFB`; phụ `#95969C`, `#979C9E`, `#878787`, `#C5C5C5` |
| Nhấn | Vàng `#FFC446`; đỏ `#C90000` |

- **Font:** font hệ thống, các độ đậm 400 / 600 / 700.
- **Animation:** Lottie cho nút +, loading khi quét, welcome, play. Video hướng dẫn nằm trong `res/raw`.
- **Ngôn ngữ:** en (mặc định), ar, de, es, fr, hi, in, it, ja, ko, pt, th, tr, vi.
- **Chuỗi UI:** `decompiled/smartplayer/resources/res/values/strings.xml`, khoảng 1.200 chuỗi.
- **Ảnh chụp các màn:** `screenshots/` (home, tab, sheet Add, form import, danh sách kênh, sort/filter, settings, paywall, cổng quảng cáo thưởng).

---

## 11. Lỗi và điểm yếu của app gốc (sửa trong bản clone)

1. Tải file M3U **2 lần**: một lần để đếm dòng, một lần để parse.
2. **Trust-all SSL** và hostname verifier luôn trả true. Google Play có cảnh báo và có thể từ chối app vì "unsafe TrustManager".
3. Tạo mới Retrofit/OkHttp cho mỗi lần gọi; timeout 30 phút.
4. Parser bỏ qua `tvg-id`, `#EXTVLCOPT:http-user-agent/referrer` và `#KODIPROP`, nên nhiều link không phát được. Dấu phẩy trong thuộc tính làm hỏng tên kênh. `tvg-name=""` làm tên kênh rỗng.
5. Import Xtream thất bại nếu tài khoản thiếu 1 trong 3 loại live / VOD / series.
6. URL server có `/` ở cuối sinh ra `//` trong link phát. Link live không theo chuẩn `/live/…/{id}.ts|.m3u8`.
7. Không có EPG, chọn audio track, tỉ lệ khung hình, tốc độ phát, cử chỉ vuốt, MediaSession.
8. Quảng cáo là có hỗ trợ RTSP/RTMP nhưng không đóng gói extension tương ứng.
9. `onUserLeaveHint` gọi đệ quy vô hạn (`m20694n`). Khôi phục độ sáng chia nhầm cho 225 thay vì 255.
10. Passcode lưu dạng chữ thường, dùng chung cho cả app.
11. Chèn nhiều quảng cáo tới mức ảnh hưởng trải nghiệm: app open, native full màn, interstitial, rồi rewarded trước khi phát.
