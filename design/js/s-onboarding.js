/* ===== Luồng lần đầu: Language → Onboarding ×4 → Main, kèm các vị trí quảng cáo =====
   Dựng theo đúng layout của TranslateApp:
     • ab_activity_language_app.xml  – danh sách ngôn ngữ + nút + native ở đáy
     • ab_fragment_on_boarding_1..4  – ảnh trên, tiêu đề, mô tả, dots + NEXT, native ở đáy
     • ab_ad_unified_language.xml    – CTA 48dp → icon 36dp + headline + "Ad" → body → media 112dp
     • ad_unified_full_obd.xml       – media → icon 50dp + headline + body → CTA 52dp (phủ toàn trang)
   Ảnh onboarding: design/img/img1.png → trang 1, img2.png → trang 2, img3.png → trang cuối.
   Màn Language và 4 trang onboarding ẩn cả thanh trạng thái lẫn thanh điều hướng (immersive).

   Quảng cáo: Language (NATIVE_LANGUAGES1) · Onboarding 1 (NATIVE_OBD1) · Onboarding 3 phủ toàn trang
   (NATIVE_OBD_FULL) · Onboarding 4 (NATIVE_OBD3) · trước Main (NATIVE_INTER) · Splash (INTER_SPLASH)
   · quay lại app (APP_OPEN) · banner ở đáy mọi màn. Nút "Quảng cáo" ở thanh dưới để bật/tắt. */

const OBD = [
  { art: 'img/img1.png', title: 'Thêm nguồn của bạn', body: 'Dán link playlist M3U, nhập tài khoản từ nhà cung cấp hoặc mở một link stream.', ad: 'bottom', unit: 'NATIVE_OBD1' },
  { art: 'img/img2.png', title: 'Phim và series theo thể loại', body: 'Poster theo từng thể loại, xem tiếp đúng chỗ đang dở, tìm kiếm trong mọi nguồn.', ad: 'none', unit: null },
  { art: null, title: '', body: '', ad: 'full', unit: 'NATIVE_OBD_FULL' },
  { art: 'img/img3.png', title: 'Kênh Live và danh sách kênh', body: 'Xem kênh toàn màn hình, danh sách kênh ngay bên dưới để đổi kênh thật nhanh.', ad: 'bottom', unit: 'NATIVE_OBD3' },
];

state.adsOn = true;

/** Nút "Quảng cáo" ở thanh dưới: xem luồng khi có / không có quảng cáo. */
function toggleAds() {
  state.adsOn = !state.adsOn;
  const b = document.getElementById('adsToggle');
  if (b) b.textContent = 'Quảng cáo: ' + (state.adsOn ? 'Bật' : 'Tắt');
  refresh();
}

/** Splash gọi hàm này ở lần mở đầu: có quảng cáo thì qua interstitial trước. */
function startFirstRun() {
  if (state.adsOn) replace('adSplashInter', { next: 'langapp' });
  else replace('langapp');
}

/* ---------- quảng cáo ---------- */

/**
 * Native dạng thẻ (ab_ad_unified_language.xml): CTA 48dp, rồi icon 36dp + headline + "Ad" + nhà quảng cáo,
 * rồi mô tả, rồi ảnh media 112dp. Trong app đây là 1 thẻ do AdMob trả về; nội dung dưới chỉ là mẫu.
 */
function adNativeInline(unit, ctaStyle) {
  if (!state.adsOn) return '';
  return `<div class="adn-wrap">
    <div class="adn-tag">Quảng cáo · ${unit}</div>
    <div class="adn">
    <button class="adn-cta ${ctaStyle === 'grey' ? 'off' : ''}">Mở</button>
    <div class="adn-row">
      <div class="adn-icon">${ic('tv')}</div>
      <div class="adn-head">
        <b>Ứng dụng xem truyền hình</b>
        <div class="adn-meta"><span class="adn-badge">Ad</span><span>Nhà quảng cáo</span></div>
      </div>
    </div>
    <p class="adn-body">Nội dung do Google AdMob trả về. App không kiểm soát nội dung này.</p>
    <div class="adn-media">${ic('movie', 's32')}</div>
    </div>
  </div>`;
}

