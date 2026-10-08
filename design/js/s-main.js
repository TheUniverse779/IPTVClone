/* ===== Splash / Disclaimer / Main (4 tabs) / Add-source sheet ===== */

def('splash', {
  cls: 'SplashActivity', layout: 'activity_splash', group: 'Khởi động',
  desc: 'Tải Remote Config (tối đa 3–5 s) rồi điều hướng. Lần đầu: nối vào luồng Language/Onboarding bạn tự code → Disclaimer. Các lần sau: vào thẳng Main.',
  immersive: () => false,
  render: () => `<div class="splash">
      <div class="brandmark">${ic('playCircle')}</div>
      <div class="brandname">IPTV Player</div>
      <div class="foot"><div class="bar"><i style="width:0%" id="splbar"></i></div><span>Đang chuẩn bị…</span></div>
    </div>`,
  mount: (el) => {
    const b = el.querySelector('#splbar'); let w = 0;
    const t = setInterval(() => {
      w += 12; if (b) b.style.width = Math.min(w, 100) + '%';
      if (w >= 110) { clearInterval(t); if (state.stack[state.stack.length - 1].id !== 'splash') return;
        if (state.firstRun) startFirstRun(); else resetTo('main'); }
    }, 110);
  },
});

def('firstrun', {
  kind: 'Mock', cls: '[Language + Onboarding của bạn]', group: 'Khởi động',
  desc: 'Bản cũ khi luồng lần đầu còn là chỗ nối bạn tự code. Giờ thay bằng luồng thật ở nhóm "Khởi động & Quảng cáo".',
  render: () => `<div class="empty" style="flex:1;justify-content:center">
      <div class="art">${ic('lang', 's32')}</div>
      <b>Luồng Language + Onboarding</b>
      <p>Phần này bạn tự code. Prototype bỏ qua để xem luồng phía sau.</p>
      <div class="spacer"></div>
      <button class="btn primary block" onclick="replace('disclaimer')">Tiếp tục tới Disclaimer</button>
    </div>`,
});

def('disclaimer', {
  cls: 'DisclaimerActivity', layout: 'activity_disclaimer', group: 'Khởi động',
  desc: 'Chạy 1 lần. Nút Accept chỉ bật khi đã tích ô xác nhận.',
  render: (p) => `
    ${toolbar({ title: 'License agreement', backBtn: false })}
    <div class="content pad"><div class="legal">
      <p>IPTV Player là trình phát, hỗ trợ playlist M3U, M3U8 và tài khoản Xtream do bạn tự thêm. App không cung cấp kênh hay nội dung nào.</p>
      <p>Bạn chịu trách nhiệm về quyền sử dụng các playlist và nội dung mình thêm vào app.</p>
      <p>Các trang gợi ý trong phần hướng dẫn thuộc bên thứ ba. Chúng tôi không kiểm soát và không bảo đảm nội dung trên đó.</p>
      <p>Chúng tôi không liên kết với bất kỳ nhà cung cấp nội dung nào.</p>
      <p>Không sử dụng app để phát nội dung có bản quyền khi chưa được phép.</p>
      <p>Tiếp tục nghĩa là bạn đã đọc và đồng ý với các điều khoản trên.</p>
    </div><div class="spacer"></div></div>
    <div class="bottom-actions">
      <button class="check ${p.ok ? 'on' : ''}" onclick="setParams({ok:!topParams().ok})" style="text-align:left">
        <span class="box">${p.ok ? ic('check', 's16') : ''}</span>
        <span>Tôi đã đọc và đồng ý với <a>License agreement</a></span></button>
      <button class="btn primary block" ${p.ok ? '' : 'disabled'} onclick="state.firstRun=false; resetTo('main')">Accept</button>
    </div>`,
});

/* ---------------- MainActivity ---------------- */
const MAIN_TABS = [
  { id: 'home', label: 'Home', icon: 'home', frag: 'HomeFragment', layout: 'fragment_home' },
  { id: 'playlists', label: 'Channels', icon: 'list', frag: 'PlaylistsFragment', layout: 'fragment_playlists' },
  { id: 'xtream', label: 'Xtream', icon: 'xtream', frag: 'XtreamProfilesFragment', layout: 'fragment_xtream_profiles' },
  { id: 'sport', label: 'Sport', icon: 'sport', frag: 'SportFragment', layout: 'fragment_sport' },
];

