package livehider.text;

import livehider.ScoreboardRule;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ScoreboardGlyphKeyTest {
    private static final Style ICON = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.parse("server:icons")));
    private ScoreboardRuleChain rule(String key) {
        return new ScoreboardRuleChain(List.of(new ScoreboardRule(true, key, "")));
    }

    @Test void sameCharacterInOtherFontsIsUntouched() {
        var source = Component.empty().append(Component.literal("\ue001"))
            .append(Component.literal("\ue001").setStyle(ICON)).append(Component.literal("\ue001"));
        assertEquals("\ue001\ue001", rule("glyph:server:icons|U+E001").applyComponent(source).getString());
    }

    @Test void inheritedFontAndCrossLeafSequencesMatch() {
        var source = Component.literal("\ue001").setStyle(ICON).append(Component.literal("\ue002"));
        assertEquals("", rule("glyph:server:icons|U+E001 U+E002").applyComponent(source).getString());
    }

    @Test void allCharactersMustHaveSpecifiedFontAndRejectedOverlapDoesNotHideLaterMatch() {
        var source = Component.empty().append(Component.literal("A"))
            .append(Component.literal("AA").setStyle(ICON));
        assertEquals("A", rule("glyph:server:icons|U+0041 U+0041").applyComponent(source).getString());
        var mixed = Component.literal("\ue001").setStyle(ICON)
            .append(Component.literal("\ue002").setStyle(Style.EMPTY.withFont(FontDescription.DEFAULT)));
        assertSame(mixed, rule("glyph:server:icons|U+E001 U+E002").applyComponent(mixed));
    }

    @Test void invalidKeysAndFontlessStringsDoNotMatch() {
        for (String key : List.of("glyph:server:icons", "glyph:BAD FONT|U+E001", "glyph:server:icons|", "glyph:server:icons|U+D800"))
            assertEquals("\ue001", rule(key).applyComponent(Component.literal("\ue001").setStyle(ICON)).getString());
        assertEquals("\ue001", rule("glyph:server:icons|U+E001").apply("\ue001"));
    }

    @Test void diagnosticKeyRoundTripsAndUnicodeRulesStillMatchAllFonts() {
        var source = Component.literal("\ue001").setStyle(ICON);
        var segment = ScoreboardDiagnostics.describe("row", source).getAsJsonArray("segments").get(0).getAsJsonObject();
        assertEquals("server:icons", segment.get("fontId").getAsString());
        String key = segment.get("glyphMatchKey").getAsString();
        assertEquals("glyph:server:icons|U+E001", key);
        assertEquals("", rule(key).applyComponent(source).getString());
        assertEquals("", rule("unicode:U+E001").applyComponent(Component.literal("\ue001")).getString());
    }
}
