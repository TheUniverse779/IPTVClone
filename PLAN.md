# Kế hoạch clone: Smart IPTV Player

Chi tiết kỹ thuật của app gốc nằm trong [docs/ANALYSIS.md](docs/ANALYSIS.md). Ảnh chụp app thật nằm trong [screenshots/](screenshots/).

## 0. Mục tiêu & nguyên tắc

- **Tính năng và trải nghiệm tương đương app gốc:** playlist M3U, Xtream, Single Stream, player, Favourite / Recent, passcode, thể thao, community, hướng dẫn tìm nguồn (WebView + Custom Tabs), chatbot. **Tạm thời không làm IAP và quảng cáo** (gói mua, paywall, giới hạn bản free, mọi loại ads).
- **App không chứa nội dung:** người dùng tự tìm link trên các trang được gợi ý rồi dán vào, giống app gốc.
- **Làm tốt hơn app gốc ở những điểm yếu đã liệt kê** trong [ANALYSIS §11](docs/ANALYSIS.md): parser đầy đủ hơn, không trust-all SSL, player nhiều tính năng hơn, import Xtream không bắt buộc đủ 3 loại.
- **Gần như mọi thứ bật/tắt được qua Remote Config:** tính năng, danh sách trang gợi ý. Nhờ vậy bạn đổi hành vi mà không phải phát hành bản mới.
- **Thiết kế:** giữ bố cục và phối màu (nền tối, accent cam), nhưng **tự vẽ asset riêng**: icon, banner. Không dùng logo ESPN, Sky, beIN… trong banner hay ảnh store.
- **Duyệt giao diện bằng HTML trước khi code:** mỗi màn được dựng bằng HTML để bạn duyệt; chỉ code XML sau khi đã duyệt (xem §5).

## 1. Stack công nghệ

> **UI dùng View system: XML + ViewBinding, mỗi màn là 1 Activity; Fragment chỉ dùng cho tab và dialog.** App gốc viết bằng Compose, nhưng bản clone làm lại toàn bộ giao diện bằng XML, theo kiểu các project bạn đang làm (4KWallpaper, ClockLiveWallpaper, EdgeLight2026…).

| Hạng mục | Chọn | Ghi chú |
|---|---|---|
| Ngôn ngữ | Kotlin 2.x | Code UI viết bằng Kotlin, giao diện là XML |
| UI | XML layout + **ViewBinding**, Material Components (MaterialButton, MaterialCardView, BottomSheetDialogFragment, TabLayout, ChipGroup, TextInputLayout, Slider), ConstraintLayout, RecyclerView + ListAdapter/DiffUtil, ViewPager2 (chỉ cho carousel/onboarding nếu cần) | Không dùng DataBinding: ViewBinding nhẹ và nhanh build hơn |
| Kiến trúc | MVVM: `ViewModel` + `StateFlow`; Activity/Fragment `collect` bằng `repeatOnLifecycle(STARTED)` | **Mỗi màn là 1 Activity**; Fragment chỉ dùng cho tab và dialog (xem §2). Base class: `BaseActivity<VB>`, `BaseFragment<VB>`, `BaseDialog<VB>`, `BaseBottomSheet<VB>` |
| DI | Hilt + KSP | `@AndroidEntryPoint` cho Activity/Fragment; mỗi Activity có ViewModel riêng (`by viewModels()`), tab trong cùng Activity dùng chung qua `activityViewModels()` |
| Điều hướng | **Intent** giữa các Activity (mỗi Activity có `companion fun start(context, …)` đóng gói extras). **Không dùng** Navigation Component / `nav_graph.xml` | Tab trong Activity chuyển bằng `FragmentManager` show/hide (giữ trạng thái, không tạo lại) |
| DB | Room + KSP, Paging 3 cho danh sách lớn | Kênh / VOD có thể lên tới hàng chục nghìn dòng |
| Cài đặt | DataStore Preferences (proto nếu cần) | Thay cho Hawk |
| Mạng | OkHttp 4/5 + Retrofit + kotlinx.serialization | **Một client dùng chung**, có User-Agent tùy chỉnh |
| Ảnh | Glide (hoặc Coil 3 bản View) | Bạn đã quen Glide |
| Player | Media3 ExoPlayer + `PlayerView` (XML, `use_controller=false`, controller tự làm bằng layout riêng) + HLS / DASH / RTSP / datasource-rtmp, **MediaSession** | Hỗ trợ đủ các giao thức app gốc ghi là có (RTSP / RTMP) |
| Nền | WorkManager (auto update playlist, nhắc trận đấu) | |
| Firebase | Remote Config, Analytics, Crashlytics, Realtime Database (community), FCM | |
| Animation | Lottie (`LottieAnimationView`), Shimmer cho trạng thái loading | |
| minSdk / target | 24 / 36 | App gốc để minSdk 32, quá cao; hạ xuống để phủ nhiều máy hơn |

## 2. Cấu trúc project

### 2.1 Quy tắc màn hình
- **Mỗi màn = 1 Activity**, mở bằng Intent; tham số truyền qua extras, gói trong `companion fun start(context, …)`.
- **Fragment chỉ dùng ở chỗ bắt buộc:**
  - **Tab của bottom bar** (`MainActivity`, `XtreamHomeActivity`): mỗi tab 1 Fragment.
  - **Dialog / bottom sheet**: `DialogFragment` / `BottomSheetDialogFragment`, để không bị mất khi xoay màn.
- **Tab ở đầu màn chỉ đổi dữ liệu của danh sách** (Category/Channels, IPTV/Xtream/Single, Movie & Series/Live…): dùng `TabLayout` + `RecyclerView`, **không cần Fragment**.
- **Form nhiều tab** (màn Import URL / Xtream / Single): 3 layout `<include>` bật/tắt, không cần Fragment.
- Popup nhỏ (Sort by, menu ⋮): `PopupWindow` / `PopupMenu`, không phải Fragment.

### 2.2 Cây package

Dùng **1 module `app`**, chia package như các project hiện tại của bạn:

