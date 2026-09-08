# StreamShield

**StreamShield** is a client-side privacy mod for Minecraft streaming. It rewrites sensitive text only on your client and can keep selected HUD elements out of OBS Game Capture while they remain visible in-game.

Available for **Fabric** and **NeoForge** on Minecraft **1.21.11** and **26.1.x** (tested on 26.1.2).

## Highlights

- **Player-name privacy** — replaces other player names with stable, per-session aliases across name tags, TAB, chat, and scoreboards.
- **Self-name modes** — hide your own name, use a custom formatted nickname, keep your real name, or use a random alias. Your chosen self-name mode takes priority everywhere.
- **Chat-input sanitization** — optionally hides known player names while you type without changing the message actually sent to the server.
- **Text redaction** — hides configured words, server addresses, server names, and optional strict safety-list terms in rendered text.
- **Scoreboard rules** — create ordered match-and-replace rules with enable, add, delete, and move controls.
- **Localized item-name normalization** — renamed items display their normal localized Minecraft name on your client only; item data, anvil input, and server behavior are unchanged.
- **Skin obfuscation** — render players with Steve, or assign configurable substitute skins from a Mojang-account ID pool.
- **OBS overlay** — redirects selected HUD components, including chat, TAB, scoreboards, titles, effects, hotbar, health, armor, hunger, air, vehicle health, and experience, away from OBS capture while keeping them visible to you. Windows only.

## Compatibility and requirements

Choose the jar matching both your Minecraft version and mod loader.

| Loader | Required dependencies |
| --- | --- |
| Fabric | Fabric Loader, Fabric API, Architectury API, Cloth Config API |
| NeoForge | NeoForge, Architectury API (NeoForge), Cloth Config API (NeoForge) |

| Minecraft | Java |
| --- | --- |
| 1.21.11 | 21+ |
| 26.1.x | 25+ |

Mod Menu is optional on Fabric and provides a convenient **Configure** button. The OBS overlay is Windows-only and is intended for OBS Game Capture; test your capture setup before streaming.

## Usage

Open the mod's Configure screen to manage redaction, names, items, scoreboards, skins, and OBS-overlay behavior.

Custom self names support Minecraft legacy formatting codes such as `§d§l`, plus `&` aliases such as `&d&l`. Use `&&` for a literal ampersand.

## Important notes

- StreamShield changes client-side presentation only. It does not alter server data or outgoing chat messages.
- Some server-rendered scoreboards use unusual rendering paths and may not be fully rewriteable.

## License and attribution

StreamShield is released under the **MIT License**. Its OBS overlay implementation is ported from [obs-overlay](https://github.com/zziger/obs-overlay) by **zziger / Artem Dzhemesiuk**, also under MIT. The required notice and license are bundled with the mod.
