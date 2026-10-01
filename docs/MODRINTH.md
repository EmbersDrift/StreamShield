# StreamShield

**StreamShield** is a client-side privacy mod for Minecraft streaming. It rewrites sensitive text only on your client and can keep selected HUD elements out of OBS Game Capture while they remain visible in-game.

Available for **Fabric** and **NeoForge** on Minecraft **1.21.11** and **26.1.x** (tested on 26.1.2).

## Highlights

- **Player-name privacy** — replaces other player names with stable, per-session aliases across name tags, TAB, chat, and scoreboards.
- **Self-name modes** — hide your own name, use a custom formatted nickname, keep your real name, or use a random alias. Self and other-player anonymization have independent master switches; disabling either also disables its chat-input replacements.
- **Chat-input sanitization** — optionally hides known player names while you type, subject to the self/other-player master switches, without changing the message sent to the server.
- **Text redaction** — hides configured words, server addresses, server names, and optional strict safety-list terms in rendered text.
- **Scoreboard rules** — create ordered match-and-replace rules with enable, add, delete, and move controls.
- **Localized item-name normalization** — items display the name resolved from their native translation key, including active resource-pack translations; item data, anvil input, and server behavior are unchanged.
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

### Hitboxes hidden from OBS

On Windows, enable the OBS overlay and **Hide hitboxes from OBS**, then press **F3+B** to show entity hitboxes. This setting is off by default and requires no Streamproof installation. Only entity hitbox debug geometry is routed to the overlay; players and held items stay in the normal world render.

Check OBS **Game Capture** before streaming. Display/window capture is not guaranteed to exclude the overlay. If the overlay is unavailable, hitboxes remain visible normally, including in the capture; turn off F3+B if necessary. Do not enable Streamproof's hitbox-hiding feature at the same time.

Open the mod's Configure screen to manage redaction, names, items, scoreboards, skins, and OBS-overlay behavior.

Custom self names support Minecraft legacy formatting codes such as `§d§l`, plus `&` aliases such as `&d&l`. Use `&&` for a literal ampersand.

## Important notes

- StreamShield changes client-side presentation only. It does not alter server data or outgoing chat messages.
- Some server-rendered scoreboards use unusual rendering paths and may not be fully rewriteable.

## License and attribution

StreamShield is released under the **MIT License**. Its OBS overlay implementation is ported from [obs-overlay](https://github.com/zziger/obs-overlay) by **zziger / Artem Dzhemesiuk**, also under MIT. The required notice and license are bundled with the mod.
