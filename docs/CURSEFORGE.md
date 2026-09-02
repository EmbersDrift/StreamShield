# StreamShield — Fabric Stream Privacy and OBS Overlay

StreamShield is a client-side Fabric mod that helps keep Minecraft streams private. It hides or rewrites sensitive display text locally and can move selected HUD elements out of OBS Game Capture while you still see them in-game.

## Features

- Anonymize player names with stable, per-session aliases
- Configure your own name separately: hide it, set a custom nickname, show the real name, or randomize it
- Sanitize player names in the chat input independently from global anonymization
- Redact words, server addresses, and server names in rendered text
- Create ordered scoreboard replacement rules and move them up/down
- Show renamed items with their normal localized Minecraft name without changing anvil behavior or item data
- Replace visible player skins with Steve
- Hide or redirect chat, TAB, scoreboards, subtitles, titles, effects, and other HUD components for OBS

## Requirements

- Minecraft **1.21.11** with Java **21+**, or Minecraft **26.1–26.1.2** with Java **25+**
- Fabric Loader
- Fabric API
- Architectury API
- Cloth Config API

Mod Menu is optional but recommended for opening the in-game configuration screen.

## Configuration

Use the Configure button in Mod Menu. The mod offers separate categories for name privacy, text redaction, item names, scoreboards, and OBS overlay behavior.

Custom self names accept Minecraft `§` formatting codes and `&` aliases. Example: `&b&lLive` renders as aqua bold text. Use `&&` to display a normal ampersand.

## Notes

- All name, item, and text changes are local visual changes. They do not alter server data or outgoing chat messages.
- The OBS overlay is **Windows-only** and should be tested with your capture setup before streaming.
- Scoreboard rewriting depends on how a server renders its sidebar; some servers may not be fully compatible.

## License

MIT licensed. The OBS overlay portion is based on the MIT-licensed obs-overlay project by zziger / Artem Dzhemesiuk. Required attribution and license notices are included with the mod.

