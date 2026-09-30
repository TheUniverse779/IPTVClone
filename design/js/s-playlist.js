/* ===== PlaylistDetailActivity / SortPopup / SearchActivity ===== */

const SORTS = { order_asc: 'Thứ tự gốc', order_desc: 'Thứ tự gốc (ngược)', az: 'Tên A → Z', za: 'Tên Z → A', count: 'Nhiều kênh nhất' };

function sortChannels(list, s) {
  const a = [...list];
  if (s === 'az') a.sort((x, y) => x.name.localeCompare(y.name));
  if (s === 'za') a.sort((x, y) => y.name.localeCompare(x.name));
  if (s === 'order_desc') a.reverse();
  return a;
}

def('playlist', {
  cls: 'PlaylistDetailActivity', layout: 'activity_playlist_detail', group: 'Playlist',
  desc: 'Tab Category / Channels = TabLayout + 2 RecyclerView ẩn/hiện (không Fragment). Bấm 1 nhóm → sang tab Channels đã lọc. Danh sách lớn dùng Paging 3.',
  render: (p) => {
    const pl = DATA.playlists.find(x => x.id === p.id) || DATA.playlists[0];
    const tab = p.tab || 'groups';
    const view = p.view || 'list';
    const sort = p.sort || 'order_asc';
    const q = (p.q || '').toLowerCase();
    const all = DATA.channels.filter(c => c.playlistId === 1);
    let body = '';
    if (tab === 'groups') {
      const gs = groups(all).filter(g => g[0].toLowerCase().includes(q));
      if (p.gsort === 'count') gs.sort((a, b) => b[1] - a[1]);
      body = `<div class="list-card group-row">
        <div class="row" onclick="setParams({tab:'channels', group:null})"><div class="main"><b>Tất cả kênh</b></div><span class="count">${all.length}</span>${ic('chevron', 's20')}</div>
        ${gs.map(([g, n]) => `<div class="row" onclick="setParams({tab:'channels', group:'${g}'})"><div class="main"><b>${g}</b></div><span class="count">${n}</span>${ic('chevron', 's20')}</div>`).join('')}</div>`;
    } else {
      let list = all.filter(c => (!p.group || c.group === p.group) && c.name.toLowerCase().includes(q));
      list = sortChannels(list, sort);
      const filter = `<div class="filter-bar">${p.group ? `<button class="chip on" onclick="setParams({group:null})">${p.group} ${ic('close', 's16')}</button>` : ''}
        <span class="hint">${list.length} kênh · ${SORTS[sort]}</span></div>`;
      if (!list.length) body = filter + `<div class="empty"><div class="art">${ic('search', 's32')}</div><b>Không có kênh "${esc(p.q)}"</b><p>Thử tên khác, hoặc tìm trên mọi playlist.</p>
        <button class="btn tonal small" onclick="go('search',{q:'${esc(p.q)}'})">Tìm trên mọi playlist</button></div>`;
      else if (view === 'grid') body = filter + `<div class="grid">${list.map(chTile).join('')}</div>`;
      else body = filter + `<div class="list-card">${list.map((c, i) => chRow(c, i)).join('')}</div>`;
    }
    const toolsRight = tab === 'channels'
      ? `<button class="icon-btn boxed" onclick="setParams({view:'${view === 'list' ? 'grid' : 'list'}'})" aria-label="Đổi kiểu xem">${ic(view === 'list' ? 'grid' : 'rows')}</button>
         <button class="icon-btn boxed" onclick="openSort(this)" aria-label="Sắp xếp">${ic('sort')}</button>`
      : `<button class="icon-btn boxed" onclick="setParams({gsort: topParams().gsort==='count'?'name':'count'}); toast(topParams().gsort==='count'?'Sắp xếp nhóm theo số kênh':'Sắp xếp nhóm theo tên')" aria-label="Sắp xếp nhóm">${ic('sort')}</button>`;
    return `${toolbar({ title: pl.name, sub: `${pl.count} kênh · ${pl.updated}`, right: `<button class="icon-btn" onclick="openPlMenu(this, ${pl.id})">${ic('more')}</button>` })}
      <div class="tools"><label class="search">${ic('search', 's20')}<input placeholder="${tab === 'groups' ? 'Tìm nhóm' : 'Tìm kênh'}" value="${esc(p.q || '')}" oninput="topParams().q=this.value; clearTimeout(window._qt); window._qt=setTimeout(()=>{refresh(); const i=document.querySelector('.search input'); i.focus(); i.setSelectionRange(99,99);},300)"></label>${toolsRight}</div>
      <div class="pad" style="padding-bottom:12px"><div class="seg">
        <button class="${tab === 'groups' ? 'on' : ''}" onclick="setParams({tab:'groups'})">Category</button>
        <button class="${tab === 'channels' ? 'on' : ''}" onclick="setParams({tab:'channels'})">Channels</button></div></div>
      <div class="content">${body}<div class="spacer"></div></div>`;
  },
});

