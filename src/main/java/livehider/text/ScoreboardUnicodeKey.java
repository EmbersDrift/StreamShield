package livehider.text;

import java.util.Locale;

/** Explicit opt-in code-point syntax; ordinary rule keys retain their literal meaning. */
final class ScoreboardUnicodeKey {
    static final String PREFIX = "unicode:";
    static final String GLYPH_PREFIX = "glyph:";
    record GlyphKey(net.minecraft.resources.Identifier font, String text) {}

    static GlyphKey decodeGlyph(String key) {
        int separator = key.indexOf('|', GLYPH_PREFIX.length());
        if (separator < 0) return null;
        var font = net.minecraft.resources.Identifier.tryParse(key.substring(GLYPH_PREFIX.length(), separator));
        String text = decode(PREFIX + key.substring(separator + 1));
        return font == null || text == null || text.isEmpty() ? null : new GlyphKey(font, text);
    }

    static String encodeGlyph(net.minecraft.resources.Identifier font, String text) {
        return GLYPH_PREFIX + font + "|" + encode(text).substring(PREFIX.length());
    }
    private ScoreboardUnicodeKey() {}

    static String decode(String key) {
        String body = key.substring(PREFIX.length()).trim();
        if (body.isEmpty()) return null;
        StringBuilder result = new StringBuilder();
        for (String token : body.split("\\s+")) {
            if (!token.matches("U\\+[0-9a-fA-F]{4,6}")) return null;
            int codePoint = Integer.parseInt(token.substring(2), 16);
            if (!Character.isValidCodePoint(codePoint) || (codePoint >= 0xd800 && codePoint <= 0xdfff)) return null;
            result.appendCodePoint(codePoint);
        }
        return result.toString();
    }

    static String encode(String text) {
        StringBuilder key = new StringBuilder(PREFIX);
        text.codePoints().forEach(cp -> {
            if (key.length() > PREFIX.length()) key.append(' ');
            key.append(String.format(Locale.ROOT, "U+%04X", cp));
        });
        return key.toString();
    }
}
