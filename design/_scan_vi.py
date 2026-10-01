"""List Vietnamese text outside values-vi (hard-coded strings that won't switch with the app language)."""
import glob, io, os, re, sys

sys.stdout.reconfigure(encoding='utf-8')
ROOT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main')
VN = re.compile('[ạảãàáâậầấẩẫăặằắẳẵẹẻẽèéêệềếểễịỉĩìíọỏõòóôộồốổỗơợờớởỡụủũùúưựừứửữỵỷỹỳýđĐ]')

print('--- Kotlin string literals')
for f in glob.glob(os.path.join(ROOT, 'java', '**', '*.kt'), recursive=True):
    for i, line in enumerate(io.open(f, encoding='utf-8'), 1):
        s = line.strip()
        if s.startswith('//') or s.startswith('*'):
            continue
        for m in re.findall(r'"([^"\n]*)"', line):
            if VN.search(m) and 'Tiếng Việt' not in m:
                print(os.path.relpath(f, ROOT), i, m[:90])

print('--- res (except values-vi)')
for f in glob.glob(os.path.join(ROOT, 'res', '**', '*.*'), recursive=True):
    if 'values-vi' in f or not f.endswith(('.xml', '.json')):
        continue
    text = io.open(f, encoding='utf-8', errors='ignore').read()
    for m in re.findall(r'"([^"\n]*)"|>([^<]*)<', text):
        t = m[0] or m[1]
        if VN.search(t):
            print(os.path.relpath(f, ROOT), t.strip()[:90])