```
app/src/main/java/<package>/
├─ App.kt                 # @HiltAndroidApp: Firebase, WorkManager
├─ base/                  # BaseActivity<VB>, BaseFragment<VB>, BaseDialog<VB>, BaseBottomSheet<VB>, BaseAdapter
├─ di/                    # AppModule, DatabaseModule, NetworkModule, PlayerModule
├─ data/
│  ├─ database/           # AppDatabase, entity, dao, migration
│  ├─ datastore/          # SettingsDataStore (thay Hawk)
│  ├─ network/            # OkHttpProvider, XtreamApi, EspnApi, dto
│  ├─ parser/             # M3uParser, M3uLineTokenizer, JsonPlaylistParser
│  └─ repository/         # PlaylistRepo, ChannelRepo, XtreamRepo, SportRepo, CommunityRepo, GuideRepo
├─ player/                # PlayerManager (ExoPlayer), MediaSessionService, PipHelper, SleepTimer
├─ remote/                # RemoteKeys, RemoteConfigRepo (+ res/xml/remote_config_defaults.xml)
├─ analytics/             # Events, Analytics
├─ ui/
│  ├─ splash/             # SplashActivity
│  ├─ disclaimer/         # DisclaimerActivity            (language + onboarding: bạn tự code)
│  ├─ main/               # MainActivity + tab fragment + AddSourceBottomSheet
│  ├─ importer/           # ImportActivity, UploadM3uActivity, ImportProgressDialog
│  ├─ playlist/           # PlaylistDetailActivity, EditPlaylistDialog, SortPopup, adapter
│  ├─ search/             # SearchActivity
│  ├─ passcode/           # PasscodeDialog
│  ├─ xtream/             # AddEditProfileActivity, XtreamHomeActivity + tab fragment, XtreamCategoryActivity,
│  │                      # MovieDetailActivity, SeriesDetailActivity, XtreamRecentActivity, XtreamSyncDialog,
│  │                      # ProfileActionsSheet, XtreamAccountSheet, ProfileSwitcherSheet, XtreamExpiredDialog
│  ├─ player/             # PlayerActivity + các sheet của player
│  ├─ sport/              # SportMatchesActivity, MyMatchActivity, MatchDetailActivity, các sheet
│  ├─ community/          # GetLinkActivity, MyShareActivity, CommunityFavoriteActivity, ShareDialog
│  ├─ guide/              # HowToAddActivity, FaqActivity, FaqDetailActivity, ChatbotActivity, WebGuideBottomSheet
│  └─ settings/           # SettingsActivity, SettingsFragment, FeedbackActivity, LicenseDialog, RateDialog
└─ util/                  # UrlUtils, TimeFormat, Clipboard, Extensions, KeyboardUtils
```

### 2.3 Danh sách Activity (mỗi màn 1 Activity)

| # | Màn | Activity | Layout | Ghi chú |
|---|---|---|---|---|
| 1 | Splash | `SplashActivity` | `activity_splash` | Launcher; nối vào luồng language/onboarding của bạn khi mở lần đầu |
| 2 | License / Disclaimer | `DisclaimerActivity` | `activity_disclaimer` | Chạy lần đầu |
| 3 | Màn chính (bottom bar) | `MainActivity` | `activity_main` + `layout_bottom_bar` | Chứa 4 tab fragment (§2.4) |
| 4 | Import URL / Xtream / Single | `ImportActivity` | `activity_import` + `form_import_url` / `form_import_xtream` / `form_import_single` | Extra `tab` để mở đúng tab |
| 5 | Upload M3U | `UploadM3uActivity` | `activity_upload_m3u` | Chọn tối đa 5 file |
| 6 | Chi tiết playlist (Category / Channels) | `PlaylistDetailActivity` | `activity_playlist_detail` | 2 tab = 2 RecyclerView ẩn/hiện |
| 7 | Tìm kiếm kênh | `SearchActivity` | `activity_search` | Tìm trên mọi playlist + lịch sử |
| 8 | Thêm / sửa profile Xtream | `AddEditProfileActivity` | `activity_add_edit_profile` | |
| 9 | Trang chủ Xtream | `XtreamHomeActivity` | `activity_xtream_home` | Chứa 4 tab fragment (§2.4) |
| 10 | Danh sách theo category / See all | `XtreamCategoryActivity` | `activity_xtream_category` | Extra: type, categoryId, keyword |
| 11 | Chi tiết phim | `MovieDetailActivity` | `activity_movie_detail` | App gốc dùng bottom sheet; tách thành Activity cho đủ chỗ backdrop, trailer, "More like this" |
| 12 | Chi tiết series | `SeriesDetailActivity` | `activity_series_detail` | Chọn mùa → danh sách tập |
| 13 | Continue watching | `XtreamRecentActivity` | `activity_xtream_recent` | |
| 14 | Player | `PlayerActivity` | `activity_player` + `layout_player_controller` | PiP, xoay ngang, `launchMode=singleTask` |
| 15 | Lịch / kết quả thi đấu | `SportMatchesActivity` | `activity_sport_matches` | Lọc ngày, giải |
| 16 | Trận của tôi | `MyMatchActivity` | `activity_my_match` | |
| 17 | Chi tiết trận | `MatchDetailActivity` | `activity_match_detail` | Nút Watch mở sheet chọn kênh |
| 18 | Community: Get Link | `GetLinkActivity` | `activity_get_link` | Tab IPTV / Xtream / Single = TabLayout lọc dữ liệu |
| 19 | Community: link tôi chia sẻ | `MyShareActivity` | `activity_my_share` | |
| 20 | Community: đã lưu | `CommunityFavoriteActivity` | `activity_community_favorite` | |
| 21 | Hướng dẫn thêm nguồn | `HowToAddActivity` | `activity_how_to_add` | Extra `type`: url / upload / device / single / xtream |
| 22 | FAQ | `FaqActivity` | `activity_faq` | |
| 23 | Chi tiết FAQ | `FaqDetailActivity` | `activity_faq_detail` | |
| 24 | Chatbot | `ChatbotActivity` | `activity_chatbot` | |
| 25 | Cài đặt | `SettingsActivity` | `activity_settings` | Chỉ chứa `SettingsFragment`, dùng chung với tab Settings |
| 26 | Feedback | `FeedbackActivity` | `activity_feedback` | |

Màn Language và Onboarding: bạn tự code.

### 2.4 Fragment (chỉ cho tab)

