# Checklist đưa app lên Google Play

Đi lần lượt từ trên xuống. Xong bước nào thì báo, Claude sẽ đánh dấu `[x]` và ghi ngày.

- **Bạn**: việc bạn tự làm hoặc cần bạn quyết định / gửi thông tin.
- **Claude**: việc Claude làm trong code.

---

## Giai đoạn 0: Dọn nội dung nhạy cảm

- [x] **Claude**: Ẩn tab Sport, thay bằng tab Settings (`Features.SPORT = false`). *(01/10/2026)*
- [x] **Claude**: Ẩn Community (`Features.COMMUNITY = false`). *(01/10/2026)*
- [x] **Claude**: Ẩn phần "chỉ chỗ lấy nguồn": trang gợi ý, nút tìm Google, các dải hướng dẫn trong form (`Features.GUIDE_SITES = false`). Sửa lại câu chữ FAQ / hướng dẫn / License. *(06/10/2026)*
- [x] **Claude**: Làm rỗng `res/raw/guide_sites.json`, APK không còn link playlist nào. *(06/10/2026)*
- [ ] **Bạn** *(tuỳ chọn)*: Chuyển hai repo GitHub `xtream-playlist` và `single_stream` sang private hoặc xoá. App không trỏ tới, nên việc này chỉ để bảo vệ tài khoản GitHub.

## Giai đoạn 1: Thông tin bạn cần gửi

- [x] **Bạn**: Tên app: **IPTV Smart Player - Live TV**. *(07/10/2026)*
- [x] **Bạn**: applicationId: **`com.cp.livetv.iptvplayer`**. Lên Play rồi thì không đổi được nữa. *(07/10/2026)*
- [x] **Bạn**: Icon app (TV trên nền xanh), kèm `ic_launcher-playstore.png` 512×512 để upload lên Play. *(07/10/2026)*
- [ ] **Bạn**: Link Privacy policy (bắt buộc trên Play Console, phải là trang web công khai, phải nhắc Firebase Analytics + Crashlytics). Claude soạn nội dung được.
- [x] ~~**Bạn**: Link Terms of use.~~ **Không cần cho bản đầu**: đã ẩn cụm Legal trong app. *(07/10/2026)*
- [ ] **Bạn**: Email liên hệ cho trang Play (Play Console bắt buộc). Trong app đã ẩn Send feedback nên không cần sửa code.
- [x] **Bạn**: Chốt target SDK: **36**. *(06/10/2026)*
- [ ] **Bạn** *(tuỳ chọn)*: Màn chọn ngôn ngữ + onboarding. Bạn tự code; hook đã có sẵn trong `SplashActivity`.
- [x] **Claude**: Tạm ẩn dòng Language trong Settings (`Features.LANGUAGE_SETTING = false`). App mặc định tiếng Anh; Android 13+ vẫn chọn được ngôn ngữ của app trong cài đặt hệ thống. *(06/10/2026)*

## Giai đoạn 2: Sửa code trước khi build release