def('main', {
  cls: 'MainActivity', layout: 'activity_main', group: 'Màn chính',
  desc: '4 tab là Fragment (show/hide qua FragmentManager). Nút + nổi mở AddSourceBottomSheet. Tab thứ 4 là Sport hoặc Settings tùy RC show_sport.',
  fragment: () => {
    const t = MAIN_TABS.find(x => x.id === state.tab);
    if (state.tab === 'sport' && !state.sportOn) return { kind: 'Fragment', cls: 'SettingsFragment', layout: 'fragment_settings' };
    return { kind: 'Fragment', cls: t.frag, layout: t.layout, desc: TAB_DESC[t.id] };
  },
  render: () => {
    const tabs = MAIN_TABS.map(t => (t.id === 'sport' && !state.sportOn) ? { ...t, label: 'Settings', icon: 'settings' } : t);
    const tabBtn = (t) => `<button class="tab ${state.tab === t.id ? 'on' : ''}" onclick="state.tab='${t.id}'; refresh()">${ic(t.icon)}<span>${t.label}</span></button>`;
    return `<div class="content" id="tabc">${TAB_RENDER[state.tab]()}</div>
      ${state.tab === 'home' ? `<button class="bot-fab" onclick="go('chatbot')" aria-label="Chatbot">${ic('chat')}</button>` : ''}
      <nav class="bottombar">${tabBtn(tabs[0])}${tabBtn(tabs[1])}<span></span>${tabBtn(tabs[2])}${tabBtn(tabs[3])}
        <button class="fab" onclick="openOverlay('addSource')" aria-label="Thêm nguồn">${ic('plus')}</button></nav>
      ${adBannerStrip()}`;
  },
  mount: (el) => { const f = TAB_MOUNT[state.tab]; if (f) f(el); },
});

const TAB_DESC = {
  home: 'Đang phát gần đây, playlist của bạn, Favourite / Recent. Chưa có nguồn thì hiện hướng dẫn thêm nguồn.',
  playlists: 'Tất cả nguồn đã thêm: playlist M3U, file, single stream. Bấm 1 playlist → PlaylistDetailActivity.',
  xtream: '"Who\'s watching?": chạm profile → (passcode nếu khóa) → (cảnh báo nếu hết hạn) → XtreamHomeActivity. Nhấn giữ / ⋮ → ProfileActionsSheet. Profile vừa thêm có nhãn "Mới".',
  sport: 'Trận đang đá, sắp đá, trận của tôi. Dữ liệu từ ESPN, giải lấy từ RC sport_data.',
};

function homeHeader() {
  return `<div class="home-head"><div class="brand">IPTV <span>Player</span></div>
    <button class="icon-btn boxed" onclick="go('search')" aria-label="Tìm kiếm">${ic('search')}</button>
    <button class="icon-btn boxed" onclick="go('howto',{type:'url'})" aria-label="Hướng dẫn">${ic('help')}</button>
    <button class="icon-btn boxed" onclick="go('settings')" aria-label="Cài đặt">${ic('settings')}</button></div>`;
}

