# Omni Launcher

Omni Launcher is a Lawnchair 15-based Android launcher focused on stability and performance on low-RAM devices.

## Project status

- This repository currently targets personal use and validation on low-tier hardware.
- The primary goal is smooth and reliable home-screen performance with minimal lag or freezing.
- Launcher engine internals come from the upstream Launcher3/Lawnchair stack; this fork focuses on controlled, reviewable customization.

## Branding notes

- App display name has been rebranded to **Omni Launcher**.
- Current app icon wiring uses **placeholder Omni icon resources** that still reference inherited Lawnchair assets.
- Final Omni icon artwork should replace the referenced assets in:
  - `res/mipmap-*/ic_launcher_home_background.png`
  - `res/mipmap-*/ic_launcher_home_foreground.png`
  - `res/drawable/ic_launcher_home_monochrome.xml`

## Attribution and license

This project includes code from:

- Android Open Source Project (Launcher3)
- Lawnchair Launcher

Both the original and modified code in this repository are distributed under the Apache License, Version 2.0.  
See [`LICENSE.txt`](LICENSE.txt) for full license text and copyright notices.
