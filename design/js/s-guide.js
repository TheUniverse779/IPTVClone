/* ===== Guide: HowTo, WebGuide sheet, FAQ, Chatbot, Settings, Feedback, Rate ===== */

const HOWTO = {
  url: { title: 'Thêm playlist URL', site: 'iptv', steps: [
    ['Mở <b>trang gợi ý</b> bên dưới', 'hoặc tìm "free iptv m3u playlist" trên Google.'],
    ['Chạm giữ vào link <b>.m3u / .m3u8</b> rồi chọn Copy', 'Trong trang gợi ý của app, link bạn copy sẽ được nhận diện ngay.'],
    ['Quay lại app, bấm <b>+</b> › <b>Playlist URL</b>', 'Dán link, đặt tên, bật passcode nếu muốn.'],
    ['Bấm <b>Thêm playlist</b>', 'App đọc danh sách kênh và chia theo nhóm.'],
  ] },
  xtream: { title: 'Thêm tài khoản Xtream', site: 'xtream', steps: [
    ['Lấy <b>Server URL, Username, Password</b> từ nhà cung cấp', 'hoặc từ trang hướng dẫn bên dưới.'],
    ['Bấm <b>+</b> › <b>Xtream Codes</b>', 'Nếu bạn có link dạng get.php?username=…, dán vào ô Server URL: app tự tách 3 ô.'],
    ['Bấm <b>Đăng nhập & đồng bộ</b>', 'App tải Live, Movies, Series về máy.'],
    ['Vào tab <b>Xtream</b>, chọn profile để xem', ''],
  ] },
  single: { title: 'Phát single stream', site: 'single', steps: [
    ['Copy link stream (.m3u8, .mp4, rtmp://…)', ''],
    ['Bấm <b>+</b> › <b>Single stream</b>, dán link', ''],
    ['Bấm <b>Phát ngay</b>', 'Bật "Lưu vào danh sách" để mở lại sau.'],
  ] },
  upload: { title: 'Nhập file M3U', site: 'iptv', steps: [
    ['Tải file .m3u / .m3u8 về máy', ''],
    ['Bấm <b>+</b> › <b>File M3U</b>, chọn tối đa 5 file', ''],
    ['Đặt tên, bấm <b>Nhập</b>', 'File gốc có thể xóa sau khi nhập.'],
  ] },
};

def('howto', {
  cls: 'HowToAddActivity', layout: 'activity_how_to_add', group: 'Hướng dẫn',
  desc: 'Extra type: url / xtream / single / upload. Có video hướng dẫn (res/raw), trang gợi ý (WebView sheet) và nút tìm trên Google (Custom Tabs). Danh sách trang lấy từ RC guide_sites.',
  render: (p) => {
    const t = p.type || 'url'; const h = HOWTO[t];
    const sites = DATA.guideSites[h.site];
    return `${toolbar({ title: 'Hướng dẫn' })}
      <div class="chips" style="padding-bottom:14px">${Object.entries(HOWTO).map(([k, v]) => `<button class="chip ${k === t ? 'on' : ''}" onclick="setParams({type:'${k}'})">${{ url: 'Playlist URL', xtream: 'Xtream', single: 'Single stream', upload: 'File M3U' }[k]}</button>`).join('')}</div>
      <div class="content pad">
        <div class="video portrait" style="border-radius:16px;height:180px"><div class="pic" style="--pv:linear-gradient(135deg,#2a1a10,#151419)"></div>
          <div style="position:absolute;inset:0;display:grid;place-items:center"><button class="btn primary small" onclick="toast('Phát video hướng dẫn (res/raw)')">${ic('play', 's16')}Xem video 0:48</button></div></div>
        <h2 class="h-cond" style="font-size:24px;margin:18px 0 12px">${h.title}</h2>
        <ol class="steps">${h.steps.map(s => `<li><p><b>${s[0]}</b>${s[1] ? `<br>${s[1]}` : ''}</p></li>`).join('')}</ol>
        <div class="sec-h" style="padding:22px 0 10px"><h3>Trang gợi ý</h3></div>
        <div class="stack">${sites.map((s, i) => `<button class="action-card" onclick="openOverlay('webGuide',{type:'${h.site}', i:${i}})"><span class="ic-wrap">${ic('globe')}</span>
          <span class="main"><b>${s.title}</b><small>${s.host}</small></span>${ic('chevron', 's20')}</button>`).join('')}
          <button class="action-card" onclick="toast('Mở Chrome Custom Tab: google.com/search?q=${DATA.guideSites.search}')"><span class="ic-wrap">${ic('search')}</span>
          <span class="main"><b>Tìm trên Google</b><small>"${DATA.guideSites.search}"</small></span>${ic('external', 's20')}</button></div>
        <p class="hint" style="margin-top:14px">Các trang trên thuộc bên thứ ba. App không kiểm soát nội dung và không bảo đảm link còn hoạt động.</p>
        <div class="spacer"></div>
        <button class="btn primary block" onclick="go('import',{tab:'${t === 'upload' ? 'url' : t}'})">${t === 'upload' ? 'Chọn file M3U' : 'Mở form thêm nguồn'}</button><div class="spacer"></div></div>`;
  },
});

