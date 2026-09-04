# Changelog

## Unreleased

### Added

- Added an optional strict chat-filter preset for the former broad safetext word list.
- Added CUSTOM skin obfuscation: assign substitute skins from a configurable Mojang-account ID pool, with repeated assignments allowed.
- Added independent chat-input sanitization, so known player names can be hidden while typing without enabling global name anonymization.
- Custom self names now support Minecraft `§` formatting codes and `&` aliases; use `&&` for a literal ampersand.
- Added localized, client-display-only normalization for renamed item names.
- Added score-rule move up/down controls and clearer configuration tooltips.
- Added Modrinth and CurseForge release descriptions.

### Changed

- Replaced the default broad safetext list with a narrow privacy-focused preset to reduce false positives in normal chat.
- Self-name mode now takes priority over global player-name anonymization across name tags, TAB, chat, scoreboards, and chat-input display.
- Item-name normalization now changes only client rendering. It no longer affects anvil input, server item data, or rename experience costs.
- Player name-tag and TAB rewriting is cached per player, avoiding repeated full text/regex rewriting during rendering.
- Scoreboard rules are compiled when settings change instead of during scoreboard rendering.
- Scoreboard format-code stripping no longer compiles a regular expression for every rendered row.
- Rule add, delete, and move operations retain other unsaved edits before reopening the configuration page.
- Formatting-code input handling is restricted to the StreamShield configuration screen and no longer affects unrelated text fields.

### Fixed

- Fixed name replacement maps remaining stale after changing the self-name mode until a player-list update occurred.
- Fixed custom self-name formatting being displayed literally in chat and other rich-text surfaces.
- Avoided applying player-name processing to non-player entity name tags, improving busy-area performance.
- Fixed scoreboards anonymizing other players when global name anonymization was disabled but a self-name mode was active.
- Made malformed or partially edited configuration files resilient to null values and empty rule entries.
