/* ===== Review sidebar: scenarios + screen index ===== */

const SCENARIOS = [
  { id: 'first', name: 'Mở app lần đầu (có quảng cáo)', setup: () => { state.firstRun = true; state.adsOn = true; DATA.playlists = []; DATA.profiles = []; state.tab = 'home'; resetTo('splash'); },
    steps: ['Splash → xin consent → interstitial splash', 'Chọn ngôn ngữ (native neo ở đáy, nút tick trên đầu)', 'Onboarding 1 (ảnh 1): native ở đáy trang', 'Onboarding 2 (ảnh 2): không quảng cáo', 'Onboarding 3 (ảnh 3): native ở đáy trang', 'Onboarding 4: native phủ toàn trang', 'Native toàn màn trước khi vào Main', 'Disclaimer: tích ô → Accept', 'Home trống'],
    match: ['splash', 'adSplashInter', 'langapp', 'onb1', 'onb2', 'onb3', 'onb4', 'adNativeInter', 'disclaimer', 'main'] },
  { id: 'ads', name: 'Quảng cáo ở các điểm chuyển màn', setup: () => { restoreData(); state.adsOn = true; state.tab = 'home'; resetTo('main'); },
    steps: ['Bật/tắt bằng nút "Quảng cáo" dưới khung điện thoại', 'Banner nằm ở đáy mọi màn', 'Mở 1 nguồn (playlist / profile) → native toàn màn', 'Rời app rồi quay lại → App Open'],
    extra: `<button onclick="go('adNativeInter',{next:'main'})">Xem native toàn màn (đổi màn)</button><button onclick="go('adAppOpen')">Xem App Open (quay lại app)</button><button onclick="go('adSplashInter',{next:'main'})">Xem interstitial lúc mở app</button>` },
  { id: 'find', name: 'Tìm link → thêm playlist → xem', setup: () => { restoreData(); DATA.playlists = []; state.tab = 'home'; resetTo('main'); },
    steps: ['Home trống: bấm "Xem cách tìm playlist"', 'Hướng dẫn: mở 1 trang gợi ý', 'Trang web: chạm 1 link .m3u → "Dùng link này"', 'Form đã điền link → Thêm playlist', 'Import xong → Xem kênh', 'Chọn 1 kênh → Player'], match: ['main', 'howto', 'webGuide', 'import', 'importProgress', 'player'] },
  { id: 'watch', name: 'Xem kênh & điều khiển player', setup: () => { restoreData(); state.tab = 'home'; resetTo('main'); },
    steps: ['Home: bấm Play ở thẻ "Xem tiếp" hoặc 1 kênh', 'Player dọc: đổi kênh ở danh sách bên dưới', 'Bấm ⛶ để xem ngang, mở panel Kênh', 'Thử Hẹn giờ, Phụ đề, Tỉ lệ, Khóa', 'Bấm PiP: xem nổi trên app'], match: ['main', 'player'] },
  { id: 'xtream', name: 'Thêm Xtream → profile mới', setup: () => { restoreData(); state.tab = 'xtream'; state.newProfile = null; resetTo('main'); },
    steps: ['Tab Xtream: bấm ô "Thêm profile"', 'Bấm "Dán" để tự điền, rồi Đăng nhập & đồng bộ', 'Dialog "Đã thêm": chọn "Về danh sách"', 'Who\'s watching: profile mới có nhãn "Mới"', 'Chạm profile mới → Xtream home'],
    extra: `<button onclick="restoreData(); state.tab='xtream'; resetTo('main'); go('profileEdit',{srv:'http://live.example.tv:8080', user:'demo', pass:'x', name:'Chỉ có Live'})">Thử: tài khoản chỉ có Live</button><button onclick="restoreData(); state.tab='xtream'; resetTo('main'); go('profileEdit',{srv:'http://bad.example:8080', user:'demo', pass:'x'})">Thử: sai Server URL</button><button onclick="restoreData(); state.tab='xtream'; resetTo('main'); go('profileEdit',{srv:'http://line.example.tv:8080', user:'wrong_user', pass:'x'})">Thử: sai mật khẩu</button><button onclick="DATA.profiles=[]; state.tab='xtream'; resetTo('main')">Thử: chưa có profile nào</button>` },
  { id: 'xtprofile', name: 'Chạm profile Xtream → xem', setup: () => { restoreData(); state.tab = 'xtream'; resetTo('main'); },
    steps: ['"My Xtream": chạm → Xtream home (Movies)', 'Bấm tên profile trên đầu → đổi sang profile khác', '"Gia đình" (có khóa): nhập passcode 1234', '"Sports line": chỉ có Live → mở thẳng tab Live', '"Old line" (hết hạn): cảnh báo → Vẫn mở / Sửa', 'Bấm ⋮ cạnh tên: đồng bộ, thông tin, khóa, xóa'] },
  { id: 'lock', name: 'Khóa playlist bằng passcode', setup: () => { restoreData(); DATA.settings.passcode = null; state.tab = 'home'; resetTo('main'); },
    steps: ['Mở playlist "News world" (đang khóa)', 'Nhập passcode (mặc định demo: 1234)', 'Hoặc: + › Playlist URL, bật Khóa → tạo passcode 2 lần'], match: ['main', 'passcode', 'playlist'] },
  { id: 'sport', name: 'Thể thao → xem trận', setup: () => { restoreData(); state.tab = 'sport'; state.sportOn = true; resetTo('main'); },
    steps: ['Tab Sport: trận đang diễn ra', 'Bấm chuông ở trận sắp đá để nhắc', 'Mở chi tiết trận → "Xem trên kênh của bạn"', 'Chọn kênh thể thao trong playlist của bạn'], match: ['main', 'matchDetail', 'watchSheet', 'player'] },
  { id: 'err', name: 'Lỗi: link hỏng / kênh chết', setup: () => { restoreData(); resetTo('main'); go('import', { tab: 'url', url: 'https://example.org/bad-playlist.m3u' }); },
    steps: ['Link hỏng → dialog lỗi + gợi ý hướng dẫn', 'Mở 1 kênh, rồi xem trạng thái lỗi của player (nút bên dưới)'], match: ['import', 'importProgress', 'player'],
    extra: `<button onclick="go('player',{ch:5, err:true})">Xem player khi kênh lỗi</button><button onclick="go('player',{ch:3, buffering:true})">Xem player đang tải</button>` },
];
const SEED = { playlists: DATA.playlists.slice(), profiles: DATA.profiles.map(x => ({ ...x, counts: { ...x.counts } })) };
function restoreData() {
  if (!DATA.playlists.length) DATA.playlists = SEED.playlists.slice();
  if (!DATA.profiles.length) DATA.profiles = SEED.profiles.map(x => ({ ...x, counts: { ...x.counts } }));
  state.firstRun = false;
}

