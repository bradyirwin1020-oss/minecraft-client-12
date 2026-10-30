#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APPDIR="$ROOT/packaging/AppDir"
OUT="$ROOT/dist/AegisClient-x86_64.AppImage"
APPIMAGETOOL="${APPIMAGETOOL:-$(command -v appimagetool || true)}"
PYINSTALLER="${PYINSTALLER:-$(command -v pyinstaller || true)}"

if [[ -z "$PYINSTALLER" || ! -x "$PYINSTALLER" ]]; then
  cat >&2 <<'MSG'
PyInstaller was not found. Install the build dependency with:
  python3 -m pip install -r requirements-build.txt
Then run this script again.
MSG
  exit 2
fi
if [[ -z "$APPIMAGETOOL" || ! -x "$APPIMAGETOOL" ]]; then
  cat >&2 <<'MSG'
appimagetool was not found.
Install the official AppImageKit appimagetool and run:
  APPIMAGETOOL=/path/to/appimagetool ./packaging/build-appimage.sh
This project intentionally does not fetch an unverified packaging binary.
MSG
  exit 2
fi

mkdir -p "$APPDIR/usr/share/aegis/project" "$APPDIR/usr/share/aegis/launcher" "$APPDIR/usr/bin" "$ROOT/dist"
rm -rf "$APPDIR/usr/share/aegis/project"/*
cp -a "$ROOT/build.gradle" "$ROOT/settings.gradle" "$ROOT/gradle.properties" \
  "$ROOT/src" "$ROOT/LICENSE" "$ROOT/build-mod.sh" "$ROOT/README.md" \
  "$ROOT/BUILDING.md" "$ROOT/requirements-build.txt" "$APPDIR/usr/share/aegis/project/"
install -m 755 "$ROOT/launcher/aegis_launcher.py" "$APPDIR/usr/share/aegis/launcher/aegis_launcher.py"
chmod +x "$APPDIR/AppRun" "$APPDIR/usr/share/aegis/project/build-mod.sh"

# Freeze Python + Tk into a single executable so the resulting AppImage does not
# depend on the end-user having Python/Tk installed on their system.
"$PYINSTALLER" --clean --noconfirm --onefile --windowed --name aegis-launcher \
  --distpath "$ROOT/dist/pyinstaller" --workpath "$ROOT/build/pyinstaller" \
  "$ROOT/launcher/aegis_launcher.py"
install -m 755 "$ROOT/dist/pyinstaller/aegis-launcher" "$APPDIR/usr/bin/aegis-launcher"
ARCH=x86_64 "$APPIMAGETOOL" "$APPDIR" "$OUT"
echo "Built: $OUT"
echo "The AppImage bundles the Aegis Python/Tk launcher. Prism is downloaded separately and SHA-256 verified on first use."
