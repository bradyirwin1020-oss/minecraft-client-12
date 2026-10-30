#!/usr/bin/env python3
"""Aegis Client launcher shell for Linux. Microsoft credentials are never collected here."""
from __future__ import annotations

import hashlib
import json
import os
import shutil
import subprocess
import sys
import threading
import urllib.request
import zipfile
from pathlib import Path
import tkinter as tk
from tkinter import ttk, filedialog, messagebox

APP_NAME = "Aegis Client"
MINECRAFT_VERSION = "26.1.2"
FABRIC_LOADER_VERSION = "0.19.2"
FABRIC_API_VERSION = "0.155.3+26.1.2"
FABRIC_API_URL = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.155.3%2B26.1.2/fabric-api-0.155.3%2B26.1.2.jar"
PRISM_VERSION = "11.1.1"
PRISM_URL = (
    "https://github.com/PrismLauncher/PrismLauncher/releases/download/11.1.1/"
    "PrismLauncher-Linux-x86_64.AppImage"
)
PRISM_SHA256 = "bb81038c56a09e944659e4b808dbfbc52d52d5e3a3ffe32216689a3ca1508d3d"

HOME = Path.home()
AEGIS_HOME = Path(os.environ.get("XDG_DATA_HOME", HOME / ".local/share")) / "aegis-client"
RUNTIME_DIR = AEGIS_HOME / "runtime"
PACK_DIR = AEGIS_HOME / "packs"
PRISM_APPIMAGE = RUNTIME_DIR / "PrismLauncher.AppImage"
PROJECT_DIR = Path(os.environ.get("AEGIS_PROJECT_DIR", Path(__file__).resolve().parents[1])).expanduser().resolve()
BUILT_JAR = PROJECT_DIR / "build/libs/aegis-client-2.0.0.jar"

PRISM_ROOTS = [
    Path(os.environ.get("XDG_DATA_HOME", HOME / ".local/share")) / "PrismLauncher",
    HOME / ".var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher",
]

