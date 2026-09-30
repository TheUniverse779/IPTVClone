/* ===== Community: GetLink / MyShare / Favorite + ShareDialog ===== */

const COMM_TABS = [['iptv', 'Playlist'], ['xtream', 'Xtream'], ['single', 'Stream']];
function commItem(type, it) {
  const title = type === 'xtream' ? it.server.replace(/^https?:\/\//, '') : it.name;
  const sub = type === 'xtream' ? `User: ${it.user}` : it.url;
  const use = type === 'xtream' ? `go('profileEdit',{srv:'${it.server}', user:'${it.user}', pass:'${it.pass}', name:'Community ${it.user}'})` : type === 'single' ? `go('player',{single:1})` : `go('import',{tab:'url', url:'${it.url}'})`;
  return `<div class="card" style="padding:12px 12px 10px">
    <div style="display:flex;gap:12px;align-items:center"><div class="logo" style="background:var(--surface-3);color:var(--accent)">${ic(type === 'xtream' ? 'xtream' : type === 'single' ? 'playCircle' : 'list', 's20')}</div>
      <div style="flex:1;min-width:0"><b style="display:block;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${title}</b><small class="hint" style="display:block;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${sub}</small></div>
      <button class="icon-btn fav ${it.saved ? 'on' : ''}" onclick="event.stopPropagation(); this.classList.toggle('on'); toast('Đã lưu')">${ic(it.saved ? 'bookmarkFill' : 'bookmark', 's20')}</button></div>
    <div style="display:flex;gap:8px;margin-top:10px"><button class="btn small primary" style="flex:1" onclick="${use}">${type === 'single' ? 'Phát' : 'Dùng link này'}</button>
      <button class="btn small tonal" onclick="toast('Đã copy')">${ic('copy', 's16')}</button>
      <button class="btn small tonal" onclick="openOverlay('confirm',{title:'Báo cáo link?',msg:'Link chết, sai nội dung hoặc vi phạm bản quyền. Link bị nhiều người báo cáo sẽ tự ẩn.',ok:'Báo cáo',then:()=>toast('Đã gửi báo cáo')})">${ic('flag', 's16')}</button></div></div>`;
}

def('community', {
  cls: 'GetLinkActivity', layout: 'activity_get_link', group: 'Community',
  desc: 'Link người dùng chia sẻ (Firebase Realtime DB). Tab lọc = TabLayout, không Fragment. Có Report + ngưỡng tự ẩn.',
  render: (p) => {
    const tab = p.tab || 'iptv';
    const list = DATA.community[tab];
    return `${toolbar({ title: 'Community', right: `<button class="icon-btn" onclick="go('myShare')">${ic('share')}</button><button class="icon-btn" onclick="go('commFav')">${ic('bookmark')}</button>` })}
      <div class="pad" style="padding-bottom:12px"><div class="seg">${COMM_TABS.map(([k, l]) => `<button class="${tab === k ? 'on' : ''}" onclick="setParams({tab:'${k}'})">${l}</button>`).join('')}</div></div>
      <div class="content"><div class="stack pad">${list.map(it => commItem(tab, it)).join('')}</div>
        <p class="hint" style="padding:14px 16px">Link do người dùng chia sẻ, không phải của app. Hãy báo cáo link chết hoặc vi phạm.</p></div>`;
  },
});
def('myShare', {
  cls: 'MyShareActivity', layout: 'activity_my_share', group: 'Community',
  render: () => `${toolbar({ title: 'Link tôi đã chia sẻ' })}
    <div class="content"><div class="empty"><div class="art">${ic('share', 's32')}</div><b>Bạn chưa chia sẻ link nào</b><p>Chia sẻ playlist, Xtream hoặc stream để người khác dùng.</p>
      <button class="btn primary" onclick="openOverlay('shareDialog',{type:'iptv'})">Chia sẻ link</button></div></div>`,
});
def('commFav', {
  cls: 'CommunityFavoriteActivity', layout: 'activity_community_favorite', group: 'Community',
  render: () => `${toolbar({ title: 'Đã lưu' })}<div class="content"><div class="stack pad">${commItem('iptv', DATA.community.iptv[1])}</div></div>`,
});
def('shareDialog', {
  kind: 'Dialog', cls: 'ShareDialog', layout: 'dialog_share', group: 'Community',
  render: (p) => `<div class="dialog" style="text-align:left"><h3 style="text-align:center">Chia sẻ lên Community</h3><div style="height:14px"></div>
    <div class="seg" style="margin-bottom:14px">${COMM_TABS.map(([k, l]) => `<button class="${p.type === k ? 'on' : ''}" onclick="state.overlays[state.overlays.length-1].params.type='${k}'; refreshOverlay()">${l}</button>`).join('')}</div>
    ${p.type === 'xtream' ? `<label class="field"><span class="label">Server URL</span><span class="input"><input value="http://line.example.tv:8080"></span></label>
      <label class="field"><span class="label">Username</span><span class="input"><input value="demo_user"></span></label>`
    : `<label class="field"><span class="label">Tên</span><span class="input"><input value="Vietnam free TV"></span></label>
      <label class="field"><span class="label">URL</span><span class="input"><input value="https://iptv-org.github.io/iptv/countries/vn.m3u"></span></label>`}
    <p class="hint">Chỉ chia sẻ link bạn có quyền chia sẻ. Link vi phạm sẽ bị gỡ.</p>
    <div class="actions"><button class="btn tonal" onclick="closeOverlay()">Hủy</button><button class="btn primary" onclick="closeOverlay(); toast('Đã chia sẻ')">Chia sẻ</button></div></div>`,
});