- [x] **Claude**: Đổi applicationId thành `com.cp.livetv.iptvplayer` (giữ `namespace = com.iptvplayer.app` cho code, không ảnh hưởng gì tới Play). *(07/10/2026)*
- [x] **Claude**: Rà icon và tên mới khắp app: màn Splash dùng icon mới; nền adaptive icon đổi từ xanh lá mẫu của Android Studio sang xanh `#1682D7` khớp icon; `roundIcon` trỏ sang `ic_launcher_round`; xoá icon vector cũ; đổi chữ "IPTV Player" ở header Home, License, câu chia sẻ, tiêu đề email feedback. *(07/10/2026)*
- [x] **Claude**: Tích hợp **Firebase Analytics + Crashlytics** (BOM 34.19.0, `app/google-services.json`, file đã nằm trong `.gitignore`). Gỡ quyền Advertising ID do Analytics tự thêm (`AD_ID`, `ACCESS_ADSERVICES_*`) và tắt thu thập ad ID, vì app không có quảng cáo. Đã kiểm tra trên LDPlayer 9: Crashlytics khởi tạo và tải settings từ Firebase; Analytics ghi `first_open`, `session_start`, `screen_view` cho đúng app. *(07/10/2026)*
- [ ] **Bạn**: Mở Firebase Console › Crashlytics, đợi app báo lần đầu (vài phút sau khi mở app) để xác nhận Crashlytics đã nhận dữ liệu.
- [x] ~~**Claude**: Thay `PRIVACY_URL`, `TERMS_URL`, `SUPPORT_EMAIL` trong `ui/settings/Settings.kt`.~~ **Hoãn**: đã ẩn cụm Legal (`Features.LEGAL`) và Send feedback (`Features.FEEDBACK`) trong Settings, nên app không còn chỗ nào mở link/email giả. Vẫn cần link Privacy policy **trên Play Console** (bắt buộc). *(07/10/2026)*
- [x] **Claude**: Ẩn công tắc "Picture-in-picture when leaving the app" (`Features.AUTO_PIP_SETTING`); tự vào PiP vẫn bật theo mặc định. *(07/10/2026)*
- [x] **Claude**: Nâng `targetSdk` / `compileSdk` lên **36**, AGP 8.7.2 → 8.11.1. Build debug + release và unit test đều qua; thư viện native đã căn trang 16 KB. *(06/10/2026)*
- [ ] **Claude + Bạn**: Test lại trên máy **Android 16** (Pixel). API 36 bắt buộc edge-to-edge, có predictive back, và trên màn hình lớn bỏ qua khoá xoay. Hiện mới test trên emulator Android 9.
- [x] ~~**Claude**: Sửa khôi phục backup: hiện chỉ khôi phục được profile Xtream đầu tiên.~~ **Hoãn**: đã tạm ẩn Sao lưu / Khôi phục (`Features.BACKUP = false`). Sửa lỗi này trước khi bật lại. *(06/10/2026)*
- [x] **Claude**: Tạm ẩn Cast (`Features.CAST = false`): nút Cast ở player (cả thanh dọc lẫn hàng công cụ toàn màn hình) và câu FAQ "How do I watch on my TV?". *(06/10/2026)*
- [x] **Claude**: Passcode: Settings › Passcode giờ mở menu "Đổi passcode / Tắt passcode" khi đã có mã. Đổi = nhập mã cũ (hoặc vân tay) → mã mới → nhập lại. Tắt = xác nhận → nhập mã → mở khoá mọi playlist / kênh / profile Xtream rồi xoá mã. *(06/10/2026)*
- [ ] **Bạn**: Test passcode trên máy: tạo → khoá 1 playlist → đổi mã (mã cũ không mở được, mã mới mở được) → tắt passcode (playlist hết khoá).
- [x] **Claude**: Ẩn cài đặt Decoder (`Features.DECODER_SETTING = false`); player cũng bỏ qua giá trị Decoder đã lưu từ bản cũ. *(06/10/2026)*
- [x] **Claude**: Tạm ẩn "Phát tiếp khi tắt màn hình" (`Features.BACKGROUND_AUDIO = false`). Comment `FOREGROUND_SERVICE*` + `<service PlaybackService>` trong manifest; gỡ luôn `FOREGROUND_SERVICE` + `SystemForegroundService` do thư viện WorkManager tự thêm. Đã kiểm tra manifest đã gộp của bản release: không còn foreground service nào. PiP vẫn chạy. *(06/10/2026)*
- [x] **Claude**: Bỏ hỏi quyền thông báo lúc mở app (`Features.NOTIFICATION_PERMISSION = false`) và comment `POST_NOTIFICATIONS` trong manifest. APK release giờ chỉ còn quyền: INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK, USE_BIOMETRIC / USE_FINGERPRINT, RECEIVE_BOOT_COMPLETED (do WorkManager thêm). *(06/10/2026)*
- [x] **Claude**: Tắt R8 / ProGuard cho bản release (`isMinifyEnabled = false`, `isShrinkResources = false`): không thu gọn, không làm rối tên class, nên lỗi Gson/reflection do R8 gây ra không còn xảy ra được. APK release tăng từ 5,2 MB lên 11,7 MB. *(06/10/2026)*
- [ ] **Bạn**: Tạo keystore ký release (hoặc để Claude tạo). **Cất file và mật khẩu cẩn thận**, mất là không cập nhật app được nữa.
- [ ] **Claude**: Cấu hình ký release (đọc mật khẩu từ file ngoài git) và build **AAB** (`bundleRelease`).

## Giai đoạn 3: Test bản release

