/* ===== ImportActivity / UploadM3uActivity / progress / passcode / edit ===== */

const IMPORT_TABS = [['url', 'URL'], ['xtream', 'Xtream'], ['single', 'Single stream']];

def('import', {
  cls: 'ImportActivity', layout: 'activity_import', group: 'Thêm nguồn',
  desc: 'Tab pill = 3 layout <include> bật/tắt (form_import_url / _xtream / _single), không dùng Fragment. Extra "tab" mở đúng tab.',
  render: (p) => {
    const tab = p.tab || 'url';
    const title = { url: 'Nhập playlist URL', xtream: 'Nhập tài khoản Xtream', single: 'Phát single stream' }[tab];
    return `${toolbar({ title, right: `<button class="icon-btn" onclick="go('howto',{type:'${tab}'})">${ic('help')}</button>` })}
      <div class="pad" style="padding-bottom:14px"><div class="seg">${IMPORT_TABS.map(([k, l]) =>
        `<button class="${tab === k ? 'on' : ''}" onclick="setParams({tab:'${k}', err:null})">${l}</button>`).join('')}</div></div>
      <div class="content pad">${IMPORT_FORM[tab](p)}</div>`;
  },
});

function guideStrip(type, text) {
  return `<button class="guide-link" style="margin:18px 0 0;width:100%" onclick="openOverlay('webGuide',{type:'${type}'})">
    ${ic('globe')}<span>${text}</span>${ic('chevron', 's20')}</button>`;
}

const IMPORT_FORM = {
  url: (p) => `
    <label class="field ${p.err ? 'error' : ''}"><span class="label">Playlist URL <i>*</i></span>
      <span class="input">${ic('link', 's20')}<input id="f-url" placeholder="https://…/playlist.m3u" value="${esc(p.url || '')}">
      <button class="paste-chip" onclick="pasteInto('f-url')">Dán</button></span>
      <span class="helper">${p.err || 'Hỗ trợ M3U, M3U8 và JSON. Giao thức: http, https, rtmp, rtsp, udp.'}</span></label>
    <label class="field"><span class="label">Tên playlist</span>
      <span class="input"><input placeholder="Playlist 3" id="f-name"></span></label>
    <div class="switch-row"><div class="txt">Khóa bằng passcode<small>Phải nhập passcode khi mở playlist</small></div>
      <button class="switch ${p.lock ? 'on' : ''}" onclick="setParams({lock:!topParams().lock})"></button></div>
    <div class="switch-row"><div class="txt">Tự động cập nhật<small>Tải lại playlist mỗi 24 giờ</small></div>
      <button class="switch ${p.auto !== false ? 'on' : ''}" onclick="setParams({auto:topParams().auto===false})"></button></div>
    ${guideStrip('iptv', 'Chưa có link? <b>Mở trang gợi ý</b> để tìm và copy playlist')}
    <div class="spacer"></div>
    <button class="btn primary block" onclick="submitUrl()">Thêm playlist</button>
    <p class="hint" style="text-align:center;margin:10px 0 20px">Bấm Thêm playlist là bạn đồng ý với <span class="accent">License agreement</span></p>`,
  xtream: (p) => `
    ${xtreamFormFields(p)}
    <button class="guide-link" style="margin:12px 0 0;width:100%" onclick="go('community',{tab:'xtream'})">${ic('share')}<span>Hoặc <b>chọn 1 tài khoản</b> người dùng khác chia sẻ trên Community</span>${ic('chevron', 's20')}</button>
    ${guideStrip('xtream', 'Chưa có tài khoản? <b>Mở trang hướng dẫn</b> Xtream')}
    <div class="spacer"></div>
    <button class="btn primary block" onclick="submitProfile(null)">Đăng nhập & đồng bộ</button><div class="spacer"></div>`,
  single: (p) => `
    <label class="field"><span class="label">Link stream <i>*</i></span>
      <span class="input">${ic('playCircle', 's20')}<input id="s-url" placeholder="https://…/index.m3u8" value="${esc(p.surl || '')}">
      <button class="paste-chip" onclick="pasteInto('s-url')">Dán</button></span>
      <span class="helper">Hỗ trợ HLS, DASH, MP4, RTSP, RTMP, UDP.</span></label>
    <label class="field"><span class="label">Tên (tùy chọn)</span><span class="input"><input placeholder="Sự kiện tối nay"></span></label>
    <div class="switch-row"><div class="txt">Lưu vào danh sách<small>Để mở lại sau ở tab Channels</small></div><button class="switch on"></button></div>
    ${guideStrip('single', 'Tìm link stream ở <b>trang gợi ý</b>')}
    <div class="spacer"></div>
    <button class="btn primary block" onclick="go('player',{single:1, url:document.getElementById('s-url').value})">${ic('play', 's20')}Phát ngay</button>`,
};

