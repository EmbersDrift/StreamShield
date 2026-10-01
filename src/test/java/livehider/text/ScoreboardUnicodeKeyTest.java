package livehider.text;

import livehider.ScoreboardRule;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ScoreboardUnicodeKeyTest {
    private ScoreboardRuleChain rule(String key) {
        return new ScoreboardRuleChain(List.of(new ScoreboardRule(true, key, "")));
    }

    @Test void removesPrivateGlyphNextToLettersWithoutWordBoundaries() {
        assertEquals("AB", rule("unicode:U+E001").apply("A\ue001B"));
    }

    @Test void supportsSupplementaryCodePointsAndSplitComponents() {
        String glyph = new String(Character.toChars(0xf0001));
        var source = Component.literal("\ue001").append(Component.literal(glyph)).append(Component.literal("left"));
        assertEquals("left", rule("unicode:U+E001 U+F0001").applyComponent(source).getString());
    }

    @Test void malformedKeysAreDisabledAndCannotMatchEverything() {
        for (String key : List.of("unicode:", "unicode:U+D800", "unicode:U+110000", "unicode:U+xyz", "unicode:.*", "unicode:U+E001 garbage"))
            assertEquals("safe", rule(key).apply("safe"));
    }

    @Test void advancedMatchingIsCaseSensitiveAndOrdinaryRulesUnchanged() {
        assertEquals("a", rule("unicode:U+0041").apply("Aa"));
        assertEquals("foo2 ", rule("foo").apply("foo2 FOO"));
        assertEquals("", rule("\\uE001").apply("\\uE001"));
    }

    @Test void diagnosticsProvideCopyableKeysAndFontMetadata() {
        var source = Component.literal("\ue001\u00a7r");
        var report = ScoreboardDiagnostics.describe("row", source);
        assertEquals("unicode:U+E001", report.get("unicodeMatchKey").getAsString());
        var segment = report.getAsJsonArray("segments").get(0).getAsJsonObject();
        assertEquals("unicode:U+E001", segment.get("unicodeMatchKey").getAsString());
        assertTrue(segment.has("font"));
        assertEquals("", rule(segment.get("unicodeMatchKey").getAsString()).applyComponent(source).getString());
    }

    @Test void roundTripsWhitespaceSymbolsAndSupplementaryGlyphs() {
        String text = " A\ue001" + new String(Character.toChars(0x100001));
        assertEquals(text, ScoreboardUnicodeKey.decode(ScoreboardUnicodeKey.encode(text)));
    }
}
