// Mock data. Names mirror a real public M3U so grouping/sorting looks realistic.
const CH_RAW = [
  ['An Giang TV 1', 'General', '1080p'], ['An Giang TV 2', 'Entertainment', '1080p'], ['An Ninh TV HD', 'News', '1080p'],
  ['Ca Mau TV', 'General', '720p'], ['Can Tho TV', 'General', '1080p'], ['Can Tho TV 2', 'Education', '1080p'],
  ['Cao Bang TV', 'General', '720p'], ['Da Nang TV 1', 'General', '1080p'], ['Da Nang TV 2', 'General', '1080p'],
  ['Dong Nai TV 1', 'News', '720p'], ['Dong Nai TV 2', 'Sports', '720p'], ['Dong Thap TV', 'General', '720p'],
  ['Ha Tinh TV', 'General', '720p'], ['Hanoi TV 1', 'General', '720p'], ['Hanoi TV 2', 'General', '1080p'],
  ['HTV1', 'General', '720p'], ['HTV2', 'Entertainment', '720p'], ['HTV3', 'Series', '720p'], ['HTV4', 'Education', '1080p'],
  ['HTV7', 'General', '720p'], ['HTV9', 'News', '720p'], ['HTV Sports', 'Sports', '1080p'], ['Khanh Hoa TV', 'General', '1080p'],
  ['Lam Dong TV', 'General', '480p'], ['MNB World', 'General', '1080p'], ['Ninh Thuan TV', 'General', '1080p'],
  ['ON Kids', 'Kids', '720p'], ['ON Life', 'Lifestyle', '720p'], ['ON Movies', 'Movies', '720p'], ['ON Vie Drama', 'Series', '720p'],
  ['QPVN HD', 'News', '1080p'], ['RTM ASEAN', 'News', '720p'], ['SCTV2', 'Entertainment', '720p'], ['SCTV3', 'Kids', '720p'],
  ['SCTV6', 'Movies', '720p'], ['SCTV9', 'Series', '720p'], ['SCTV15', 'Sports', '720p'], ['Vinh Long 1', 'General', '1080p'],
  ['Vinh Long 2', 'Entertainment', '1080p'], ['VTC1', 'News', '720p'], ['VTC3', 'Sports', '720p'], ['VTC7', 'Entertainment', '720p'],
];
const LOGO_COLORS = ['#2b5cb8', '#b83a2b', '#1f8a6b', '#8a4bd0', '#c7861b', '#157a9a', '#a1336b', '#4a6b1f', '#5b5bd6', '#9a5a2b'];
function initials(name) {
  const clean = name.replace(/\(.*?\)|\[.*?\]/g, '').trim();
  const words = clean.split(/\s+/);
  if (words.length === 1) return clean.slice(0, 4).toUpperCase();
  const nums = words.filter(w => /^\d+$/.test(w)).join('');
  const letters = words.filter(w => !/^\d+$/.test(w)).map(w => w[0]).join('').slice(0, 3);
  return (letters + nums).toUpperCase().slice(0, 4);
}
function hash(s) { let h = 0; for (const c of s) h = (h * 31 + c.charCodeAt(0)) >>> 0; return h; }
function logoColor(name) { return LOGO_COLORS[hash(name) % LOGO_COLORS.length]; }