const TAB_RENDER = {
  home: () => {
    if (!DATA.playlists.length && !DATA.profiles.length) return homeHeader() + homeEmpty();
    const recent = DATA.recentIds.map(id => DATA.channels.find(c => c.id === id)).filter(Boolean);
    const last = recent[0];
    const favs = DATA.channels.filter(c => c.fav);
    const chip = state.homeChip || 'recent';
    const listFor = chip === 'fav' ? favs : recent;
    const onair = DATA.playlists.length && last ? `
      <section class="onair" aria-label="Xem tiếp">
        <div class="screenlet" style="--pv:linear-gradient(135deg, ${logoColor(last.name)}, #0c0f18)">
          <span class="live">LIVE</span>
          <div class="meta"><b>${last.name}</b><small>Xem lần cuối · ${plName()}</small></div>
          <button class="playbtn" onclick="go('player',{ch:${last.id}})" aria-label="Phát">${ic('play')}</button>
        </div>
        <div class="strip">${recent.slice(1).map(c => `<button onclick="go('player',{ch:${c.id}})">${chLogo(c.name)}${c.name}</button>`).join('')}</div>
      </section>` : '';
    const xtRow = DATA.profiles.length ? `<div class="sec-h"><h3>Xtream</h3><button onclick="state.tab='xtream'; refresh()">Quản lý</button></div>
      <div class="hscroll">${DATA.profiles.map(pr => `<button class="xt-chip" onclick="openProfile('${pr.id}')">${avatar(pr, 36, 10)}<span><b>${esc(pr.name)}</b>${profileSub(pr)}</span>${pr.locked ? ic('lock', 's16') : ''}</button>`).join('')}</div>` : '';
    return homeHeader() + onair + `
      ${DATA.playlists.length ? `<div class="sec-h"><h3>Playlist của bạn</h3><button onclick="state.tab='playlists'; refresh()">Xem tất cả</button></div>
      <div class="stack pad">${DATA.playlists.map(plCard).join('')}</div>` : ''}
      ${xtRow}
      <div class="sec-h"><h3>Kênh</h3></div>
      <div class="chips"><button class="chip ${chip === 'recent' ? 'on' : ''}" onclick="state.homeChip='recent';refresh()">${ic('history', 's16')}Recent</button>
        <button class="chip ${chip === 'fav' ? 'on' : ''}" onclick="state.homeChip='fav';refresh()">${ic('heart', 's16')}Favourite</button></div>
      <div class="grid" style="margin-top:12px">${listFor.map(chTile).join('') || `<p class="hint" style="grid-column:1/-1">Chưa có kênh yêu thích. Bấm ♡ ở kênh bất kỳ để thêm.</p>`}</div>
      <div style="height:96px"></div>`;
  },
  playlists: () => `
    ${toolbar({ title: 'Nguồn của bạn', backBtn: false, right: `<button class="icon-btn boxed" onclick="go('search')">${ic('search')}</button>` })}
    <div class="stack pad">${DATA.playlists.map(plCard).join('')}</div>
    <div class="sec-h"><h3>Single stream</h3></div>
    <div class="list-card">${DATA.singles.map(s => `<div class="row" onclick="go('player',{single:${s.id}})">
      <div class="logo" style="background:var(--surface-3);color:var(--accent)">${ic('link', 's20')}</div>
      <div class="main"><b>${s.name}</b><small>${s.url}</small></div><div class="trail"><button class="icon-btn">${ic('more')}</button></div></div>`).join('')}</div>
    <button class="guide-link" onclick="openOverlay('addSource')">${ic('plus')}<span><b>Thêm nguồn mới</b>: playlist URL, file M3U, Xtream hoặc 1 link stream</span></button>
    <button class="guide-link" onclick="go('community',{tab:'iptv'})">${ic('share')}<span><b>Community</b>: link người dùng khác chia sẻ</span>${ic('chevron', 's20')}</button>
    <div style="height:96px"></div>`,
  xtream: () => xtreamProfilesTab(),
  sport: () => state.sportOn ? sportTab() : settingsBody(false),
};
const TAB_MOUNT = {};

function homeEmpty() {
  return `<div class="welcome"><h2>Thêm nguồn để bắt đầu xem</h2>
      <p>App là trình phát, không có sẵn kênh. Hãy thêm playlist M3U, tài khoản Xtream hoặc một link stream của bạn.</p>
      <button class="import-hero" onclick="go('import',{tab:'url'})"><span class="ic-wrap">${ic('link')}</span>
        <span><b>Nhập playlist URL</b><small>Dán link .m3u / .m3u8</small></span></button></div>
    <div class="stack pad" style="margin-top:10px">
      ${actionCard('xtream', 'Nhập tài khoản Xtream', 'Server, username, password', "go('import',{tab:'xtream'})")}
      ${actionCard('playCircle', 'Phát 1 link stream', 'Xem ngay sự kiện, kênh lẻ', "go('import',{tab:'single'})")}
    </div>
    <button class="guide-link" onclick="go('howto',{type:'url'})">${ic('help')}<span>Chưa có link? <b>Xem cách tìm playlist</b> và gợi ý trang để lấy link</span>${ic('chevron', 's20')}</button>
    <div style="height:96px"></div>`;
}
function actionCard(icon, title, sub, onclick) {
  return `<button class="action-card" onclick="${onclick}"><span class="ic-wrap">${ic(icon)}</span>
    <span class="main"><b>${title}</b><small>${sub}</small></span>${ic('chevron', 's20')}</button>`;
}
function plCard(pl) {
  const open = pl.locked ? `openOverlay('passcode',{mode:'enter',name:'${esc(pl.name)}',then:()=>go('playlist',{id:${pl.id}})})` : `go('playlist',{id:${pl.id}})`;
  return `<div class="pl-card" onclick="${open}">
    <div class="pl-ic">${ic('list')}${pl.locked ? `<span class="lk">${ic('lock', 's16')}</span>` : ''}</div>
    <div class="main"><b>${pl.name}</b><small><em>${pl.count}</em> kênh · ${pl.updated}</small></div>
    <button class="icon-btn fav ${pl.fav ? 'on' : ''}" onclick="event.stopPropagation(); DATA.playlists.find(x=>x.id===${pl.id}).fav^=1; refresh()">${ic(pl.fav ? 'heartFill' : 'heart')}</button>
    <button class="icon-btn" onclick="event.stopPropagation(); openPlMenu(this, ${pl.id})">${ic('more')}</button></div>`;
}
function chTile(c) {
  return `<div class="tile" onclick="go('player',{ch:${c.id}})">${chLogo(c.name)}<div class="name">${c.name}</div>
    <button class="icon-btn tr fav ${c.fav ? 'on' : ''}" style="width:30px;height:30px" onclick="event.stopPropagation(); toggleFav(${c.id})">${ic(c.fav ? 'heartFill' : 'heart', 's16')}</button></div>`;
}
function toggleFav(id) { const c = DATA.channels.find(x => x.id === id); c.fav = !c.fav; toast(c.fav ? 'Đã thêm vào Favourite' : 'Đã bỏ khỏi Favourite'); refresh(); }

