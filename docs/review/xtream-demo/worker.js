/**
 * Xtream Codes API demo server for Google Play review (Cloudflare Worker, no dependencies).
 *
 * Serves only public test streams published for video-player developers and open movies
 * (Blender Foundation, CC BY 3.0), the same sources as docs/review/test-playlist.m3u.
 * No TV channels and no copyrighted programming.
 *
 * Login:   server = this Worker's URL, username = reviewer, password = review2026
 * Routes:  /player_api.php?username=&password=[&action=...]   Xtream API (JSON)
 *          /live|movie|series/<user>/<pass>/<id>.<ext>          302 redirect to the test stream
 *          /get.php?username=&password=&type=m3u_plus           the same content as an M3U playlist
 *          /                                                     short info page
 */

const USER = 'reviewer';
const PASS = 'review2026';

const LIVE_CATS = [
  { category_id: '1', category_name: 'Live test streams' },
  { category_id: '2', category_name: 'Player test patterns' },
];

const LIVE = [
  { id: 1001, cat: '1', name: 'Unified Streaming Live Demo', url: 'https://demo.unified-streaming.com/k8s/live/stable/live.isml/.m3u8' },
  { id: 1002, cat: '1', name: 'Shaka Player Live Test', url: 'https://storage.googleapis.com/shaka-live-assets/player-source.m3u8' },
  { id: 1003, cat: '2', name: 'Apple BipBop (fMP4, multi-audio)', url: 'https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8' },
  { id: 1004, cat: '2', name: 'Apple BipBop 16:9', url: 'https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8' },
  { id: 1005, cat: '2', name: 'Apple BipBop 4:3', url: 'https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8' },
];

const VOD_CATS = [
  { category_id: '10', category_name: 'Open movies' },
  { category_id: '11', category_name: 'Short clips' },
];

