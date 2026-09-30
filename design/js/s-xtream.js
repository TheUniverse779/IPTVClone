/* ===== Xtream: profiles tab, add/sync, profile actions, home (4 tabs), switcher, account, detail ===== */

const PROFILE_COLORS = ['#5b5bd6', '#1f8a6b', '#c7861b', '#157a9a', '#a1336b', '#b83a2b', '#4a6b1f'];
function getProfile(id) { return DATA.profiles.find(x => x.id === id); }
function hasVod(pr) { return pr.counts.vod + pr.counts.series > 0; }
function fmtN(n) { return n ? n.toLocaleString('vi-VN') : '—'; }
function hostOf(url) { return String(url || '').replace(/^https?:\/\//, '').replace(/\/.*$/, ''); }
function avatar(pr, size = 40, radius = 12) {
  return `<span class="av-sm" style="width:${size}px;height:${size}px;border-radius:${radius}px;background:${pr.color};font-size:${Math.round(size * 0.46)}px">${esc(pr.name[0].toUpperCase())}</span>`;
}
function statusChip(pr) { return pr.status === 'Expired' ? `<span class="status-chip bad">Hết hạn</span>` : `<span class="status-chip ok">Active</span>`; }
function profileSub(pr) {
  if (pr.status === 'Expired') return `<small class="err">Hết hạn ${pr.exp}</small>`;
  if (!hasVod(pr)) return `<small>Chỉ có kênh Live</small>`;
  return `<small>Hạn dùng ${pr.exp}</small>`;
}

/* ---------------- XtreamProfilesFragment (tab 3 of MainActivity) ---------------- */
function xtreamProfilesTab() {
  const help = `<button class="icon-btn boxed" onclick="go('howto',{type:'xtream'})" aria-label="Hướng dẫn">${ic('help')}</button>`;
  if (!DATA.profiles.length) return `${toolbar({ title: 'Xtream', backBtn: false, right: help })}
    <div class="empty" style="padding-top:40px"><div class="art">${ic('xtream', 's32')}</div><b>Chưa có tài khoản Xtream</b>
      <p>Thêm Server URL, username và password từ nhà cung cấp để xem Live, phim và series.</p>
      <button class="btn primary" onclick="go('profileEdit',{})">${ic('plus', 's20')}Thêm profile</button></div>
    <button class="guide-link" onclick="go('howto',{type:'xtream'})">${ic('help')}<span>Chưa có tài khoản? <b>Xem hướng dẫn Xtream</b></span>${ic('chevron', 's20')}</button>
    <div style="height:96px"></div>`;
  return `${toolbar({ title: '', backBtn: false, right: help })}
    <div class="who"><h2>Who's watching?</h2><p>Chọn tài khoản để xem Live, phim và series.</p></div>
    <div class="profiles">${DATA.profiles.map(profileTile).join('')}
      <div class="profile add"><button class="av" onclick="go('profileEdit',{})" aria-label="Thêm profile">${ic('plus', 's32')}</button><div class="pname"><b>Thêm profile</b></div></div></div>
    <p class="hint" style="text-align:center;margin:22px 32px 0">Nhấn giữ hoặc bấm ⋮ để đồng bộ lại, xem thông tin tài khoản, khóa hoặc xóa profile.</p>
    <div style="height:96px"></div>`;
}
function profileTile(pr) {
  const expired = pr.status === 'Expired', isNew = state.newProfile === pr.id;
  return `<div class="profile ${isNew ? 'is-new' : ''}">
    <button class="av ${expired ? 'dim' : ''}" style="background:${pr.color}" onclick="openProfile('${pr.id}')"
      oncontextmenu="event.preventDefault(); openOverlay('profileActions',{id:'${pr.id}'})">${esc(pr.name[0].toUpperCase())}
      ${pr.locked ? `<span class="lk">${ic('lock', 's16')}</span>` : ''}
      ${isNew ? `<span class="tag new">Mới</span>` : expired ? `<span class="tag exp">Hết hạn</span>` : ''}</button>
    <div class="pname"><b>${esc(pr.name)}</b><button class="icon-btn" onclick="openOverlay('profileActions',{id:'${pr.id}'})" aria-label="Tùy chọn profile">${ic('more', 's20')}</button></div>
    ${profileSub(pr)}</div>`;
}

/* Tap a profile: passcode (if locked) → expiry warning (if expired) → XtreamHomeActivity */
function openProfile(id, how = 'go') {
  const pr = getProfile(id);
  const open = () => {
    state.lastProfile = id;
    if (state.newProfile === id) state.newProfile = null;
    state.xtTab = hasVod(pr) ? 'movie' : 'live';
    (how === 'replace' ? replace : go)('xtreamHome', { p: id });
  };
  const checkExpiry = () => pr.status === 'Expired' ? openOverlay('expired', { id, then: open }) : open();
  if (pr.locked) openOverlay('passcode', { mode: 'enter', name: pr.name, then: checkExpiry }); else checkExpiry();
}
function toggleProfileLock(id) {
  const pr = getProfile(id);
  const set = (v) => { pr.locked = v; refresh(); toast(v ? 'Đã khóa profile' : 'Đã bỏ khóa profile'); };
  if (pr.locked) openOverlay('passcode', { mode: 'enter', name: pr.name, then: () => set(false) });
  else if (!DATA.settings.passcode) openOverlay('passcode', { mode: 'create', then: () => set(true) });
  else set(true);
}
function confirmDeleteProfile(id, after) {
  const pr = getProfile(id);
  openOverlay('confirm', {
    title: `Xóa "${esc(pr.name)}"?`, ok: 'Xóa', danger: true,
    msg: 'Kênh, phim, series đã đồng bộ và lịch sử xem của profile này sẽ bị xóa khỏi máy. Tài khoản ở nhà cung cấp không bị ảnh hưởng.',
    then: () => { DATA.profiles = DATA.profiles.filter(x => x.id !== id); toast('Đã xóa profile'); if (after) after(); else refresh(); },
  });
}

def('profileActions', {
  kind: 'BottomSheet', cls: 'ProfileActionsSheet', layout: 'sheet_profile_actions', group: 'Xtream',
  desc: 'Mở khi nhấn giữ avatar hoặc bấm ⋮ cạnh tên. Gom mọi thao tác trên 1 profile.',
  render: (p) => {
    const pr = getProfile(p.id); if (!pr) return '';
    return `<div class="sheet"><div class="grab"></div>
      <div class="pf-head">${avatar(pr, 48, 14)}<div class="main"><b>${esc(pr.name)}</b><small>${esc(hostOf(pr.server))} · ${esc(pr.user)}</small></div>${statusChip(pr)}</div>
      <div class="sheet-body opt-list">
        <button onclick="closeOverlay(); openProfile('${pr.id}')">${ic('play', 's20')}<span class="grow">Xem</span></button>
        <button onclick="closeOverlay(); openOverlay('xtreamSync',{id:'${pr.id}'})">${ic('refresh', 's20')}<span class="grow">Đồng bộ lại<small>Lần cuối: ${pr.synced}</small></span></button>
        <button onclick="closeOverlay(); openOverlay('xtAccount',{id:'${pr.id}'})">${ic('info', 's20')}<span class="grow">Thông tin tài khoản<small>Hạn dùng, số kết nối, số kênh</small></span></button>
        <button onclick="closeOverlay(); go('profileEdit',{id:'${pr.id}'})">${ic('edit', 's20')}<span class="grow">Sửa profile</span></button>
        <button onclick="toggleProfileLock('${pr.id}')">${ic('lock', 's20')}<span class="grow">Khóa bằng passcode</span><span class="switch ${pr.locked ? 'on' : ''}"></span></button>
        <button class="danger-row" onclick="closeOverlay(); confirmDeleteProfile('${pr.id}')">${ic('trash', 's20')}<span class="grow">Xóa profile</span></button>
      </div></div>`;
  },
});

def('expired', {
  kind: 'Dialog', cls: 'XtreamExpiredDialog', layout: 'dialog_xtream_expired', group: 'Xtream',
  desc: 'Khi chạm profile có user_info.status = Expired (hoặc exp_date đã qua). Vẫn cho mở dữ liệu đã đồng bộ.',
  render: (p) => {
    const pr = getProfile(p.id);
    return `<div class="dialog"><div class="hero-ic" style="background:#3a1d1d;color:#ff7a7a">${ic('calendar', 's32')}</div>
      <h3>Tài khoản đã hết hạn</h3>
      <p>"${esc(pr.name)}" hết hạn ngày ${pr.exp}. Bạn vẫn xem được danh sách đã đồng bộ, nhưng kênh và phim sẽ không phát cho tới khi nhà cung cấp gia hạn.</p>
      <div class="stack" style="margin-top:20px"><button class="btn primary block" id="ex-sync">${ic('refresh', 's20')}Kiểm tra lại</button>
        <button class="btn tonal block" id="ex-edit">Sửa thông tin đăng nhập</button>
        <button class="btn block" style="color:var(--text-2)" id="ex-open">Vẫn mở</button></div></div>`;
  },
  mount: (el, p) => {
    el.querySelector('#ex-edit').onclick = () => { closeOverlay(); go('profileEdit', { id: p.id }); };
    el.querySelector('#ex-sync').onclick = () => { closeOverlay(); openOverlay('xtreamSync', { id: p.id, fail: 'expired' }); };
    el.querySelector('#ex-open').onclick = () => { closeOverlay(); p.then && p.then(); };
  },
});

/* ---------------- Add / edit profile ---------------- */
function xtreamFormFields(p, pr = {}) {
  const val = (k, d) => esc(p[k] ?? d ?? '');
  return `
    <label class="field"><span class="label">Tên profile</span><span class="input"><input id="pe-name" value="${val('name', pr.name)}" placeholder="My Xtream ${DATA.profiles.length + 1}"></span></label>
    <label class="field ${p.err ? 'error' : ''}"><span class="label">Server URL <i>*</i></span>
      <span class="input">${ic('globe', 's20')}<input id="pe-srv" value="${val('srv', pr.server)}" placeholder="http://host:port">
      <button class="paste-chip" onclick="smartPasteXtream()">Dán</button></span>
      <span class="helper">${p.err || 'Dán cả link get.php?username=…&password=… — app tự tách 3 ô.'}</span></label>
    <label class="field"><span class="label">Username <i>*</i></span><span class="input">${ic('user', 's20')}<input id="pe-user" value="${val('user', pr.user)}"></span></label>
    <label class="field"><span class="label">Password <i>*</i></span><span class="input">${ic('lock', 's20')}<input id="pe-pass" type="password" value="${val('pass', pr.id ? 'secret12' : '')}">${ic('eye', 's20')}</span></label>
    <div class="switch-row"><div class="txt">Khóa profile bằng passcode<small>Phải nhập passcode khi mở profile</small></div>
      <button id="pe-lock" class="switch ${(p.lock ?? pr.locked) ? 'on' : ''}" onclick="this.classList.toggle('on')"></button></div>`;
}
function formVal(id) { return ((document.getElementById(id) || {}).value || '').trim(); }
function smartPasteXtream() {
  setParams({ name: formVal('pe-name'), srv: 'http://line.example.tv:8080', user: 'demo_user', pass: 'demo_pass', err: null,
    lock: document.getElementById('pe-lock').classList.contains('on') });
  toast('Đã tách Server / Username / Password từ link get.php');
}
function submitProfile(id) {
  const name = formVal('pe-name'), srv = formVal('pe-srv'), user = formVal('pe-user'), pass = formVal('pe-pass');
  const lock = document.getElementById('pe-lock').classList.contains('on');
  if (!srv || !user || !pass) return setParams({ name, srv, user, pass, lock, err: 'Nhập đủ Server URL, username và password.' });
  if (id) Object.assign(getProfile(id), { name: name || getProfile(id).name, server: srv, user, locked: lock });
  const run = () => openOverlay('xtreamSync', id ? { id } : { isNew: true, name, server: srv, user, lock });
  if (lock && !DATA.settings.passcode) openOverlay('passcode', { mode: 'create', then: run }); else run();
}

def('profileEdit', {
  cls: 'AddEditProfileActivity', layout: 'activity_add_edit_profile', group: 'Xtream',
  desc: 'Thêm/sửa profile. Lưu → XtreamSyncDialog (đăng nhập player_api.php rồi đồng bộ). Thử lỗi: Server chứa "bad" → không tìm thấy host; username chứa "wrong" → sai mật khẩu; Server chứa "live" → tài khoản chỉ có Live.',
  render: (p) => {
    const pr = getProfile(p.id);
    return `${toolbar({ title: pr ? 'Sửa profile' : 'Thêm profile', right: `<button class="icon-btn" onclick="go('howto',{type:'xtream'})">${ic('help')}</button>` })}
      <div class="content pad">
        <div style="display:flex;justify-content:center;margin:4px 0 20px"><div class="profile"><div class="av" style="background:${pr ? pr.color : PROFILE_COLORS[DATA.profiles.length % PROFILE_COLORS.length]};width:96px;height:96px">${pr ? esc(pr.name[0]) : ic('user', 's32')}
          <span class="ed" onclick="toast('Chọn màu / ảnh đại diện')">${ic('edit', 's16')}</span></div></div></div>
        ${xtreamFormFields(p, pr || {})}
        ${pr ? `<button class="card" style="margin-top:8px;display:flex;align-items:center;gap:12px;width:100%;text-align:left" onclick="openOverlay('xtAccount',{id:'${pr.id}'})">
          ${ic('info')}<span style="flex:1"><b>Thông tin tài khoản</b><span class="hint" style="display:block">${pr.status === 'Expired' ? 'Đã hết hạn' : 'Hạn dùng ' + pr.exp} · Đồng bộ ${pr.synced}</span></span>${statusChip(pr)}</button>`
          : `<button class="guide-link" style="margin:12px 0 0;width:100%" onclick="go('community',{tab:'xtream'})">${ic('share')}<span>Hoặc <b>chọn 1 tài khoản</b> người dùng khác chia sẻ trên Community</span>${ic('chevron', 's20')}</button>`}
        <div class="spacer"></div>
        <button class="btn primary block" onclick="submitProfile(${pr ? `'${pr.id}'` : 'null'})">${pr ? 'Lưu & đồng bộ lại' : 'Đăng nhập & đồng bộ'}</button>
        ${pr ? `<button class="btn block" style="color:#ff7a7a;margin-top:8px" onclick="confirmDeleteProfile('${pr.id}', ()=>{ state.tab='xtream'; resetTo('main'); })">Xóa profile</button>` : ''}
        <div class="spacer"></div></div>`;
  },
});

/* ---------------- Sync (login + download) ---------------- */
const SYNC_FAILS = {
  host: ['Không kết nối được server', (p) => `Không tìm thấy máy chủ <b>${esc(hostOf(p.server))}</b>. Kiểm tra lại Server URL, nhớ ghi cả cổng (vd. :8080).`],
  auth: ['Sai username hoặc password', () => 'Server từ chối đăng nhập. Kiểm tra lại thông tin nhà cung cấp gửi, chú ý chữ hoa và chữ thường.'],
  expired: ['Tài khoản vẫn hết hạn', () => 'Server báo tài khoản đã hết hạn. Liên hệ nhà cung cấp để gia hạn rồi thử lại.'],
};
def('xtreamSync', {
  kind: 'Dialog', cls: 'XtreamSyncDialog', layout: 'dialog_xtream_sync', group: 'Xtream', cancelable: false,
  desc: 'Đăng nhập (user_info) → tải song song Live / Phim / Series. Thành công nếu có ít nhất 1 loại (app gốc bắt buộc đủ 3). Profile mới → dialog "Đã thêm", rồi hiện trong Who\'s watching với nhãn Mới.',
  render: (p) => {
    const pr = p.id ? getProfile(p.id) : null;
    if (p.fail && p.failShown) {
      const f = SYNC_FAILS[p.fail];
      return `<div class="dialog"><div class="hero-ic" style="background:#3a1d1d;color:#ff7a7a">${ic(p.fail === 'host' ? 'wifiOff' : p.fail === 'auth' ? 'lock' : 'calendar', 's32')}</div>
        <h3>${f[0]}</h3><p>${f[1](p.isNew ? p : pr)}</p>
        <div class="actions">${p.fail === 'expired' ? `<button class="btn primary" onclick="closeOverlay()">Đóng</button>`
          : `<button class="btn tonal" onclick="closeOverlay(); go('howto',{type:'xtream'})">Hướng dẫn</button><button class="btn primary" onclick="closeOverlay()">Sửa lại</button>`}</div></div>`;
    }
    if (p.done && p.isNew) {
      const np = getProfile(p.id);
      return `<div class="dialog"><div class="added-av">${avatar(np, 72, 22)}<span class="ok-dot">${ic('check', 's16')}</span></div>
        <h3>Đã thêm "${esc(np.name)}"</h3><p>Profile đã có trong Xtream › Who's watching.</p>
        <div class="stat-row"><div><b>${fmtN(np.counts.live)}</b><small>kênh live</small></div><div><b>${fmtN(np.counts.vod)}</b><small>phim</small></div><div><b>${fmtN(np.counts.series)}</b><small>series</small></div></div>
        ${hasVod(np) ? '' : '<p class="hint" style="margin-top:10px">Tài khoản này chỉ có kênh Live.</p>'}
        <div class="actions"><button class="btn tonal" onclick="state.tab='xtream'; resetTo('main'); toast('Đã thêm profile ${esc(np.name)}')">Về danh sách</button>
          <button class="btn primary" onclick="state.tab='xtream'; resetTo('main'); openProfile('${np.id}')">Xem ngay</button></div></div>`;
    }
    if (p.done) return `<div class="dialog"><div class="hero-ic" style="background:rgba(60,207,122,.14);color:var(--ok)">${ic('check', 's32')}</div><h3>Đồng bộ xong</h3>
      <div class="stat-row"><div><b>${fmtN(pr.counts.live)}</b><small>kênh live</small></div><div><b>${fmtN(pr.counts.vod)}</b><small>phim</small></div><div><b>${fmtN(pr.counts.series)}</b><small>series</small></div></div>
      <div class="actions"><button class="btn primary" onclick="closeOverlay(); refresh()">Đóng</button></div></div>`;
    const counts = p.counts;
    const rows = [['Đăng nhập', 'auth'], ['Kênh live', 'live'], ['Phim', 'vod'], ['Series', 'series']];
    const st = p.st || {};
    const right = (k) => st[k] !== 2 ? `<span class="hint">${st[k] === 1 ? 'Đang tải…' : 'Chờ'}</span>`
      : k === 'auth' ? `<span class="hint">OK</span>` : counts[k] ? `<span class="h-cond" style="font-size:16px">${fmtN(counts[k])}</span>` : `<span class="hint">Không có</span>`;
    return `<div class="dialog" style="text-align:left"><h3 style="text-align:center">${p.isNew ? 'Đang thêm profile' : 'Đang đồng bộ'}</h3>
      <p style="text-align:center;margin-top:4px">${esc(hostOf(p.isNew ? p.server : pr.server))}</p><div style="height:14px"></div>
      <div class="stack">${rows.map(([l, k]) => `<div style="display:flex;align-items:center;gap:12px;min-height:28px">
        <span style="width:24px;display:grid;place-items:center;color:${st[k] === 2 ? 'var(--ok)' : 'var(--text-3)'}">${st[k] === 2 ? ic('check', 's20') : st[k] === 1 ? '<div class="spinner" style="width:18px;height:18px;border-width:2px"></div>' : '•'}</span>
        <span style="flex:1">${l}</span>${right(k)}</div>`).join('')}</div>
      <div class="actions"><button class="btn tonal" style="flex:1" onclick="clearInterval(window._xs); closeOverlay()">Hủy</button></div></div>`;
  },
  mount: (el, p) => {
    if (p.done || p.failShown || p._running) return;
    p._running = true;
    const pr = p.id ? getProfile(p.id) : null;
    if (p.isNew) {
      if (/bad/i.test(p.server)) p.fail = 'host';
      else if (/wrong/i.test(p.user)) p.fail = 'auth';
      p.counts = /live/i.test(p.server) ? { live: 212, vod: 0, series: 0 } : { live: 1284, vod: 3910, series: 612 };
    } else p.counts = pr.counts;
    p.st = {}; const order = ['auth', 'live', 'vod', 'series']; let i = 0;
    clearInterval(window._xs);
    window._xs = setInterval(() => {
      if (i === 1 && p.fail) { clearInterval(window._xs); p.failShown = true; return refreshOverlay(); }
      if (i > 0) p.st[order[i - 1]] = 2;
      if (i < order.length) p.st[order[i]] = 1;
      i++;
      if (i > order.length) {
        clearInterval(window._xs); p.done = true;
        if (p.isNew) {
          const id = 'p' + (Date.now() % 100000);
          DATA.profiles.push({ id, name: p.name || `My Xtream ${DATA.profiles.length + 1}`, server: p.server, user: p.user, exp: '15/09/2027', status: 'Active', conn: '0 / 1',
            locked: !!p.lock, color: PROFILE_COLORS[DATA.profiles.length % PROFILE_COLORS.length], counts: p.counts, synced: 'Vừa xong' });
          p.id = id; state.newProfile = id;
        } else pr.synced = 'Vừa xong';
      }
      refreshOverlay();
    }, 420);
  },
});

/* ---------------- Account info / switcher ---------------- */
def('xtAccount', {
  kind: 'BottomSheet', cls: 'XtreamAccountSheet', layout: 'sheet_xtream_account', group: 'Xtream',
  desc: 'Thông tin từ player_api.php (user_info + server_info) và lần đồng bộ cuối. App gốc không có màn này.',
  render: (p) => {
    const pr = getProfile(p.id); if (!pr) return '';
    return `<div class="sheet"><div class="grab"></div>
      <div class="pf-head">${avatar(pr, 48, 14)}<div class="main"><b>${esc(pr.name)}</b><small>Thông tin tài khoản</small></div>${statusChip(pr)}</div>
      <div class="sheet-body">
        <dl class="kv"><dt>Hạn dùng</dt><dd class="${pr.status === 'Expired' ? 'err' : ''}">${pr.exp}</dd>
          <dt>Kết nối</dt><dd>${pr.conn} <span class="hint">đang dùng / tối đa</span></dd>
          <dt>Định dạng</dt><dd>m3u8, ts</dd><dt>Server</dt><dd>${esc(pr.server)}</dd>
          <dt>Username</dt><dd>${esc(pr.user)}</dd><dt>Múi giờ</dt><dd>Asia/Ho_Chi_Minh</dd></dl>
        <div class="stat-row" style="margin-top:16px"><div><b>${fmtN(pr.counts.live)}</b><small>kênh live</small></div><div><b>${fmtN(pr.counts.vod)}</b><small>phim</small></div><div><b>${fmtN(pr.counts.series)}</b><small>series</small></div></div>
        <p class="hint" style="margin-top:8px">Đồng bộ lần cuối: ${pr.synced}</p>
        <div style="display:flex;gap:10px;margin-top:16px"><button class="btn tonal" style="flex:1" onclick="closeOverlay(); go('profileEdit',{id:'${pr.id}'})">${ic('edit', 's20')}Sửa</button>
          <button class="btn primary" style="flex:1" onclick="closeOverlay(); openOverlay('xtreamSync',{id:'${pr.id}'})">${ic('refresh', 's20')}Đồng bộ lại</button></div></div></div>`;
  },
});

def('profileSwitcher', {
  kind: 'BottomSheet', cls: 'ProfileSwitcherSheet', layout: 'sheet_profile_switcher', group: 'Xtream',
  desc: 'Bấm tên profile trên thanh tiêu đề của Xtream home để đổi nhanh, không phải quay lại Who\'s watching. Profile khóa vẫn phải nhập passcode.',
  render: () => {
    const cur = topParams().p;
    return `<div class="sheet"><div class="grab"></div><div class="sheet-h"><span style="width:40px"></span><h3>Đổi profile</h3><button class="icon-btn" onclick="closeOverlay()">${ic('close')}</button></div>
      <div class="sheet-body opt-list">${DATA.profiles.map(pr => `<button class="${pr.id === cur ? 'on' : ''}" onclick="closeOverlay(); ${pr.id === cur ? '' : `openProfile('${pr.id}','replace')`}">
        ${avatar(pr, 36, 10)}<span class="grow">${esc(pr.name)}${profileSub(pr)}</span>${pr.locked ? ic('lock', 's16') : ''}${pr.id === cur ? ic('check', 's20') : ''}</button>`).join('')}
        <div class="divider"></div>
        <button onclick="closeOverlay(); state.tab='xtream'; resetTo('main')">${ic('user', 's20')}<span class="grow">Quản lý profile</span>${ic('chevron', 's20')}</button>
        <button onclick="closeOverlay(); go('profileEdit',{})">${ic('plus', 's20')}<span class="grow">Thêm profile</span></button></div></div>`;
  },
});

function openXtMenu(btn) {
  const r = btn.getBoundingClientRect(), v = vp().getBoundingClientRect();
  openOverlay('xtMenu', { top: r.bottom - v.top, right: v.right - r.right });
}
def('xtMenu', {
  kind: 'Popup', cls: 'PopupMenu (Xtream home)', group: 'Xtream',
  render: (p) => { const id = state.stack[state.stack.length - 1].params.p;
    return `<div class="popup" style="top:${p.top}px; right:${p.right}px">
      <button onclick="closeOverlay(); openOverlay('xtAccount',{id:'${id}'})">${ic('info', 's20')}Thông tin tài khoản</button>
      <button onclick="closeOverlay(); openOverlay('xtreamSync',{id:'${id}'})">${ic('refresh', 's20')}Đồng bộ lại</button>
      <button onclick="closeOverlay(); go('xtreamRecent')">${ic('history', 's20')}Xem tiếp</button>
      <button onclick="closeOverlay(); go('profileEdit',{id:'${id}'})">${ic('edit', 's20')}Sửa profile</button></div>`; },
});

/* ---------------- XtreamHomeActivity ---------------- */
const XT_TABS = [
  { id: 'movie', label: 'Movies', icon: 'movie', frag: 'XtreamMovieFragment', layout: 'fragment_xtream_movie' },
  { id: 'live', label: 'Live', icon: 'tv', frag: 'XtreamLiveFragment', layout: 'fragment_xtream_live' },
  { id: 'search', label: 'Search', icon: 'search', frag: 'XtreamSearchFragment', layout: 'fragment_xtream_search' },
  { id: 'fav', label: 'Favorite', icon: 'heart', frag: 'XtreamFavoriteFragment', layout: 'fragment_xtream_favorite' },
];
def('xtreamHome', {
  cls: 'XtreamHomeActivity', layout: 'activity_xtream_home', group: 'Xtream',
  desc: 'BottomNavigationView 4 tab (Fragment, giống app gốc). Thanh tiêu đề là tên profile: bấm để đổi profile. ⋮: thông tin tài khoản, đồng bộ lại, sửa. Tài khoản chỉ có Live mở thẳng tab Live.',
  fragment: () => { const t = XT_TABS.find(x => x.id === state.xtTab); return { kind: 'Fragment', cls: t.frag, layout: t.layout }; },
  render: (p) => {
    const pr = getProfile(p.p) || DATA.profiles[0];
    if (!pr) return `${toolbar({ title: 'Xtream' })}<div class="empty"><b>Profile không còn tồn tại</b></div>`;
    const expired = pr.status === 'Expired';
    return `<div class="toolbar"><button class="icon-btn" onclick="back()" aria-label="Back">${ic('back')}</button>
        <button class="title-btn" onclick="openOverlay('profileSwitcher')">${avatar(pr, 32, 10)}<span class="t"><b>${esc(pr.name)}</b><small>${expired ? 'Đã hết hạn' : 'Hạn dùng ' + pr.exp}</small></span>${ic('chevronDown', 's16')}</button>
        <button class="icon-btn" onclick="openXtMenu(this)" aria-label="Tùy chọn">${ic('more')}</button></div>
      ${expired ? `<div class="warn-bar">${ic('alert', 's20')}<span>Tài khoản đã hết hạn. Kênh và phim có thể không phát được.</span><button onclick="go('profileEdit',{id:'${pr.id}'})">Sửa</button></div>` : ''}
      <div class="content" style="display:flex;flex-direction:column">${XT_RENDER[state.xtTab](pr)}</div>
      <nav class="bnav">${XT_TABS.map(t => `<button class="${state.xtTab === t.id ? 'on' : ''}" onclick="state.xtTab='${t.id}'; refresh()">${ic(t.icon)}${t.label}</button>`).join('')}</nav>`;
  },
});

function posterCard(m, w = 112) {
  const kind = m.seasons ? 'series' : 'movie';
  return `<div class="poster-w" style="width:${typeof w === 'number' ? w + 'px' : w}" onclick="go('${kind === 'series' ? 'seriesDetail' : 'movieDetail'}',{id:${m.id}})">
    <div class="poster" style="${posterBg(m.c)}"><span class="rate">${ic('starFill', 's16')}${m.r}</span>${m.t}</div><p>${m.t}</p></div>`;
}
const XT_RENDER = {
  movie: (pr) => {
    if (!hasVod(pr)) return `<div class="empty" style="flex:1;justify-content:center"><div class="art">${ic('movie', 's32')}</div><b>Tài khoản này không có phim</b>
      <p>Nhà cung cấp chỉ cấp kênh Live cho "${esc(pr.name)}". Đồng bộ lại nếu bạn vừa được thêm gói phim.</p>
      <div style="display:flex;gap:8px"><button class="btn tonal small" onclick="openOverlay('xtreamSync',{id:'${pr.id}'})">${ic('refresh', 's16')}Đồng bộ lại</button>
      <button class="btn primary small" onclick="state.xtTab='live'; refresh()">Xem kênh Live</button></div></div>`;
    const h = DATA.movies[2];
    const cat = state.xtCat || 'All';
    return `<div class="chips" style="padding-bottom:12px">${['All', 'Movies', 'Series'].map(x => `<button class="chip ${cat === x ? 'on' : ''}" onclick="state.xtCat='${x}'; refresh()">${x}</button>`).join('')}</div>
      <div class="hero" style="${posterBg(h.c)}"><h2>${h.t}</h2><div class="m"><span class="gold">${ic('starFill', 's16')}${h.r}</span>${h.y} · ${h.g} · ${h.d}</div>
        <div class="acts"><button class="btn primary small" onclick="go('player',{movie:${h.id}})">${ic('play', 's16')}Xem</button>
        <button class="btn tonal small" onclick="go('movieDetail',{id:${h.id}})">${ic('info', 's16')}Chi tiết</button></div></div>
      ${pr.history ? `<div class="sec-h"><h3>Xem tiếp</h3><button onclick="go('xtreamRecent')">Xem tất cả</button></div>
      <div class="hscroll">${DATA.continueWatching.map(cw => { const r = findRef(cw.ref); return `<div class="cw" onclick="go('player',{movie:${r.id}, ep:'${cw.ep}'})">
        <div class="thumb" style="${posterBg(r.c)}"><span class="pl">${ic('play', 's20')}</span>${r.t}</div><small>${cw.ep}</small><div class="progress"><i style="width:${cw.p}%"></i></div></div>`; }).join('')}</div>` : ''}
      ${cat !== 'Series' ? `<div class="sec-h"><h3>Phim mới thêm</h3><button onclick="go('xtCategory',{type:'movie', cat:'All'})">Xem tất cả</button></div>
      <div class="hscroll">${DATA.movies.map(m => posterCard(m)).join('')}</div>` : ''}
      ${cat !== 'Movies' ? `<div class="sec-h"><h3>Series</h3><button onclick="go('xtCategory',{type:'series', cat:'All'})">Xem tất cả</button></div>
      <div class="hscroll">${DATA.series.map(m => posterCard(m)).join('')}</div>` : ''}
      <div class="sec-h"><h3>Theo thể loại</h3></div>
      <div class="chips" style="flex-wrap:wrap;padding-bottom:20px">${DATA.xtMovieCats.slice(1).map(c => `<button class="chip" onclick="go('xtCategory',{type:'movie',cat:'${c}'})">${c}</button>`).join('')}</div>`;
  },
  live: () => {
    const cat = state.xtLive || 'All';
    const list = DATA.channels.filter((c, i) => cat === 'All' || (cat === 'Sports' ? c.group === 'Sports' : cat === 'News' ? c.group === 'News' : cat === 'Kids' ? c.group === 'Kids' : cat === 'Movies' ? c.group === 'Movies' : i % 3 === 0));
    return `<div class="pad" style="padding-bottom:10px"><label class="search">${ic('search', 's20')}<input placeholder="Tìm kênh live"></label></div>
      <div class="live-split"><div class="cats">${DATA.xtLiveCats.map(c => `<button class="${cat === c ? 'on' : ''}" onclick="state.xtLive='${c}'; refresh()">${c}</button>`).join('')}</div>
      <div class="chs">${list.map(c => `<div class="row" onclick="go('player',{ch:${c.id}})">${chLogo(c.name)}<div class="main"><b>${c.name}</b>
        <span class="epg-now"><em>Đang phát</em> · Bản tin 19h</span></div><button class="icon-btn fav ${c.fav ? 'on' : ''}" onclick="event.stopPropagation(); toggleFav(${c.id})">${ic(c.fav ? 'heartFill' : 'heart', 's20')}</button></div>`).join('')}</div></div>`;
  },
  search: (pr) => {
    const q = (state.xtQ || '').toLowerCase();
    const pool = hasVod(pr) ? [...DATA.movies, ...DATA.series] : [];
    const res = q ? pool.filter(m => m.t.toLowerCase().includes(q)) : [];
    return `<div class="pad" style="padding-bottom:10px"><label class="search">${ic('search', 's20')}<input placeholder="${hasVod(pr) ? 'Phim, series, kênh…' : 'Tên kênh…'}" value="${esc(state.xtQ || '')}"
      oninput="state.xtQ=this.value; clearTimeout(window._qt); window._qt=setTimeout(()=>{refresh(); const i=document.querySelector('.search input'); i.focus(); i.setSelectionRange(99,99);},300)"></label></div>
      ${!q ? `<div class="sec-h" style="padding-top:6px"><h3>Tìm gần đây</h3></div><div class="chips" style="flex-wrap:wrap">${['delta', 'harbor', 'kids'].map(h => `<button class="chip" onclick="state.xtQ='${h}'; refresh()">${ic('history', 's16')}${h}</button>`).join('')}</div>`
        : res.length ? `<div class="grid" style="margin-top:4px">${res.map(m => posterCard(m, '100%')).join('')}</div>`
        : `<div class="empty"><div class="art">${ic('search', 's32')}</div><b>Không có kết quả</b><p>Thử từ khóa ngắn hơn.</p></div>`}`;
  },
  fav: (pr) => `<div class="pad" style="padding-bottom:12px"><div class="seg"><button class="on">Phim & Series</button><button onclick="toast('Tab Kênh live')">Kênh live</button></div></div>
      ${pr.history ? `<div class="grid">${[DATA.movies[0], DATA.series[0], DATA.movies[3]].map(m => posterCard(m, '100%')).join('')}</div>`
        : `<div class="empty"><div class="art">${ic('heart', 's32')}</div><b>Chưa có mục yêu thích</b><p>Bấm ♡ ở phim, series hoặc kênh để lưu vào đây.</p></div>`}`,
};

/* ---------------- category / detail / recent ---------------- */
def('xtCategory', {
  cls: 'XtreamCategoryActivity', layout: 'activity_xtream_category', group: 'Xtream',
  desc: 'Danh sách theo category / "Xem tất cả". Lưới 3 cột, Paging 3.',
  render: (p) => {
    const src = p.type === 'series' ? DATA.series : DATA.movies;
    const list = p.cat === 'All' ? src : src.filter(m => m.g === p.cat);
    return `${toolbar({ title: p.cat === 'All' ? (p.type === 'series' ? 'Tất cả series' : 'Tất cả phim') : p.cat, sub: `${list.length} mục`, right: `<button class="icon-btn" onclick="openSort(this)">${ic('sort')}</button>` })}
      <div class="chips" style="padding-bottom:12px">${DATA.xtMovieCats.map(c => `<button class="chip ${p.cat === c ? 'on' : ''}" onclick="setParams({cat:'${c}'})">${c}</button>`).join('')}</div>
      <div class="content">${list.length ? `<div class="grid">${list.map(m => posterCard(m, '100%')).join('')}</div>` : `<div class="empty"><div class="art">${ic('movie', 's32')}</div><b>Chưa có mục nào</b></div>`}<div class="spacer"></div></div>`;
  },
});

def('movieDetail', {
  cls: 'MovieDetailActivity', layout: 'activity_movie_detail', group: 'Xtream',
  desc: 'App gốc là bottom sheet; bản clone tách Activity cho đủ chỗ. Dữ liệu từ get_vod_info. Có Resume nếu đã xem dở.',
  render: (p) => {
    const m = findRef(p.id);
    return `<div class="content"><div class="backdrop" style="${posterBg(m.c)}">${toolbar({ title: '', right: `<button class="icon-btn" onclick="toast('Đã lưu vào Favorite')">${ic('heart')}</button>` })}</div>
      <div class="detail-head"><div class="poster" style="${posterBg(m.c)}"></div><div class="t"><h1>${m.t}</h1>
        <div class="m"><span class="gold">★ ${m.r}</span><span>${m.y}</span><span>${m.g}</span><span>${m.d || ''}</span></div></div></div>
      <div class="pad" style="margin-top:18px"><div style="display:flex;gap:10px">
        <button class="btn primary" style="flex:1" onclick="go('player',{movie:${m.id}})">${ic('play', 's20')}${m.id === 101 ? 'Xem tiếp 1:10:24' : 'Xem'}</button>
        <button class="btn tonal" onclick="toast('Mở trailer YouTube')">${ic('playCircle', 's20')}Trailer</button></div>
        <p class="muted" style="margin-top:16px;line-height:1.55">Một người thợ lặn ở cảng đêm phát hiện chiếc hộp kim loại cũ, kéo theo chuỗi bí mật của cả thị trấn ven biển. Câu chuyện chậm, lạnh và nhiều khoảng lặng.</p>
        <dl class="facts" style="margin-top:14px"><dt>Đạo diễn</dt><dd>Lena Ortiz</dd><dt>Diễn viên</dt><dd>Kai Moreno, Ada Lin, Tom Reyes</dd><dt>Thể loại</dt><dd>${m.g}</dd></dl></div>
      <div class="sec-h"><h3>Cùng thể loại</h3></div><div class="hscroll">${DATA.movies.filter(x => x.id !== m.id).map(x => posterCard(x)).join('')}</div><div class="spacer"></div></div>`;
  },
});

def('seriesDetail', {
  cls: 'SeriesDetailActivity', layout: 'activity_series_detail', group: 'Xtream',
  desc: 'get_series_info khi mở (cache Room). Chip chọn mùa, danh sách tập có tiến độ từng tập.',
  render: (p) => {
    const s = findRef(p.id); const season = p.season || 1;
    const eps = Array.from({ length: 8 }, (_, i) => ({ n: i + 1, t: ['Pilot', 'Signal', 'Undertow', 'Glass', 'Relay', 'Static', 'Offshore', 'Homecoming'][i], d: 42 + (i % 3) * 4, p: season === 2 && i < 3 ? 100 : season === 2 && i === 3 ? 62 : 0 }));
    return `<div class="content"><div class="backdrop" style="${posterBg(s.c)}">${toolbar({ title: '', right: `<button class="icon-btn" onclick="toast('Đã lưu vào Favorite')">${ic('heart')}</button>` })}</div>
      <div class="detail-head"><div class="poster" style="${posterBg(s.c)}"></div><div class="t"><h1>${s.t}</h1>
        <div class="m"><span class="gold">★ ${s.r}</span><span>${s.y}</span><span>${s.g}</span><span>${s.seasons} mùa</span></div></div></div>
      <div class="pad" style="margin-top:18px"><button class="btn primary block" onclick="go('player',{movie:${s.id}, ep:'S2 · E4'})">${ic('play', 's20')}Xem tiếp S2 · E4</button></div>
      <div class="chips" style="padding-top:18px">${Array.from({ length: s.seasons }, (_, i) => `<button class="chip ${season === i + 1 ? 'on' : ''}" onclick="setParams({season:${i + 1}})">Mùa ${i + 1}</button>`).join('')}</div>
      <div style="margin-top:8px">${eps.map(e => `<div class="ep" onclick="go('player',{movie:${s.id}, ep:'S${season} · E${e.n}'})"><div class="th" style="${posterBg(s.c)}"><span class="n">${e.n}</span>${e.p ? `<div class="progress"><i style="width:${e.p}%"></i></div>` : ''}</div>
        <div class="main"><b>${e.t}</b><small>${e.d} phút${e.p === 100 ? ' · Đã xem' : e.p ? ' · Đang xem' : ''}</small></div></div>`).join('')}</div><div class="spacer"></div></div>`;
  },
});

def('xtreamRecent', {
  cls: 'XtreamRecentActivity', layout: 'activity_xtream_recent', group: 'Xtream',
  desc: 'UNION phim + tập có isRecent của profile đang mở, mới nhất trước. Vuốt để xóa khỏi danh sách.',
  render: () => `${toolbar({ title: 'Xem tiếp', right: `<button class="text-btn" onclick="toast('Đã xóa lịch sử xem')">Xóa hết</button>` })}
    <div class="content">${DATA.continueWatching.map(cw => { const r = findRef(cw.ref); return `<div class="ep" onclick="go('player',{movie:${r.id}, ep:'${cw.ep}'})">
      <div class="th" style="${posterBg(r.c)}"><div class="progress"><i style="width:${cw.p}%"></i></div></div>
      <div class="main"><b>${r.t}</b><small>${cw.ep}</small></div><button class="icon-btn" onclick="event.stopPropagation(); toast('Đã xóa khỏi danh sách')">${ic('close', 's20')}</button></div>`; }).join('')}</div>`,
});
