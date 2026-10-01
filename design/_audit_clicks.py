"""Find buttons/clickable views in layouts whose id is never given a click listener in Kotlin."""
import glob, io, os, re, sys
sys.stdout.reconfigure(encoding='utf-8')
R = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main')
kt = '\n'.join(io.open(f, encoding='utf-8').read() for f in glob.glob(os.path.join(R, 'java', '**', '*.kt'), recursive=True))
def camel(s): return re.sub(r'_(\w)', lambda m: m.group(1).upper(), s)
for f in sorted(glob.glob(os.path.join(R, 'res', 'layout', '*.xml'))):
    x = io.open(f, encoding='utf-8').read()
    for m in re.finditer(r'<([\w.]+)\s[^>]*?android:id="@\+id/(\w+)"[^>]*?>', x):
        tag, vid, node = m.group(1), m.group(2), m.group(0)
        clickable = tag.endswith(('Button', 'ImageButton', 'MaterialButton', 'Chip')) or 'clickable="true"' in node or 'IconBtn' in node or 'Btn.' in node
        if not clickable: continue
        if re.search(r'\b' + re.escape(camel(vid)) + r'\b[^\n]{0,80}\.(setOnClickListener|setOnLongClickListener|setOnCheckedChangeListener|performClick)', kt): continue
        if re.search(r'\b' + re.escape(camel(vid)) + r'\.(root\.)?setOnClickListener', kt): continue
        print(os.path.basename(f), vid, tag.split('.')[-1])