// ext decides the stream URL the app builds (/movie/u/p/<id>.<ext>) and therefore the player format.
const VOD = [
  { id: 2001, cat: '10', name: 'Big Buck Bunny', ext: 'm3u8', year: '2008', secs: 596, rating: 4.1,
    plot: 'A giant rabbit takes on three bullying rodents. Open movie by the Blender Foundation.', url: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8' },
  { id: 2002, cat: '10', name: 'Tears of Steel', ext: 'm3u8', year: '2012', secs: 734, rating: 3.9,
    plot: 'Science-fiction short film with subtitles. Open movie by the Blender Foundation.', url: 'https://test-streams.mux.dev/tos_ismc/main.m3u8' },
  { id: 2003, cat: '10', name: 'Sintel', ext: 'mpd', year: '2010', secs: 888, rating: 4.4,
    plot: 'A girl searches for her baby dragon. Open movie by the Blender Foundation (DASH).', url: 'https://storage.googleapis.com/shaka-demo-assets/sintel/dash.mpd' },
  { id: 2004, cat: '10', name: 'Tears of Steel (DASH)', ext: 'mpd', year: '2012', secs: 734, rating: 3.9,
    plot: 'The same open movie delivered as MPEG-DASH.', url: 'https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.mpd' },
  { id: 2005, cat: '11', name: 'Sintel Trailer', ext: 'mp4', year: '2010', secs: 52, rating: 4.0,
    plot: 'Trailer of the open movie Sintel (MP4).', url: 'https://download.blender.org/durian/trailer/sintel_trailer-720p.mp4' },
  { id: 2006, cat: '11', name: 'Big Buck Bunny (10 s clip)', ext: 'mp4', year: '2008', secs: 10, rating: 3.5,
    plot: 'Ten-second MP4 test clip.', url: 'https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_5MB.mp4' },
  { id: 2007, cat: '11', name: 'Sintel (10 s clip)', ext: 'mp4', year: '2010', secs: 10, rating: 3.5,
    plot: 'Ten-second MP4 test clip.', url: 'https://test-videos.co.uk/vids/sintel/mp4/h264/720/Sintel_720_10s_5MB.mp4' },
];

const SERIES_CATS = [{ category_id: '20', category_name: 'Open movie collections' }];

const SERIES = [
  { id: 3001, cat: '20', name: 'Blender Open Movies', genre: 'Animation', year: '2008-05-30',
    plot: 'Open movies by the Blender Foundation, as a series with one episode per film.',
    seasons: { 1: [
      { id: '300101', title: 'Big Buck Bunny', ext: 'm3u8', secs: 596, url: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8' },
      { id: '300102', title: 'Sintel', ext: 'mpd', secs: 888, url: 'https://storage.googleapis.com/shaka-demo-assets/sintel/dash.mpd' },
      { id: '300103', title: 'Tears of Steel', ext: 'm3u8', secs: 734, url: 'https://test-streams.mux.dev/tos_ismc/main.m3u8' },
    ] } },
  { id: 3002, cat: '20', name: 'Player Test Patterns', genre: 'Test', year: '2020-01-01',
    plot: 'Apple HLS test streams, useful to check subtitles, audio tracks and aspect ratios.',
    seasons: {
      1: [
        { id: '300201', title: 'BipBop 16:9', ext: 'm3u8', secs: 1800, url: 'https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8' },
        { id: '300202', title: 'BipBop 4:3', ext: 'm3u8', secs: 1800, url: 'https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8' },
      ],
      2: [
        { id: '300203', title: 'BipBop fMP4 (multi-audio)', ext: 'm3u8', secs: 600, url: 'https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8' },
      ],
    } },
];

const ADDED = '1759000000'; // fixed "added" timestamp: newest first keeps the list order stable

function json(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' } });
}

function authed(u, p) { return u === USER && p === PASS; }

function userInfo(url) {
  const exp = Math.floor(Date.now() / 1000) + 365 * 86400; // always one year ahead: never expires during review
  return {
    user_info: { auth: 1, status: 'Active', exp_date: String(exp), active_cons: '0', max_connections: '2', allowed_output_formats: ['m3u8', 'ts'], username: USER },
    server_info: { url: url.hostname, port: url.port || (url.protocol === 'https:' ? '443' : '80'), timezone: 'UTC' },
  };
}

function api(url) {
  const q = url.searchParams;
  if (!authed(q.get('username'), q.get('password'))) return json({ user_info: { auth: 0 } });
  switch (q.get('action') || '') {
    case '': return json(userInfo(url));
    case 'get_live_categories': return json(LIVE_CATS);
    case 'get_vod_categories': return json(VOD_CATS);
    case 'get_series_categories': return json(SERIES_CATS);
    case 'get_live_streams':
      return json(LIVE.map((c, i) => ({ num: i + 1, stream_id: c.id, name: c.name, stream_icon: '', category_id: c.cat, epg_channel_id: '', stream_type: 'live', added: ADDED })));
    case 'get_vod_streams':
      return json(VOD.map((v, i) => ({ num: i + 1, stream_id: v.id, name: v.name, stream_icon: '', category_id: v.cat, rating_5based: v.rating, container_extension: v.ext, added: String(Number(ADDED) - i), stream_type: 'movie' })));
    case 'get_series':
      return json(SERIES.map((s, i) => ({ num: i + 1, series_id: s.id, name: s.name, cover: '', category_id: s.cat, rating_5based: 4.0, plot: s.plot, cast: 'Blender Foundation', director: 'Blender Foundation', genre: s.genre, releaseDate: s.year, youtube_trailer: '' })));
    case 'get_vod_info': {
      const v = VOD.find((x) => String(x.id) === q.get('vod_id'));
      if (!v) return json({ info: {}, movie_data: {} });
      const hms = new Date(v.secs * 1000).toISOString().substring(11, 19);
      return json({
        info: { plot: v.plot, cast: 'Blender Foundation', director: 'Blender Foundation', genre: v.cat === '10' ? 'Animation' : 'Test clip', releasedate: `${v.year}-01-01`, duration: hms, duration_secs: v.secs, rating: String(v.rating * 2), backdrop_path: [], youtube_trailer: '' },
        movie_data: { stream_id: v.id, name: v.name, container_extension: v.ext },
      });
    }
    case 'get_series_info': {
      const s = SERIES.find((x) => String(x.id) === q.get('series_id'));
      if (!s) return json({ seasons: [], episodes: {} });
      const episodes = {};
      for (const [season, eps] of Object.entries(s.seasons)) {
        episodes[season] = eps.map((e, i) => ({ id: e.id, episode_num: i + 1, title: e.title, container_extension: e.ext, season: Number(season), info: { movie_image: '', duration_secs: e.secs } }));
      }
      return json({ seasons: Object.keys(s.seasons).map((n) => ({ season_number: Number(n), name: `Season ${n}`, cover: '' })), info: { name: s.name, plot: s.plot }, episodes });
    }
    default: return json([]);
  }
}

function streamTarget(kind, id) {
  if (kind === 'live') return LIVE.find((c) => String(c.id) === id)?.url;
  if (kind === 'movie') return VOD.find((v) => String(v.id) === id)?.url;
  for (const s of SERIES) for (const eps of Object.values(s.seasons)) { const e = eps.find((x) => x.id === id); if (e) return e.url; }
  return undefined;
}

function m3u(url) {
  const base = `${url.origin}`;
  const lines = ['#EXTM3U'];
  const group = (cats, id) => cats.find((c) => c.category_id === id)?.category_name || '';
  for (const c of LIVE) lines.push(`#EXTINF:-1 tvg-id="${c.id}" group-title="${group(LIVE_CATS, c.cat)}",${c.name}`, `${base}/live/${USER}/${PASS}/${c.id}.m3u8`);
  for (const v of VOD) lines.push(`#EXTINF:-1 group-title="${group(VOD_CATS, v.cat)}",${v.name}`, `${base}/movie/${USER}/${PASS}/${v.id}.${v.ext}`);
  return new Response(lines.join('\n') + '\n', { headers: { 'content-type': 'audio/x-mpegurl; charset=utf-8', 'cache-control': 'no-store' } });
}

const INFO = `Xtream Codes API demo server for app review.

Content: public test streams for video-player developers and Blender Foundation open movies (CC BY 3.0).
No TV channels, no copyrighted programming.

Server URL: this address
Username:   ${USER}
Password:   ${PASS}
`;

export default {
  async fetch(request) {
    const url = new URL(request.url);
    const path = url.pathname;
    if (path.endsWith('/player_api.php')) return api(url);
    if (path.endsWith('/get.php')) {
      return authed(url.searchParams.get('username'), url.searchParams.get('password')) ? m3u(url) : new Response('Unauthorized', { status: 401 });
    }
    const m = path.match(/^\/(live|movie|series)\/([^/]+)\/([^/]+)\/([^/.]+)(?:\.\w+)?$/);
    if (m) {
      if (!authed(decodeURIComponent(m[2]), decodeURIComponent(m[3]))) return new Response('Unauthorized', { status: 401 });
      const target = streamTarget(m[1], m[4]);
      return target ? Response.redirect(target, 302) : new Response('Not found', { status: 404 });
    }
    if (path === '/' || path === '') return new Response(INFO, { headers: { 'content-type': 'text/plain; charset=utf-8' } });
    return new Response('Not found', { status: 404 });
  },
};
