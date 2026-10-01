# Advanced scoreboard glyph rules

Resource packs can draw an entire logo or address using one private-use character. StreamShield matches the underlying text, not the image inside a font glyph.

1. Open settings (F8 by default), enable sensitive scoreboard diagnostics, then display the scoreboard.
2. Find `[LiveHider][scoreboard-diagnostic]` in the log. Inspect `segments`: each segment includes text, font metadata and a `unicodeMatchKey`. Use the font and surrounding text to identify the glyph; a code point alone cannot tell you what the image looks like.
3. Copy a key such as `unicode:U+E001` into a scoreboard rule's match field, without JSON quotes. Leave the replacement empty to remove it. Enable the rule and scoreboard rules, then save.
4. Disable diagnostics after testing. These logs can expose names and server addresses.

Multiple code points are separated by spaces, e.g. `unicode:U+E001 U+F0001`. This matches that consecutive sequence, even across styled components. `U+0020` represents an actual space. Advanced matching is case-sensitive and ignores word boundaries. Malformed advanced keys are ignored. Ordinary keys keep their previous behavior; raw backslash escapes are not decoded.

The top-level key represents the sampled visible row; a segment key represents only that segment. Legacy color codes are excluded. Samples marked `truncated` are incomplete; do not treat their keys as complete rows. Empty keys do nothing.

## Font-qualified glyph rules

For resource-pack icons, prefer the segment's `glyphMatchKey`, for example `glyph:server:icons|U+E001`. The diagnostic also provides a clean `fontId`. Copy the full key into the match field, without quotes, and leave the replacement empty to hide the glyph.

This rule matches only when every character in the sequence uses the specified effective resource font, including inherited fonts. It works across components with different colors. Mixed-font sequences do not match. Font-qualified rules are not applied to plain strings without font metadata. Invalid keys are ignored. Existing `unicode:` rules remain font-independent.

Replace `server:icons` and `U+E001` with actual diagnostic values, not this example. A font ID identifies the font, not the resource-pack file; different packs can override the same font and change its appearance. Unmatched characters retain their font and color. This feature does not affect non-text images or automatically recognize text drawn inside textures.

## Live preview

Expand a rule to see a live glyph preview below its match field. It reads unsaved input without enabling the rule or saving settings. `glyph:` selects its specified font; ordinary text and `unicode:` use the default font. Load the corresponding resource pack first (for server packs, open settings while connected). Missing fonts/glyphs may display boxes; preview is not proof that a rule matches a particular scoreboard row.

The preview shows at most 128 code points in a fixed clipped area. Oversized glyphs can be partially clipped, but cannot cover nearby controls. Incomplete advanced syntax displays a status message. The input supports at least 32,768 UTF-16 units and retains longer existing values; preview truncation never shortens the saved rule.