def('webGuide', {
  kind: 'BottomSheet', cls: 'WebGuideBottomSheet', layout: 'sheet_web_guide', group: 'Hướng dẫn',
  desc: 'WebView trong app như app gốc (JS + DOM storage + zoom). Thêm: phát hiện link .m3u/.m3u8/get.php khi người dùng copy hoặc chạm → snackbar "Dùng link này" điền thẳng vào form. Thử: chạm vào 1 link trong bảng.',
  render: (p) => {
    const s = DATA.guideSites[p.type || 'iptv'][p.i || 0];
    return `<div class="sheet full"><div class="grab"></div>
      <div class="sheet-h"><button class="text-btn" onclick="closeOverlay()">Đóng</button><h3 style="font-size:17px">Trang gợi ý</h3>
        <button class="icon-btn" onclick="toast('Mở bằng trình duyệt')">${ic('external', 's20')}</button></div>
      <div class="webbar"><button class="icon-btn" style="width:36px;height:36px">${ic('back', 's20')}</button><div class="addr">${ic('lock', 's16')}${s.url.replace('https://', '')}</div>
        <button class="icon-btn" style="width:36px;height:36px" onclick="toast('Tải lại')">${ic('refresh', 's20')}</button></div>
      <div class="bar" style="height:2px;border-radius:0"><i style="width:100%"></i></div>
      <div class="webpage"><div class="gh-top">${ic('doc', 's16')} ${s.url.split('/').slice(-2).join(' / ')}</div>
        <div class="gh-body"><h1>Free IPTV playlists</h1><p>Danh sách playlist công khai, sắp xếp theo quốc gia. Link có thể thay đổi, hãy kiểm tra trước khi dùng.</p>
          <h2>By country</h2><table><tr><th>Country</th><th>Channels</th><th>Playlist</th></tr>
          ${[['Vietnam', 82, 'vn'], ['Japan', 190, 'jp'], ['France', 240, 'fr'], ['Brazil', 310, 'br'], ['Canada', 150, 'ca']].map(r => `<tr><td>${r[0]}</td><td>${r[1]}</td><td><code onclick="webLinkTap(this)">https://iptv-org.github.io/iptv/countries/${r[2]}.m3u</code></td></tr>`).join('')}</table>
          <h2>By category</h2><table><tr><th>Category</th><th>Playlist</th></tr>
          ${['news', 'kids', 'music', 'documentary'].map(r => `<tr><td>${r}</td><td><code onclick="webLinkTap(this)">https://iptv-org.github.io/iptv/categories/${r}.m3u</code></td></tr>`).join('')}</table></div></div></div>`;
  },
});
function webLinkTap(el) {
  document.querySelectorAll('.webpage code').forEach(c => c.classList.remove('sel'));
  el.classList.add('sel');
  const url = el.textContent;
  snackbar('Đã copy link playlist .m3u', 'Dùng link này', () => { closeAllOverlays(); go('import', { tab: 'url', url }); toast('Đã điền link vào form'); });
}

/* ---------------- FAQ ---------------- */
const FAQS = [
  ['Tìm playlist ở đâu?', 'Mở Hướng dẫn › Trang gợi ý, hoặc tìm "free iptv m3u playlist" trên Google. Copy link kết thúc bằng .m3u / .m3u8 rồi dán vào app.'],
  ['Kênh không phát được thì làm gì?', 'Link có thể đã chết, bị chặn theo khu vực, hoặc cần User-Agent riêng. Thử kênh khác, bấm Cập nhật playlist, hoặc đổi User-Agent trong Cài đặt › Nâng cao.'],
  ['Sửa, xóa, khóa playlist thế nào?', 'Ở Home hoặc tab Channels, bấm ⋮ bên cạnh playlist › Sửa (đổi tên, passcode, tự cập nhật) hoặc Xóa.'],
  ['Xtream báo sai tài khoản?', 'Kiểm tra Server URL có đúng cổng (vd. :8080), username, password. Tài khoản hết hạn sẽ hiện ở màn Sửa profile.'],
  ['Chiếu lên TV thế nào?', 'Trong player bấm Cast › Mở cài đặt, chọn TV cùng Wi-Fi. Hoặc dùng PiP để vừa xem vừa dùng app khác.'],
];
def('faq', {
  cls: 'FaqActivity', layout: 'activity_faq', group: 'Hướng dẫn',
  desc: 'Danh sách câu hỏi mở rộng tại chỗ (không cần FaqDetailActivity nếu nội dung ngắn — cần bạn quyết).',
  render: (p) => `${toolbar({ title: 'Câu hỏi thường gặp' })}
    <div class="content pad"><div class="list-card" style="margin:0">${FAQS.map((f, i) => `<div class="row" style="flex-wrap:wrap;padding-right:12px" onclick="setParams({open:${p.open === i ? -1 : i}})">
      <div class="main"><b style="white-space:normal">${f[0]}</b></div>${ic(p.open === i ? 'chevronDown' : 'chevron', 's20')}
      ${p.open === i ? `<p class="muted" style="flex-basis:100%;padding:4px 0 8px">${f[1]}</p>` : ''}</div>`).join('')}</div>
      <div class="card" style="margin-top:16px;display:flex;gap:12px;align-items:center">${ic('chat')}<div style="flex:1"><b>Vẫn cần giúp?</b><p class="hint">Hỏi trợ lý hoặc gửi phản hồi.</p></div>
        <button class="btn small tonal" onclick="go('chatbot')">Hỏi trợ lý</button></div><div class="spacer"></div></div>`,
});

/* ---------------- Chatbot ---------------- */
const BOT_ANSWERS = {
  m3u: { t: 'Thêm playlist M3U:', s: ['Bấm + › Playlist URL', 'Dán link .m3u / .m3u8', 'Bấm Thêm playlist'], a: [['Thêm playlist', "go('import',{tab:'url'})"], ['Xem hướng dẫn', "go('howto',{type:'url'})"]] },
  single: { t: 'Phát 1 link stream:', s: ['Bấm + › Single stream', 'Dán link', 'Bấm Phát ngay'], a: [['Mở form', "go('import',{tab:'single'})"]] },
  xtream: { t: 'Thiết lập Xtream:', s: ['Bấm + › Xtream Codes', 'Nhập Server URL, Username, Password', 'Bấm Đăng nhập & đồng bộ'], a: [['Thêm Xtream', "go('import',{tab:'xtream'})"], ['Xem hướng dẫn', "go('howto',{type:'xtream'})"]] },
};
function botMatch(q) {
  q = q.toLowerCase();
  if (/m3u|playlist|danh sách/.test(q)) return 'm3u';
  if (/single|stream|link/.test(q)) return 'single';
  if (/xtream|username|password|server|host/.test(q)) return 'xtream';
  return null;
}
def('chatbot', {
  cls: 'ChatbotActivity', layout: 'activity_chatbot', group: 'Hướng dẫn',
  desc: 'Offline, khớp từ khóa (có tiếng Việt). 3 câu gợi ý (bỏ câu về Premium). Hiệu ứng đang gõ 1,5 s.',
  render: (p) => {
    const msgs = p.msgs || [];
    const bubble = (m) => {
      if (m.user) return `<div class="bubble user">${esc(m.text)}</div>`;
      if (m.typing) return `<div class="bubble bot"><span class="typing"><i></i><i></i><i></i></span></div>`;
      if (m.key === null) return `<div class="bubble bot">Mình chưa hiểu câu này. Thử hỏi về playlist M3U, single stream hoặc Xtream nhé.
        <div class="bacts"><button class="btn small tonal" onclick="go('faq')">Xem FAQ</button><button class="btn small tonal" onclick="go('feedback')">Liên hệ hỗ trợ</button></div></div>`;
      const a = BOT_ANSWERS[m.key];
      return `<div class="bubble bot">${a.t}<ol>${a.s.map(x => `<li>${x}</li>`).join('')}</ol><div class="bacts">${a.a.map(b => `<button class="btn small primary" onclick="${b[1]}">${b[0]}</button>`).join('')}</div></div>`;
    };
    return `${toolbar({ title: 'Trợ lý IPTV', sub: 'Trả lời ngay, không cần mạng' })}
      <div class="content"><div class="chat">
        <div class="bubble bot">Chào bạn! Mình có thể hướng dẫn thêm playlist, Xtream hoặc link stream.</div>
        ${msgs.map(bubble).join('')}
        ${msgs.length ? '' : `<div class="stack" style="margin-top:6px">${['Thêm playlist M3U thế nào?', 'Phát 1 link stream thế nào?', 'Thiết lập Xtream thế nào?'].map(q => `<button class="chip" style="height:auto;padding:10px 14px;border-radius:14px;white-space:normal;text-align:left;justify-content:flex-start" onclick="botAsk('${q}')">${q}</button>`).join('')}</div>`}
      </div></div>
      <div class="composer"><label class="input"><input id="botq" placeholder="Nhập câu hỏi…" onkeydown="onEnter(event, ()=>botAsk(this.value))"></label>
        <button class="send" onclick="botAsk(document.getElementById('botq').value)">${ic('send')}</button></div>`;
  },
});
function botAsk(q) {
  if (!q.trim()) return;
  const p = topParams(); p.msgs = [...(p.msgs || []), { user: true, text: q }, { typing: true }]; refresh();
  setTimeout(() => { const t = topParams(); t.msgs.pop(); t.msgs.push({ key: botMatch(q) }); refresh(); const c = document.querySelector('.content'); c.scrollTop = c.scrollHeight; }, 1500);
}

/* ---------------- Settings ---------------- */
function settingsBody(withBack) {
  const row = (icon, label, onclick, trail = ic('chevron', 's20')) => `<div class="row" onclick="${onclick}">${ic(icon)}<div class="main"><b style="font-weight:500">${label}</b></div><div class="trail" style="padding-right:8px">${trail}</div></div>`;
  return `${toolbar({ title: 'Cài đặt', backBtn: withBack })}
    <div class="set-title">Chung</div>
    <div class="set-group">${row('lang', 'Ngôn ngữ', "toast('Mở màn Language của bạn')", '<span class="hint">Tiếng Việt</span>')}
      ${row('lock', 'Passcode', "openOverlay('passcode',{mode:'create'})", `<span class="hint">${DATA.settings.passcode ? 'Đã bật' : 'Chưa tạo'}</span>`)}</div>
    <div class="set-title">Phát video</div>
    <div class="set-group">${row('pip', 'Tự vào PiP khi rời app', '', '<button class="switch on" onclick="event.stopPropagation(); this.classList.toggle(\'on\')"></button>')}
      ${row('audio', 'Phát tiếp khi tắt màn hình', '', '<button class="switch" onclick="event.stopPropagation(); this.classList.toggle(\'on\')"></button>')}
      ${row('globe', 'User-Agent mặc định', "toast('Chọn: App / VLC / Chrome / Tùy chỉnh')", '<span class="hint">App</span>')}
      ${row('bolt', 'Bộ giải mã', "toast('Phần cứng / Phần mềm')", '<span class="hint">Phần cứng</span>')}</div>
    <div class="set-title">Dữ liệu</div>
    <div class="set-group">${row('upload', 'Sao lưu playlist (JSON)', "toast('Đã xuất file backup')")}${row('folder', 'Khôi phục từ file', "toast('Chọn file backup')")}
      ${row('trash', 'Xóa cache ảnh', "toast('Đã xóa 48 MB')")}</div>
    <div class="set-title">Hỗ trợ</div>
    <div class="set-group">${row('help', 'Hướng dẫn thêm nguồn', "go('howto',{type:'url'})")}${row('chat', 'Câu hỏi thường gặp', "go('faq')")}
      ${row('mail', 'Gửi phản hồi', "go('feedback')")}${row('star', 'Đánh giá app', "openOverlay('rate')")}${row('share', 'Chia sẻ app', "toast('Mở share sheet')")}</div>
    <div class="set-title">Pháp lý</div>
    <div class="set-group">${row('shield', 'Chính sách quyền riêng tư', "toast('Mở Custom Tab')", ic('external', 's20'))}${row('doc', 'Điều khoản sử dụng', "toast('Mở Custom Tab')", ic('external', 's20'))}
      ${row('info', 'License agreement', "go('disclaimer',{ok:true, view:true})")}</div>
    <p class="hint" style="text-align:center;margin:8px 0 100px">Phiên bản 1.0.0</p>`;
}
def('settings', {
  cls: 'SettingsActivity', layout: 'activity_settings', group: 'Cài đặt',
  desc: 'Chỉ chứa SettingsFragment (dùng chung với tab thứ 4 khi RC tắt Sport). Không còn Get Pro / Restore Purchase / quảng cáo chéo.',
  fragment: () => ({ kind: 'Fragment', cls: 'SettingsFragment', layout: 'fragment_settings' }),
  render: () => `<div class="content">${settingsBody(true)}</div>`,
});
def('feedback', {
  cls: 'FeedbackActivity', layout: 'activity_feedback', group: 'Cài đặt',
  render: () => `${toolbar({ title: 'Gửi phản hồi' })}<div class="content pad">
    <p class="muted" style="margin-bottom:16px">Mô tả lỗi hoặc góp ý. Nếu lỗi ở 1 kênh, hãy ghi tên kênh và playlist.</p>
    <div class="chips" style="padding:0 0 16px">${['Kênh không phát', 'Import lỗi', 'Góp ý', 'Khác'].map((x, i) => `<button class="chip ${i === 0 ? 'on' : ''}">${x}</button>`).join('')}</div>
    <label class="field"><span class="label">Email (để phản hồi lại bạn)</span><span class="input"><input placeholder="ban@email.com"></span></label>
    <label class="field"><span class="label">Nội dung <i>*</i></span><span class="input area"><textarea placeholder="Chuyện gì đã xảy ra?"></textarea></span></label>
    <button class="btn primary block" onclick="back(); toast('Đã gửi phản hồi')">Gửi</button></div>`,
});
def('rate', {
  kind: 'Dialog', cls: 'RateDialog', layout: 'dialog_rate', group: 'Cài đặt',
  desc: '4–5 sao → In-App Review API. 1–3 sao → mở FeedbackActivity.',
  render: (p) => `<div class="dialog"><h3>Bạn thấy app thế nào?</h3><p>Đánh giá giúp mình biết cần cải thiện gì.</p>
    <div style="display:flex;justify-content:center;gap:6px;margin-top:16px;color:var(--gold)">${[1, 2, 3, 4, 5].map(i => `<button onclick="state.overlays[state.overlays.length-1].params.s=${i}; refreshOverlay()">${ic(i <= (p.s || 0) ? 'starFill' : 'star', 's32')}</button>`).join('')}</div>
    <div class="actions"><button class="btn tonal" onclick="closeOverlay()">Để sau</button>
      <button class="btn primary" ${p.s ? '' : 'disabled'} onclick="closeOverlay(); ${p.s >= 4 ? "toast('Mở In-App Review của Google Play')" : "go('feedback')"}">Gửi</button></div></div>`,
});
