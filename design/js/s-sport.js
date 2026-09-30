/* ===== Sport: tab fragment, matches, my match, detail, league picker, watch sheet ===== */

const CREST = ['#c0392b', '#2471a3', '#1e8449', '#7d3c98', '#b9770e', '#117a65', '#2e4053', '#a93226'];
function crest(name) { return `<span class="crest" style="background:${CREST[hash(name) % CREST.length]}">${name.slice(0, 3).toUpperCase()}</span>`; }
function matchCard(m, compact = false) {
  const center = m.st === 'live' ? `<div class="score">${m.hs} – ${m.as}<small>${m.min}</small></div>`
    : m.st === 'post' ? `<div class="score">${m.hs} – ${m.as}<small style="color:var(--text-3)">Kết thúc</small></div>`
    : `<div class="score pre">${m.time}<small>${m.date || ''}</small></div>`;
  const followed = (state.myMatches || ['m3']).includes(m.id);
  return `<div class="match" onclick="go('matchDetail',{id:'${m.id}'})">
    <div class="lg">${ic('trophy', 's16')}<span class="grow">${m.lg}</span>${m.st === 'live' ? '<span class="live">LIVE</span>' : ''}
      ${m.st === 'pre' ? `<button class="icon-btn ${followed ? 'on' : ''}" style="width:32px;height:32px;margin:-8px -6px -8px 0" onclick="event.stopPropagation(); toggleRemind('${m.id}')">${ic(followed ? 'bellOn' : 'bell', 's20')}</button>` : ''}</div>
    <div class="teams"><div class="team">${crest(m.home)}${m.home}</div>${center}<div class="team">${crest(m.away)}${m.away}</div></div>
    ${!compact && m.st === 'live' ? `<div class="acts"><button class="btn primary small" onclick="event.stopPropagation(); openOverlay('watchSheet',{id:'${m.id}'})">${ic('play', 's16')}Xem trên kênh của bạn</button></div>` : ''}</div>`;
}
function toggleRemind(id) {
  state.myMatches = state.myMatches || ['m3'];
  const on = state.myMatches.includes(id);
  state.myMatches = on ? state.myMatches.filter(x => x !== id) : [...state.myMatches, id];
  toast(on ? 'Đã tắt nhắc trận' : 'Sẽ nhắc bạn 15 phút trước giờ đá');
  refresh();
}

function sportTab() {
  const live = DATA.matches.filter(m => m.st === 'live');
  const up = DATA.matches.filter(m => m.st === 'pre');
  const mine = DATA.matches.filter(m => (state.myMatches || ['m3']).includes(m.id));
  return `${toolbar({ title: 'Thể thao', backBtn: false, right: `<button class="icon-btn boxed" onclick="openOverlay('leaguePicker')">${ic('trophy')}</button>` })}
    <div class="chips" style="padding-bottom:4px">${['Bóng đá', 'Bóng rổ', 'Tennis', 'F1'].map((x, i) => `<button class="chip ${i === 0 ? 'on' : ''}">${x}</button>`).join('')}</div>
    <div class="sec-h"><h3>Đang diễn ra</h3><span class="live">${live.length} trận</span></div>
    <div class="stack pad">${live.map(m => matchCard(m)).join('')}</div>
    <div class="sec-h"><h3>Trận của tôi</h3><button onclick="go('myMatch')">Xem tất cả</button></div>
    <div class="stack pad">${mine.length ? mine.map(m => matchCard(m, true)).join('') : `<p class="hint">Bấm chuông ở 1 trận sắp đá để theo dõi và nhận nhắc.</p>`}</div>
    <div class="sec-h"><h3>Sắp diễn ra</h3><button onclick="go('sportMatches')">Lịch thi đấu</button></div>
    <div class="stack pad">${up.map(m => matchCard(m, true)).join('')}</div><div style="height:100px"></div>`;
}

