// Mini router that mimics Android: Activity stack, tab fragments, dialogs/sheets on top.
const SCREENS = {};   // id -> { kind, cls, layout, title, group, desc, render(params) -> html, mount?(el, params) }
const state = { stack: [], overlays: [], firstRun: true, tab: 'home', xtTab: 'movie', sportOn: true, orientation: 'portrait' };

function def(id, spec) { SCREENS[id] = Object.assign({ id, kind: 'Activity', group: 'Khác' }, spec); }

const vp = () => document.getElementById('vp');
const phoneEl = () => document.getElementById('phone');

function renderTop() {
  const top = state.stack[state.stack.length - 1];
  if (!top) return;
  const s = SCREENS[top.id];
  const el = document.createElement('div');
  el.className = 'screen' + (s.screenClass ? ' ' + s.screenClass : '');
  el.dataset.id = top.id;
  el.innerHTML = s.render(top.params || {});
  vp().innerHTML = '';
  vp().appendChild(el);
  const ph = phoneEl();
  ph.classList.toggle('landscape', !!(s.landscape && s.landscape(top.params || {})));
  ph.classList.toggle('immersive', !!(s.immersive && s.immersive(top.params || {})));
  if (s.mount) s.mount(el, top.params || {});
  state.overlays.forEach(o => mountOverlay(o, false));
  updateInfo();
}

function go(id, params = {}) {
  closeAllOverlays(false);
  state.stack.push({ id, params });
  renderTop();
}
function replace(id, params = {}) {
  closeAllOverlays(false);
  state.stack.pop();
  state.stack.push({ id, params });
  renderTop();
}
function resetTo(id, params = {}) {
  closeAllOverlays(false);
  state.stack = [{ id, params }];
  renderTop();
}
function back() {
  if (state.overlays.length) { closeOverlay(); return; }
  const top = state.stack[state.stack.length - 1];
  const s = top && SCREENS[top.id];
  if (s && s.onBack && s.onBack(top.params) === false) return;
  if (state.stack.length > 1) { state.stack.pop(); renderTop(); }
  else toast('Nhấn Back lần nữa để thoát');
}
function refresh() { renderTop(); }
function setParams(patch) { const top = state.stack[state.stack.length - 1]; Object.assign(top.params, patch); renderTop(); }
function topParams() { return state.stack[state.stack.length - 1].params; }

/* ---------- overlays (DialogFragment / BottomSheetDialogFragment / PopupWindow) ---------- */
function openOverlay(id, params = {}) {
  const o = { id, params };
  state.overlays.push(o);
  mountOverlay(o, true);
  updateInfo();
}
function mountOverlay(o, fresh) {
  const s = SCREENS[o.id];
  const wrap = document.createElement('div');
  wrap.className = 'overlay ' + (s.full ? 'full' : s.kind === 'Dialog' ? 'center' : '') + (s.kind === 'Popup' ? ' clear' : '');
  if (!fresh) wrap.style.animation = 'none';
  wrap.dataset.ov = o.id;
  wrap.innerHTML = s.render(o.params);
  wrap.addEventListener('click', e => { if (e.target === wrap && s.cancelable !== false) closeOverlay(); });
  if (!fresh) { const inner = wrap.firstElementChild; if (inner) inner.style.animation = 'none'; }
  vp().appendChild(wrap);
  if (s.mount) s.mount(wrap, o.params);
}
function closeOverlay() {
  const o = state.overlays.pop();
  const el = [...vp().querySelectorAll('.overlay')].pop();
  if (el) el.remove();
  updateInfo();
  return o;
}
function closeAllOverlays(render = true) {
  state.overlays = [];
  vp().querySelectorAll('.overlay').forEach(e => e.remove());
  if (render) updateInfo();
}
function refreshOverlay() {
  const o = state.overlays[state.overlays.length - 1];
  const el = [...vp().querySelectorAll('.overlay')].pop();
  if (!o || !el) return;
  const s = SCREENS[o.id];
  el.innerHTML = s.render(o.params);
  const inner = el.firstElementChild; if (inner) inner.style.animation = 'none';
  if (s.mount) s.mount(el, o.params);
}

/* ---------- toast / snackbar ---------- */
function toast(msg, ms = 1800) {
  const t = document.createElement('div');
  t.className = 'toast'; t.textContent = msg;
  const layer = document.getElementById('toasts');
  layer.innerHTML = ''; layer.appendChild(t);
  setTimeout(() => t.remove(), ms);
}
function snackbar(msg, action, fn, ms = 5000) {
  const layer = document.getElementById('toasts');
  layer.innerHTML = `<div class="snackbar"><span>${msg}</span><button>${action}</button></div>`;
  layer.querySelector('button').onclick = () => { layer.innerHTML = ''; fn(); };
  setTimeout(() => { if (layer.firstChild && layer.firstChild.classList.contains('snackbar')) layer.innerHTML = ''; }, ms);
}

/* ---------- info panel ---------- */
function updateInfo() {
  const info = document.getElementById('info');
  if (!info) return;
  const top = state.stack[state.stack.length - 1];
  const s = top && SCREENS[top.id];
  const ov = state.overlays[state.overlays.length - 1];
  const so = ov && SCREENS[ov.id];
  const block = (x) => x ? `<div class="blk"><span class="kind ${x.kind}">${x.kind}</span><div class="cls">${x.cls || x.id}</div>
    ${x.layout ? `<code>res/layout/${x.layout}.xml</code>` : ''}${x.desc ? `<p>${x.desc}</p>` : ''}</div>` : '';
  let tabFrag = '';
  if (s && s.fragment) { const f = s.fragment(top.params || {}); if (f) tabFrag = block(f); }
  info.innerHTML = `
    <h2>Đang xem</h2>
    ${block(s)}${tabFrag}${block(so)}
    <h2 style="margin-top:18px">Back stack</h2>
    <div class="stack-list">${state.stack.map(x => `<div>${SCREENS[x.id].cls || x.id}</div>`).join('')}
      ${state.overlays.map(x => `<div>↳ ${SCREENS[x.id].cls || x.id}</div>`).join('')}</div>
    ${window.renderScenarioSteps ? window.renderScenarioSteps() : ''}`;
  if (window.onNav) window.onNav(top, ov);
}

/* ---------- helpers used by screens ---------- */
function toolbar({ title, sub, backBtn = true, right = '', center = false }) {
  return `<div class="toolbar ${center ? 'center' : ''}">
    ${backBtn ? `<button class="icon-btn" onclick="back()" aria-label="Back">${ic('back')}</button>` : ''}
    <div class="title"><b>${title}</b>${sub ? `<small>${sub}</small>` : ''}</div>${right}</div>`;
}
function esc(s) { return String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c])); }
function onEnter(e, fn) { if (e.key === 'Enter') fn(); }

document.addEventListener('keydown', e => { if (e.key === 'Escape') back(); });
document.addEventListener('DOMContentLoaded', () => {
  const sb = document.getElementById('sbic');
  if (sb) sb.innerHTML = ic('signal', 's16') + ic('wifi', 's16') + ic('battery', 's20');
});
