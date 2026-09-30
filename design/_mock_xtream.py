"""Mock Xtream Codes server for on-device testing (no real provider needed).

    python design/_mock_xtream.py            # listens on :8766
    adb reverse tcp:8766 tcp:8766            # server URL in the app: http://127.0.0.1:8766

Accounts: demo/demo (active), expired/expired or old/old (exp_date in the past),
big/big (active, ~28k live / 158k VOD / 50k series, to test large syncs). Anything else → auth 0.
Streams redirect to public test media (Mux HLS test stream, test-videos.co.uk MP4s).
"""
import json, time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

HLS = 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'
MP4 = ['https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4',
       'https://test-videos.co.uk/vids/jellyfish/mp4/h264/360/Jellyfish_360_10s_1MB.mp4',
       'https://test-videos.co.uk/vids/sintel/mp4/h264/360/Sintel_360_10s_1MB.mp4',
       'https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8']
IMG = 'https://picsum.photos/seed/{}/300/450'

LIVE_CATS = [('1', 'News'), ('2', 'Sports'), ('3', 'Kids'), ('4', 'Music')]
LIVE = [{'stream_id': i, 'name': n, 'stream_icon': '', 'category_id': c, 'epg_channel_id': ''} for i, (n, c) in enumerate([
    ('Demo News 24', '1'), ('World Report HD', '1'), ('Sport Arena 1', '2'), ('Sport Arena 2', '2'), ('Football Live', '2'),
    ('Cartoon Time', '3'), ('Kids Planet', '3'), ('Hits Music', '4'), ('Chill Radio TV', '4'), ('Business Today', '1')], start=101)]
VOD_CATS = [('10', 'Action'), ('11', 'Animation'), ('12', 'Sci-Fi')]
VOD = [{'stream_id': i, 'name': n, 'stream_icon': IMG.format(i), 'category_id': c, 'rating_5based': r, 'container_extension': 'mp4', 'added': str(1727000000 + i)}
       for i, (n, c, r) in enumerate([('Big Buck Bunny', '11', 4.1), ('Elephants Dream', '11', 3.6), ('Sintel', '12', 4.4), ('Tears of Steel', '12', 3.9),
                                       ('Night Chase', '10', 3.2), ('Last Stand', '10', 3.5)], start=201)]
SERIES_CATS = [('20', 'Drama'), ('21', 'Documentary')]
SERIES = [{'series_id': i, 'name': n, 'cover': IMG.format(i), 'category_id': c, 'rating_5based': 4.0, 'plot': f'{n}: a demo series for testing.',
           'cast': 'Jane Roe, John Doe', 'director': 'Demo Director', 'genre': 'Drama', 'releaseDate': '2024-05-01', 'youtube_trailer': ''}
          for i, (n, c) in enumerate([('Open Movie Stories', '20'), ('Blender Diaries', '21'), ('The Test Pattern', '20')], start=301)]


def user(u, p):
    if (u, p) in (('demo', 'demo'), ('big', 'big')): exp = int(time.time()) + 45 * 86400
    elif (u, p) in (('expired', 'expired'), ('old', 'old')): exp = int(time.time()) - 3 * 86400
    else: return {'user_info': {'auth': 0}}
    return {'user_info': {'auth': 1, 'status': 'Active' if exp > time.time() else 'Expired', 'exp_date': str(exp), 'active_cons': '0',
                          'max_connections': '2', 'allowed_output_formats': ['m3u8', 'ts']},
            'server_info': {'url': '127.0.0.1', 'port': '8766', 'timezone': 'Asia/Ho_Chi_Minh'}}


