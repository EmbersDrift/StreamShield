# Changelog

## v1.1.3 (Unreleased — OBS acceptance testing required)

### Added

- Windows Game Capture configuration protection: a latched solid capture shield keeps the private GUI in the local overlay. Includes child dialogs, explicit resume confirmation, protected transition frames, and a non-sensitive fallback when the overlay is unavailable.
- Configurable emergency shield key (F9 by default, in-game).
- A scoreboard rule assistant with bounded in-memory pre-rewrite samples, editable replacements, and font-qualified glyph previews.
- Opt-in server-address privacy presets for text and HUD options, with separate global defaults and hashed profile filenames.
- Privacy coverage examples and a link to the pre-stream readiness checks.
- Confirmed preset export/import with a field allowlist, import size limit, unique export filenames, and pre-import backups. Credentials, local skin paths, diagnostic opt-ins and configuration protection are excluded.
- Separate opt-in display filters for item tooltips, container titles and book-view text. Editing and server data are unchanged.

### Fixed

- Rebuild configuration widgets after importing so stale controls cannot overwrite the imported values.
- Complete pending preset switches before releasing the capture shield; switch failures retain protection and can be retried.
- Preserve the capture shield briefly after closing private UI to cover deferred GUI frames.

### Validation

- See `V1.1.3_ACCEPTANCE.md` for mandatory OBS recording, transition-frame, resource-pack and compatibility checks. Build/unit/startup checks do not certify capture privacy.

## v1.1.2

### Fixed

- Expanded scoreboard match-field capacity for diagnostic-generated Unicode keys, and prevented diagnostic truncation from splitting supplementary Unicode characters into invalid surrogate keys.
- Scoreboard rules now match across separately styled characters, fixing gradient server addresses and titles that could evade replacement. Unmatched text retains its colors and resource-pack icon fonts.
- Hidden hitbox overlays no longer draw over inventory, pause, chat or other screens, or resource-loading overlays. Pending frames are discarded while a screen is open; normal world visibility resumes after closing it.

- Self-name and other-player anonymization now have independent master switches. Disabled identities are not assigned aliases, and chat-input sanitization cannot override either master switch.
- Clearing/changing name settings invalidates cached aliases and display names. Player identity uses profile names, including during early connection setup.
- Chat history retains its original text and rebuilds visible lines when settings change, so disabling anonymization restores names in existing messages. Chat logging still uses the current redaction policy.
- Item-name normalization now resolves the item's native translation key through the current language and resource packs, rather than trusting stack-supplied CUSTOM_NAME or ITEM_NAME. Tooltips and selected-item labels use the same resolver without modifying item data.

### Added

- Live scoreboard match-key previews use the selected resource font and current resource packs, update without saving, and show invalid-key feedback. Preview rendering is bounded and clipped to protect nearby controls from oversized glyphs.
- Font-qualified scoreboard glyph rules (`glyph:namespace:font|U+E001`) distinguish identical code points in different resource fonts. Diagnostics include `fontId` and copyable `glyphMatchKey` values; inherited fonts and cross-component sequences are supported.
- Advanced scoreboard rules accept `unicode:U+E001` or multi-code-point sequences to remove resource-pack glyphs. Sensitive diagnostics include copyable `unicodeMatchKey` values alongside font metadata. Advanced matching is exact and has no word boundaries; ordinary rules keep their existing behavior.
- Added a separate, default-off sensitive scoreboard diagnostic option. Captures bounded pre-rewrite text segments, colors and fonts (up to 32 distinct samples) to investigate split-text matching; logs may contain player names and server addresses.

- Added an optional Windows OBS-overlay setting for F3+B entity hitboxes: keep them visible in-game while excluding them from OBS Game Capture. Disabled by default; Streamproof is not required.
- Hitbox rendering uses a dedicated buffer and render target instead of flushing shared entity/item buffers, avoiding the shared-buffer path that can hide other players' held items. Previous render targets are restored even if drawing fails.

- World sign text can use the existing redaction rules or be hidden completely while retaining the sign model. Covers both faces of all vanilla standing, wall, hanging and wall-hanging signs, including dyed and glowing text. Editing and server data remain unchanged.
- Added a configurable key binding to open StreamShield settings (F8 by default, while no screen is open).


## v1.1.1

### Added

- Added an optional strict chat-filter preset for the former broad safetext word list.
- Added CUSTOM skin obfuscation: assign substitute skins from a configurable Mojang-account ID pool, with repeated assignments allowed.
- Added independent chat-input sanitization, so known player names can be hidden while typing without enabling global name anonymization.
- Custom self names now support Minecraft `§` formatting codes and `&` aliases; use `&&` for a literal ampersand.
- Added localized, client-display-only normalization for renamed item names.
- Added score-rule move up/down controls and clearer configuration tooltips.
- Added Modrinth and CurseForge release descriptions.
- Added NeoForge builds for Minecraft 1.21.11 and 26.1.x.

### Changed

- Replaced the default broad safetext list with a narrow privacy-focused preset to reduce false positives in normal chat.
- Self-name mode now takes priority over global player-name anonymization across name tags, TAB, chat, scoreboards, and chat-input display.
- Item-name normalization now changes only client rendering. It no longer affects anvil input, server item data, or rename experience costs.
- Player name-tag and TAB rewriting is cached per player, avoiding repeated full text/regex rewriting during rendering.
- Scoreboard rules are compiled when settings change instead of during scoreboard rendering.
- Scoreboard format-code stripping no longer compiles a regular expression for every rendered row.
- Rule add, delete, and move operations retain other unsaved edits before reopening the configuration page.
- Formatting-code input handling is restricted to the StreamShield configuration screen and no longer affects unrelated text fields.
- Updated the OBS overlay renderer for NeoForge's layered GUI pipeline and its vanilla full-screen compositor.
- NeoForge now delays OBS-overlay initialization until after the initial resource-loading period.

### Fixed

- Fixed name replacement maps remaining stale after changing the self-name mode until a player-list update occurred.
- Fixed custom self-name formatting being displayed literally in chat and other rich-text surfaces.
- Avoided applying player-name processing to non-player entity name tags, improving busy-area performance.
- Fixed scoreboards anonymizing other players when global name anonymization was disabled but a self-name mode was active.
- Made malformed or partially edited configuration files resilient to null values and empty rule entries.
- Fixed NeoForge HUD routing so health, armor, hunger, air, vehicle health, hotbar, and experience are excluded from OBS captures.
- Fixed NeoForge startup failures caused by version-specific GUI mixin signatures and early configuration initialization.
