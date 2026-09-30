#!/usr/bin/env bash
# Local build helper (Git Bash on Windows). Usage: ./build.sh [gradle tasks...]  (default: assembleDebug)
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
export PATH="$JAVA_HOME/bin:$PATH"
GW=$(ls -d ~/.gradle/wrapper/dists/gradle-8.13-bin/*/gradle-8.13/bin/gradle 2>/dev/null | head -1)
[ -z "$GW" ] && { echo "Gradle 8.13 not found; open the project once in Android Studio or run gradlew.bat"; exit 1; }
cd "$(dirname "$0")"
"$GW" "${@:-assembleDebug}" --console=plain
