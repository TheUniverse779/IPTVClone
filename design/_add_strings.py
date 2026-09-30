"""Append guide / FAQ / chatbot strings (EN + VI) to res/values*/strings.xml if missing."""
import io, os
RES = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res')
APOS = "\\'"  # Android string resources need escaped apostrophes

EN = {
    'howto_title_url': 'Add a playlist URL', 'howto_title_xtream': 'Add an Xtream account',
    'howto_title_single': 'Play a single stream', 'howto_title_upload': 'Import an M3U file',
    'howto_url_1': 'Open a <b>suggested site</b> below', 'howto_url_1b': 'or search "free iptv m3u playlist" on Google.',
    'howto_url_2': 'Long-press a <b>.m3u / .m3u8</b> link and copy it',
    'howto_url_2b': f'Inside the app{APOS}s suggested-site viewer, the copied link is detected right away.',
    'howto_url_3': 'Back in the app, tap <b>+</b> › <b>Playlist URL</b>', 'howto_url_3b': 'Paste the link, name it, turn on the passcode if you like.',
    'howto_url_4': 'Tap <b>Add playlist</b>', 'howto_url_4b': 'The app reads the channels and groups them.',
    'howto_xt_1': 'Get the <b>Server URL, Username, Password</b> from your provider', 'howto_xt_1b': 'or from the guide page below.',
    'howto_xt_2': 'Tap <b>+</b> › <b>Xtream Codes</b>',
    'howto_xt_2b': 'If you have a get.php?username=… link, paste it into Server URL — the app fills all 3 fields.',
    'howto_xt_3': 'Tap <b>Log in &amp; sync</b>', 'howto_xt_3b': 'The app downloads Live, Movies and Series.',
    'howto_xt_4': 'Open the <b>Xtream</b> tab and pick the profile',
    'howto_single_1': 'Copy a stream link (.m3u8, .mp4, rtmp://…)', 'howto_single_2': 'Tap <b>+</b> › <b>Single stream</b> and paste it',
    'howto_single_3': 'Tap <b>Play now</b>', 'howto_single_3b': 'Turn on "Save to list" to open it again later.',
    'howto_upload_1': 'Download the .m3u / .m3u8 file to your phone', 'howto_upload_2': 'Tap <b>+</b> › <b>M3U file</b> and pick up to 5 files',
    'howto_upload_3': 'Name it and tap <b>Import</b>', 'howto_upload_3b': 'You can delete the original file afterwards.',
    'faq_q1': 'Where do I find a playlist?',
    'faq_a1': 'Open Guide › Suggested sites, or search "free iptv m3u playlist" on Google. Copy a link ending in .m3u / .m3u8 and paste it into the app.',
    'faq_q2': f'A channel doesn{APOS}t play — what can I do?',
    'faq_a2': 'The link may be dead, geo-blocked, or need its own User-Agent. Try another channel, update the playlist, or change the User-Agent in Settings › Playback.',
    'faq_q3': 'How do I edit, delete or lock a playlist?',
    'faq_a3': 'On Home or the Channels tab, tap ⋮ next to the playlist › Edit (rename, passcode, auto update) or Delete.',
    'faq_q4': 'Xtream says wrong account?',
    'faq_a4': 'Check the Server URL includes the port (e.g. :8080), and the username and password. An expired account is shown on the Edit profile screen.',
    'faq_q5': 'How do I watch on my TV?',
    'faq_a5': 'In the player tap Cast › Open settings and pick a TV on the same Wi‑Fi. Or use picture-in-picture to keep watching while using other apps.',
    'bot_a_m3u': 'Add an M3U playlist:', 'bot_a_m3u_s': '1. Tap + › Playlist URL\\n2. Paste the .m3u / .m3u8 link\\n3. Tap Add playlist',
    'bot_a_single': 'Play a stream link:', 'bot_a_single_s': '1. Tap + › Single stream\\n2. Paste the link\\n3. Tap Play now',
    'bot_a_xtream': 'Set up Xtream:', 'bot_a_xtream_s': '1. Tap + › Xtream Codes\\n2. Enter Server URL, Username, Password\\n3. Tap Log in &amp; sync',
    'open_form': 'Open form', 'language_title': 'Language',
}
VI = {
    'howto_title_url': 'Thêm playlist URL', 'howto_title_xtream': 'Thêm tài khoản Xtream',
    'howto_title_single': 'Phát single stream', 'howto_title_upload': 'Nhập file M3U',
    'howto_url_1': 'Mở <b>trang gợi ý</b> bên dưới', 'howto_url_1b': 'hoặc tìm "free iptv m3u playlist" trên Google.',
    'howto_url_2': 'Chạm giữ vào link <b>.m3u / .m3u8</b> rồi chọn Copy', 'howto_url_2b': 'Trong trang gợi ý của app, link bạn copy sẽ được nhận diện ngay.',
    'howto_url_3': 'Quay lại app, bấm <b>+</b> › <b>Playlist URL</b>', 'howto_url_3b': 'Dán link, đặt tên, bật passcode nếu muốn.',
    'howto_url_4': 'Bấm <b>Thêm playlist</b>', 'howto_url_4b': 'App đọc danh sách kênh và chia theo nhóm.',
    'howto_xt_1': 'Lấy <b>Server URL, Username, Password</b> từ nhà cung cấp', 'howto_xt_1b': 'hoặc từ trang hướng dẫn bên dưới.',
    'howto_xt_2': 'Bấm <b>+</b> › <b>Xtream Codes</b>', 'howto_xt_2b': 'Nếu bạn có link dạng get.php?username=…, dán vào ô Server URL: app tự tách 3 ô.',
    'howto_xt_3': 'Bấm <b>Đăng nhập &amp; đồng bộ</b>', 'howto_xt_3b': 'App tải Live, Movies, Series về máy.',
    'howto_xt_4': 'Vào tab <b>Xtream</b>, chọn profile để xem',
    'howto_single_1': 'Copy link stream (.m3u8, .mp4, rtmp://…)', 'howto_single_2': 'Bấm <b>+</b> › <b>Single stream</b>, dán link',
    'howto_single_3': 'Bấm <b>Phát ngay</b>', 'howto_single_3b': 'Bật "Lưu vào danh sách" để mở lại sau.',
    'howto_upload_1': 'Tải file .m3u / .m3u8 về máy', 'howto_upload_2': 'Bấm <b>+</b> › <b>File M3U</b>, chọn tối đa 5 file',
    'howto_upload_3': 'Đặt tên, bấm <b>Nhập</b>', 'howto_upload_3b': 'File gốc có thể xóa sau khi nhập.',
    'faq_q1': 'Tìm playlist ở đâu?',
    'faq_a1': 'Mở Hướng dẫn › Trang gợi ý, hoặc tìm "free iptv m3u playlist" trên Google. Copy link kết thúc bằng .m3u / .m3u8 rồi dán vào app.',
    'faq_q2': 'Kênh không phát được thì làm gì?',
    'faq_a2': 'Link có thể đã chết, bị chặn theo khu vực, hoặc cần User-Agent riêng. Thử kênh khác, bấm Cập nhật playlist, hoặc đổi User-Agent trong Cài đặt › Phát video.',
    'faq_q3': 'Sửa, xóa, khóa playlist thế nào?',
    'faq_a3': 'Ở Home hoặc tab Channels, bấm ⋮ bên cạnh playlist › Sửa (đổi tên, passcode, tự cập nhật) hoặc Xóa.',
    'faq_q4': 'Xtream báo sai tài khoản?',
    'faq_a4': 'Kiểm tra Server URL có đúng cổng (vd. :8080), username, password. Tài khoản hết hạn sẽ hiện ở màn Sửa profile.',
    'faq_q5': 'Chiếu lên TV thế nào?',
    'faq_a5': 'Trong player bấm Cast › Mở cài đặt, chọn TV cùng Wi‑Fi. Hoặc dùng PiP để vừa xem vừa dùng app khác.',
    'bot_a_m3u': 'Thêm playlist M3U:', 'bot_a_m3u_s': '1. Bấm + › Playlist URL\\n2. Dán link .m3u / .m3u8\\n3. Bấm Thêm playlist',
    'bot_a_single': 'Phát 1 link stream:', 'bot_a_single_s': '1. Bấm + › Single stream\\n2. Dán link\\n3. Bấm Phát ngay',
    'bot_a_xtream': 'Thiết lập Xtream:', 'bot_a_xtream_s': '1. Bấm + › Xtream Codes\\n2. Nhập Server URL, Username, Password\\n3. Bấm Đăng nhập &amp; đồng bộ',
    'open_form': 'Mở form', 'language_title': 'Ngôn ngữ',
}

def add(rel, d):
    path = os.path.join(RES, rel)
    s = io.open(path, encoding='utf-8').read()
    extra = ''.join(f'    <string name="{k}">{v}</string>\n' for k, v in d.items() if f'name="{k}"' not in s)
    io.open(path, 'w', encoding='utf-8').write(s.replace('</resources>', extra + '</resources>'))
    return extra.count('<string')

print(add('values/strings.xml', EN), add('values-vi/strings.xml', VI))