| Activity chứa | Fragment | Layout |
|---|---|---|
| `MainActivity` | `HomeFragment` | `fragment_home` |
| | `PlaylistsFragment` (tab Channels: danh sách playlist) | `fragment_playlists` |
| | `XtreamProfilesFragment` ("Who's Watching?") | `fragment_xtream_profiles` |
| | `SportFragment`, hoặc `SettingsFragment` khi RC `show_sport` tắt | `fragment_sport` / `fragment_settings` |
| `XtreamHomeActivity` | `XtreamMovieFragment`, `XtreamLiveFragment`, `XtreamSearchFragment`, `XtreamFavoriteFragment` | `fragment_xtream_movie`, `fragment_xtream_live`, `fragment_xtream_search`, `fragment_xtream_favorite` |
| `SettingsActivity` | `SettingsFragment` (tái sử dụng) | `fragment_settings` |

### 2.5 Dialog / Bottom sheet

| Class | Loại | Dùng ở |
|---|---|---|
| `AddSourceBottomSheet` | BottomSheet | Nút + ở màn chính |
| `ImportProgressDialog` | Dialog | Import M3U ("Scanning Channels", %) |
| `XtreamSyncDialog` | Dialog | Đăng nhập + đồng bộ Xtream; kết quả "Đã thêm" / lỗi |
| `ProfileActionsSheet` | BottomSheet | Nhấn giữ / ⋮ ở 1 profile: xem, đồng bộ, thông tin, sửa, khóa, xóa |
| `XtreamAccountSheet` | BottomSheet | Thông tin tài khoản Xtream (user_info) |
| `ProfileSwitcherSheet` | BottomSheet | Đổi profile nhanh từ `XtreamHomeActivity` |
| `XtreamExpiredDialog` | Dialog | Chạm vào profile đã hết hạn |
| `EditPlaylistDialog` | Dialog | Sửa playlist |
| `PasscodeDialog` | Dialog full màn | Tạo / xác nhận / nhập passcode |
| `ConfirmDialog` | Dialog | Xóa, thoát, xác nhận chung |
| `ChannelListSheet`, `SubtitleSheet`, `AudioTrackSheet`, `AspectRatioSheet`, `TimerSheet` | BottomSheet | Player (màn dọc; khi fullscreen, danh sách kênh là panel bên cạnh nằm ngay trong `activity_player`) |
| `PlayerErrorDialog` | Dialog | Player fullscreen: "Oops… not available" + Retry |
| `LeaguePickerSheet`, `WatchMatchSheet` | BottomSheet | Thể thao |
| `ShareDialog` | Dialog | Community |
| `WebGuideBottomSheet` | BottomSheet full chiều cao | Guide: WebView mở trang gợi ý |
| `LicenseDialog`, `RateDialog` | Dialog | Settings, lần đầu import |

`SortPopup` và menu ⋮ dùng `PopupWindow` / `PopupMenu`.

### 2.6 Layout XML khác
- **Item:** `item_playlist_grid`, `item_playlist_list`, `item_group`, `item_channel_list`, `item_channel_grid`, `item_profile`, `item_movie_poster`, `item_continue_watching`, `item_episode`, `item_season_chip`, `item_match`, `item_league`, `item_faq`, `item_chat_bot`, `item_chat_user`, `item_community_link`
- **Player:** `layout_player_controller` (overlay tự vẽ), `layout_player_error`, `layout_player_side_channels`
- **Dùng chung:** `layout_toolbar`, `layout_empty_state`, `layout_bottom_bar`

**Resource dùng chung:**
- `values/colors.xml` chứa bảng màu tối (`#151419`, `#262626`, `#F56F10`…).
- `values/themes.xml`: `Theme.Material3.Dark.NoActionBar`, tùy chỉnh `colorPrimary` = cam.
- `values/styles.xml` cho button, card, text.
- `drawable/`: shape bo góc, gradient thẻ, selector cho tab.
- `font/`: Inter hoặc hệ thống.
- `raw/`: Lottie và video hướng dẫn.
- `values-*/strings.xml` cho 14 ngôn ngữ.

## 3. Mô hình dữ liệu (Room)

Giữ cấu trúc bảng gần giống app gốc (xem [ANALYSIS §4](docs/ANALYSIS.md)), bổ sung các cột để sửa hạn chế cũ:

| Bảng | Thay đổi so với app gốc |
|---|---|
| `playlist` | Thêm `sourceType` (URL / FILE), `autoUpdate`, `userAgent?`, `epgUrl?`, `lastSync` |
| `channel` | Thêm `tvgId`, `tvgChno`, `userAgent?`, `referrer?`, `headersJson?`, `drmJson?`, `catchup?`, `sortIndex`; index `(playlistId, groupName)` và `(playlistId, name)` |
| `xtream_profile` | Thêm `status`, `expDate`, `activeCons`, `maxConnections`, `allowedOutputFormats`, `timezone` (lấy từ `user_info` / `server_info`); `passcodeLocked`; `avatarColor`; `liveCount`, `vodCount`, `seriesCount`, `lastSync`; `isNew` (hiện nhãn "Mới" tới khi mở lần đầu) |
| `xtream_category`, `xtream_live`, `xtream_vod`, `xtream_series`, `xtream_season`, `xtream_episode` | Tách VOD và series thành 2 bảng cho rõ ràng; giữ `timeViewed` / `totalTime` / `isRecent` / `isFavorite` |
| `search_history` | Như cũ |
| `single_stream` | **Mới**: lưu các link single stream (app gốc chỉ đếm số lượng) |
| `community_*` | Như cũ |
| `favourite_match`, `match_reminder` | Như cũ |
| `epg_program` (làm sau) | channelTvgId, start, end, title, desc |

## 4. Tính năng theo module

### 4.1 Khởi động (`ui/splash`, `ui/onboarding`)
- **Không làm màn chọn ngôn ngữ và màn onboarding giới thiệu lần đầu**: bạn tự code theo cách của bạn.
  - Luồng lần đầu: `Splash` → **[language / onboarding của bạn]** → Disclaimer → `Main`.
  - Chừa sẵn chỗ nối: `SplashActivity` gọi 1 hàm điều hướng (vd. `openFirstRunFlow()`) khi `isFirstOpen`; luồng của bạn xong thì mở `DisclaimerActivity` (hoặc thẳng `MainActivity`).
  - Vẫn giữ file dịch `values-*/strings.xml` cho các chuỗi của app.
- **Splash:** logo + Lottie, tải Remote Config (timeout 3–5 s), rồi điều hướng. Không có quảng cáo hay consent.
- **Rate prompt** (tùy chọn qua RC `check_rate`). Vị trí gọi trong luồng lần đầu do bạn quyết định.
- **Disclaimer / License Agreement:** 10 đoạn, checkbox, nút Accept. Hiện lần đầu, và lần đầu import (RC `check_disclaimer`).

