# Device test helper: source it, then `shot name`, `tap x y` (dp coords, 360-wide), `ui` (dump clickable texts).
export MSYS_NO_PATHCONV=1
export PYTHONIOENCODING=utf-8
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
SHOTS="C:/Users/ADMIN/Documents/GitHub/IPTVClone/design/device"
mkdir -p "$SHOTS"
D=$("$ADB" shell wm density | tail -1 | awk '{print $NF}' | tr -d '\r')
S=$(python -c "print($D/160)")
shot() { sleep "${2:-1.2}"; "$ADB" exec-out screencap -p > "$SHOTS/$1.png"; python - "$SHOTS/$1.png" <<'EOF'
import sys
from PIL import Image
p = sys.argv[1]; im = Image.open(p); w, h = im.size
im.convert('RGB').resize((w * 2 // 5, h * 2 // 5)).save(p)
EOF
}
tap() { "$ADB" shell input tap $(python -c "print(int($1*$S), int($2*$S))"); }
swipe() { "$ADB" shell input swipe $(python -c "print(int($1*$S), int($2*$S), int($3*$S), int($4*$S))") ${5:-300}; }
key() { "$ADB" shell input keyevent "$1"; }
txt() { "$ADB" shell input text "$1"; }
ui() { "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; "$ADB" exec-out cat /sdcard/ui.xml | python -c "
import sys, re
x = sys.stdin.read(); s = $S
for m in re.finditer(r'<node [^>]*>', x):
    n = m.group(0)
    t = (re.search(r' text=\"([^\"]*)\"', n) or [0, ''])[1]; d = (re.search(r'content-desc=\"([^\"]*)\"', n) or [0, ''])[1]
    i = re.search(r'resource-id=\"[^\"]*/([^\"]*)\"', n); b = list(map(int, re.findall(r'\d+', re.search(r'bounds=\"([^\"]*)\"', n).group(1))))
    if t or d or (i and 'clickable=\"true\"' in n):
        print(f'{(b[0]+b[2])//2/s:.0f},{(b[1]+b[3])//2/s:.0f}', i.group(1) if i else '', repr(t or d)[:60])
"; }
crash() { "$ADB" logcat -d -b crash | tail -40; }
# tapid <resource-id or text> [index]: tap the n-th node whose id or text matches exactly.
tapid() { local p; p=$(ui | awk -v q="$1" -v n="${2:-1}" '{ t=$0; sub(/^[^ ]+ /, "", t); id=t; sub(/ .*/, "", id); txt=t; sub(/^[^ ]* /, "", txt); gsub(/^'\''|'\''$/, "", txt); if (id==q || txt==q) { c++; if (c==n) { print $1; exit } } }'); [ -z "$p" ] && { echo "not found: $1"; return 1; }; tap ${p%,*} ${p#*,}; }
# portrait: lock the phone upright (user may rotate it while tests run)
portrait() { "$ADB" shell settings put system accelerometer_rotation 0; "$ADB" shell settings put system user_rotation 0; sleep 1; }
top() { "$ADB" shell dumpsys activity activities | grep -m1 topResumedActivity | grep -oE '[a-z.]+/[A-Za-z.]+' | sed 's#com.iptvplayer.app/##'; }
