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

## Giai đoạn 2: Sửa code trước khi build release

- [ ] **Claude**: Đổi tên app, applicationId, icon theo Giai đoạn 1.
- [ ] **Claude**: Thay `PRIVACY_URL`, `TERMS_URL`, `SUPPORT_EMAIL` trong `ui/settings/Settings.kt`.
- [x] **Claude**: Nâng `targetSdk` / `compileSdk` lên **36**, AGP 8.7.2 → 8.11.1. Build debug + release và unit test đều qua; thư viện native đã căn trang 16 KB. *(06/10/2026)*
- [ ] **Claude + Bạn**: Test lại trên máy **Android 16** (Pixel). API 36 bắt buộc edge-to-edge, có predictive back, và trên màn hình lớn bỏ qua khoá xoay. Hiện mới test trên emulator Android 9.
- [x] ~~**Claude**: Sửa khôi phục backup: hiện chỉ khôi phục được profile Xtream đầu tiên.~~ **Hoãn**: đã tạm ẩn Sao lưu / Khôi phục (`Features.BACKUP = false`). Sửa lỗi này trước khi bật lại. *(06/10/2026)*
- [x] **Claude**: Tạm ẩn Cast (`Features.CAST = false`): nút Cast ở player (cả thanh dọc lẫn hàng công cụ toàn màn hình) và câu FAQ "How do I watch on my TV?". *(06/10/2026)*
- [ ] **Claude**: Passcode: thêm đổi mã / tắt passcode (hiện bấm vào là luôn tạo mã mới).
- [ ] **Claude**: Ẩn cài đặt Decoder (không có FFmpeg nên không có tác dụng).
- [x] **Claude**: Tạm ẩn "Phát tiếp khi tắt màn hình" (`Features.BACKGROUND_AUDIO = false`). Comment `FOREGROUND_SERVICE*` + `<service PlaybackService>` trong manifest; gỡ luôn `FOREGROUND_SERVICE` + `SystemForegroundService` do thư viện WorkManager tự thêm. Đã kiểm tra manifest đã gộp của bản release: không còn foreground service nào. PiP vẫn chạy. *(06/10/2026)*
- [ ] **Claude**: Cân nhắc bỏ hỏi quyền thông báo (`POST_NOTIFICATIONS`) lúc mở app: Sport và nghe nền đều đang tắt nên không còn tính năng nào gửi thông báo.
- [ ] **Claude**: Kiểm tra ProGuard cho bản release: các class Gson nằm ngoài `dto/` (`GuideSites`, `Backup`…) có thể bị R8 làm hỏng khi minify.
- [ ] **Bạn**: Tạo keystore ký release (hoặc để Claude tạo). **Cất file và mật khẩu cẩn thận**, mất là không cập nhật app được nữa.
- [ ] **Claude**: Cấu hình ký release (đọc mật khẩu từ file ngoài git) và build **AAB** (`bundleRelease`).

## Giai đoạn 3: Test bản release

- [x] **Claude**: Soạn file M3U test hợp pháp: [docs/review/test-playlist.m3u](review/test-playlist.m3u), 15 kênh trong 5 nhóm (live test, open movie HLS/DASH/MP4, test pattern). Đã import và phát thử đủ 15 kênh trên app. Hướng dẫn + văn bản dán vào Play Console: [docs/review/README.md](review/README.md). *(06/10/2026)*
- [x] **Bạn**: Đưa `test-playlist.m3u` lên repo public [iptvstore779/iptv_review](https://github.com/iptvstore779/iptv_review). Claude đã kiểm tra: file giống hệt bản local; cài mới app, import bằng link Raw ra 15 kênh / 5 nhóm, mỗi nhóm phát thử 1 kênh đều chạy. *(06/10/2026)*
  Link cho reviewer: `https://raw.githubusercontent.com/iptvstore779/iptv_review/refs/heads/main/test-playlist.m3u`
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
- Hướng dẫn chỗ lấy nguồn: `Features.GUIDE_SITES`. **Không bật lại bằng Remote Config sau khi đã duyệt**, Google coi đó là lách kiểm duyệt.
