"""Convert design/js/icons.js stroke icons to Android VectorDrawables (res/drawable/ic_*.xml).

Handles the subset used in icons.js: <path d>, <circle>, <rect> (with rx), with optional
fill="currentColor" / stroke="none". Output uses strokeWidth 1.8, round caps/joins, tint via
android:tint so the XML can be recolored with app:tint / ImageView tint.
"""
import io, os, re, sys

SRC = os.path.join(os.path.dirname(__file__), 'js', 'icons.js')
OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'drawable')

def snake(name):
    return re.sub(r'([A-Z])', lambda m: '_' + m.group(1).lower(), name)

def circle_path(cx, cy, r):
    return f'M{cx - r},{cy}a{r},{r} 0 1,0 {2 * r},0a{r},{r} 0 1,0 {-2 * r},0'

def rect_path(x, y, w, h, rx):
    if rx <= 0:
        return f'M{x},{y}h{w}v{h}h{-w}z'
    rx = min(rx, w / 2, h / 2)
    return (f'M{x + rx},{y}h{w - 2 * rx}a{rx},{rx} 0 0,1 {rx},{rx}v{h - 2 * rx}'
            f'a{rx},{rx} 0 0,1 {-rx},{rx}h{-(w - 2 * rx)}a{rx},{rx} 0 0,1 {-rx},{-rx}'
            f'v{-(h - 2 * rx)}a{rx},{rx} 0 0,1 {rx},{-rx}z')

_NUM = re.compile(r'[+-]?(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?')

def normalize_path(d):
    """Rewrite arc commands with explicit separators.

    Browsers accept the compact SVG form `a1 1 0 01-1 1` (flags `0` `1` glued together and to the
    next number); Android's PathParser does not. Parse each arc's 7 params token by token,
    reading the two flags as single characters, and emit them space-separated.
    """
    out, i, n = [], 0, len(d)
    cmd = None
    while i < n:
        ch = d[i]
        if ch.isalpha():
            cmd = ch
            out.append(ch)
            i += 1
            continue
        if ch in ' ,\t\n':
            i += 1
            continue
        if cmd in ('a', 'A'):
            vals = []
            while len(vals) < 7 and i < n:
                while i < n and d[i] in ' ,\t\n':
                    i += 1
                if len(vals) in (3, 4):  # large-arc / sweep flag: exactly one char
                    vals.append(d[i]); i += 1
                else:
                    m = _NUM.match(d, i)
                    vals.append(m.group(0)); i = m.end()
            out.append(' '.join(vals) + ' ')
        else:
            m = _NUM.match(d, i)
            out.append(m.group(0) + ' '); i = m.end()
    return ''.join(out).strip()

def attrs(tag):
    return dict(re.findall(r'([\w-]+)="([^"]*)"', tag))

def fnum(v):
    return float(v) if v else 0.0

def element_paths(body):
    out = []
    for m in re.finditer(r'<(path|circle|rect)\b([^>]*)/?>', body):
        kind, a = m.group(1), attrs(m.group(2))
        if kind == 'path':
            d = normalize_path(a['d'])
        elif kind == 'circle':
            d = circle_path(fnum(a['cx']), fnum(a['cy']), fnum(a['r']))
        else:
            d = rect_path(fnum(a['x']), fnum(a['y']), fnum(a['width']), fnum(a['height']), fnum(a.get('rx')))
        filled = a.get('fill') == 'currentColor'
        stroked = a.get('stroke') != 'none'
        out.append((d, filled, stroked))
    return out

def to_vd(body):
    parts = []
    for d, filled, stroked in element_paths(body):
        p = [f'        android:pathData="{d}"']
        if filled:
            p.append('        android:fillColor="#FFFFFFFF"')
        if stroked:
            p += ['        android:strokeColor="#FFFFFFFF"', '        android:strokeWidth="1.8"',
                  '        android:strokeLineCap="round"', '        android:strokeLineJoin="round"']
        parts.append('    <path\n' + '\n'.join(p) + ' />')
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- Generated from design/js/icons.js by design/_svg2vd.py -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="24dp" android:height="24dp"\n'
            '    android:viewportWidth="24" android:viewportHeight="24"\n'
            '    android:tint="#FFFFFFFF">\n' + '\n'.join(parts) + '\n</vector>\n')

def main():
    src = io.open(SRC, encoding='utf-8').read()
    block = src[src.index('const ICONS'):src.index('};')]
    icons = re.findall(r"^\s*(\w+):\s*'(.*)',\s*$", block, re.M)
    os.makedirs(OUT, exist_ok=True)
    for name, body in icons:
        io.open(os.path.join(OUT, f'ic_{snake(name)}.xml'), 'w', encoding='utf-8').write(to_vd(body))
    print(f'{len(icons)} icons -> {os.path.normpath(OUT)}')

if __name__ == '__main__':
    main()
