/* ===== PlayerActivity + sheets ===== */

function curChannel(p) {
  if (p.single) return { id: 0, name: 'Concert live', group: 'Single stream', q: 'HLS' };
  if (p.movie) { const m = findRef(p.movie); return { id: 0, name: m.t + (p.ep ? ' · ' + p.ep : ''), group: m.g, q: '1080p', vod: true, c: m.c }; }
  return DATA.channels.find(c => c.id === (p.ch || 1)) || DATA.channels[0];
}

def('player', {
  cls: 'PlayerActivity', layout: 'activity_player', group: 'Player', screenClass: 'player-screen',
  desc: 'Activity riêng: supportsPictureInPicture, configChanges=orientation, launchMode=singleTask. PlayerView (use_controller=false) + layout_player_controller tự vẽ, tự ẩn sau 6 s. Dọc: video 16:9 + danh sách kênh; ngang: toàn màn + panel kênh bên phải.',
  landscape: (p) => !!p.land,
  immersive: (p) => !!p.land,
  onBack: (p) => { if (p.land) { setParams({ land: false, panel: false }); return false; } if (p.locked) { toast('Màn hình đang khóa. Bấm biểu tượng khóa để mở.'); return false; } },
  render: (p) => {
    const c = curChannel(p);
    const land = !!p.land;
    const pv = c.vod ? `linear-gradient(135deg, ${c.c[0]}, ${c.c[1]})` : `linear-gradient(135deg, ${logoColor(c.name)}, #0c0f18)`;
    const playing = p.paused ? false : true;
    const ctlHidden = p.hideCtl ? 'hidden' : '';
    let overlay = '';
    if (p.err) overlay = `<div class="player-err">${ic('wifiOff', 's32')}<b>Kênh không phát được</b>
        <p>Nguồn không phản hồi sau 3 lần thử. Kênh có thể đang tắt hoặc bị chặn theo khu vực.</p>
        <div class="row-btn"><button class="btn small tonal" onclick="setParams({err:false})">${ic('refresh', 's16')}Thử lại</button>
        <button class="btn small primary" onclick="setParams({err:false, ch:${(c.id % 42) + 1}})">Kênh tiếp theo ${ic('next', 's16')}</button></div></div>`;
    else if (p.buffering) overlay = `<div class="buffer"><div class="spinner"></div></div>`;
    const lockLayer = p.locked ? `<button class="lock-only" onclick="setParams({locked:false})" aria-label="Mở khóa">${ic('lock')}</button>` : '';
    const seek = c.vod
      ? `<span>1:10:24</span><div class="seek"><i style="width:62%"></i><b style="left:62%"></b></div><span>1:52:00</span>`
      : `<span class="live" style="height:18px">LIVE</span><div class="seek livebar"><i></i></div>`;
    const ctl = p.locked || p.err ? '' : `<div class="ctl ${ctlHidden}">
        <div class="top">
          <button class="icon-btn" onclick="back()">${ic('back')}</button>
          <div class="name"><b>${c.name}</b><small>${c.group} · ${c.q}</small></div>
          ${p.timer ? `<span class="chip" style="height:26px;background:rgba(0,0,0,.4);color:#fff;font-size:12px">${ic('timer', 's16')}${p.timer}:00</span>` : ''}
          <button class="icon-btn" onclick="toast('Chia sẻ link app')">${ic('share', 's20')}</button>
          <button class="icon-btn ${p.timer ? 'on' : ''}" onclick="openOverlay('timerSheet')">${ic('timer', 's20')}</button>
          ${c.id ? `<button class="icon-btn ${c.fav ? 'on' : ''}" onclick="toggleFav(${c.id})">${ic(c.fav ? 'heartFill' : 'heart', 's20')}</button>` : ''}
        </div>
        <div class="mid">
          ${c.vod ? `<button class="skip" onclick="toast('-10 giây')">${ic('rewind')}<small>10</small></button>` : `<button class="skip" onclick="setParams({ch:${Math.max(1, c.id - 1)}})" aria-label="Kênh trước">${ic('prev')}</button>`}
          <button class="big" onclick="setParams({paused:${playing}})">${ic(playing ? 'pause' : 'play')}</button>
          ${c.vod ? `<button class="skip" onclick="toast('+10 giây')">${ic('forward')}<small>10</small></button>` : `<button class="skip" onclick="setParams({ch:${(c.id % 42) + 1}})" aria-label="Kênh sau">${ic('next')}</button>`}
        </div>
        <div>
          <div class="bottom">${seek}<button class="icon-btn" onclick="setParams({land:${!land}, panel:false})">${ic(land ? 'exitFull' : 'fullscreen', 's20')}</button></div>
          ${land ? `<div class="tools-row">
            <button onclick="setParams({panel:!topParams().panel})" class="${p.panel ? 'on' : ''}">${ic('channels', 's20')}Kênh</button>
            <button onclick="openOverlay('subSheet')">${ic('subtitle', 's20')}Phụ đề</button>
            <button onclick="openOverlay('audioSheet')">${ic('audio', 's20')}Audio</button>
            <button onclick="openOverlay('aspectSheet')">${ic('aspect', 's20')}Tỉ lệ</button>
            <button onclick="enterPip()">${ic('pip', 's20')}PiP</button>
            <button onclick="openOverlay('castDialog')">${ic('cast', 's20')}Cast</button>
            <button onclick="setParams({locked:true})">${ic('lock', 's20')}Khóa</button></div>` : ''}
        </div></div>`;
    const gesture = p.gesture ? `<div class="gesture-pill">${ic(p.gesture === 'b' ? 'sun' : 'volume', 's20')}${p.gesture === 'b' ? 'Độ sáng' : 'Âm lượng'} 70%</div>` : '';
    const video = `<div class="video ${land ? 'land' : 'portrait'}" onclick="if(event.target.closest('button'))return; setParams({hideCtl:!topParams().hideCtl})">
        <div class="pic" style="--pv:${pv}"><span class="wm">${initials(c.name)}</span>${p.sub ? `<span class="caption">[Phụ đề: ${p.sub}] Xin chào các bạn…</span>` : ''}</div>
        ${ctl}${overlay}${lockLayer}${gesture}
        ${land && p.panel ? sidePanel(c) : ''}</div>`;
    if (land) return video;
    const list = DATA.channels.filter(x => x.playlistId === 1);
    return `${video}
      <div class="below">
        <div class="now">${c.vod ? '' : chLogo(c.name)}<div class="main"><b>${c.name}</b><small>${c.vod ? c.group : `${plName()} · ${c.group}`}</small></div>
          ${c.id ? `<button class="icon-btn boxed fav ${c.fav ? 'on' : ''}" onclick="toggleFav(${c.id})">${ic(c.fav ? 'heartFill' : 'heart')}</button>` : ''}</div>
        <div class="chips" style="padding-bottom:12px">
          <button class="chip" onclick="openOverlay('subSheet')">${ic('subtitle', 's16')}Phụ đề</button>
          <button class="chip" onclick="openOverlay('audioSheet')">${ic('audio', 's16')}Audio</button>
          <button class="chip" onclick="openOverlay('aspectSheet')">${ic('aspect', 's16')}Tỉ lệ</button>
          <button class="chip" onclick="enterPip()">${ic('pip', 's16')}PiP</button>
          <button class="chip" onclick="openOverlay('castDialog')">${ic('cast', 's16')}Cast</button>
          <button class="chip" onclick="setParams({locked:true})">${ic('lock', 's16')}Khóa</button>
        </div>
        ${c.vod ? `<div class="pad"><div class="card"><b>Xem tiếp từ 1:10:24</b><p class="hint" style="margin-top:4px">Vị trí được lưu mỗi 10 giây và khi thoát.</p></div></div>` : `
        <div class="sec-h" style="padding-top:4px"><h3>Kênh trong playlist</h3><button onclick="openOverlay('chListSheet')">Tất cả</button></div>
        <div class="content" style="flex:1" id="plist"><div class="list-card">${list.map((x, i) => `<div class="row ${x.id === c.id ? 'cur' : ''}" style="${x.id === c.id ? 'background:var(--accent-soft)' : ''}" onclick="setParams({ch:${x.id}, err:false})">
          <span class="num">${i + 1}</span>${chLogo(x.name)}<div class="main"><b style="${x.id === c.id ? 'color:var(--accent)' : ''}">${x.name}</b><small>${x.group}</small></div>
          ${x.id === c.id ? '<span class="eq" style="margin-right:12px"><i></i><i></i><i></i></span>' : ''}</div>`).join('')}</div><div class="spacer"></div></div>`}
      </div>`;
  },
  mount: (el, p) => {
    const cur = el.querySelector('#plist .row.cur');
    if (cur) { const sc = el.querySelector('#plist'); sc.scrollTop += cur.getBoundingClientRect().top - sc.getBoundingClientRect().top - 8; }
    if (!p.err && !p.locked && !p.hideCtl && !p._timerSet) {
      p._timerSet = true;
      clearTimeout(window._hideT);
      window._hideT = setTimeout(() => { const t = state.stack[state.stack.length - 1]; if (t.id === 'player' && !state.overlays.length && !t.params.panel) { t.params.hideCtl = true; t.params._timerSet = false; refresh(); } }, 6000);
    }
    if (p.hideCtl) p._timerSet = false;
  },
});