def('sportMatches', {
  cls: 'SportMatchesActivity', layout: 'activity_sport_matches', group: 'Thể thao',
  desc: 'Lọc ngày (dải ngày + nút lịch) và giải. ESPN scoreboard?dates=. Polling: live 30 s, <1h 60 s, <24h 5 phút.',
  render: (p) => {
    const d = p.d ?? 2;
    const days = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'].map((w, i) => [w, 28 + i > 30 ? 28 + i - 30 : 28 + i]);
    return `${toolbar({ title: 'Lịch thi đấu', right: `<button class="icon-btn" onclick="toast('Mở DatePicker')">${ic('calendar')}</button><button class="icon-btn" onclick="openOverlay('leaguePicker')">${ic('trophy')}</button>` })}
      <div class="date-strip">${days.map((x, i) => `<button class="${d === i ? 'on' : ''}" onclick="setParams({d:${i}})">${i === 2 ? 'Hôm nay' : x[0]}<b>${x[1]}</b></button>`).join('')}</div>
      <div class="chips" style="padding-top:12px">${['Tất cả', 'Premier League', 'LaLiga', 'Serie A', 'Champions League'].map((x, i) => `<button class="chip ${i === 0 ? 'on' : ''}">${x}</button>`).join('')}</div>
      <div class="content" style="padding-top:12px"><div class="stack pad">${(d === 1 ? DATA.matches.filter(m => m.st === 'post') : d === 2 ? DATA.matches.filter(m => m.st !== 'post') : d === 3 ? DATA.matches.filter(m => m.date === 'Ngày mai') : []).map(m => matchCard(m, true)).join('') ||
        `<div class="empty"><div class="art">${ic('calendar', 's32')}</div><b>Không có trận nào</b><p>Chọn ngày khác hoặc thêm giải đấu.</p></div>`}</div><div class="spacer"></div></div>`;
  },
});

def('myMatch', {
  cls: 'MyMatchActivity', layout: 'activity_my_match', group: 'Thể thao',
  desc: 'Trận đã theo dõi (Room favourite_match). Nhắc 15 phút trước bằng WorkManager.',
  render: () => { const mine = DATA.matches.filter(m => (state.myMatches || ['m3']).includes(m.id));
    return `${toolbar({ title: 'Trận của tôi' })}<div class="content"><div class="stack pad">${mine.length ? mine.map(m => matchCard(m, true)).join('') : `<div class="empty"><div class="art">${ic('bell', 's32')}</div><b>Chưa theo dõi trận nào</b><p>Bấm chuông ở trận sắp đá để nhận nhắc trước 15 phút.</p></div>`}</div></div>`; },
});