function pasteInto(id) {
  const v = { 'f-url': 'https://iptv-org.github.io/iptv/countries/vn.m3u', 's-url': 'https://example.org/live/concert/index.m3u8' }[id];
  document.getElementById(id).value = v; toast('Đã dán từ clipboard');
}
function submitUrl() {
  const v = document.getElementById('f-url').value.trim();
  if (!v) return setParams({ err: 'Nhập link playlist trước khi thêm.' });
  if (!/^(https?|rtmp|rtsp|udp):\/\//i.test(v) && !/\.(m3u8?|json)$/i.test(v)) return setParams({ url: v, err: 'Link phải bắt đầu bằng http(s):// hoặc kết thúc bằng .m3u / .m3u8.' });
  const lock = topParams().lock;
  const run = () => openOverlay('importProgress', { name: 'Playlist ' + (DATA.playlists.length + 1), fail: /fail|bad/i.test(v) });
  if (lock && !DATA.settings.passcode) openOverlay('passcode', { mode: 'create', then: run }); else run();
}

/* ---------------- Upload M3U ---------------- */
def('upload', {
  cls: 'UploadM3uActivity', layout: 'activity_upload_m3u', group: 'Thêm nguồn',
  desc: 'Chọn file qua SAF (ACTION_OPEN_DOCUMENT, lọc .m3u/.m3u8), tối đa 5 file. Mỗi file thành 1 playlist.',
  render: (p) => {
    const files = p.files || [];
    return `${toolbar({ title: 'Nhập file M3U', right: `<button class="icon-btn" onclick="go('howto',{type:'upload'})">${ic('help')}</button>` })}
      <div class="content pad">
        <button class="action-card" style="border:1.5px dashed var(--line);background:transparent;justify-content:center;flex-direction:column;padding:28px" onclick="setParams({files:[...(topParams().files||[]), ['vn_free.m3u','42 KB'],['news.m3u8','180 KB']].slice(0,5)})">
          <span class="ic-wrap">${ic('upload')}</span><b style="margin-top:8px">Chọn file</b><small class="muted">.m3u, .m3u8 · tối đa 5 file</small></button>
        <div class="stack" style="margin-top:14px">${files.map((f, i) => `<div class="pl-card"><div class="pl-ic">${ic('doc')}</div>
          <div class="main"><b>${f[0]}</b><small>${f[1]}</small></div><button class="icon-btn" onclick="const a=topParams().files; a.splice(${i},1); refresh()">${ic('close', 's20')}</button></div>`).join('')}</div>
        ${files.length ? `<label class="field" style="margin-top:16px"><span class="label">Tên playlist</span><span class="input"><input value="vn_free"></span></label>
          <div class="switch-row"><div class="txt">Khóa bằng passcode</div><button class="switch"></button></div>` : ''}
        <div class="spacer"></div>
        <button class="btn primary block" ${files.length ? '' : 'disabled'} onclick="openOverlay('importProgress',{name:'vn_free'})">Nhập ${files.length || ''} file</button>
      </div>`;
  },
});

/* ---------------- Import progress ---------------- */
def('importProgress', {
  kind: 'Dialog', cls: 'ImportProgressDialog', layout: 'dialog_import_progress', group: 'Thêm nguồn', cancelable: false,
  desc: 'Tải 1 lần, parse dạng stream, đếm kênh theo thời gian thực. Xong → kết quả (số kênh / nhóm). Lỗi → gợi ý mở hướng dẫn.',
  render: (p) => {
    if (p.fail) return `<div class="dialog"><div class="hero-ic" style="background:#3a1d1d;color:#ff7a7a">${ic('alert', 's32')}</div>
      <h3>Không đọc được playlist</h3><p>Link trả về lỗi 404 hoặc không phải định dạng M3U. Kiểm tra lại link, hoặc tìm link khác ở trang gợi ý.</p>
      <div class="actions"><button class="btn tonal" onclick="closeOverlay()">Sửa link</button><button class="btn primary" onclick="closeOverlay(); go('howto',{type:'url'})">Xem hướng dẫn</button></div></div>`;
    if (p.done) return `<div class="dialog"><div class="hero-ic" style="background:rgba(60,207,122,.14);color:var(--ok)">${ic('check', 's32')}</div>
      <h3>${p.update ? 'Đã cập nhật' : 'Đã thêm'} ${p.name}</h3>
      <div class="stat-row"><div><b>42</b><small>kênh</small></div><div><b>11</b><small>nhóm</small></div></div>
      <div class="actions"><button class="btn tonal" onclick="closeOverlay(); resetTo('main')">Về Home</button>
      <button class="btn primary" onclick="closeOverlay(); ${p.update ? '' : `DATA.playlists.push({id:9,name:'${p.name}',count:42,url:'',fav:false,locked:false,auto:true,updated:'Vừa xong'}); `}go('playlist',{id:1})">Xem kênh</button></div></div>`;
    return `<div class="dialog"><div class="import-progress">
      <div class="radar"><div class="core">${ic('channels', 's32')}</div></div>
      <div class="big-num" id="ipn">0</div><p class="muted" style="margin-top:0">kênh đã đọc · <span id="ipg">0</span> nhóm</p>
      <div class="bar" style="width:100%"><i id="ipb" style="width:0%"></i></div>
      <p class="hint">Đang tải ${p.name}…</p></div>
      <div class="actions"><button class="btn tonal" style="flex:1" onclick="clearInterval(window._ipt); closeOverlay(); toast('Đã hủy nhập playlist')">Hủy</button></div></div>`;
  },
  mount: (el, p) => {
    if (p.fail || p.done) return;
    let n = 0; clearInterval(window._ipt);
    window._ipt = setInterval(() => {
      n += 3; const q = (id) => el.querySelector(id);
      if (!q('#ipn')) return clearInterval(window._ipt);
      q('#ipn').textContent = Math.min(n, 42); q('#ipg').textContent = Math.min(Math.ceil(n / 4), 11);
      q('#ipb').style.width = Math.min(n / 42 * 100, 100) + '%';
      if (n >= 42) { clearInterval(window._ipt); p.done = true; setTimeout(refreshOverlay, 250); }
    }, 90);
  },
});

/* ---------------- Passcode ---------------- */
def('passcode', {
  kind: 'Dialog', cls: 'PasscodeDialog', layout: 'dialog_passcode', group: 'Playlist', cancelable: false, full: true,
  desc: '1 passcode chung cho app (lưu hash). Mode: create → confirm, hoặc enter. Thử: nhập 1234. Có tùy chọn vân tay.',
  render: (p) => {
    const mode = p.mode;
    const t = { create: ['Tạo passcode', 'Dùng để khóa playlist và profile riêng tư'], confirm: ['Nhập lại passcode', 'Nhập lại 4 số vừa tạo'], enter: ['Nhập passcode', `Mở khóa "${p.name || ''}"`] }[mode];
    const n = (p.v || '').length;
    return `<div style="display:flex;flex-direction:column;background:var(--bg)">
      <div class="toolbar"><button class="icon-btn" onclick="closeOverlay()">${ic('close')}</button></div>
      <div class="pin"><div class="hero-ic" style="width:64px;height:64px;border-radius:20px;background:var(--accent-soft);color:var(--accent);display:grid;place-items:center">${ic('lock', 's32')}</div>
        <h2>${t[0]}</h2><p class="${p.err ? 'err' : ''}">${p.err || t[1]}</p>
        <div class="dots ${p.err ? 'shake' : ''}">${[0, 1, 2, 3].map(i => `<i class="${i < n ? 'on' : ''}"></i>`).join('')}</div>
        <div class="keypad">${[1, 2, 3, 4, 5, 6, 7, 8, 9].map(k => `<button onclick="pinKey('${k}')">${k}</button>`).join('')}
          <button onclick="${mode === 'enter' ? "toast('Xác thực vân tay (BiometricPrompt)')" : ''}" aria-label="Vân tay" style="font-size:14px">${mode === 'enter' ? ic('user') : ''}</button>
          <button onclick="pinKey('0')">0</button><button onclick="pinKey('del')">${ic('back')}</button></div></div></div>`;
  },
});
function pinKey(k) {
  const o = state.overlays[state.overlays.length - 1]; const p = o.params;
  p.err = null; p.v = p.v || '';
  if (k === 'del') p.v = p.v.slice(0, -1); else if (p.v.length < 4) p.v += k;
  refreshOverlay();
  if (p.v.length < 4) return;
  setTimeout(() => {
    if (p.mode === 'create') { p.first = p.v; p.v = ''; p.mode = 'confirm'; return refreshOverlay(); }
    if (p.mode === 'confirm') {
      if (p.v !== p.first) { p.v = ''; p.err = 'Passcode không khớp. Nhập lại.'; return refreshOverlay(); }
      DATA.settings.passcode = p.v; closeOverlay(); toast('Đã tạo passcode'); return p.then && p.then();
    }
    if (p.v !== (DATA.settings.passcode || '1234')) { p.v = ''; p.err = 'Sai passcode. Thử lại.'; return refreshOverlay(); }
    closeOverlay(); p.then && p.then();
  }, 150);
}

/* ---------------- Edit playlist ---------------- */
def('editPlaylist', {
  kind: 'Dialog', cls: 'EditPlaylistDialog', layout: 'dialog_edit_playlist', group: 'Playlist',
  render: (p) => {
    const pl = DATA.playlists.find(x => x.id === p.id) || DATA.playlists[0];
    return `<div class="dialog" style="text-align:left"><h3 style="text-align:center">Sửa playlist</h3><div style="height:16px"></div>
      <label class="field"><span class="label">Tên</span><span class="input"><input value="${esc(pl.name)}"></span></label>
      <label class="field"><span class="label">URL</span><span class="input"><input value="${esc(pl.url)}" readonly>
        <button class="paste-chip" onclick="toast('Đã copy URL')">Copy</button></span></label>
      <div class="switch-row"><div class="txt">Khóa bằng passcode</div><button class="switch ${pl.locked ? 'on' : ''}" onclick="this.classList.toggle('on')"></button></div>
      <div class="switch-row"><div class="txt">Tự động cập nhật</div><button class="switch ${pl.auto ? 'on' : ''}" onclick="this.classList.toggle('on')"></button></div>
      <div class="actions"><button class="btn tonal" onclick="closeOverlay()">Hủy</button><button class="btn primary" onclick="closeOverlay(); toast('Đã lưu thay đổi')">Lưu</button></div></div>`;
  },
});
