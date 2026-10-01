package livehider.text;

import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScoreboardKeyPreviewTest {
    @Test void glyphPreviewUsesDecodedFontAndText() {
        var preview = ScoreboardKeyPreview.create("glyph:server:icons|U+E001 U+F0001");
        assertEquals("\ue001" + new String(Character.toChars(0xf0001)), preview.getString());
        var font = (FontDescription.Resource) preview.getSiblings().getFirst().getStyle().getFont();
        assertEquals("server:icons", font.id().toString());
    }

    @Test void unicodePreviewUsesDefaultFontAndOrdinaryKeysStayLiteral() {
        var preview = ScoreboardKeyPreview.create("unicode:U+E001");
        assertEquals("\ue001", preview.getString());
        assertEquals(FontDescription.DEFAULT, preview.getSiblings().getFirst().getStyle().getFont());
        assertEquals("literal &a", ScoreboardKeyPreview.create("literal &a").getString());
    }

    @Test void incompleteInputDisplaysHelpfulStatusWithoutThrowing() {
        assertEquals(Component.translatable("live_hider.scoreboard.preview.empty"), ScoreboardKeyPreview.create(null));
        for (String key : new String[]{"glyph:", "glyph:server:icons|", "unicode:U+", "unicode:U+D800"})
            assertEquals(Component.translatable("live_hider.scoreboard.preview.invalid"), ScoreboardKeyPreview.create(key));
    }

    @Test void longPreviewDoesNotSplitSupplementaryCharacters() {
        String text = new String(Character.toChars(0xf0001)).repeat(200);
        assertEquals(new String(Character.toChars(0xf0001)).repeat(128) + " ...", ScoreboardKeyPreview.create(text).getString());
    }

    @Test void diagnosticTruncationDoesNotProduceInvalidSurrogateKey() {
        String text = "a".repeat(255) + new String(Character.toChars(0xf0001));
        var report = ScoreboardDiagnostics.describe("row", Component.literal(text));
        assertTrue(report.get("truncated").getAsBoolean());
        assertEquals("a".repeat(255), ScoreboardUnicodeKey.decode(report.get("unicodeMatchKey").getAsString()));
    }
}