function openPlMenu(btn, id) {
  const r = btn.getBoundingClientRect(), v = vp().getBoundingClientRect();
  openOverlay('plMenu', { id, top: r.bottom - v.top, right: v.right - r.right });
}
def('plMenu', {
  kind: 'Popup', cls: 'PopupMenu (playlist)', group: 'Playlist',
  desc: 'Menu ⋮ của playlist. App gốc chỉ có Edit / Delete; bản clone thêm Update now, Copy URL.',
  render: (p) => `<div class="popup" style="top:${p.top}px; right:${p.right}px">
      <button onclick="closeOverlay(); openOverlay('editPlaylist',{id:${p.id}})">${ic('edit', 's20')}Sửa</button>
      <button onclick="closeOverlay(); openOverlay('importProgress',{name:'${esc(DATA.playlists.find(x => x.id === p.id).name)}', update:true})">${ic('refresh', 's20')}Cập nhật ngay</button>
      <button onclick="closeOverlay(); toast('Đã copy URL')">${ic('copy', 's20')}Copy URL</button>
      <button onclick="closeOverlay(); openOverlay('shareDialog',{type:'iptv'})">${ic('share', 's20')}Chia sẻ lên Community</button>
      <button class="danger" onclick="closeOverlay(); openOverlay('confirm',{title:'Xóa playlist?', msg:'Playlist và toàn bộ kênh, favourite trong đó sẽ bị xóa khỏi máy.', ok:'Xóa', danger:true, then:()=>{DATA.playlists=DATA.playlists.filter(x=>x.id!==${p.id}); refresh(); toast('Đã xóa playlist');}})">${ic('trash', 's20')}Xóa</button></div>`,
});

def('confirm', {
  kind: 'Dialog', cls: 'ConfirmDialog', group: 'Dùng chung', layout: 'dialog_confirm',
  render: (p) => `<div class="dialog"><h3>${p.title}</h3><p>${p.msg}</p>
    <div class="actions"><button class="btn tonal" onclick="closeOverlay()">Hủy</button>
    <button class="btn ${p.danger ? 'danger' : 'primary'}" id="okb">${p.ok || 'OK'}</button></div></div>`,
  mount: (el, p) => { el.querySelector('#okb').onclick = () => { closeOverlay(); p.then && p.then(); }; },
});

/* ---------------- Add source sheet ---------------- */
def('addSource', {
  kind: 'BottomSheet', cls: 'AddSourceBottomSheet', layout: 'sheet_add_source', group: 'Thêm nguồn',
  desc: '5 cách thêm nguồn như app gốc. Icon ? mở hướng dẫn. Không có slot quảng cáo.',
  render: () => `<div class="sheet"><div class="grab"></div>
    <div class="sheet-h"><button class="text-btn" onclick="closeOverlay()">Đóng</button><h3>Thêm nguồn</h3>
      <button class="icon-btn" onclick="closeOverlay(); go('howto',{type:'url'})">${ic('help')}</button></div>
    <div class="sheet-body"><div class="src-list">
      ${actionCard('link', 'Playlist URL', 'Dán link .m3u / .m3u8', "closeOverlay(); go('import',{tab:'url'})")}
      ${actionCard('xtream', 'Xtream Codes', 'Server URL, username, password', "closeOverlay(); go('import',{tab:'xtream'})")}
      ${actionCard('playCircle', 'Single stream', 'Phát ngay 1 link', "closeOverlay(); go('import',{tab:'single'})")}
      ${actionCard('upload', 'File M3U', 'Chọn tối đa 5 file trong máy', "closeOverlay(); go('upload')")}
      ${actionCard('device', 'Video trong máy', 'Mở trình chọn video của hệ thống', "closeOverlay(); toast('Mở Photo Picker (video)')")}
    </div></div></div>`,
});