### 4.2 Home & thêm nguồn (`ui/main`, `ui/home`)
- **Bottom bar 4 tab + nút + nổi ở giữa:** Home, Channels, Xtream, Sport/Settings (RC `show_sport`).
  - Làm bằng `layout_bottom_bar.xml` tự vẽ: ConstraintLayout + nền `bottom_nav` bo góc + 4 `ImageView` + `FloatingActionButton` / Lottie nổi ở giữa.
  - Chuyển tab bằng `FragmentManager` show/hide (tạo 1 lần, giữ state khi đổi tab).
- **Home:**
  - Chip Playlist / Recent / Favourite. (Bỏ banner "Get Pro" của app gốc.)
  - Lưới Playlist + ô "+"; mục Recent.
  - Empty state có 3 nút lớn.
  - Chatbot nổi ở góc.
- **Sheet "Add Playlist":** `AddSourceBottomSheet` (`BottomSheetDialogFragment`), 5 lựa chọn + icon "?" dẫn tới hướng dẫn.
- **Form import 3 tab (URL / Xtream / Single Stream):** `ImportActivity` gồm `TabLayout` (kiểu pill, như ảnh chụp) + 3 layout `<include>` bật/tắt (không dùng Fragment); ô nhập dùng `TextInputLayout`, toggle dùng `SwitchMaterial`.
  - URL: toggle Passcode, toggle Auto Update, link "How to add?" (mở Guide), checkbox License.
  - Xtream: Server URL (tự chuẩn hóa: thêm `http://`, bỏ `/` thừa), User, Pass. **Nút "Dán" thông minh:** nhận diện chuỗi `http://host:port/get.php?username=..&password=..` hoặc định dạng "Server URL: … User Name: … Password: …" rồi tự tách vào 3 ô.
  - Single Stream: link + tên (tùy chọn); Play ngay và lưu lại.
- **Upload M3U:** `UploadM3uActivity`, SAF chọn tối đa 5 file `.m3u` / `.m3u8`, đặt tên, passcode.
- **Load From Device:** Photo Picker (video). App gốc chỉ cho premium; bản clone mở cho mọi người.
- **Màn tiến trình import:** `ImportProgressDialog` gồm Lottie "Scanning Channels", `LinearProgressIndicator` % tiến độ, nút Cancel. Kết quả: số kênh, số nhóm. Lỗi: dialog "Oops! Look like your playlist is invalid. Do you need help?" (mở Guide).

### 4.3 Engine M3U (`data/parser`, `data/repository`)
- **Tải 1 lần, parse dạng stream:**
  - OkHttp GET, User-Agent tùy chỉnh (mặc định kiểu VLC/Chrome, sửa được trong Settings nâng cao).
  - Tự giải nén gzip; timeout 30 s; **không** trust-all SSL.
  - Progress tính theo `Content-Length` nếu có, không thì hiện dạng không xác định.
- **Parser đầy đủ:**
  - `#EXTM3U url-tvg / x-tvg-url`
  - `#EXTINF` với thuộc tính dạng `key="value"` tổng quát: `tvg-id`, `tvg-name`, `tvg-logo`, `group-title`, `tvg-chno`, `catchup*`
  - Tiêu đề = chữ sau dấu phẩy **cuối cùng nằm ngoài dấu ngoặc kép**
  - `#EXTGRP`, `#EXTVLCOPT:http-user-agent / http-referrer / http-origin`
  - `#KODIPROP:inputstream.adaptive.license_type / license_key` (ClearKey / Widevine)
  - URL dạng `url|User-Agent=...&Referer=...` (kiểu Kodi)
- **Ghi DB:** batch insert 1000 dòng / transaction.
- **Chống trùng:** URL giống nhau trong cùng playlist thì bỏ qua (tùy chọn).
- **Auto Update:** WorkManager định kỳ (12–24 h) tải lại playlist có `autoUpdate=true`, giữ lại favourite theo `(name, url)`.
- **Hỗ trợ thêm playlist JSON** (mảng `{name, url, logo, group}`), vì app gốc ghi là có mà không làm.
- Viết **unit test** cho parser với bộ file mẫu (iptv-org, file có lỗi, Kodi, VLC opts).

### 4.4 Danh sách kênh (`ui/playlist`, `ui/passcode`, `ui/search`)
- **Màn:** `PlaylistDetailActivity` (mở từ Home hoặc tab Channels, extra `playlistId`).
- **Header:** tên playlist + số kênh, icon settings.
- **Hai tab:** **Category** (nhóm + số kênh, có nhóm "All") và **Channels**, làm bằng `TabLayout` pill + 2 `RecyclerView` ẩn/hiện (không dùng Fragment). Bấm 1 nhóm thì chuyển sang tab Channels đã lọc theo nhóm đó.
- **Công cụ:** ô tìm kiếm (`EditText` + debounce 300 ms bằng Flow); nút chuyển list/grid; nút sắp xếp mở `PopupWindow` "Sort by" gồm 0-9 (thứ tự gốc) và A-Z, mỗi loại có tăng/giảm.
- **Mỗi dòng kênh:** logo (Glide, placeholder), tên, tên playlist, tim, menu ⋮ (`PopupMenu` / popup tự làm: đổi tên, khóa, xóa).
- **Danh sách lớn:** **Paging 3 + `PagingDataAdapter`**. Đổi list ↔ grid bằng cách thay `LayoutManager` (Linear ↔ Grid 3 cột) và `viewType`.
- **Sửa playlist:** đổi tên, passcode, auto update, copy URL, update now, xóa.
- **Passcode:**
  - Một mã chung như app gốc, nhưng **lưu hash** (SHA-256 + salt) trong DataStore.
  - Luồng Create → Confirm; mở mục bị khóa thì phải Enter. Làm bằng `PasscodeDialog` (DialogFragment full màn: 4–6 ô + bàn phím số).
  - Tùy chọn mở bằng vân tay (BiometricPrompt).
- **Tìm kiếm toàn cục:** `SearchActivity`, tìm kênh ở mọi playlist, có lịch sử.

