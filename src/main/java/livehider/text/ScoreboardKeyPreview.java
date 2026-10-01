package livehider.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;

/** Read-only preview using the same decoder as matching; never allocates aliases or applies rules. */
public final class ScoreboardKeyPreview {
    private ScoreboardKeyPreview() {}

    public static Component create(String key) {
        if (key == null || key.isEmpty()) return Component.translatable("live_hider.scoreboard.preview.empty");
        String text = key;
        Style style = Style.EMPTY;
        if (key.startsWith(ScoreboardUnicodeKey.GLYPH_PREFIX)) {
            var glyph = ScoreboardUnicodeKey.decodeGlyph(key);
            if (glyph == null) return invalid();
            text = glyph.text();
            style = style.withFont(new FontDescription.Resource(glyph.font()));
        } else if (key.startsWith(ScoreboardUnicodeKey.PREFIX)) {
            text = ScoreboardUnicodeKey.decode(key);
            if (text == null) return invalid();
        }
        // Bound preview work without truncating the stored matching rule or splitting a surrogate pair.
        int count = text.codePointCount(0, text.length());
        var result = Component.empty().append(Component.literal(count > 128
            ? text.substring(0, text.offsetByCodePoints(0, 128)) : text).setStyle(style));
        if (count > 128) result.append(Component.literal(" ..."));
        return result;
    }

    private static Component invalid() {
        return Component.translatable("live_hider.scoreboard.preview.invalid");
    }
}