function sidePanel(c) {
  const list = DATA.channels.filter(x => x.playlistId === 1);
  return `<div class="side-panel" onclick="event.stopPropagation()"><div class="toolbar" style="height:48px"><div class="title"><b style="font-size:17px">${plName()}</b></div>
    <button class="icon-btn" onclick="setParams({panel:false})">${ic('close', 's20')}</button></div>
    <div class="content">${list.map((x, i) => `<div class="row ${x.id === c.id ? 'cur' : ''}" onclick="setParams({ch:${x.id}})"><span class="num">${i + 1}</span>${chLogo(x.name)}<div class="main"><b>${x.name}</b><small>${x.group}</small></div></div>`).join('')}</div></div>`;
}

function enterPip() {
  const c = curChannel(topParams());
  const pv = c.vod ? `linear-gradient(135deg, ${c.c[0]}, ${c.c[1]})` : `linear-gradient(135deg, ${logoColor(c.name)}, #0c0f18)`;
  resetTo('main');
  const el = document.createElement('div');
  el.className = 'pip-demo';
  el.innerHTML = `<div class="video land" style="position:absolute;inset:0"><div class="pic" style="--pv:${pv}"><span class="wm">${initials(c.name)}</span></div>
    <div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;gap:18px;background:rgba(0,0,0,.25);color:#fff">
      ${ic('prev', 's20')}${ic('pause')}${ic('next', 's20')}</div>
    <button class="icon-btn" style="position:absolute;top:0;right:0;color:#fff;width:32px;height:32px">${ic('close', 's16')}</button></div>`;
  el.querySelector('.icon-btn').onclick = (e) => { e.stopPropagation(); el.remove(); };
  el.onclick = () => { el.remove(); go('player', { ch: c.id || 1 }); };
  document.getElementById('phone').appendChild(el);
  toast('Picture-in-picture: điều khiển Play/Pause, kênh trước/sau');
}