/**
 * Native phủ toàn trang (ad_unified_full_obd.xml): media trên, icon 50dp + headline + body, CTA 52dp ở đáy.
 * Dùng cho trang onboarding 3 và cho "interstitial native" giữa các màn.
 */
function adNativeFull(unit, opts = {}) {
  return `<div class="adf">
    <div class="adf-bar"><span class="ad-tag">${opts.label || 'Native toàn màn hình'} · ${unit}</span>
      ${opts.noClose ? '' : `<button class="adf-x" onclick="adNext()">✕</button>`}</div>
    <div class="adf-media">${ic('tv', 's48')}</div>
    <div class="adf-row">
      <div class="adf-icon">${ic('tv')}</div>
      <div class="adf-head"><b>Ứng dụng xem truyền hình</b>
        <div class="adn-meta"><span class="adn-badge">Ad</span><span>Nhà quảng cáo</span></div></div>
    </div>
    <p class="adf-body">Quảng cáo toàn màn hình do Google AdMob trả về, kèm ảnh và nút hành động của nhà quảng cáo.</p>
    <div class="spacer"></div>
    <button class="adf-cta">Xem chi tiết</button>
  </div>`;
}

/** Interstitial / App Open: quảng cáo toàn màn hình, không có nội dung native phía sau. */
function adInterstitial(unit, label) {
  return `<div class="adi">
    <div class="adf-bar"><span class="ad-tag">${label} · ${unit}</span>
      <button class="adf-x" onclick="adNext()">✕</button></div>
    <div class="spacer"></div>
    <div class="adi-logo">${ic('tv', 's48')}</div>
    <b class="adi-name">IPTV Smart Player</b>
    <p class="adi-sub">${label === 'App Open' ? 'Quảng cáo hiện khi bạn quay lại app.' : 'Quảng cáo toàn màn hình khi mở app.'}</p>
    <div class="spacer"></div>
    <button class="adf-cta">Tải ngay</button>
    <div class="adi-foot">Tự đóng sau 5 giây</div>
  </div>`;
}

/** Sau khi quảng cáo đóng: đi tiếp tới màn kế của luồng. */
function adNext() {
  const p = topParams();
  replace(p.next || 'main', p.nextParams || {});
}

/** Vào màn quảng cáo chỉ khi đang bật; tắt thì đi thẳng tới màn kế. */
function adGo(id, params) {
  if (!state.adsOn) { const p = params || {}; replace(p.next || 'main', p.nextParams || {}); return; }
  replace(id, params);
}

/** Dải banner AdMob ở đáy activity — nằm dưới cả thanh tab. */
function adBannerStrip() {
  if (!state.adsOn) return '';
  return `<div class="ad-banner">
    <span class="ad-banner-tag">Test Ad</span>
    <div class="ad-banner-body"><b>AdMob Adaptive Banner</b><small>Banner luôn ở đáy màn hình</small></div>
    <button class="ad-banner-cta">OPEN</button></div>`;
}

/* ---------- màn hình ---------- */

def('adSplashInter', {
  kind: 'Mock', cls: 'InterstitialAd (splash)', group: 'Khởi động & Quảng cáo',
  desc: 'Splash: xin consent UMP → nạp interstitial → hiện quảng cáo này → xong mới đi tiếp. Tải lỗi, chưa đủ thời gian chờ hoặc mạng yếu thì bỏ qua và đi thẳng.',
  render: () => adInterstitial('INTER_SPLASH', 'Interstitial'),
});

