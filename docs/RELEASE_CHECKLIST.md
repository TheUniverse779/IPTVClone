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

- [ ] **Bạn**: Tên app hiển thị (hiện là `IPTV Player`).
- [ ] **Bạn**: applicationId / package name (hiện là `com.iptvplayer.app`, quá chung). Lên Play rồi thì **không đổi được nữa**.
- [ ] **Bạn**: Icon app: file PNG 512×512, hoặc để Claude vẽ icon vector.
- [ ] **Bạn**: Link Privacy policy (bắt buộc, phải là trang web công khai).
- [ ] **Bạn**: Link Terms of use (có thể chung trang với Privacy).
- [ ] **Bạn**: Email hỗ trợ (hiện là `support@example.com`).
- [x] **Bạn**: Chốt target SDK: **36**. *(06/10/2026)*
- [ ] **Bạn** *(tuỳ chọn)*: Màn chọn ngôn ngữ + onboarding. Bạn tự code; hook đã có sẵn trong `SplashActivity`.
- [x] **Claude**: Tạm ẩn dòng Language trong Settings (`Features.LANGUAGE_SETTING = false`). App mặc định tiếng Anh; Android 13+ vẫn chọn được ngôn ngữ của app trong cài đặt hệ thống. *(06/10/2026)*

## Giai đoạn 2: Sửa code trước khi build release

- [ ] **Claude**: Đổi tên app, applicationId, icon theo Giai đoạn 1.
- [ ] **Claude**: Thay `PRIVACY_URL`, `TERMS_URL`, `SUPPORT_EMAIL` trong `ui/settings/Settings.kt`.
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
- [x] **Bạn**: Chọn **phương án B** cho Xtream: không cấp tài khoản cho reviewer, ghi chú trong App access rằng Xtream cần thông tin đăng nhập của nhà cung cấp. Đã thêm đoạn ghi chú tiếng Anh vào [docs/review/README.md](review/README.md). *(06/10/2026)*
  Rủi ro đã biết: reviewer không test được Xtream, có thể bị hỏi lại hoặc từ chối. Nếu Play hỏi, phương án dự phòng là dựng server Xtream demo.
- [ ] **Claude + Bạn**: Cài bản release (đã minify) lên Pixel, chạy lại toàn bộ luồng: Home, Import URL/Xtream/Single, Player (PiP, nghe nền, hẹn giờ), Xtream (Movies, Live, Search, Favorite), Settings.
- [ ] **Bạn**: Test chọn file M3U thật trên máy.
- [x] ~~**Bạn**: Test sao lưu → xoá app → cài lại → khôi phục.~~ **Không cần cho bản đầu**: đã tạm ẩn. *(06/10/2026)*
- [x] ~~**Bạn**: Test Cast lên TV.~~ **Không cần cho bản đầu**: đã tạm ẩn. *(06/10/2026)*

## Giai đoạn 4: Play Console

- [ ] **Bạn**: Tài khoản developer. Nếu là tài khoản **cá nhân tạo sau 11/2023**, cần chạy closed test (xem bước cuối giai đoạn này).
- [ ] **Bạn**: Tạo app mới (tên, ngôn ngữ mặc định, App / Free).
- [ ] **Claude**: Soạn mô tả ngắn + mô tả dài (EN, VI). Tránh các cụm "free TV", "free channels", "watch sports", "movies for free".
- [ ] **Bạn**: Ảnh chụp màn hình (ít nhất 2 ảnh điện thoại) + feature graphic 1024×500. **Không** để lộ poster/logo Netflix, HBO hay kênh có thương hiệu: dùng profile Demo của mock server hoặc file M3U test.
- [ ] **Bạn**: Điền link Privacy policy.
- [ ] **Bạn**: App access: "Cần hướng dẫn đặc biệt" → dán link M3U test + cách thêm (bấm + › Playlist URL › dán › Add playlist).
- [ ] **Bạn**: Ads: chọn **No ads**.
- [ ] **Bạn**: Data safety: app không thu thập hay chia sẻ dữ liệu (không analytics, không tài khoản; dữ liệu chỉ lưu trên máy).
- [ ] **Bạn**: Content rating (bảng câu hỏi IARC).
- [ ] **Bạn**: Target audience: 18+ hoặc 13+, **không** chọn trẻ em.
- [x] ~~**Bạn**: Khai báo Foreground service `mediaPlayback` + video quay màn hình.~~ **Không cần cho bản đầu**: đã tạm ẩn nghe nền, app không còn foreground service. *(06/10/2026)*
- [ ] **Bạn**: Upload AAB lên Internal testing, cài thử từ Play.
- [ ] **Bạn**: Closed testing với **ít nhất 12 người trong 14 ngày** (nếu tài khoản cá nhân).
- [ ] **Bạn**: Nộp bản Production và chờ duyệt.

## Giai đoạn 5: Sau khi lên

- [ ] **Claude**: Push code lên `origin` (hiện có 3 commit chưa push).
- [ ] **Bạn + Claude**: Theo dõi crash / ANR trong Play Console (Android vitals) và sửa.

---

### Đang tạm ẩn, để sau (không cần cho lần lên đầu)

- Tab Sport: `Features.SPORT`
- Community: `Features.COMMUNITY` (cần `google-services.json`)
- Phát tiếp khi tắt màn hình: `Features.BACKGROUND_AUDIO`. Bật lại thì phải: (1) bỏ comment 2 quyền `FOREGROUND_SERVICE*` và `<service PlaybackService>` trong `AndroidManifest.xml`, (2) xoá 2 dòng `tools:node="remove"` (quyền `FOREGROUND_SERVICE` và `SystemForegroundService`), (3) khai báo foreground service `mediaPlayback` + video trong Play Console.
- Cast lên TV: `Features.CAST` (mở cài đặt cast / màn hình không dây của Android). Bật lại thì test với TV thật.
- Sao lưu / khôi phục: `Features.BACKUP`. Bật lại thì **phải sửa trước**: khôi phục hiện chỉ lấy profile Xtream đầu tiên.
- Quyền thông báo: `Features.NOTIFICATION_PERMISSION`. Bật lại cùng lúc với Sport hoặc nghe nền, và bỏ comment `POST_NOTIFICATIONS` trong manifest.
- Chọn ngôn ngữ trong Settings: `Features.LANGUAGE_SETTING`. Bật lại khi có thêm ngôn ngữ, hoặc thay bằng màn Language bạn tự code.
- Cài đặt Decoder: `Features.DECODER_SETTING`. Chỉ nên bật lại khi đã thêm decoder phần mềm (ví dụ media3 FFmpeg).
- Hướng dẫn chỗ lấy nguồn: `Features.GUIDE_SITES`. **Không bật lại bằng Remote Config sau khi đã duyệt**, Google coi đó là lách kiểm duyệt.
