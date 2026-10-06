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
- [ ] **Claude**: Sửa khôi phục backup: hiện chỉ khôi phục được **profile Xtream đầu tiên**.
- [ ] **Claude**: Passcode: thêm đổi mã / tắt passcode (hiện bấm vào là luôn tạo mã mới).
- [ ] **Claude**: Ẩn cài đặt Decoder (không có FFmpeg nên không có tác dụng).
- [ ] **Claude**: Kiểm tra ProGuard cho bản release: các class Gson nằm ngoài `dto/` (`GuideSites`, `Backup`…) có thể bị R8 làm hỏng khi minify.
- [ ] **Bạn**: Tạo keystore ký release (hoặc để Claude tạo). **Cất file và mật khẩu cẩn thận**, mất là không cập nhật app được nữa.
- [ ] **Claude**: Cấu hình ký release (đọc mật khẩu từ file ngoài git) và build **AAB** (`bundleRelease`).

## Giai đoạn 3: Test bản release

- [ ] **Claude**: Soạn file M3U test hợp pháp (chỉ stream mẫu: Big Buck Bunny, Mux test…).
- [ ] **Bạn**: Đưa file M3U test lên GitHub của bạn, gửi link raw cho Claude.
- [ ] **Claude + Bạn**: Cài bản release (đã minify) lên Pixel, chạy lại toàn bộ luồng: Home, Import URL/Xtream/Single, Player (PiP, nghe nền, hẹn giờ), Xtream (Movies, Live, Search, Favorite), Settings.
- [ ] **Bạn**: Test chọn file M3U thật trên máy.
- [ ] **Bạn**: Test sao lưu → xoá app → cài lại → khôi phục.
- [ ] **Bạn**: Test Cast lên TV.

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
- [ ] **Bạn**: Khai báo Foreground service `mediaPlayback` + video ngắn quay cảnh nghe nền khi tắt màn hình.
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
- Hướng dẫn chỗ lấy nguồn: `Features.GUIDE_SITES`. **Không bật lại bằng Remote Config sau khi đã duyệt**, Google coi đó là lách kiểm duyệt.