let curScenario = null;
window.renderScenarioSteps = () => {
  if (!curScenario) return '';
  const s = SCENARIOS.find(x => x.id === curScenario);
  return `<h2 style="margin-top:18px">Kịch bản: ${s.name}</h2><div class="blk"><ol>${s.steps.map(x => `<li>${x}</li>`).join('')}</ol></div>`;
};

function buildSide() {
  const groups = {};
  Object.values(SCREENS).forEach(s => { if (s.kind === 'Mock') return; (groups[s.group] = groups[s.group] || []).push(s); });
  const openers = {
    Activity: (s) => `go('${s.id}', ${JSON.stringify(DEFAULT_PARAMS[s.id] || {}).replace(/"/g, "'")})`,
  };
  document.getElementById('side').innerHTML = `
    <h1>IPTV Player</h1><div class="sub">Prototype duyệt luồng · khung 360×800 dp (Pixel 6a)</div>
    <h2>Kịch bản</h2><div class="scen">${SCENARIOS.map(s => `<button data-s="${s.id}" onclick="runScenario('${s.id}')">${s.name}</button>`).join('')}</div>
    <div class="scen" id="scen-extra" style="margin-top:6px"></div>
    <h2>Tab thứ 4 <small>RC show_sport</small></h2>
    <div class="toggle-row"><button id="rc-on" onclick="state.sportOn=true; syncRc(); refresh()">Sport</button><button id="rc-off" onclick="state.sportOn=false; syncRc(); refresh()">Settings</button></div>
    <h2>Tất cả màn <small>${Object.values(SCREENS).filter(s => s.kind !== 'Mock').length} màn</small></h2>
    ${Object.entries(groups).map(([g, list]) => `<details class="scr-group" ${g === 'Màn chính' ? 'open' : ''}><summary>${g}</summary>
      ${list.map(s => `<a href="#" onclick="event.preventDefault(); openScreen('${s.id}')">${s.cls}<code>${s.kind === 'Activity' ? '' : s.kind}</code></a>`).join('')}</details>`).join('')}
    <p class="sub" style="margin-top:16px">Esc = Back · Chạm video để ẩn/hiện điều khiển · Passcode demo: 1234</p>`;
  syncRc();
}
function syncRc() { document.getElementById('rc-on').classList.toggle('on', state.sportOn); document.getElementById('rc-off').classList.toggle('on', !state.sportOn); }

