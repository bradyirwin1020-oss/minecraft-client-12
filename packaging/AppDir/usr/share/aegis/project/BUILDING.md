# Build checklist

## 1. Compile the Fabric client

Use a JDK 25 installation and a Gradle version supported by Fabric Loom:

```bash
cd /path/to/aegis-client
java -version
./build-mod.sh
```

Check `build/libs/aegis-client-2.0.0.jar` exists.

## 2. Run the desktop launcher from source

```bash
python3 -m tkinter  # confirms Tk support is installed
python3 launcher/aegis_launcher.py
```

## 3. Package AppImage

Create a Python virtual environment, install PyInstaller, and make the official AppImageKit `appimagetool` for x86_64 available:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements-build.txt
```

Then:

```bash
APPIMAGETOOL=/path/to/appimagetool ./packaging/build-appimage.sh
```

PyInstaller bundles the Aegis Python/Tk launcher. The launcher downloads and SHA-256 verifies Prism Launcher when needed.