def('langapp', {
  cls: 'LanguageAppActivity', layout: 'activity_language_app', group: 'Khởi động & Quảng cáo',
  immersive: () => true,
  desc: 'Màn chọn ngôn ngữ: danh sách cuộn ở trên, khối quảng cáo native neo ở ĐÁY màn hình. Không có nút Continue — nút sang màn sau là dấu tick trên thanh tiêu đề, ẩn cho tới khi bạn chọn một ngôn ngữ. Màn này ẩn cả thanh trạng thái và thanh điều hướng. Hai ad unit: NATIVE_LANGUAGES1 (layout ab_ad_unified_language1.xml, nút CTA xám #E4E7EC) hiện khi chưa chọn; chọn xong thì NATIVE_LANGUAGES2 (layout ab_ad_unified_language.xml, nút CTA xanh #017DFF) đè lên trên.',
  render: (p) => {
    const picked = p.pick || null;
    return `<div class="lang">
    <div class="lang-bar"><span></span><b>Language</b>
      <button class="lang-ok ${picked ? 'show' : ''}"
        onclick="${picked ? `adGo('adNativeInter', {next:'onb1'})` : `toast('Chọn 1 ngôn ngữ')`}">${ic('check', 's24')}</button></div>
    <div class="lang-list">
      ${[['gb', 'English'], ['vn', 'Tiếng Việt']].map(([f, name]) => `
        <button class="lang-row ${picked === f ? 'on' : ''}" onclick="setParams({pick:'${f}'})">
          <span class="flag flag-${f}"></span><span class="lang-name">${name}</span>
          <span class="radio"></span></button>`).join('')}
    </div>
    <!-- Chưa chọn: ad unit 1 với nút CTA xám. Chọn rồi: ad unit 2 đè lên, nút CTA xanh. -->
    ${picked ? adNativeInline('NATIVE_LANGUAGES2', 'blue') : adNativeInline('NATIVE_LANGUAGES1', 'grey')}
  </div>`;
  },
});

def('onb1', onbPage(0));
def('onb2', onbPage(1));
def('onb3', onbPage(2));
def('onb4', onbPage(3));

function onbPage(i) {
  const page = OBD[i];
  const last = i === OBD.length - 1;
  const next = last ? `adGo('adNativeInter', {next:'main'})` : `replace('onb${i + 2}')`;
  const full = page.ad === 'full';
  return {
    cls: `OnBoardingFragment${i + 1}`, layout: `ab_fragment_on_boarding_${i + 1}`, group: 'Khởi động & Quảng cáo',
    immersive: () => true,
    desc: full
      ? 'Trang lấy quảng cáo native phủ toàn trang (NATIVE_OBD_FULL): ảnh media ở trên, chữ ở giữa, nút hành động ở đáy. Chấm trang bị ẩn, chỉ còn nút NEXT ở góc trên.'
      : page.ad === 'bottom'
        ? 'Ảnh tràn viền ở trên, tiêu đề + mô tả, hàng chấm trang và NEXT, dưới cùng là thẻ quảng cáo native.'
        : 'Trang không có quảng cáo. Ẩn cả hai thanh hệ thống.',
    render: () => (full && state.adsOn) ? `<div class="onb-full">
        ${adNativeFull(page.unit, { label: 'Native toàn màn hình', noClose: true })}
        <button class="onb-next float" onclick="${next}">${last ? 'BẮT ĐẦU' : 'NEXT'}</button>
      </div>` : `<div class="onb">
      <div class="onb-art"><img src="${page.art}" alt=""></div>
      <h2 class="onb-title">${esc(page.title)}</h2>
      <p class="onb-sub">${esc(page.body)}</p>
      <div class="spacer"></div>
      <div class="onb-foot">
        <div class="dots-sm">${OBD.map((_, k) => `<i class="${k === i ? 'on' : ''}"></i>`).join('')}</div>
        <button class="onb-next" onclick="${next}">${last ? 'BẮT ĐẦU' : 'NEXT'}</button>
      </div>
      ${page.ad === 'bottom' ? adNativeInline(page.unit) : ''}
    </div>`,
  };
}

def('adNativeInter', {
  kind: 'Mock', cls: 'NativeInterActivity', layout: 'activity_native_inter', group: 'Khởi động & Quảng cáo',
  desc: 'Thay cho interstitial ở các điểm chuyển màn. Là một Activity riêng hiển thị 1 quảng cáo native đã nạp sẵn; không có quảng cáo thì đi tiếp ngay.',
  render: () => adNativeFull('NATIVE_INTER', { label: 'Native toàn màn hình' }),
});

def('adAppOpen', {
  kind: 'Mock', cls: 'AppOpenAd', group: 'Khởi động & Quảng cáo',
  desc: 'Hiện khi người dùng quay lại app từ nền. Không hiện ở lần mở đầu, không hiện khi vừa từ màn quảng cáo trở về, và cách nhau ít nhất 60 giây.',
  render: () => adInterstitial('APP_OPEN', 'App Open'),
});