### 4.5 Xtream (`ui/xtream`)
- **"Who's watching?":** tab `XtreamProfilesFragment` trong `MainActivity`.
  - Lưới 3 cột. Mỗi ô profile gồm avatar màu, tên, nút ⋮ và 1 dòng trạng thái: "Hạn dùng …", "Chỉ có kênh Live" hoặc "Hết hạn …" (màu đỏ).
  - Nhãn trên avatar: 🔒 nếu profile bị khóa; **"Mới"** cho profile vừa thêm (mất khi mở lần đầu); **"Hết hạn"** (avatar mờ đi).
  - Ô cuối cùng là "Thêm profile". Chưa có profile nào thì hiện empty state với nút Thêm profile và link hướng dẫn.
- **Thêm profile** (`AddEditProfileActivity` hoặc tab Xtream của `ImportActivity`, dùng chung form):
  - Nút "Dán" tự tách Server / Username / Password từ link `get.php?username=…&password=…`.
  - Bấm Đăng nhập & đồng bộ → `XtreamSyncDialog`. Thành công → dialog **"Đã thêm …"** (số kênh live / phim / series), có 2 nút **Về danh sách** và **Xem ngay**. Profile mới xuất hiện trong lưới với nhãn "Mới".
  - Lỗi hiện rõ trong dialog: không tìm thấy server, sai username/password, tài khoản hết hạn. Có nút Sửa lại và Hướng dẫn.
- **Chạm vào 1 profile:**
  1. Profile bị khóa → `PasscodeDialog` (có vân tay).
  2. Hết hạn → `XtreamExpiredDialog` gồm Kiểm tra lại / Sửa thông tin đăng nhập / Vẫn mở (vẫn xem được dữ liệu đã đồng bộ).
  3. Mở `XtreamHomeActivity`. Tài khoản chỉ có Live thì mở thẳng tab Live, còn tab Movies hiện "Tài khoản này không có phim".
- **Nhấn giữ hoặc bấm ⋮** → `ProfileActionsSheet`: Xem, Đồng bộ lại (hiện lần đồng bộ cuối), Thông tin tài khoản, Sửa, Khóa bằng passcode (switch), Xóa (xác nhận; xóa hết dữ liệu đã đồng bộ, không ảnh hưởng tài khoản ở nhà cung cấp).
- **Thông tin tài khoản** (`XtreamAccountSheet`, app gốc không có): trạng thái, hạn dùng, số kết nối đang dùng / tối đa, định dạng stream, server, username, múi giờ, số kênh / phim / series, lần đồng bộ cuối.
- **Đăng nhập:** gọi `player_api.php` (không có `action`) để kiểm tra `user_info.auth`, `status`, `exp_date`. Báo lỗi rõ ràng: sai mật khẩu, hết hạn, không tìm thấy host.
- **Đồng bộ:**
  - Chạy song song 3 danh mục + live + VOD + series.
  - **Thành công nếu có ít nhất 1 loại** (app gốc bắt buộc đủ cả 3).
  - Progress thật; nút "Refresh" để đồng bộ lại.
- **`XtreamHomeActivity`:** `BottomNavigationView` gồm **Movie / Live / Search / Favorite** (giống app gốc), mỗi tab là 1 fragment. "See All" / category mở `XtreamCategoryActivity`; Continue watching mở `XtreamRecentActivity`.
  - **Thanh tiêu đề** là avatar + tên profile + hạn dùng. Bấm vào mở `ProfileSwitcherSheet` để đổi nhanh sang profile khác (vẫn hỏi passcode nếu khóa), Quản lý profile, Thêm profile.
  - **Menu ⋮:** Thông tin tài khoản, Đồng bộ lại, Xem tiếp, Sửa profile.
  - **Hết hạn:** thanh cảnh báo đỏ dưới tiêu đề, có nút Sửa.
  - **Trang Home của app** có hàng "Xtream" (chip các profile) để mở nhanh, không cần vào tab Xtream.
  - Movies/Series: `NestedScrollView` hoặc `RecyclerView` nhiều viewType gồm hero ngẫu nhiên, hàng "Continue watching" (RecyclerView ngang + thanh tiến độ), "Trending / Recently added", hàng theo category, "See All".
  - Live: category bên trái hoặc trên, danh sách kênh; (làm sau) EPG ngắn qua `get_short_epg`.
- **Chi tiết phim:** `MovieDetailActivity` (app gốc dùng bottom sheet) gồm poster/backdrop, rating, thể loại, thời lượng, cast, đạo diễn, cốt truyện, trailer YouTube, Play / Resume, Favourite, "More like this" (cùng category).
- **Chi tiết series:** `SeriesDetailActivity` gồm chip chọn mùa (`ChipGroup` ngang), danh sách tập có ảnh, tiến độ từng tập, resume.
- **URL phát:**
  - Live: `{server}/live/{u}/{p}/{id}.{ts|m3u8}`, ưu tiên theo `allowed_output_formats`; fallback về dạng không có `/live/` giống app gốc.
  - Movie: `{server}/movie/{u}/{p}/{id}.{ext}`
  - Episode: `{server}/series/{u}/{p}/{id}.{ext}`
- **Recent / Continue watching:** UNION phim + tập có `isRecent`, mới nhất trước.

### 4.6 Player (`ui/player` + `player/`)
- **ExoPlayer:**
  - `DefaultHttpDataSource` với User-Agent / Referer / header riêng từng kênh.
  - Hỗ trợ HLS, DASH, RTSP, RTMP (extension), UDP, ClearKey / Widevine từ KODIPROP.
  - LoadControl chỉnh cho live: buffer 15–50 s; giới hạn live offset.
  - `playlist` có dấu `;` thì tách thành nhiều nguồn; **tự thử nguồn kế tiếp khi lỗi**.
  - **Tự retry khi lỗi mạng**: 3 lần, backoff.
- **`PlayerActivity` + `activity_player.xml`:**
  - `PlayerView` (`app:use_controller="false"`, `resize_mode` đổi được).
  - Overlay `layout_player_controller.xml` tự vẽ, tự ẩn sau 6 s bằng `Handler` / coroutine.
  - `GestureDetector` cho chạm, chạm đúp và vuốt.
  - Màn dọc: `ConstraintLayout` gồm video 16:9 ở trên + phần dưới (tên playlist, `RecyclerView` lưới kênh).
  - Fullscreen: `requestedOrientation = SENSOR_LANDSCAPE`, ẩn system bars (`WindowInsetsControllerCompat`), đổi `ConstraintSet` để video chiếm full.
