# Bountu Windows Installer (Client Frontend)

This creates a Windows installer for the Bountu client and enforces install to a local fixed drive. It also sets up a first‑launch update check.

## Prerequisites
- Windows 10/11
- Inno Setup 6 (download: https://jrsoftware.org/isinfo.php)
- Your built client files (EXE + resources). By default we expect them under `dist\win-unpacked\*` relative to this repo root. You can change `Source:` in `bountu.iss`.

## What it does
- Enforces install to a local HDD/SSD (blocks removable/network drives)
- Installs app to `%ProgramFiles%\Bountu` by default
- Registers a RunOnce entry to execute `check_update.ps1` on the first launch after install
- Adds Start Menu shortcuts

## Build steps
1. Open `installer\bountu.iss` in Inno Setup.
2. (Optional) Edit `#define MyAppVersion` and `#define SourceDir` to match your build path.
3. Compile (Build → Compile). The resulting `BountuSetup.exe` will be in `installer\Output`.

## First‑launch update check
- The script `installer\scripts\check_update.ps1` runs once after the first user login post‑install.
- It checks the latest version from GitHub Releases (configure `Owner`/`Repo`) and prompts to update if a newer version exists.

## Customize
- Update URL & repo in `installer\scripts\check_update.ps1`.
- Change install directory, icons, and additional tasks in `bountu.iss`.
