# StreamShield

A **Minecraft (Fabric, 1.21.11) client mod** that keeps stream content safe. It anonymizes player names, normalizes item names, rewrites scoreboard/keyword content, obfuscates skins, and provides an OBS overlay layer so sensitive HUD info never leaks to your stream.

> 一个直播安全的 Minecraft (Fabric, 1.21.11) 客户端模组：匿名化玩家名、归一化物品名、重写计分板关键词、遮蔽皮肤，并提供 OBS 遮罩层，让敏感 HUD 信息不会泄露到直播画面。

## Features / 功能

- **Name anonymization** — real player names become `[Player]+#0000` (your own name can be HIDE / CUSTOM / RANDOM).
- **Item name normalization** — renamed items display their registry name.
- **Content redaction** — keyword/regex + auto-grab server IP & name.
- **Scoreboard mapping** — chain keyword → replacement rules.
- **Skin obfuscation** — force Steve.
- **OBS overlay** — hide/redirect HUD elements to a dedicated overlay target.

## Build / 构建

Requires JDK 21.

```powershell
gradlew.bat build
```

The built jar is at `build/libs/live-hider-1.0.0.jar`.

## License / 许可

This project is released under the **MIT License**. See [LICENSE](LICENSE).

## Third-party attribution / 第三方声明

This project **contains code ported from [OBS Overlay](https://github.com/zziger/obs-overlay)** by **zziger (Artem Dzhemesiuk)**, also licensed under the **MIT License**. In accordance with its terms, the original copyright notice and permission notice are reproduced in [NOTICE.txt](src/main/resources/NOTICE.txt) and bundled inside the jar. See also [Modrinth](https://modrinth.com/mod/obs-overlay).

> 本项目包含对 [OBS Overlay](https://github.com/zziger/obs-overlay)（作者 zziger / Artem Dzhemesiuk，同样为 MIT 许可）代码的移植。按其许可要求，原始版权声明与许可文本见 [NOTICE.txt](src/main/resources/NOTICE.txt)，并已打包进 jar。