class AegisLauncher(tk.Tk):
    def __init__(self) -> None:
        super().__init__()
        self.title("Aegis Client Launcher")
        self.geometry("900x610")
        self.minsize(780, 540)
        self.configure(bg="#0b111c")
        self.instance_map: dict[str, tuple[Path, str]] = {}
        self.status = tk.StringVar(value="Ready · Minecraft Java 26.1.2")
        self.instance_var = tk.StringVar()
        self._style()
        self._build_ui()
        self.refresh_instances()

    def _style(self) -> None:
        style = ttk.Style(self)
        style.theme_use("clam")
        style.configure("TFrame", background="#0b111c")
        style.configure("Card.TFrame", background="#111d2c")
        style.configure("TLabel", background="#0b111c", foreground="#e7f0fb", font=("Sans", 10))
        style.configure("Muted.TLabel", background="#111d2c", foreground="#9aacbf", font=("Sans", 9))
        style.configure("Title.TLabel", background="#0b111c", foreground="#8bd7ff", font=("Sans", 24, "bold"))
        style.configure("SubTitle.TLabel", background="#0b111c", foreground="#9aacbf", font=("Sans", 10))
        style.configure("CardTitle.TLabel", background="#111d2c", foreground="#eaf5ff", font=("Sans", 12, "bold"))
        style.configure("Aegis.TButton", background="#1d85b8", foreground="#ffffff", padding=(13, 9), borderwidth=0, font=("Sans", 10, "bold"))
        style.map("Aegis.TButton", background=[("active", "#299dce"), ("disabled", "#425568")])
        style.configure("Secondary.TButton", background="#1c2a3c", foreground="#d9e7f4", padding=(12, 8), borderwidth=0)
        style.map("Secondary.TButton", background=[("active", "#2a3b50")])
        style.configure("TCombobox", fieldbackground="#162436", background="#162436", foreground="#e7f0fb", arrowcolor="#8bd7ff", padding=7)
        style.map("TCombobox", fieldbackground=[("readonly", "#162436")], foreground=[("readonly", "#e7f0fb")])
        style.configure("Horizontal.TProgressbar", troughcolor="#172536", background="#46b5e8")

    def _build_ui(self) -> None:
        outer = ttk.Frame(self, padding=24)
        outer.pack(fill="both", expand=True)

        header = ttk.Frame(outer)
        header.pack(fill="x")
        ttk.Label(header, text="AEGIS", style="Title.TLabel").pack(side="left")
        ttk.Label(header, text="CLIENT  /  PVP INFORMATION SUITE", style="SubTitle.TLabel").pack(side="left", padx=(14, 0), pady=(10, 0))
        ttk.Label(outer, text="A focused Minecraft profile with a custom in-game HUD, visual tools and defensive modules.", style="SubTitle.TLabel").pack(anchor="w", pady=(3, 18))

        hero = ttk.Frame(outer, style="Card.TFrame", padding=18)
        hero.pack(fill="x")
        ttk.Label(hero, text="PLAY", style="CardTitle.TLabel").pack(anchor="w")
        ttk.Label(hero, text="Choose your Aegis installation. First-time setup opens the trusted launcher for Microsoft sign-in.", style="Muted.TLabel").pack(anchor="w", pady=(5, 13))

        row = ttk.Frame(hero, style="Card.TFrame")
        row.pack(fill="x")
        self.instance_box = ttk.Combobox(row, textvariable=self.instance_var, state="readonly", width=42)
        self.instance_box.pack(side="left", fill="x", expand=True)
        ttk.Button(row, text="Refresh", style="Secondary.TButton", command=self.refresh_instances).pack(side="left", padx=(8, 0))
        ttk.Button(row, text="LAUNCH AEGIS", style="Aegis.TButton", command=self.launch_selected).pack(side="left", padx=(10, 0))

        grid = ttk.Frame(outer)
        grid.pack(fill="both", expand=True, pady=16)
        left = ttk.Frame(grid, style="Card.TFrame", padding=16)
        right = ttk.Frame(grid, style="Card.TFrame", padding=16)
        left.pack(side="left", fill="both", expand=True, padx=(0, 8))
        right.pack(side="left", fill="both", expand=True, padx=(8, 0))

        ttk.Label(left, text="ACCOUNT & GAME", style="CardTitle.TLabel").pack(anchor="w")
        ttk.Label(left, text="Microsoft authentication stays inside Prism Launcher. Aegis never asks for your Microsoft password or stores auth tokens.", style="Muted.TLabel", wraplength=355).pack(anchor="w", pady=(7, 13))
        ttk.Button(left, text="Sign in / Manage Microsoft account", style="Secondary.TButton", command=self.open_prism).pack(fill="x", pady=4)
        ttk.Button(left, text="Create Aegis 26.1.2 profile", style="Secondary.TButton", command=self.create_profile).pack(fill="x", pady=4)
        ttk.Button(left, text="Import worlds & resource packs", style="Secondary.TButton", command=self.import_game_data).pack(fill="x", pady=4)
        ttk.Button(left, text="Open selected game folder", style="Secondary.TButton", command=self.open_game_folder).pack(fill="x", pady=4)

        ttk.Label(right, text="CLIENT TOOLS", style="CardTitle.TLabel").pack(anchor="w")
        ttk.Label(right, text="Build and install the Aegis Fabric client into a Prism instance.", style="Muted.TLabel").pack(anchor="w", pady=(7, 13))
        ttk.Button(right, text="Build Aegis mod (JDK 25 + Gradle)", style="Secondary.TButton", command=self.build_mod).pack(fill="x", pady=4)
        ttk.Button(right, text="Install built mod into selected profile", style="Secondary.TButton", command=self.install_mod).pack(fill="x", pady=4)
        ttk.Button(right, text="Download / open Prism Launcher", style="Secondary.TButton", command=self.open_prism).pack(fill="x", pady=4)
        ttk.Button(right, text="Reaction-time practice", style="Secondary.TButton", command=self.reaction_trainer).pack(fill="x", pady=4)
        ttk.Label(right, text="In-game menu: RIGHT SHIFT\nModule settings and HUD layout save locally.", style="Muted.TLabel").pack(anchor="w", pady=(14, 0))

        bottom = ttk.Frame(outer)
        bottom.pack(fill="x")
        ttk.Separator(bottom).pack(fill="x", pady=(0, 9))
        self.progress = ttk.Progressbar(bottom, mode="determinate", maximum=100)
        self.progress.pack(fill="x", pady=(0, 7))
        ttk.Label(bottom, textvariable=self.status, style="SubTitle.TLabel").pack(anchor="w")

    def set_status(self, text: str) -> None:
        self.after(0, self.status.set, text)

    def prism_command(self) -> list[str] | None:
        candidates = [
            PRISM_APPIMAGE,
            HOME / "Applications/PrismLauncher-Linux-x86_64.AppImage",
            HOME / ".local/bin/PrismLauncher.AppImage",
            HOME / "Downloads/PrismLauncher-Linux-x86_64.AppImage",
        ]
        for path in candidates:
            if path.is_file():
                try:
                    path.chmod(path.stat().st_mode | 0o111)
                except OSError:
                    pass
                return [str(path), "--appimage-extract-and-run"]
        binary = shutil.which("prismlauncher") or shutil.which("PrismLauncher")
        if binary:
            return [binary]
        if shutil.which("flatpak"):
            try:
                result = subprocess.run(["flatpak", "info", "org.prismlauncher.PrismLauncher"], capture_output=True, text=True)
                if result.returncode == 0:
                    return ["flatpak", "run", "org.prismlauncher.PrismLauncher"]
            except OSError:
                pass
        return None

    def ensure_prism(self) -> list[str] | None:
        command = self.prism_command()
        if command:
            return command
        if not messagebox.askyesno("Install launcher engine", "Prism Launcher is not installed. Download its official x86_64 AppImage?\n\nThe download is verified against the SHA-256 published for the selected release."):
            return None
        self.download_prism()
        return None

    def download_prism(self) -> None:
        def worker() -> None:
            try:
                RUNTIME_DIR.mkdir(parents=True, exist_ok=True)
                part = PRISM_APPIMAGE.with_suffix(".AppImage.part")
                digest = hashlib.sha256()
                request = urllib.request.Request(PRISM_URL, headers={"User-Agent": "AegisClientLauncher/2.0"})
                with urllib.request.urlopen(request, timeout=30) as response, open(part, "wb") as output:
                    total = int(response.headers.get("Content-Length", "0"))
                    received = 0
                    while True:
                        chunk = response.read(1024 * 1024)
                        if not chunk:
                            break
                        output.write(chunk)
                        digest.update(chunk)
                        received += len(chunk)
                        if total:
                            self.after(0, lambda value=min(100, received * 100 / total): self.progress.configure(value=value))
                if digest.hexdigest().lower() != PRISM_SHA256:
                    part.unlink(missing_ok=True)
                    raise RuntimeError("SHA-256 verification failed. The downloaded file was discarded.")
                part.replace(PRISM_APPIMAGE)
                PRISM_APPIMAGE.chmod(0o755)
                self.set_status("Prism Launcher downloaded and verified. Press Sign in / Manage Microsoft account.")
                self.after(0, lambda: messagebox.showinfo("Ready", "Prism Launcher is installed and verified. Opening it so you can sign in."))
                self.after(0, self.open_prism)
            except Exception as exc:
                self.set_status(f"Prism download failed: {exc}")
                self.after(0, lambda: messagebox.showerror("Download failed", str(exc)))
        self.set_status("Downloading official Prism Launcher…")
        threading.Thread(target=worker, daemon=True).start()

    def open_prism(self) -> None:
        command = self.prism_command()
        if not command:
            self.ensure_prism()
            return
        try:
            subprocess.Popen(command, start_new_session=True)
            self.set_status("Prism Launcher opened. Complete Microsoft sign-in there; Aegis does not handle credentials.")
        except OSError as exc:
            messagebox.showerror("Could not open launcher", str(exc))

    def roots(self) -> list[Path]:
        override = os.environ.get("PRISMLAUNCHER_LAUNCHER_DIR")
        roots = [Path(override).expanduser()] if override else list(PRISM_ROOTS)
        return roots

    def refresh_instances(self) -> None:
        self.instance_map.clear()
        entries: list[str] = []
        for root in self.roots():
            instances = root / "instances"
            if not instances.is_dir():
                continue
            for folder in sorted(instances.iterdir()):
                if not folder.is_dir():
                    continue
                inst_id = folder.name
                name = inst_id
                cfg = folder / "instance.cfg"
                if cfg.is_file():
                    try:
                        for line in cfg.read_text(errors="replace").splitlines():
                            if line.startswith("name="):
                                name = line.split("=", 1)[1].strip() or inst_id
                                break
                    except OSError:
                        pass
                label = f"{name}  [{inst_id}]"
                self.instance_map[label] = (folder, inst_id)
                entries.append(label)
        self.instance_box["values"] = entries
        if entries:
            current = self.instance_var.get()
            self.instance_var.set(current if current in entries else entries[0])
            self.set_status(f"Found {len(entries)} Prism instance(s).")
        else:
            self.instance_var.set("")
            self.set_status("No Prism instances yet. Sign in, then create an Aegis profile.")

    def selected_instance(self) -> tuple[Path, str] | None:
        return self.instance_map.get(self.instance_var.get())

    @staticmethod
    def minecraft_dir(instance_folder: Path) -> Path:
        for name in ("minecraft", ".minecraft"):
            candidate = instance_folder / name
            if candidate.is_dir():
                return candidate
        # Prism's modern per-instance game directory uses "minecraft".
        return instance_folder / "minecraft"

    def launch_selected(self) -> None:
        selected = self.selected_instance()
        command = self.prism_command()
        if not command:
            self.ensure_prism()
            return
        if not selected:
            messagebox.showinfo("No profile selected", "Create an Aegis profile or select an existing Prism instance first.")
            self.open_prism()
            return
        _, inst_id = selected
        try:
            subprocess.Popen(command + ["--launch", inst_id], start_new_session=True)
            self.set_status(f"Launching {inst_id} through Prism Launcher…")
        except OSError as exc:
            messagebox.showerror("Launch failed", str(exc))

    def find_mod_jar(self) -> Path | None:
        if BUILT_JAR.is_file():
            return BUILT_JAR
        candidates = sorted((PROJECT_DIR / "build/libs").glob("aegis-client-*.jar")) if (PROJECT_DIR / "build/libs").is_dir() else []
        return candidates[-1] if candidates else None

    def create_profile(self) -> None:
        command = self.prism_command()
        if not command:
            self.ensure_prism()
            return
        jar = self.find_mod_jar()
        if not jar:
            messagebox.showwarning("Build required", "Build the Aegis mod first. This requires JDK 25 and Gradle.\n\nThen press Create Aegis 26.1.2 profile again.")
            return

        def worker() -> None:
            try:
                PACK_DIR.mkdir(parents=True, exist_ok=True)
                fabric_api_jar = PACK_DIR / f"fabric-api-{FABRIC_API_VERSION}.jar"
                checksum_url = FABRIC_API_URL + ".sha1"

                # Download Fabric API and verify it against the checksum served by Fabric's Maven.
                request = urllib.request.Request(FABRIC_API_URL, headers={"User-Agent": "AegisClientLauncher/2.0"})
                with urllib.request.urlopen(request, timeout=45) as response:
                    artifact = response.read(32 * 1024 * 1024 + 1)
                if len(artifact) > 32 * 1024 * 1024:
                    raise RuntimeError("Fabric API download exceeded the safety size limit.")
                check_request = urllib.request.Request(checksum_url, headers={"User-Agent": "AegisClientLauncher/2.0"})
                with urllib.request.urlopen(check_request, timeout=20) as response:
                    expected_sha1 = response.read(256).decode("ascii", errors="strict").strip().split()[0].lower()
                actual_sha1 = hashlib.sha1(artifact).hexdigest()
                if actual_sha1 != expected_sha1:
                    raise RuntimeError("Fabric API checksum verification failed; profile creation was cancelled.")
                fabric_api_jar.write_bytes(artifact)

                pack = PACK_DIR / "Aegis-Client-26.1.2.mrpack"
                manifest = {
                    "formatVersion": 1,
                    "game": "minecraft",
                    "versionId": "aegis-26.1.2",
                    "name": "Aegis Client 26.1.2",
                    "summary": "Aegis client-side HUD, PvP information, visual, training and defensive modules.",
                    "files": [],
                    "dependencies": {
                        "minecraft": MINECRAFT_VERSION,
                        "fabric-loader": FABRIC_LOADER_VERSION
                    }
                }
                with zipfile.ZipFile(pack, "w", compression=zipfile.ZIP_DEFLATED) as archive:
                    archive.writestr("modrinth.index.json", json.dumps(manifest, indent=2))
                    archive.write(jar, f"overrides/mods/{jar.name}")
                    archive.write(fabric_api_jar, f"overrides/mods/{fabric_api_jar.name}")
                subprocess.Popen(command + ["-I", str(pack)], start_new_session=True)
                self.set_status("Aegis profile import started. Finish the import in Prism, then refresh profiles.")
                self.after(0, lambda: messagebox.showinfo("Import started", "Prism is importing the Aegis profile with Fabric API included. Finish the import in Prism, then press Refresh."))
            except Exception as exc:
                self.set_status(f"Profile creation failed: {exc}")
                self.after(0, lambda: messagebox.showerror("Profile creation failed", str(exc)))

        self.set_status("Preparing Aegis profile and verifying Fabric API…")
        threading.Thread(target=worker, daemon=True).start()

    def build_mod(self) -> None:
        build_script = PROJECT_DIR / "build-mod.sh"
        if not build_script.is_file():
            messagebox.showerror("Build script missing", f"Could not locate {build_script}")
            return

        def worker() -> None:
            try:
                result = subprocess.run([str(build_script)], cwd=PROJECT_DIR, capture_output=True, text=True)
                if result.returncode != 0:
                    tail = (result.stdout + "\n" + result.stderr)[-5000:]
                    raise RuntimeError(tail or "Build failed. Check that JDK 25 and Gradle are installed.")
                self.set_status(f"Build successful: {BUILT_JAR.name}")
                self.after(0, lambda: messagebox.showinfo("Build complete", f"Aegis mod built successfully:\n{BUILT_JAR}"))
            except Exception as exc:
                self.set_status("Build failed. See details.")
                self.after(0, lambda: messagebox.showerror("Build failed", str(exc)))

        self.set_status("Building Aegis with JDK 25 and Gradle…")
        threading.Thread(target=worker, daemon=True).start()

    def install_mod(self) -> None:
        selected = self.selected_instance()
        if not selected:
            messagebox.showinfo("Select a profile", "Choose a Prism instance first.")
            return
        jar = self.find_mod_jar()
        if not jar:
            chosen = filedialog.askopenfilename(title="Select Aegis mod JAR", filetypes=[("Java mod", "*.jar")])
            if not chosen:
                return
            jar = Path(chosen)
            if not jar.is_file():
                return
        folder, inst_id = selected
        pack_file = folder / "mmc-pack.json"
        if pack_file.is_file():
            try:
                metadata = json.loads(pack_file.read_text())
                components = metadata.get("components", [])
                has_fabric = any("fabric" in str(component.get("uid", "")).lower() for component in components)
                if not has_fabric:
                    if not messagebox.askyesno("Fabric not detected", "This instance does not appear to include Fabric. Install Aegis anyway? It may not load until Fabric is added."):
                        return
            except (OSError, json.JSONDecodeError):
                pass
        mods = self.minecraft_dir(folder) / "mods"
        mods.mkdir(parents=True, exist_ok=True)
        destination = mods / jar.name
        shutil.copy2(jar, destination)
        self.set_status(f"Installed {jar.name} into {inst_id}.")
        messagebox.showinfo("Installed", f"Aegis was copied to:\n{destination}\n\nRestart Minecraft if it is already running.")

    def import_game_data(self) -> None:
        selected = self.selected_instance()
        if not selected:
            messagebox.showinfo("Select a profile", "Choose a Prism instance first.")
            return
        source = filedialog.askdirectory(title="Choose the existing .minecraft folder")
        if not source:
            return
        source_path = Path(source).expanduser()
        if not (source_path / "saves").exists() and not (source_path / "resourcepacks").exists():
            if not messagebox.askyesno("Folder check", "This folder does not look like a .minecraft folder. Copy common game data from it anyway?"):
                return
        instance_folder, inst_id = selected
        destination = self.minecraft_dir(instance_folder)
        destination.mkdir(parents=True, exist_ok=True)
        copied = []
        for name in ("saves", "resourcepacks", "screenshots", "shaderpacks", "options.txt", "servers.dat"):
            item = source_path / name
            if not item.exists():
                continue
            target = destination / name
            try:
                if item.is_dir():
                    shutil.copytree(item, target, dirs_exist_ok=True)
                else:
                    shutil.copy2(item, target)
                copied.append(name)
            except OSError as exc:
                messagebox.showwarning("Partial import", f"Could not copy {name}: {exc}")
        self.set_status(f"Imported {len(copied)} item(s) into {inst_id}.")
        messagebox.showinfo("Import complete", "Copied: " + (", ".join(copied) if copied else "no matching game data found") + "\n\nOriginal files were left untouched.")

    def reaction_trainer(self) -> None:
        import time
        import random
        window = tk.Toplevel(self)
        window.title("Aegis Reaction Trainer")
        window.geometry("420x280")
        window.configure(bg="#101b29")
        window.transient(self)
        window.grab_set()
        state = {"armed": False, "started": 0.0, "after_id": None}
        title = tk.Label(window, text="REACTION TRAINER", bg="#101b29", fg="#8bd7ff", font=("Sans", 17, "bold"))
        title.pack(pady=(24, 8))
        info = tk.Label(window, text="Press Start, wait for the panel to turn blue, then click it.", bg="#101b29", fg="#a8bacd", wraplength=350)
        info.pack(pady=8)
        result = tk.Label(window, text="Ready", bg="#101b29", fg="#e6f0fa", font=("Sans", 12))
        result.pack(pady=8)
        area = tk.Button(window, text="START", bg="#24364a", fg="#ffffff", activebackground="#338ab5", relief="flat", width=22, height=3)
        area.pack(pady=16)

        def ready() -> None:
            state["armed"] = True
            state["started"] = time.perf_counter()
            area.configure(text="CLICK NOW", bg="#1686bd", state="normal")
            result.configure(text="Go!")

        def press() -> None:
            if not state["armed"]:
                if area.cget("text") == "START":
                    area.configure(text="WAIT…", state="disabled")
                    info.configure(text="Wait for the signal. Clicking early resets the attempt.")
                    state["after_id"] = window.after(random.randint(1200, 4200), ready)
                return
            elapsed = (time.perf_counter() - state["started"]) * 1000.0
            state["armed"] = False
            result.configure(text=f"Reaction: {elapsed:.0f} ms")
            area.configure(text="TRY AGAIN", bg="#24364a", state="normal")

        area.configure(command=press)
        window.protocol("WM_DELETE_WINDOW", lambda: (window.after_cancel(state["after_id"]) if state["after_id"] else None, window.destroy()))

    def open_game_folder(self) -> None:
        selected = self.selected_instance()
        if not selected:
            messagebox.showinfo("Select a profile", "Choose a Prism instance first.")
            return
        folder = self.minecraft_dir(selected[0])
        folder.mkdir(parents=True, exist_ok=True)
        try:
            subprocess.Popen(["xdg-open", str(folder)], start_new_session=True)
        except OSError as exc:
            messagebox.showerror("Could not open folder", str(exc))

if __name__ == "__main__":
    try:
        app = AegisLauncher()
        app.mainloop()
    except tk.TclError as exc:
        print(f"Aegis Launcher could not open its GUI: {exc}", file=sys.stderr)
        print("Install Python 3 with Tk support or run Prism Launcher directly.", file=sys.stderr)
        raise SystemExit(2)