const DEFAULT_PARAMS = {
  import: { tab: 'url' }, playlist: { id: 1 }, player: { ch: 16 }, profileEdit: { id: 'p1' }, xtreamHome: { p: 'p1' },
  xtCategory: { type: 'movie', cat: 'All' }, movieDetail: { id: 101 }, seriesDetail: { id: 201 }, matchDetail: { id: 'm1' },
  howto: { type: 'url' }, community: { tab: 'iptv' }, disclaimer: {},
};
const OVERLAY_PARAMS = {
  plMenu: { id: 1, top: 300, right: 16 }, sortPopup: { top: 110, right: 16 }, chMenu: { id: 1, top: 300, right: 16 },
  passcode: { mode: 'create' }, editPlaylist: { id: 1 }, importProgress: { name: 'Playlist 3' }, confirm: { title: 'Xóa playlist?', msg: 'Playlist và toàn bộ kênh sẽ bị xóa khỏi máy.', ok: 'Xóa', danger: true },
  webGuide: { type: 'iptv', i: 0 }, watchSheet: { id: 'm1' }, shareDialog: { type: 'iptv' }, xtreamSync: { id: 'p1' },
  profileActions: { id: 'p1' }, xtAccount: { id: 'p1' }, expired: { id: 'p4' }, xtMenu: { top: 60, right: 8 },
};
const OVERLAY_HOST = { profileActions: 'main', expired: 'main', profileSwitcher: 'xtreamHome', xtMenu: 'xtreamHome', xtAccount: 'xtreamHome', timerSheet: 'player', subSheet: 'player', audioSheet: 'player', aspectSheet: 'player', chListSheet: 'player', castDialog: 'player', sortPopup: 'playlist', chMenu: 'playlist', plMenu: 'main', watchSheet: 'matchDetail', leaguePicker: 'main' };

function openScreen(id) {
  curScenario = null; markScenario();
  const s = SCREENS[id];
  restoreData();
  if (s.kind === 'Activity' || s.kind === 'Mock') { if (id === 'main') { resetTo('main'); } else { resetTo('main'); go(id, JSON.parse(JSON.stringify(DEFAULT_PARAMS[id] || {}))); } return; }
  const host = OVERLAY_HOST[id] || 'main';
  resetTo('main'); if (host !== 'main') go(host, JSON.parse(JSON.stringify(DEFAULT_PARAMS[host] || {})));
  if (id === 'leaguePicker') { state.tab = 'sport'; refresh(); }
  if (id === 'profileActions' || id === 'expired') { state.tab = 'xtream'; refresh(); }
  openOverlay(id, Object.assign({}, OVERLAY_PARAMS[id] || {}));
}
function runScenario(id) {
  curScenario = id; markScenario();
  const s = SCENARIOS.find(x => x.id === id);
  document.getElementById('scen-extra').innerHTML = s.extra || '';
  s.setup();
}
function markScenario() { document.querySelectorAll('.scen button[data-s]').forEach(b => b.classList.toggle('on', b.dataset.s === curScenario)); if (!curScenario) { const e = document.getElementById('scen-extra'); if (e) e.innerHTML = ''; } }
function toMain() { restoreData(); resetTo('main'); }
function restart() { runScenario('first'); }

buildSide();
// Deep link for review/screenshots: prototype.html#screen=<id> or #scenario=<id>
(function boot() {
  const h = new URLSearchParams(location.hash.slice(1));
  window.__errors = [];
  if (h.get('bare')) document.body.classList.add('bare');
  // #run=<js> executes steps after boot (used for automated flow screenshots)
  if (h.get('run')) setTimeout(() => { try { new Function(h.get('run'))(); } catch (e) { document.title = 'ERR ' + e.message; } }, 60);
  if (h.get('ov')) setTimeout(() => openOverlay(h.get('ov'), Object.assign({}, OVERLAY_PARAMS[h.get('ov')] || {})), 50);
  if (h.get('screen') && SCREENS[h.get('screen')]) {
    if (h.get('tab')) state.tab = h.get('tab');
    if (h.get('xt')) state.xtTab = h.get('xt');
    openScreen(h.get('screen'));
    if (h.get('empty')) { DATA.playlists = []; DATA.profiles = []; refresh(); }
    if (h.get('p')) { Object.assign(topParams(), JSON.parse(decodeURIComponent(h.get('p')))); refresh(); }
  } else runScenario(h.get('scenario') || 'first');
})();