- **Điều khiển giống app gốc:**
  - Thanh trên: back, tên, share, timer, favourite.
  - Giữa: tua −10 s / play-pause / +10 s. Dưới: seekbar, fullscreen.
  - Brightness, volume; channel list; subtitle; Popup Play (PiP); Cast; Lock.
  - Màn dọc: dưới video là tên playlist và lưới kênh (bỏ thẻ "Unlock Unlimited" của app gốc).
- **Bổ sung so với app gốc:**
  - Vuốt dọc bên trái/phải để chỉnh sáng/âm lượng; chạm đúp để tua.
  - Tỉ lệ khung hình: Fit / Fill / Zoom / 16:9 / 4:3.
  - Chọn audio track và chất lượng video; tốc độ phát (VOD).
  - Kênh trước/sau (live).
- **Hẹn giờ ngủ:** `TimerSheet` 15 / 30 / 45 / 60 phút + hiện đếm ngược; hết giờ thì pause. Timer đặt trong `PlayerManager` (singleton) để không mất khi xoay màn hoặc vào PiP.
- **PiP:** trên `PlayerActivity` riêng. Tự vào khi rời app đang phát (`setAutoEnterEnabled`, API 31+; API thấp hơn dùng `onUserLeaveHint`), RemoteAction play/pause, kênh trước/sau; `onPictureInPictureModeChanged` ẩn overlay điều khiển.
- **MediaSession + phát nền audio** (tùy chọn trong Settings).
- **Cast:**
  - Mức 1 (như app gốc, nhưng không cần xem quảng cáo): mở cài đặt screen mirroring của hệ thống.
  - Mức 2 (làm sau): Google Cast SDK, với link HLS/MP4 công khai.
- **Resume:** lưu vị trí VOD mỗi 10 s và khi thoát; còn dưới 4 s thì coi như đã xem xong.
- **Ghi Recent:** cập nhật `isRecent` + thời gian khi bắt đầu phát.
- **Lỗi:** overlay "Channel currently unavailable" có nút Retry và kênh kế tiếp; fullscreen hiện dialog.

### 4.7 Hướng dẫn tìm nguồn & chatbot (`ui/guide`)
- **Màn "How to add"** (Playlist URL / Upload M3U / Device / Single Stream / Xtream):
  - Các bước có số thứ tự (text có span in đậm và link bấm được), ảnh minh họa, video hướng dẫn (`PlayerView` phát file `raw`).
  - Nút **"Search in browser"** mở Custom Tabs.
  - Link chữ bấm được, mở **sheet WebView**.
- **Sheet WebView (như app gốc):** `WebGuideBottomSheet` (`BottomSheetDialogFragment` mở full chiều cao, tắt kéo khi WebView đang cuộn) + `WebView` + `LinearProgressIndicator`.
  - Tiêu đề, Cancel, thanh progress, JavaScript + DOM storage + zoom.
  - Bổ sung:
    - Nút **"Mở bằng trình duyệt"**.
    - Nếu người dùng chọn chữ / copy trong trang và clipboard có URL `.m3u` / `.m3u8` / `get.php`: hiện snackbar **"Dùng link này"**, điền thẳng vào form import. Có thể bắt thêm việc chạm vào link `.m3u` qua `shouldOverrideUrlLoading`.
- **Danh sách trang gợi ý đọc từ Remote Config `guide_sites`** (không ghi cứng):
  ```json
  {
    "iptv":   [{"title":"…","url":"https://…","mode":"webview|customtab"}],
    "xtream": [{"title":"…","url":"https://…","mode":"webview"}],
    "single": [{"title":"…","url":"https://…","mode":"webview"}],
    "search_query": "newest free global iptv"
  }
  ```
  Giá trị mặc định có thể là các trang app gốc dùng, hoặc repo GitHub gợi ý do bạn tự quản lý.
- **FAQ:** Finding a playlist? / How to add / Edit–delete–private / Troubleshooting (6 bước).
- **Chatbot offline:** `ChatbotActivity` gồm `RecyclerView` 2 viewType (bot / user), ô nhập, các chip câu hỏi gợi ý.
  - Khớp từ khóa (có thể thêm tiếng Việt) → trả hướng dẫn từng bước + nút hành động.
  - 3 câu hỏi gợi ý (playlist M3U / single stream / Xtream); bỏ câu thứ 4 của app gốc về gói Premium. Hiệu ứng "đang gõ" 1,5 s.
  - Làm sau: kịch bản đọc từ Remote Config JSON để sửa không cần build lại.

### 4.8 Thể thao (`ui/sport`)
- **ESPN API** (scoreboard / summary); danh sách môn và giải lấy từ RC `sport_data` (có mặc định: EPL, La Liga, Serie A, Bundesliga, Ligue 1, UCL, NBA…).
- **Tab Sport** (`SportFragment` trong `MainActivity`): `RecyclerView` nhiều section. "See All" mở `SportMatchesActivity`, My Match mở `MyMatchActivity`, bấm trận mở `MatchDetailActivity`.
  - Chọn môn; Top Event / My Match / Live / Upcoming.
  - Lọc ngày (hôm nay, mai, hôm qua, 7 ngày, tháng, năm, chọn ngày); chọn giải.
- **Chi tiết trận:** tỷ số, trạng thái, sự kiện (bàn thắng, thẻ), sân, đội hình nếu có. Live polling theo khoảng cách tới giờ đá như app gốc.
- **My Match:** lưu trận yêu thích; **nhắc trước 15 phút** bằng WorkManager + thông báo "Live now: A vs B".
- **Nút "Watch":** mở sheet "Ready to Watch?":
  - Chọn kênh từ playlist hoặc Xtream **của người dùng**, có ô tìm kiếm điền sẵn tên đội / giải để lọc kênh nhanh.
  - Người dùng chưa có nguồn thì hiện: Add Playlist / Continue with Xtream / Maybe later.
  - Không tự đẩy link trận từ server (xem §10).

### 4.9 Community (`ui/community`)
- **Firebase Realtime DB:** node `iptv` / `xtream` / `singleStream`.
- **Màn hình:**
  - `GetLinkActivity`: Copy / Save / Report.
  - `MyShareActivity`: Start Sharing, sửa, xóa link mình chia sẻ (qua `ShareDialog`).
  - `CommunityFavoriteActivity`: các link đã lưu.
  - Mỗi màn có tab IPTV / Xtream / Single dạng `TabLayout` lọc dữ liệu.
