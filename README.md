**[English](README.md) | [简体中文](README.zh.md)**

# StreamShield

A **Minecraft (Fabric, 26.1.2) client mod** that keeps your stream safe. It anonymizes player names, normalizes item names, rewrites scoreboard/keyword content, obfuscates skins, and provides an OBS overlay layer so sensitive HUD info never leaks to your stream.

## Features

- **Name anonymization** — real player names become `[Player]+#0000` (your own name can be `HIDE` / `CUSTOM` / `RANDOM`).
- **Item name normalization** — renamed items display their registry name.
- **Content redaction** — keyword/regex matching + auto-grab of the server's IP & name.
- **Scoreboard mapping** — chain keyword → replacement rules.
- **Skin obfuscation** — force Steve.
- **OBS overlay** — hide/redirect HUD elements to a dedicated overlay target.

## Screenshots

| Original | Name anonymization |
| --- | --- |
| ![Original](docs/default.jpg) | ![Hide player names](docs/name-hiding.jpg) |

| Skin obfuscation (Steve) | Custom scoreboard |
| --- | --- |
| ![Skin obfuscation](docs/skin-hiding.jpg) | ![Custom scoreboard](docs/scoreboard.jpg) |

| Item name normalization | OBS overlay (full) |
| --- | --- |
| ![Item name normalization](docs/item-normalize.jpg) | ![OBS overlay](docs/overlay.jpg) |

## Requirements

- Minecraft **26.1.2** (Fabric loader) + Java **25**
- Fabric API, Architectury, Cloth Config (see [`fabric.mod.json`](src/main/resources/fabric.mod.json))

## Build

Requires JDK 25+ (Java 26 works).

```powershell
gradlew.bat build
```

The jar is output to `build/libs/[26.1.2]StreamShield-1.1.0.jar`.

## CI / Releases

GitHub Actions builds on every push and,  publishes a **GitHub Release** with the built jar. See [`.github/workflows/build.yml`](.github/workflows/build.yml).

## Publishing to Modrinth

License is **MIT**  on Modrinth.

## License / Attribution

This project is released under the **MIT License**. See [`LICENSE`](LICENSE).

It contains code ported from [OBS Overlay](https://github.com/zziger/obs-overlay) by **zziger (Artem Dzhemesiuk)**, also under the **MIT License**. In accordance with its terms, the original copyright notice and permission notice are reproduced in [`NOTICE.txt`](src/main/resources/NOTICE.txt) and [`licenses/OBS_OVERLAY_LICENSE.txt`](licenses/OBS_OVERLAY_LICENSE.txt), and are bundled inside the jar.