def('matchDetail', {
  cls: 'MatchDetailActivity', layout: 'activity_match_detail', group: 'Thể thao',
  desc: 'ESPN summary?event=. Nút Watch mở sheet chọn kênh từ nguồn của người dùng (không đẩy link từ server).',
  render: (p) => {
    const m = DATA.matches.find(x => x.id === p.id);
    return `${toolbar({ title: m.lg, center: true, right: `<button class="icon-btn" onclick="toggleRemind('${m.id}')">${ic('bell')}</button>` })}
      <div class="content">
        <div class="pad"><div class="match" style="padding:18px 14px"><div class="teams">
          <div class="team">${crest(m.home).replace('crest"', 'crest" ').replace('width:40px', '')}${m.home}</div>
          ${m.st === 'pre' ? `<div class="score pre" style="font-size:28px">${m.time}<small>${m.date}</small></div>` : `<div class="score" style="font-size:44px">${m.hs} – ${m.as}<small ${m.st === 'post' ? 'style="color:var(--text-3)"' : ''}>${m.st === 'live' ? m.min : 'Kết thúc'}</small></div>`}
          <div class="team">${crest(m.away)}${m.away}</div></div></div>
        <button class="btn primary block" style="margin-top:14px" onclick="openOverlay('watchSheet',{id:'${m.id}'})">${ic('play', 's20')}${m.st === 'post' ? 'Tìm kênh phát lại' : 'Xem trên kênh của bạn'}</button></div>
        ${m.st !== 'pre' ? `<div class="sec-h"><h3>Diễn biến</h3></div><div class="pad"><div class="card timeline">
          <div class="ev"><span class="l">⚽ K. Moreno</span><span class="m">12'</span><span class="r"></span></div>
          <div class="ev"><span class="l"></span><span class="m">34'</span><span class="r"><i class="card-y"></i> D. Silva</span></div>
          <div class="ev"><span class="l"></span><span class="m">51'</span><span class="r">⚽ A. Lind</span></div>
          <div class="ev"><span class="l">⚽ T. Reyes</span><span class="m">63'</span><span class="r"></span></div></div></div>` : `<div class="pad" style="margin-top:14px"><div class="card"><b>Sân</b><p class="muted">Harbor Stadium · Northfield</p></div></div>`}
        <div class="spacer"></div></div>`;
  },
});

def('leaguePicker', {
  kind: 'BottomSheet', cls: 'LeaguePickerSheet', layout: 'sheet_league_picker', group: 'Thể thao',
  desc: 'Danh sách giải từ RC sport_data; giải isHot bật sẵn. Lưu selected_leagues.',
  render: () => `<div class="sheet"><div class="grab"></div><div class="sheet-h"><span style="width:40px"></span><h3>Giải đấu theo dõi</h3><button class="text-btn accent" onclick="closeOverlay(); toast('Đã lưu')">Xong</button></div>
    <div class="sheet-body">${DATA.leagues.map(l => `<div class="switch-row"><span class="crest" style="background:var(--surface-3);width:36px;height:36px;font-size:11px">${l.slug.split('.')[0].toUpperCase()}</span>
      <div class="txt"><b style="font-weight:600">${l.name}</b><small>${l.sport}</small></div><button class="switch ${l.hot ? 'on' : ''}" onclick="this.classList.toggle('on')"></button></div>`).join('')}</div></div>`,
});

def('watchSheet', {
  kind: 'BottomSheet', cls: 'WatchMatchSheet', layout: 'sheet_watch_match', group: 'Thể thao',
  desc: 'Tìm trong kênh của người dùng theo tên đội/giải + nhóm Sports. Chưa có nguồn → Add Playlist / Continue with Xtream / Để sau.',
  render: (p) => {
    const m = DATA.matches.find(x => x.id === p.id);
    const hasSource = DATA.playlists.length || DATA.profiles.length;
    const sports = DATA.channels.filter(c => c.group === 'Sports');
    return `<div class="sheet"><div class="grab"></div><div class="sheet-h"><span style="width:40px"></span><h3>Chọn kênh để xem</h3><button class="icon-btn" onclick="closeOverlay()">${ic('close')}</button></div>
      <div class="sheet-body">${hasSource ? `
        <label class="search" style="margin-bottom:12px">${ic('search', 's20')}<input value="${m.home}"></label>
        <p class="hint" style="margin-bottom:8px">Kênh thể thao trong nguồn của bạn</p>
        <div class="list-card" style="margin:0">${sports.map(c => `<div class="row" onclick="closeOverlay(); go('player',{ch:${c.id}})">${chLogo(c.name)}<div class="main"><b>${c.name}</b><small>${plName()}</small></div>${ic('play', 's20')}</div>`).join('')}</div>`
      : `<div class="empty" style="padding:12px 8px"><div class="art">${ic('tv', 's32')}</div><b>Thêm nguồn để xem trận</b><p>App không có sẵn kênh. Thêm playlist hoặc tài khoản Xtream có kênh thể thao.</p></div>
        <div class="stack"><button class="btn primary block" onclick="closeOverlay(); go('import',{tab:'url'})">Thêm playlist</button>
        <button class="btn tonal block" onclick="closeOverlay(); go('import',{tab:'xtream'})">Thêm Xtream</button></div>`}</div></div>`;
  },
});