- **Chống lạm dụng:**
  - Firebase Auth ẩn danh.
  - Rule chỉ cho sửa/xóa bản ghi của chính mình.
  - Nút Report kèm ngưỡng tự ẩn.
  - Giới hạn số lần share mỗi ngày.

### 4.10 Settings (`ui/settings`)
- **Màn:** nội dung nằm trong `SettingsFragment`, hiện ở 2 chỗ: tab thứ 4 của `MainActivity` (khi RC tắt Sport) và `SettingsActivity` (mở từ icon ⚙ ở header).
- **Các mục:** Language (chỉ là dòng mở màn language bạn tự code), Privacy Policy, Terms, FAQ, License Agreement, Contact support, Share App, Rate App (in-app review + fallback), Feedback (form email).
- **Bổ sung:** User-Agent mặc định, chế độ player (hardware/software decoder), tự vào PiP, đổi / xóa passcode, xóa cache, sao lưu / khôi phục playlist (xuất JSON).

### 4.11 Kiếm tiền: tạm bỏ
- **Không làm IAP và quảng cáo** ở giai đoạn này. App không có gói mua, paywall, Restore Purchase, banner "Get Pro", giới hạn bản free, hay bất kỳ loại quảng cáo nào (open / interstitial / native / banner / rewarded). Không cần UMP consent.
- Mọi tính năng mở cho tất cả người dùng: không giới hạn playlist / Xtream / single / favourite; Cast và Load From Device dùng tự do; phát kênh không cần xem quảng cáo.
- **Chuẩn bị để thêm sau:** code không có sẵn chỗ gọi quảng cáo. Kiến trúc Activity + ViewModel + Hilt cho phép sau này thêm package `ads/` / `billing/` và gọi ở `BaseActivity` và các điểm chuyển màn mà không phải sửa lớn.

### 4.12 Analytics & Remote Config
- **Event:** giữ tên như app gốc (`first_open`, `import_*`, `play_*`, `tap_watch`, `click_chatbot`, `sport_screen`…) để dễ so sánh funnel.
- **Remote Config:** gom toàn bộ khóa vào 1 file `RemoteKeys.kt` + `remote_config_defaults.xml`.

## 5. Dựng giao diện HTML (duyệt trước khi code)

Trước khi viết layout XML, mình dựng toàn bộ giao diện bằng HTML/CSS tĩnh để bạn duyệt. **Màn nào đã duyệt mới được code XML, và code bám đúng bản đã duyệt.**

### 5.1 Cách làm
Thư mục `design/` nằm trong repo, mở trực tiếp bằng trình duyệt (không cần build, không dùng framework):

```
design/
├─ index.html        # Trang tổng: xem mọi màn theo nhóm, kèm trạng thái duyệt
├─ tokens.css        # Màu, cỡ chữ, khoảng cách, bo góc, đổ bóng → ánh xạ 1-1 sang colors.xml / dimens.xml / themes.xml
├─ components.css    # Button, card, ô nhập, tab pill, chip, bottom bar, dialog, bottom sheet, dòng danh sách
├─ icons/            # Icon SVG (sau này chuyển thành VectorDrawable)
├─ img/              # Ảnh minh họa tự vẽ / placeholder (không dùng logo đài, giải đấu)
└─ screens/          # Mỗi màn 1 file, đặt tên trùng layout XML: activity_main.html, fragment_home.html, sheet_add_source.html…
```

- **Khung điện thoại 360 × 800**, đúng kích thước dp của Pixel 6a (1080 × 2400 px, mật độ 3x). **1 px trong HTML = 1 dp trong XML**, nên số đo chuyển thẳng sang layout.
- **Mỗi màn vẽ đủ trạng thái:** bình thường, trống (empty), đang tải, lỗi; list / grid; dialog hoặc sheet đang mở; player dọc và ngang.
- **Dữ liệu mẫu:** giả nhưng giống thật (tên kênh, nhóm, số kênh, poster placeholder).
- **Bấm được để thử luồng:** các file có link sang nhau, vd. Home → + → Import → danh sách kênh → Player.
- **Chỉ dùng hiệu ứng làm lại được bằng XML.** Những thứ khó (blur nền, animation đặc biệt) phải thống nhất cách làm trên Android trước khi đưa vào mockup.

### 5.2 Quy trình duyệt
1. Mình dựng 1 đợt màn, rồi gửi bạn ảnh chụp và đường dẫn file.
2. Bạn góp ý: màu, bố cục, chữ, kích thước…
3. Mình sửa tới khi bạn duyệt, rồi đánh dấu "Đã duyệt" trong `design/index.html`.
4. Màn đã duyệt là bản chốt. Khi code XML, chỗ nào cần làm khác HTML thì mình hỏi bạn trước.

### 5.3 Các đợt duyệt

| Đợt | Nội dung |
|---|---|
| **D1 – Design system** | Bảng màu, kiểu chữ, nút, thẻ, ô nhập, tab pill, chip, bottom bar có nút + nổi, dialog, bottom sheet, toast / snackbar, empty state |
| **D2 – Màn chính & thêm nguồn** | `MainActivity` với 4 tab (Home, Channels, Xtream, Sport), sheet Add Playlist, màn Import (URL / Xtream / Single), Upload M3U, dialog tiến trình import |
| **D3 – Playlist** | Chi tiết playlist (Category / Channels, list / grid, popup Sort, menu ⋮), Search, sửa playlist, passcode |
| **D4 – Player** | Màn dọc, màn ngang (fullscreen), lớp điều khiển, lock, độ sáng / âm lượng, các sheet (kênh, phụ đề, audio, tỉ lệ, hẹn giờ), trạng thái lỗi |
| **D5 – Xtream** | Who's watching (có profile / trống / profile mới / hết hạn / khóa), thêm / sửa profile, đồng bộ (thành công / lỗi), menu thao tác profile, thông tin tài khoản, đổi profile, Xtream home (Movie / Live / Search / Favorite, tài khoản chỉ có Live, hết hạn), category, chi tiết phim, chi tiết series, continue watching |
| **D6 – Guide & khác** | Splash, Disclaimer, How to add, FAQ, sheet WebView, Chatbot, Settings, Feedback, Rate |
| **D7 – Thể thao** | Tab Sport, lịch thi đấu, My Match, chi tiết trận, chọn giải, sheet Watch |
| **D8 – Community** | Get Link, My Share, Favorite, Share dialog |

