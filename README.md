# Aegis Client 2.0.0 — Minecraft Java 26.1.2 / Linux

Aegis is a client-side Fabric client plus a Linux desktop launcher shell. It is designed around PvP information, configurable HUD/visuals, defensive inventory assistance, and a practice utility. It does **not** automate attacks, aim at players, change reach/hitboxes/knockback, or manipulate combat packets.

## What is included

### In-game client

- Custom Aegis control menu: press **Right Shift**.
- Module categories for HUD, PvP information, visuals, movement, defense and performance.
- CPS counter (left/right click events), keystrokes, FPS, ping, coordinates/direction, armor durability, potion-effect count, attack cooldown and a basic crosshair target HUD.
- Persistent per-module toggles and HUD settings in `config/aegis-client.properties`.
- HUD position X/Y, scale, five accent palettes and panel opacity controls.
- Optional AutoArmor, AutoTotem and Surround defensive modules.

### Linux desktop launcher

- Aegis-branded launcher interface.
- Microsoft account sign-in is delegated to the official Prism Launcher UI; Aegis does not collect or save Microsoft passwords or OAuth tokens.
- Downloads the official Prism Launcher x86_64 AppImage on first use and checks it against the SHA-256 value published for Prism Launcher 11.1.1.
- Creates a Modrinth-format `.mrpack` containing the built Aegis JAR, Fabric API (downloaded from Fabric Maven and checked against its published SHA-1), and Minecraft/Fabric 26.1.2 profile metadata, then asks Prism to import it.
- Launches a selected Prism instance through Prism's documented CLI.
- Can copy saves, resource packs, shader packs, screenshots and common settings from an existing `.minecraft` folder. It copies files; it does not delete the originals.
- Has a simple reaction-time practice panel.

## Important distinction: launcher engine vs. Aegis client

The Aegis launcher window is our own application. Prism Launcher is used as its trusted Minecraft installation/account/launch engine. This is intentional: Microsoft authentication and Minecraft asset/version resolution are complicated and security-sensitive, and Prism already supports Microsoft accounts and has a documented CLI. Aegis never asks you to type your Microsoft password into the Aegis window.

The Prism window is external and will retain Prism's own branding. Aegis is not affiliated with or endorsed by Prism Launcher.

## Run the Aegis desktop shell from source

With Python 3 and Tk installed, run:

```bash
./run-aegis-launcher.sh
```

The first Microsoft-account action downloads and verifies the official Prism AppImage, then opens Prism for sign-in. Once signed in, return to Aegis.

## Build requirements

Minecraft 26.1.2 requires **Java 25**. Install JDK 25 and a Gradle version supported by the configured Fabric Loom release, then run:

```bash
java -version
gradle --version
gradle --no-daemon build
```

The built mod should appear at:

```text
build/libs/aegis-client-2.0.0.jar
```

Dependencies are pinned in `gradle.properties`:

- Minecraft `26.1.2`
- Fabric Loader `0.19.2`
- Fabric API `0.155.3+26.1.2`
- Fabric Loom `1.18-SNAPSHOT`
- Java toolchain `25`

## Create a profile and launch

1. Open Aegis Launcher.
2. Select **Sign in / Manage Microsoft account**. Prism opens. Add your Microsoft account using Prism's Microsoft sign-in page. Never enter your Microsoft password into Aegis.
3. Build the Aegis mod with JDK 25 and Gradle.
4. Return to Aegis and choose **Create Aegis 26.1.2 profile**. The launcher creates an `.mrpack` with Minecraft 26.1.2, Fabric Loader 0.19.2, Fabric API, and the Aegis JAR, then invokes Prism's import command.
5. Press **Refresh**, select **Aegis Client 26.1.2**, and press **Launch Aegis**. Prism will use the selected/default Microsoft account and its managed Java runtime.

If the import dialog needs confirmation, complete it in Prism and return to Aegis to refresh the instance list.

## Linux AppImage packaging

The repository includes an AppDir and a packaging script. PyInstaller bundles the Aegis Python/Tk launcher into the AppImage; Prism remains a separate official AppImage and is installed on first use:

```bash
./packaging/build-appimage.sh
```

The script requires PyInstaller and `appimagetool` to be installed. Install the Python build dependency with `python3 -m pip install -r requirements-build.txt`; then run it. Set `APPIMAGETOOL` if the packager is not on PATH:

```bash
APPIMAGETOOL=/path/to/appimagetool ./packaging/build-appimage.sh
```

Output path:

```text
dist/AegisClient-x86_64.AppImage
```

**Runtime note:** the AppImage bundles the Aegis desktop launcher, but it downloads Prism's separately distributed official AppImage on first use. It does not bundle Minecraft or Java; Prism handles game files, Microsoft sign-in, and selecting a compatible Java runtime.

## Existing Minecraft data

Choose a Prism instance, click **Import worlds & resource packs**, and select the existing `.minecraft` folder. It copies `saves`, `resourcepacks`, `screenshots`, `shaderpacks`, `options.txt`, and `servers.dat` into the selected profile. Existing files are not deleted; same-name files may be overwritten in the destination, so back up important worlds before importing.

## Module behavior / limitations

- **CPS** records mouse press events through a client mixin. If a later Minecraft update renames the internal mouse callback, the optional mixin may not activate; the client should still load because the injector is non-required, but CPS may stay at zero.
- **Ping** shows the latency value provided by the connected server connection. Single-player has no server ping.
- **Target HUD** only reads information about the entity currently under the crosshair; it cannot reveal hidden opponent inventory or health information.
- **Potion HUD** currently reports the number of active effects, not each effect's exact timer.
- **AutoArmor** compares standard armor by material tier and does not evaluate all enchantments/attribute combinations.
- **AutoTotem** and **Surround** use ordinary client inventory/block actions. Server rules, inventory synchronization, permissions and anti-cheat remain authoritative; they may be rejected by a server.
- The in-game HUD settings currently control position, scale, opacity and theme; widgets are rendered as one grouped panel rather than freely draggable independent cards.
- The practice tool is a standalone reaction-time test; it is not a full gameplay replay or aim-training system.

## Build and verification status

Source and launcher files are included. This development environment only has Java 21, no Gradle installation, no AppImage packager, and cannot resolve external download hosts. Therefore a compiled JAR and a finished `.AppImage` could not be produced or launched here. Build commands are included, but the current Java sources still require a real Java 25 / Fabric build and in-game test before use. Do not treat this source archive as a tested release binary.

## License

Aegis project code is MIT-licensed. Prism Launcher remains a separate third-party application and is downloaded from its official release page; its own GPL-3.0-only license applies to Prism itself.