_BIG = {}
def big(kind):
    # Built once, served as pre-encoded JSON (real panels send these as one huge array).
    if kind not in _BIG:
        if kind == 'live': rows = [{'stream_id': i, 'name': f'Channel {i} HD', 'stream_icon': f'http://logo.example/{i}.png', 'category_id': str(i % 400), 'epg_channel_id': f'ch{i}.tv', 'num': i, 'stream_type': 'live', 'added': '1700000000', 'tv_archive': 0, 'direct_source': '', 'custom_sid': ''} for i in range(1, 28422)]
        elif kind == 'vod': rows = [{'stream_id': i, 'name': f'Movie Title {i} (20{i % 25:02d})', 'stream_icon': f'http://img.example/p/{i}.jpg', 'category_id': str(i % 300), 'rating': '7.1', 'rating_5based': 3.5, 'container_extension': 'mkv', 'added': str(1600000000 + i), 'num': i, 'stream_type': 'movie', 'custom_sid': '', 'direct_source': ''} for i in range(1, 158120)]
        else: rows = [{'series_id': i, 'name': f'Series {i}', 'cover': f'http://img.example/s/{i}.jpg', 'category_id': str(i % 200), 'rating_5based': 4.0, 'plot': 'A long plot description ' * 6, 'cast': 'Actor One, Actor Two, Actor Three', 'director': 'Someone', 'genre': 'Drama, Crime', 'releaseDate': '2021-01-01', 'youtube_trailer': '', 'num': i, 'last_modified': '1700000000', 'backdrop_path': []} for i in range(1, 50378)]
        _BIG[kind] = json.dumps(rows).encode()
    return _BIG[kind]


def api(q):
    u, p, a = q.get('username', [''])[0], q.get('password', [''])[0], q.get('action', [''])[0]
    auth = user(u, p)
    if not a or auth['user_info'].get('auth') != 1: return auth
    cats = lambda l: [{'category_id': i, 'category_name': n} for i, n in l]
    if u == 'big':
        n = {'get_live_categories': 400, 'get_vod_categories': 300, 'get_series_categories': 200}.get(a)
        if n: return [{'category_id': str(i), 'category_name': f'Category {i}'} for i in range(n)]
        k = {'get_live_streams': 'live', 'get_vod_streams': 'vod', 'get_series': 'series'}.get(a)
        if k: return big(k)
    if a == 'get_live_categories': return cats(LIVE_CATS)
    if a == 'get_live_streams': return LIVE
    if a == 'get_vod_categories': return cats(VOD_CATS)
    if a == 'get_vod_streams': return VOD
    if a == 'get_series_categories': return cats(SERIES_CATS)
    if a == 'get_series': return SERIES
    if a == 'get_vod_info':
        v = next(x for x in VOD if str(x['stream_id']) == q['vod_id'][0])
        return {'info': {'plot': f"{v['name']} is an open movie used here as test content.", 'cast': 'Blender Foundation', 'director': 'Open Movie Project',
                         'genre': 'Animation', 'releasedate': '2008-05-30', 'duration': '00:10:34', 'duration_secs': 634, 'rating': '7.5',
                         'backdrop_path': [f"https://picsum.photos/seed/b{v['stream_id']}/800/450"], 'youtube_trailer': ''},
                'movie_data': {'container_extension': 'mp4', 'name': v['name']}}
    if a == 'get_series_info':
        sid = int(q['series_id'][0])
        eps = {str(s): [{'id': f'{sid}{s}{e:02d}', 'episode_num': e, 'title': f'Episode {e}', 'container_extension': 'mp4', 'season': s,
                         'info': {'movie_image': f'https://picsum.photos/seed/e{sid}{s}{e}/320/180', 'duration_secs': 600 + e * 30}} for e in range(1, 6)]
               for s in (1, 2)}
        return {'seasons': [{'season_number': s, 'name': f'Season {s}', 'cover': ''} for s in (1, 2)], 'episodes': eps}
    return []


class H(BaseHTTPRequestHandler):
    def do_GET(self):
        url = urlparse(self.path)
        parts = url.path.strip('/').split('/')
        if url.path.endswith('player_api.php'):
            r = api(parse_qs(url.query))
            body = r if isinstance(r, bytes) else json.dumps(r).encode()
            self.send_response(200); self.send_header('Content-Type', 'application/json'); self.send_header('Content-Length', str(len(body))); self.end_headers()
            self.wfile.write(body); return
        if len(parts) == 4 and parts[0] in ('live', 'movie', 'series'):
            sid = int(''.join(ch for ch in parts[3].split('.')[0] if ch.isdigit()) or 0)
            self.send_response(302); self.send_header('Location', HLS if parts[0] == 'live' else MP4[sid % len(MP4)]); self.end_headers(); return
        self.send_response(404); self.end_headers()

    def log_message(self, fmt, *args): print(self.command, self.path[:120], flush=True)


if __name__ == '__main__':
    ThreadingHTTPServer(('0.0.0.0', 8766), H).serve_forever()