/* ---------------- sheets ---------------- */
function optSheet(title, opts, cur, key, extra = '') {
  return `<div class="sheet"><div class="grab"></div><div class="sheet-h"><span style="width:40px"></span><h3>${title}</h3>
    <button class="icon-btn" onclick="closeOverlay()">${ic('close')}</button></div>
    <div class="sheet-body opt-list">${opts.map(o => `<button class="${cur === o[0] ? 'on' : ''}" onclick="closeOverlay(); setParams({${key}:${o[0] === null ? 'null' : `'${o[0]}'`}})">
      <span class="grow">${o[1]}${o[2] ? `<small>${o[2]}</small>` : ''}</span>${cur === o[0] ? ic('check', 's20') : ''}</button>`).join('')}${extra}</div></div>`;
}
def('timerSheet', {
  kind: 'BottomSheet', cls: 'TimerSheet', layout: 'sheet_timer', group: 'Player',
  desc: 'Hết giờ thì pause. Timer nằm trong PlayerManager nên không mất khi xoay màn / vào PiP. Chip đếm ngược hiện trên thanh trên.',
  render: () => optSheet('Hẹn giờ tắt', [[null, 'Tắt'], ['15', '15 phút'], ['30', '30 phút'], ['45', '45 phút'], ['60', '1 giờ']], topParams().timer || null, 'timer'),
});
def('subSheet', {
  kind: 'BottomSheet', cls: 'SubtitleSheet', layout: 'sheet_subtitle', group: 'Player',
  render: () => optSheet('Phụ đề', [[null, 'Tắt'], ['Tiếng Việt', 'Tiếng Việt'], ['English', 'English', 'CC']], topParams().sub || null, 'sub'),
});
def('audioSheet', {
  kind: 'BottomSheet', cls: 'AudioTrackSheet', layout: 'sheet_audio', group: 'Player',
  desc: 'App gốc không có. Chọn audio track + chất lượng video (DefaultTrackSelector).',
  render: () => optSheet('Âm thanh & chất lượng', [['a1', 'Tiếng Việt', 'AAC stereo'], ['a2', 'English', 'AAC 5.1']], topParams().audio || 'a1', 'audio',
    `<div class="divider"></div><p class="hint" style="padding:6px 12px">Chất lượng video</p>
     <div class="chips" style="padding:4px 12px 8px"><button class="chip on">Tự động</button><button class="chip">1080p</button><button class="chip">720p</button><button class="chip">480p</button></div>`),
});
def('aspectSheet', {
  kind: 'BottomSheet', cls: 'AspectRatioSheet', layout: 'sheet_aspect', group: 'Player',
  desc: 'App gốc không có. Map sang PlayerView.resizeMode + tỉ lệ ép.',
  render: () => optSheet('Tỉ lệ khung hình', [['fit', 'Vừa màn hình', 'Giữ nguyên tỉ lệ'], ['fill', 'Lấp đầy', 'Cắt bớt viền'], ['zoom', 'Phóng to'], ['16:9', '16:9'], ['4:3', '4:3']], topParams().aspect || 'fit', 'aspect'),
});
def('chListSheet', {
  kind: 'BottomSheet', cls: 'ChannelListSheet', layout: 'sheet_channel_list', group: 'Player',
  render: () => { const cur = topParams().ch; const list = DATA.channels.filter(x => x.playlistId === 1);
    return `<div class="sheet" style="height:80%"><div class="grab"></div><div class="sheet-h"><span style="width:40px"></span><h3>${plName()}</h3><button class="icon-btn" onclick="closeOverlay()">${ic('close')}</button></div>
      <div class="pad" style="padding-bottom:8px"><label class="search">${ic('search', 's20')}<input placeholder="Tìm kênh"></label></div>
      <div class="sheet-body" style="padding:0">${list.map((x, i) => `<div class="row" style="${x.id === cur ? 'background:var(--accent-soft)' : ''}" onclick="closeOverlay(); setParams({ch:${x.id}})"><span class="num">${i + 1}</span>${chLogo(x.name)}<div class="main"><b>${x.name}</b><small>${x.group}</small></div></div>`).join('')}</div></div>`; },
});
def('castDialog', {
  kind: 'Dialog', cls: 'Cast (system settings)', group: 'Player',
  desc: 'Như app gốc: mở Settings.ACTION_CAST_SETTINGS / WIFI_DISPLAY_SETTINGS. Không cần xem quảng cáo. Google Cast SDK để sau.',
  render: () => `<div class="dialog"><div class="hero-ic">${ic('cast', 's32')}</div><h3>Chiếu lên TV</h3>
    <p>Mở phần chiếu màn hình của Android, chọn TV cùng mạng Wi-Fi rồi quay lại app để xem.</p>
    <div class="actions"><button class="btn tonal" onclick="closeOverlay()">Hủy</button><button class="btn primary" onclick="closeOverlay(); toast('Mở Cài đặt › Truyền màn hình')">Mở cài đặt</button></div></div>`,
});