function chRow(c, i) {
  return `<div class="row" onclick="go('player',{ch:${c.id}})"><span class="num">${i + 1}</span>${chLogo(c.name)}
    <div class="main"><b>${c.name}</b><small>${c.group} · ${c.q}</small></div>
    <div class="trail"><button class="icon-btn fav ${c.fav ? 'on' : ''}" onclick="event.stopPropagation(); toggleFav(${c.id})">${ic(c.fav ? 'heartFill' : 'heart', 's20')}</button>
    <button class="icon-btn" onclick="event.stopPropagation(); openChMenu(this, ${c.id})">${ic('more', 's20')}</button></div></div>`;
}

function openSort(btn) {
  const r = btn.getBoundingClientRect(), v = vp().getBoundingClientRect();
  openOverlay('sortPopup', { top: r.bottom - v.top + 4, right: v.right - r.right });
}
def('sortPopup', {
  kind: 'Popup', cls: 'SortPopup (PopupWindow)', group: 'Playlist',
  desc: 'App gốc: 0-9 và A-Z, mỗi loại bấm lại để đảo chiều. Bản clone hiện rõ 4 lựa chọn.',
  render: (p) => {
    const cur = (state.stack[state.stack.length - 1].params.sort) || 'order_asc';
    return `<div class="popup" style="top:${p.top}px; right:${p.right}px"><h4>Sắp xếp theo</h4>
      ${['order_asc', 'order_desc', 'az', 'za'].map(k => `<button class="${cur === k ? 'on' : ''}" onclick="closeOverlay(); setParams({sort:'${k}'})">${cur === k ? ic('check', 's20') : '<span style="width:20px"></span>'}${SORTS[k]}</button>`).join('')}</div>`;
  },
});

function openChMenu(btn, id) {
  const r = btn.getBoundingClientRect(), v = vp().getBoundingClientRect();
  openOverlay('chMenu', { id, top: Math.min(r.bottom - v.top, 560), right: v.right - r.right });
}
def('chMenu', {
  kind: 'Popup', cls: 'PopupMenu (kênh)', group: 'Playlist',
  render: (p) => `<div class="popup" style="top:${p.top}px; right:${p.right}px">
    <button onclick="closeOverlay(); toast('Đổi tên kênh')">${ic('edit', 's20')}Đổi tên</button>
    <button onclick="closeOverlay(); toast('Đã khóa kênh')">${ic('lock', 's20')}Khóa kênh</button>
    <button onclick="closeOverlay(); toast('Đã copy link stream')">${ic('copy', 's20')}Copy link stream</button>
    <button class="danger" onclick="closeOverlay(); toast('Đã xóa kênh')">${ic('trash', 's20')}Xóa khỏi playlist</button></div>`,
});

/* ---------------- Search ---------------- */
def('search', {
  cls: 'SearchActivity', layout: 'activity_search', group: 'Playlist',
  desc: 'Tìm kênh trên mọi playlist + single stream. Lịch sử tìm kiếm lưu trong Room (search_history).',
  render: (p) => {
    const q = (p.q || '').trim().toLowerCase();
    const res = q ? DATA.channels.filter(c => c.name.toLowerCase().includes(q)) : [];
    const hist = ['htv', 'sports', 'news'];
    let body;
    if (!q) body = `<div class="sec-h"><h3>Tìm gần đây</h3><button onclick="toast('Đã xóa lịch sử')">Xóa</button></div>
      <div class="list-card">${hist.map(h => `<div class="row" onclick="setParams({q:'${h}'})">${ic('history', 's20')}<div class="main"><b style="font-weight:500">${h}</b></div>${ic('close', 's16')}</div>`).join('')}</div>`;
    else if (!res.length) body = `<div class="empty"><div class="art">${ic('search', 's32')}</div><b>Không tìm thấy "${esc(p.q)}"</b><p>Kiểm tra chính tả, hoặc thêm playlist có kênh này.</p></div>`;
    else body = `<p class="hint" style="padding:0 16px 8px">${res.length} kênh trong ${plName()}</p><div class="list-card">${res.map((c, i) => chRow(c, i)).join('')}</div>`;
    return `<div class="toolbar"><button class="icon-btn" onclick="back()">${ic('back')}</button>
        <label class="search" style="margin-right:8px">${ic('search', 's20')}<input autofocus placeholder="Tên kênh, nhóm…" value="${esc(p.q || '')}"
          oninput="topParams().q=this.value; clearTimeout(window._qt); window._qt=setTimeout(()=>{refresh(); const i=document.querySelector('.search input'); i.focus(); i.setSelectionRange(99,99);},300)"></label></div>
      <div class="content">${body}<div class="spacer"></div></div>`;
  },
});
