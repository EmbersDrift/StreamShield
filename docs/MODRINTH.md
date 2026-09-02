# StreamShield

StreamShield is a Fabric client mod for stream-safe Minecraft gameplay. It rewrites sensitive text only on your client and can redirect selected HUD elements away from OBS capture, helping keep player names, server details, renamed items, and on-screen UI out of your stream.

## Highlights

- **Player-name privacy** — anonymize other players with stable per-session aliases.
- **Self-name modes** — hide your name, use a custom nickname, keep your real name, or use a randomized alias. Your self-name mode takes priority across name tags, TAB, chat, scoreboards, and chat-input sanitization.
- **Chat-input sanitization** — independently hide known player names while you type; it does not modify the message that is actually sent.
- **Text redaction** — redact configured words, the current server address/name, and bundled safety-list entries.
- **Scoreboard rules** — ordered match-and-replace rules with enable, add, delete, and move controls.
- **Localized item-name normalization** — renamed items display their vanilla localized name on your client. This is display-only and does not alter anvil input or server-side item data.
- **Skin obfuscation** — optionally render players with the default Steve skin.
- **OBS overlay** — redirect selected HUD components away from OBS capture while keeping them visible to you. Windows only.

## Compatibility

Install the version whose Minecraft version is marked on the release page.

| Minecraft | Java |
| --- | --- |
| 1.21.11 | 21+ |
| 26.1–26.1.2 | 25+ |

Required dependencies: Fabric Loader, Fabric API, Architectury API, and Cloth Config API. Mod Menu is optional and adds a Configure button.

## Usage

Open the mod's Configure screen from Mod Menu. Settings are grouped into redaction, names, items, scoreboards, and overlay controls.

For custom self names, legacy formatting is supported with `§` codes or `&` aliases. For example, `&d&lStreamer` is rendered as a light-purple bold name; use `&&` for a literal ampersand.

## Important notes

- StreamShield changes **client-side presentation only**. It does not change server data or the text you send in chat.
- The OBS overlay uses a Windows native hook and is intended for OBS Game Capture. Test it before going live.
- Some server-rendered scoreboards use unusual rendering paths and may not be fully rewriteable.

## License and attribution

StreamShield is licensed under MIT. Its OBS overlay implementation is ported from [obs-overlay](https://github.com/zziger/obs-overlay) by zziger / Artem Dzhemesiuk, also under MIT; the included NOTICE and license files preserve the required attribution.