- [x] **Claude**: Soạn file M3U test hợp pháp: [docs/review/test-playlist.m3u](review/test-playlist.m3u), 15 kênh trong 5 nhóm (live test, open movie HLS/DASH/MP4, test pattern). Đã import và phát thử đủ 15 kênh trên app. Hướng dẫn + văn bản dán vào Play Console: [docs/review/README.md](review/README.md). *(06/10/2026)*
- [x] **Bạn**: Đưa `test-playlist.m3u` lên repo public [iptvstore779/iptv_review](https://github.com/iptvstore779/iptv_review). Claude đã kiểm tra: file giống hệt bản local; cài mới app, import bằng link Raw ra 15 kênh / 5 nhóm, mỗi nhóm phát thử 1 kênh đều chạy. *(06/10/2026)*
  Link cho reviewer: `https://raw.githubusercontent.com/iptvstore779/iptv_review/refs/heads/main/test-playlist.m3u`
- [x] **Claude**: Single stream cho reviewer: đoạn App access trong [docs/review/README.md](review/README.md) đã có link Mux Big Buck Bunny (đã test phát được). *(06/10/2026)*
- [x] ~~**Bạn**: Chọn phương án B cho Xtream (không cấp tài khoản).~~ **Bị Google từ chối** ở bản 1 (1.0.0): thiếu thông tin đăng nhập cho màn Xtream. *(07/10/2026)*
- [x] **Claude**: Viết server Xtream demo cho reviewer ([docs/review/xtream-demo/](review/xtream-demo/)): Cloudflare Worker, chỉ chứa stream test hợp pháp, login `reviewer` / `review2026`. Đã test trên app (chạy local): đăng nhập + sync, phát phim HLS, DASH, MP4, tập series và kênh live đều chạy. Đã viết lại đoạn App access thành 2 mục (Xtream + playlist). *(07/10/2026)*
- [ ] **Bạn**: Deploy server lên Cloudflare Workers theo [hướng dẫn](review/xtream-demo/README.md), gửi Claude link Worker.
- [ ] **Claude**: Test app với link Worker thật, điền link vào đoạn App access.
- [x] **Claude**: Tăng `versionCode` 1 → 2, `versionName` 1.0.0 → 1.0.1 để nộp lại. *(07/10/2026)*
- [ ] **Bạn**: Build AAB mới, cập nhật App access trong Play Console (2 mục), nộp lại.
- [ ] **Claude + Bạn**: Cài bản release lên máy thật, chạy lại toàn bộ luồng: Home, Import URL/Xtream/Single, Player (PiP, hẹn giờ, phụ đề, âm thanh), Xtream (Movies, Live, Search, Favorite), Settings.
- [ ] **Bạn**: Test chọn file M3U thật trên máy.
- [x] ~~**Bạn**: Test sao lưu → xoá app → cài lại → khôi phục.~~ **Không cần cho bản đầu**: đã tạm ẩn. *(06/10/2026)*
- [x] ~~**Bạn**: Test Cast lên TV.~~ **Không cần cho bản đầu**: đã tạm ẩn. *(06/10/2026)*

## Giai đoạn 4: Play Console

- [ ] **Bạn**: Tài khoản developer. Nếu là tài khoản **cá nhân tạo sau 11/2023**, cần chạy closed test (xem bước cuối giai đoạn này).
- [ ] **Bạn**: Tạo app mới (tên, ngôn ngữ mặc định, App / Free).
- [x] **Claude**: Soạn mô tả ngắn + mô tả chi tiết (tiếng Anh, không có chữ "Xtream", theo cách app gốc và Smarters Pro làm). Bạn chốt bản 2 đoạn. *(07/10/2026)*
  - Ngắn: `A fast, simple player for your own M3U/M3U8 playlists and IPTV accounts.`
- [ ] **Bạn**: Ảnh chụp màn hình (ít nhất 2 ảnh điện thoại) + feature graphic 1024×500. **Không** để lộ poster/logo Netflix, HBO hay kênh có thương hiệu: dùng profile Demo của mock server hoặc file M3U test.
- [ ] **Bạn**: Điền link Privacy policy.
- [ ] **Bạn**: App access: "Cần hướng dẫn đặc biệt" → dán link M3U test + cách thêm (bấm + › Playlist URL › dán › Add playlist).
- [ ] **Bạn**: Ads: chọn **No ads**.
- [ ] **Bạn**: Data safety: app **có thu thập** qua Firebase (xem [hướng dẫn](#data-safety-với-firebase) bên dưới). Không chia sẻ cho bên thứ ba, có mã hoá khi truyền, không có tài khoản.
- [ ] **Bạn**: Advertising ID: chọn **No** (app không dùng ad ID, đã gỡ quyền `AD_ID`).
- [ ] **Bạn**: Content rating (bảng câu hỏi IARC).
- [ ] **Bạn**: Target audience: 18+ hoặc 13+, **không** chọn trẻ em.
- [x] ~~**Bạn**: Khai báo Foreground service `mediaPlayback` + video quay màn hình.~~ **Không cần cho bản đầu**: đã tạm ẩn nghe nền, app không còn foreground service. *(06/10/2026)*
- [ ] **Bạn**: Upload AAB lên Internal testing, cài thử từ Play.
- [ ] **Bạn**: Closed testing với **ít nhất 12 người trong 14 ngày** (nếu tài khoản cá nhân).
- [ ] **Bạn**: Nộp bản Production và chờ duyệt.

## Giai đoạn 5: Sau khi lên

- [ ] **Claude**: Push code lên `origin` mỗi khi bạn yêu cầu (lần gần nhất 07/10/2026, kèm `app/google-services.json` vì repo private).
- [ ] **Bạn + Claude**: Theo dõi crash / ANR trong Play Console (Android vitals) và sửa.

---

### Đang tạm ẩn, để sau (không cần cho lần lên đầu)

- Tab Sport: `Features.SPORT`
- Community: `Features.COMMUNITY` (cần bật Realtime Database trong Firebase)
- Phát tiếp khi tắt màn hình: `Features.BACKGROUND_AUDIO`. Bật lại thì phải: (1) bỏ comment 2 quyền `FOREGROUND_SERVICE*` và `<service PlaybackService>` trong `AndroidManifest.xml`, (2) xoá 2 dòng `tools:node="remove"` (quyền `FOREGROUND_SERVICE` và `SystemForegroundService`), (3) khai báo foreground service `mediaPlayback` + video trong Play Console.
- Cast lên TV: `Features.CAST` (mở cài đặt cast / màn hình không dây của Android). Bật lại thì test với TV thật.
- Sao lưu / khôi phục: `Features.BACKUP`. Bật lại thì **phải sửa trước**: khôi phục hiện chỉ lấy profile Xtream đầu tiên.
- Quyền thông báo: `Features.NOTIFICATION_PERMISSION`. Bật lại cùng lúc với Sport hoặc nghe nền, và bỏ comment `POST_NOTIFICATIONS` trong manifest.
- Chọn ngôn ngữ trong Settings: `Features.LANGUAGE_SETTING`. Bật lại khi có thêm ngôn ngữ, hoặc thay bằng màn Language bạn tự code.
- Cài đặt Decoder: `Features.DECODER_SETTING`. Chỉ nên bật lại khi đã thêm decoder phần mềm (ví dụ media3 FFmpeg).
- Cụm Legal trong Settings: `Features.LEGAL`. Bật lại khi có link Privacy / Terms thật, sửa `PRIVACY_URL` / `TERMS_URL`.
- Send feedback: `Features.FEEDBACK` (cả nút trong chatbot và khi đánh giá 1–3 sao). Bật lại khi có email hỗ trợ thật, sửa `SUPPORT_EMAIL`.
- Công tắc tự vào PiP: `Features.AUTO_PIP_SETTING`.
- Hướng dẫn chỗ lấy nguồn: `Features.GUIDE_SITES`. **Không bật lại bằng Remote Config sau khi đã duyệt**, Google coi đó là lách kiểm duyệt.

### Data safety với Firebase

Firebase Analytics + Crashlytics tự động thu thập một số dữ liệu. Trong Play Console › Data safety khai báo:

| Mục | Loại dữ liệu | Thu thập | Chia sẻ | Mục đích |
|---|---|---|---|---|
| App activity | App interactions (màn hình đã mở, sự kiện) | Có | Không | Analytics |
| App info and performance | Crash logs | Có | Không | App functionality / Analytics |
| App info and performance | Diagnostics | Có | Không | App functionality / Analytics |
| Device or other IDs | Device or other IDs (Firebase installation ID) | Có | Không | Analytics |

- Data is encrypted in transit: **Yes**.
- Users can request data deletion: tuỳ chính sách của bạn; thường chọn **No** vì không có tài khoản.
- Thu thập là **bắt buộc** (không có tuỳ chọn tắt trong app).
- Privacy policy phải nhắc tới việc dùng Firebase Analytics và Crashlytics.
