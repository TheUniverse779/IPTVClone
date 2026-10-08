/* ===== Luồng lần đầu: Language → Onboarding ×4 → Main, kèm các vị trí quảng cáo =====
   Mô phỏng theo TranslateApp nhưng dùng giao diện của app IPTV (nền tối, cam).
   Ảnh onboarding là ảnh của app Translate, dùng tạm cho tới khi có ảnh riêng.

   Quảng cáo trong luồng này:
     • Language    → 1 native (2 ad unit trong app: NATIVE_LANGUAGES1/2)
     • Onboarding 1→ native ở đáy trang   (NATIVE_OBD1)
     • Onboarding 2→ không có
     • Onboarding 3→ native toàn trang   (NATIVE_OBD_FULL)
     • Onboarding 4→ native ở đáy trang   (NATIVE_OBD3)
     • Trước Main  → native "interstitial" toàn màn (NativeInterActivity)
     • Quay lại app→ App Open (chỉ khi app đã ở Main rồi)
   Bật/tắt nhanh bằng nút "Ads" ở thanh dưới khung điện thoại. */

const OBD = [
  { art: 'img/img_obd1_ab.webp', title: 'Xem mọi kênh của bạn', body: 'Thêm playlist M3U hoặc tài khoản từ nhà cung cấp, rồi xem ngay trên điện thoại.', ad: 'bottom', unit: 'NATIVE_OBD1' },
  { art: 'img/img_obd2_ab.webp', title: 'Gọn gàng theo nhóm kênh', body: 'Kênh được chia theo nhóm, đánh dấu yêu thích và tìm kiếm trong mọi playlist.', ad: 'none', unit: null },
  { art: 'img/img_obd3_ab.webp', title: 'Giao diện xem phim đẹp mắt', body: 'Poster theo thể loại, xem tiếp đúng chỗ đang dở, phụ đề và nhiều tuỳ chọn cho trình phát.', ad: 'full', unit: 'NATIVE_OBD_FULL' },
  { art: 'img/img_obd4_ab.webp', title: 'Sẵn sàng xem', body: 'Chọn ngôn ngữ, thêm nguồn đầu tiên của bạn và bắt đầu.', ad: 'bottom', unit: 'NATIVE_OBD3' },
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

/** Thẻ quảng cáo native giả lập (trong app là 1 thẻ do AdMob trả về). */
function adNativeCard(unit, compact) {
  if (!state.adsOn) return '';
  return `<div class="ad-slot ${compact ? 'compact' : ''}">
    <div class="ad-tag">Quảng cáo · ${unit}</div>
    <div class="ad-card">
      <div class="ad-top"><div class="ad-icon">${ic('tv')}</div>
        <div class="ad-text"><b>IPTV Smart Player</b><span>Tải app, xem thử miễn phí</span></div></div>
      <div class="ad-body">Nội dung quảng cáo do Google AdMob trả về. App không kiểm soát nội dung này.</div>
      <button class="ad-cta">Tải ngay</button>
    </div>
  </div>`;
}

/** Dải banner AdMob ở đáy activity — nằm dưới cả thanh tab. */
function adBannerStrip() {
  if (!state.adsOn) return '';
  return `<div class="ad-banner">
    <span class="ad-banner-tag">Test Ad</span>
    <div class="ad-banner-body"><b>AdMob Adaptive Banner</b><small>Banner luôn ở đáy màn hình</small></div>
    <button class="ad-banner-cta">OPEN</button></div>`;
}

/** Quảng cáo toàn màn: interstitial AdMob hoặc native dạng toàn trang. */
function adFullScreen(kind, unit, title, noClose) {
  const label = kind === 'inter' ? 'Interstitial (AdMob)' : kind === 'open' ? 'App Open (AdMob)' : 'Native toàn màn hình';
  return `<div class="ad-full">
    <div class="ad-full-bar"><span class="ad-tag">${label} · ${unit}</span>
      ${noClose ? '' : `<button class="ad-close" onclick="adNext()">✕</button>`}</div>
    <div class="ad-full-art">${ic('tv', 's32')}</div>
    <h3>IPTV Smart Player</h3>
    <p>${title || 'Quảng cáo toàn màn hình do Google AdMob trả về.'}</p>
    <button class="btn primary block">Tải ngay</button>
    <div class="ad-full-foot">Đóng sau 5 giây · nút ✕ ở góc trên</div>
  </div>`;
}

/** Sau khi quảng cáo đóng: đi tiếp tới màn kế của luồng. */
function adNext() {
  const p = topParams();
  replace(p.next || 'main', p.nextParams || {});
}

def('adSplashInter', {
  kind: 'Mock', cls: 'InterstitialAd (splash)', group: 'Khởi động & Quảng cáo',
  desc: 'Splash: xin consent UMP → nạp interstitial → hiện quảng cáo này → xong mới đi tiếp. Nếu tải lỗi, chưa đủ thời gian chờ hoặc mạng yếu thì bỏ qua và đi thẳng.',
  render: () => adFullScreen('inter', 'INTER_SPLASH'),
});

def('langapp', {
  cls: 'LanguageAppActivity', layout: 'activity_language_app', group: 'Khởi động & Quảng cáo',
  desc: 'Màn chọn ngôn ngữ ở lần mở đầu. Nút Continue chạy quảng cáo native toàn màn rồi mới sang Onboarding.',
  render: () => `<div class="onb">
    <div class="onb-head"><h2>Choose your language</h2><p>Bạn có thể đổi lại trong Settings</p></div>
    <div class="lang-list">
      <button class="lang-item on"><span>English</span>${ic('check', 's16')}</button>
      <button class="lang-item"><span>Tiếng Việt</span></button>
    </div>
    <div class="spacer"></div>
    ${adNativeCard('NATIVE_LANGUAGES1')}
    <button class="btn primary block" onclick="adGo('adNativeInter', {next:'onb1'})">Continue</button>
  </div>`,
});

function adGo(id, params) {
  if (!state.adsOn) { const p = params || {}; replace(p.next || 'main', p.nextParams || {}); return; }
  replace(id, params);
}

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
    desc: full
      ? 'Trang này lấy quảng cáo native phủ toàn trang; chấm trang và nút TIẾP bị ẩn, chỉ còn nút NEXT riêng của quảng cáo ở góc trên (giống app Translate).'
      : page.ad === 'bottom'
        ? 'Ảnh + tiêu đề + mô tả, cuối trang là một thẻ quảng cáo native.'
        : 'Trang không có quảng cáo.',
    // Quảng cáo tắt: trang 3 trở về dạng ảnh + chữ như các trang khác.
    render: () => (full && state.adsOn) ? `<div class="onb-full">
        ${adFullScreen('native', page.unit, 'Quảng cáo native phủ toàn trang.', true)}
        <button class="link-next float" onclick="${next}">${last ? 'BẮT ĐẦU' : 'TIẾP'}</button>
      </div>` : `<div class="onb">
      <div class="onb-art"><img src="${page.art}" alt=""></div>
      <div class="onb-text"><h2>${esc(page.title)}</h2><p>${esc(page.body)}</p></div>
      <div class="spacer"></div>
      ${page.ad === 'bottom' ? adNativeCard(page.unit) : ''}
      <div class="onb-foot">
        <div class="dots-sm">${OBD.map((_, k) => `<i class="${k === i ? 'on' : ''}"></i>`).join('')}</div>
        <button class="link-next" onclick="${next}">${last ? 'BẮT ĐẦU' : 'TIẾP'}</button>
      </div>
    </div>`,
  };
}

def('adNativeInter', {
  kind: 'Mock', cls: 'NativeInterActivity', layout: 'activity_native_inter', group: 'Khởi động & Quảng cáo',
  desc: 'Thay cho interstitial ở các điểm chuyển màn. Thực tế đây là một Activity riêng hiển thị 1 quảng cáo native đã nạp sẵn; không có quảng cáo thì đi tiếp ngay.',
  render: (p) => adFullScreen('native', 'NATIVE_INTER', 'Quảng cáo native toàn màn hình, tự động đóng sau vài giây.'),
});

def('adAppOpen', {
  kind: 'Mock', cls: 'AppOpenAd', group: 'Khởi động & Quảng cáo',
  desc: 'Hiện khi người dùng quay lại app từ nền (không hiện ở lần mở đầu tiên, không hiện khi vừa từ màn quảng cáo trở về, tối đa 1 lần mỗi 60 giây).',
  render: () => adFullScreen('open', 'APP_OPEN', 'Quảng cáo App Open — xuất hiện khi quay lại app.'),
});