const DATA = {
  playlists: [
    { id: 1, name: 'Vietnam free TV', count: 42, url: 'https://iptv-org.github.io/iptv/countries/vn.m3u', fav: true, locked: false, auto: true, updated: 'Cập nhật 2 giờ trước' },
    { id: 2, name: 'News world', count: 318, url: 'https://example.org/news.m3u8', fav: false, locked: true, auto: false, updated: 'Cập nhật hôm qua' },
  ],
  channels: CH_RAW.map((c, i) => ({ id: i + 1, name: c[0], group: c[1], q: c[2], fav: [0, 15, 21, 28].includes(i), playlistId: 1 })),
  recentIds: [22, 16, 29, 1, 5],
  singles: [ { id: 1, name: 'Concert live', url: 'https://example.org/live/concert.m3u8' } ],
  // status/exp/conn come from player_api.php user_info; counts from the last sync
  profiles: [
    { id: 'p1', name: 'My Xtream', server: 'http://line.example.tv:8080', user: 'demo_user', exp: '12/03/2027', status: 'Active', conn: '0 / 1', locked: false, color: '#5b5bd6', counts: { live: 1284, vod: 3910, series: 612 }, synced: '2 giờ trước', history: true },
    { id: 'p2', name: 'Gia đình', server: 'http://tv.family.example:80', user: 'family01', exp: '05/11/2026', status: 'Active', conn: '1 / 2', locked: true, color: '#1f8a6b', counts: { live: 860, vod: 2140, series: 380 }, synced: 'Hôm qua', history: true },
    { id: 'p3', name: 'Sports line', server: 'http://sport.example.net:2095', user: 'sp_4471', exp: '30/12/2026', status: 'Active', conn: '0 / 1', locked: false, color: '#c7861b', counts: { live: 212, vod: 0, series: 0 }, synced: '3 ngày trước' },
    { id: 'p4', name: 'Old line', server: 'http://old.example.org:8000', user: 'old_user', exp: '01/08/2026', status: 'Expired', conn: '—', locked: false, color: '#157a9a', counts: { live: 540, vod: 1200, series: 150 }, synced: '2 tháng trước' },
  ],
  xtLiveCats: ['All', 'Vietnam', 'Sports', 'News', 'Movies', 'Kids', 'Music'],
  xtMovieCats: ['All', 'Action', 'Comedy', 'Drama', 'Sci-Fi', 'Animation', 'Documentary'],
  movies: [
    { id: 101, t: 'Midnight Harbor', y: 2024, r: 4.3, g: 'Drama', d: '1h 52m', c: ['#243b55', '#141e30'] },
    { id: 102, t: 'Velocity Line', y: 2023, r: 3.9, g: 'Action', d: '2h 04m', c: ['#b24592', '#f15f79'] },
    { id: 103, t: 'Paper Planets', y: 2025, r: 4.6, g: 'Animation', d: '1h 38m', c: ['#1d976c', '#93f9b9'] },
    { id: 104, t: 'The Quiet Lab', y: 2022, r: 4.1, g: 'Sci-Fi', d: '1h 47m', c: ['#373b44', '#4286f4'] },
    { id: 105, t: 'Salt & Sun', y: 2024, r: 3.7, g: 'Comedy', d: '1h 31m', c: ['#f7971e', '#ffd200'] },
    { id: 106, t: 'Northbound', y: 2021, r: 4.0, g: 'Documentary', d: '1h 25m', c: ['#2c3e50', '#4ca1af'] },
    { id: 107, t: 'Echo Chamber', y: 2025, r: 4.4, g: 'Drama', d: '2h 11m', c: ['#42275a', '#734b6d'] },
    { id: 108, t: 'Last Lap', y: 2023, r: 3.8, g: 'Action', d: '1h 58m', c: ['#cb2d3e', '#ef473a'] },
  ],
  series: [
    { id: 201, t: 'Delta Nine', y: 2024, r: 4.5, g: 'Sci-Fi', seasons: 3, c: ['#0f2027', '#2c5364'] },
    { id: 202, t: 'Kitchen Rules', y: 2025, r: 4.1, g: 'Comedy', seasons: 2, c: ['#ee9ca7', '#ffdde1'] },
    { id: 203, t: 'Border Town', y: 2022, r: 4.2, g: 'Drama', seasons: 4, c: ['#3a1c71', '#d76d77'] },
    { id: 204, t: 'Wild Coast', y: 2023, r: 4.7, g: 'Documentary', seasons: 1, c: ['#134e5e', '#71b280'] },
  ],
  continueWatching: [ { ref: 201, ep: 'S2 · E4', p: 62 }, { ref: 101, ep: '1h 10m còn lại', p: 38 }, { ref: 203, ep: 'S1 · E7', p: 81 } ],
  leagues: [
    { slug: 'eng.1', name: 'Premier League', sport: 'Soccer', hot: true },
    { slug: 'esp.1', name: 'LaLiga', sport: 'Soccer', hot: true },
    { slug: 'ita.1', name: 'Serie A', sport: 'Soccer', hot: true },
    { slug: 'ger.1', name: 'Bundesliga', sport: 'Soccer', hot: false },
    { slug: 'fra.1', name: 'Ligue 1', sport: 'Soccer', hot: false },
    { slug: 'uefa.champions', name: 'Champions League', sport: 'Soccer', hot: true },
    { slug: 'nba', name: 'NBA', sport: 'Basketball', hot: true },
  ],
  matches: [
    { id: 'm1', lg: 'Premier League', home: 'Northfield', away: 'Rivermouth', hs: 2, as: 1, st: 'live', min: "67'", time: '19:30' },
    { id: 'm2', lg: 'LaLiga', home: 'Costa Azul', away: 'Sierra FC', hs: 0, as: 0, st: 'live', min: "12'", time: '20:00' },
    { id: 'm3', lg: 'Serie A', home: 'Porto Nuovo', away: 'Valle Verde', st: 'pre', time: '22:45', date: 'Hôm nay' },
    { id: 'm4', lg: 'Premier League', home: 'Kingsbridge', away: 'Eastvale', st: 'pre', time: '02:00', date: 'Ngày mai' },
    { id: 'm5', lg: 'Champions League', home: 'Atlético Sur', away: 'Nordhavn', st: 'pre', time: '03:00', date: 'Ngày mai' },
    { id: 'm6', lg: 'LaLiga', home: 'Montaña', away: 'Puerto Real', hs: 3, as: 2, st: 'post', time: 'Hôm qua' },
  ],
  community: {
    iptv: [ { id: 'c1', name: 'Asia news pack', url: 'https://example.org/asia-news.m3u', date: '28/09/2026', saved: false },
            { id: 'c2', name: 'Kids cartoons', url: 'https://example.org/kids.m3u8', date: '25/09/2026', saved: true } ],
    xtream: [ { id: 'c3', server: 'http://demo.example.tv:8000', user: 'shared01', pass: 'x7Kp2', saved: false } ],
    single: [ { id: 'c4', name: 'City cam 24/7', url: 'https://example.org/cam/index.m3u8', saved: false } ],
  },
  guideSites: {
    iptv: [ { title: 'Kho playlist gợi ý (GitHub)', url: 'https://github.com/<your-account>/iptv-playlist', host: 'github.com' },
            { title: 'iptv-org – playlist công khai', url: 'https://github.com/iptv-org/iptv', host: 'github.com' } ],
    xtream: [ { title: 'Hướng dẫn tài khoản Xtream', url: 'https://github.com/<your-account>/xtream-guide', host: 'github.com' } ],
    single: [ { title: 'Link stream đơn (GitHub)', url: 'https://github.com/<your-account>/single-stream', host: 'github.com' } ],
    search: 'free iptv m3u playlist',
  },
  settings: { passcode: null },
};

function chLogo(name, extra = '') {
  return `<div class="logo ${extra}" style="background:${logoColor(name)}">${initials(name)}</div>`;
}
function groups(list) {
  const m = new Map();
  list.forEach(c => m.set(c.group, (m.get(c.group) || 0) + 1));
  return [...m.entries()].sort((a, b) => a[0].localeCompare(b[0]));
}
function plName() { return (DATA.playlists[0] || { name: 'Playlist' }).name; }
function findRef(id) { return DATA.movies.find(m => m.id === id) || DATA.series.find(s => s.id === id); }
function posterBg(c) { return `background:linear-gradient(160deg, ${c[0]}, ${c[1]})`; }
