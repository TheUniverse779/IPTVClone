#!/usr/bin/env bash
# Usage: bash design/_shoot.sh name "hash" [name "hash" ...]  → design/_shots/<name>.png (phone only)
CH="C:/Program Files/Google/Chrome/Application/chrome.exe"
OUT="C:/Users/ADMIN/Documents/GitHub/IPTVClone/design/screenshots/raw"
mkdir -p "$OUT"
while [ $# -ge 2 ]; do
  name="$1"; hash="$2"; shift 2
  w=400; h=840
  case "$hash" in *land*) w=840; h=400;; esac
  "$CH" --headless=new --disable-gpu --hide-scrollbars --window-size=$w,$h --virtual-time-budget=3500 \
    --screenshot="$OUT/$name.png" "http://localhost:5173/prototype.html#bare=1&$hash" >/dev/null 2>&1
  echo "$name"
done