### 5.4 Chuyển sang code
- `tokens.css` → `colors.xml`, `dimens.xml`, `themes.xml`, `styles.xml` (làm ở P1 – Nền tảng).
- `icons/*.svg` → `drawable/ic_*.xml` (qua Vector Asset của Android Studio).
- Mỗi `screens/<layout>.html` → `res/layout/<layout>.xml` cùng tên.

## 6. Lộ trình triển khai

| Giai đoạn | Nội dung | Ước lượng* |
|---|---|---|
| **P0 – Giao diện HTML** | Dựng và duyệt mockup HTML theo các đợt D1–D8 (§5). Đợt D1–D4 phải duyệt xong trước P1; các đợt sau dựng song song trong lúc code | 5–7 ngày (tùy số vòng góp ý) |
| **P1 – Nền tảng** | Tạo project, Gradle version catalog, Hilt, Room, DataStore, `BaseActivity` / `BaseFragment` / `BaseBottomSheet` (ViewBinding), chuyển `tokens.css` đã duyệt thành colors / dimens / themes / styles + shape drawable, `MainActivity` + 4 tab fragment (show/hide) + bottom bar XML có nút + nổi, Firebase (RC / Analytics / Crashlytics) | 3–4 ngày |
| **P2 – MVP phát được** | Import M3U (URL / file) + parser + test; Home, Channels (nhóm / kênh / tìm / sắp xếp / list-grid); Favourite / Recent; Player cơ bản (điều khiển, fullscreen, PiP, timer, subtitle, lock, brightness / volume); Single Stream | 7–10 ngày |
| **P3 – Xtream** | Profile, login + sync, xtream_home (Live / Movie / Series / Search / Fav), chi tiết phim/series, resume / continue watching | 6–8 ngày |
| **P4 – Guide & khởi động** | Splash (chừa chỗ nối luồng language/onboarding của bạn), disclaimer, rate; How to add + FAQ + sheet WebView + Custom Tabs + phát hiện link trong clipboard; chatbot offline; passcode | 4–5 ngày |
| **P5 – Thể thao** | ESPN client, màn sport / my_match / detail, lọc ngày / giải, polling, nhắc lịch, sheet "Watch" chọn kênh | 5–6 ngày |
| **P6 – Community** | Realtime DB + Auth ẩn danh + rules; Get Link / Share / Favorite; report | 3–4 ngày |
| **P7 – Hoàn thiện** | Đa ngôn ngữ (14 thứ tiếng), auto update playlist (WorkManager), EPG (tùy chọn), Google Cast (tùy chọn), sao lưu / khôi phục, tối ưu hiệu năng (Paging, baseline profile), R8, test trên máy thật, chuẩn bị store listing | 5–7 ngày |

\*Ước lượng cho 1 dev Android quen View/XML. Tổng khoảng 6–7 tuần (gồm cả bước duyệt giao diện).

**Thứ tự:** P0 (duyệt D1–D4) → P1 → P2, rồi P4 (phần Guide). Như vậy sớm có app import được link, xem được kênh và có luồng hướng dẫn tìm nguồn.

**Quy tắc:** giai đoạn code nào đụng tới màn nào thì màn đó phải được duyệt ở bản HTML trước.

| Giai đoạn code | Cần duyệt trước |
|---|---|
| P1 – Nền tảng | D1 |
| P2 – MVP phát được | D2, D3, D4 |
| P3 – Xtream | D5 |
| P4 – Guide & khởi động | D6 |
| P5 – Thể thao | D7 |
| P6 – Community | D8 |

## 7. Kiểm thử
- **Unit test:** parser M3U (nhiều biến thể), dựng URL Xtream, chuẩn hóa server URL, polling thể thao.
- **Instrumented:** migration Room, DAO truy vấn nhóm / kênh / tìm kiếm.
- **Thủ công trên Pixel 6a:**
  - Playlist công khai lớn (iptv-org `index.m3u`, khoảng 10k kênh) để đo thời gian import và độ mượt khi cuộn.
  - HLS / DASH / RTSP / UDP mẫu.
  - PiP, xoay màn, khóa màn hình, mất mạng giữa chừng.

## 8. Store & phát hành
- **Store listing (điểm Google hay soi với app IPTV):**
  - Mô tả là "trình phát, không cung cấp nội dung".
  - Ảnh chụp chỉ dùng nội dung mẫu hợp pháp hoặc demo.
  - Không dùng logo đài / giải đấu, không ghi "xem miễn phí kênh X".
- **Trong app:** Privacy Policy, Terms, Disclaimer, Data safety (Analytics / Crashlytics).
- **Kỹ thuật:** target API mới nhất, trang 16 KB, R8 full mode, bật Crashlytics mapping.

## 9. Việc cần bạn cung cấp / quyết định
1. **Tên app, package name, icon và màu thương hiệu.** Nên khác app gốc, ví dụ `com.<brand>.iptvplayer`.
2. **Firebase project** (`google-services.json`).
3. **Danh sách trang gợi ý** cho phần Guide: dùng lại các trang app gốc, hay repo GitHub riêng của bạn.
4. **Làm phần Community và Sport ở bản đầu, hay để bản sau?**

## 10. Ghi chú rủi ro
- **Link xem trận do app tự cung cấp:** app gốc lấy link live / full / highlight qua RC `sport_play` / `wc_replay`, tức là nhà phát triển trực tiếp cung cấp nội dung thể thao có bản quyền. Đây là lý do phổ biến nhất khiến app IPTV bị gỡ và bị khiếu nại DMCA. Bản clone giữ tab thể thao (tỷ số / lịch / nhắc lịch) nhưng nút Watch mở kênh từ nguồn của chính người dùng.
- **Khi thêm lại quảng cáo sau này:** Google Play có chính sách về quảng cáo gây khó chịu (disruptive ads). App gốc chèn rất dày (open ad, native toàn màn, rewarded trước khi phát), dễ bị cảnh báo và bị đánh giá thấp. Nên bắt đầu ở mức vừa phải và điều chỉnh tần suất qua RC.
- **Community:** nội dung do người dùng chia sẻ; cần Report + khả năng gỡ nhanh để tuân thủ chính sách UGC của Play.
