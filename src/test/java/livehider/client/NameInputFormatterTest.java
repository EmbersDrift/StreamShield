package livehider.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NameInputFormatterTest {
    @Test
    void editingShowsFinalColoredNameWithoutSourceCodes() {
        List<Glyph> glyphs = glyphs(NameInputFormatter.format("&9Blue", "&9Blue", 0));
        assertEquals("Blue", text(glyphs));
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.BLUE), glyphs.get(0).style().getColor());
    }

    @Test
    void sectionCodesStylesAndEscapesRenderAsFinalName() {
        List<Glyph> glyphs = glyphs(NameInputFormatter.format("\u00a79Blue &lBold&r &&", "\u00a79Blue &lBold&r &&", 0));
        assertEquals("Blue Bold &", text(glyphs));
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.BLUE), glyphs.get(0).style().getColor());
        assertTrue(glyphs.get(5).style().isBold());
        assertFalse(glyphs.get(9).style().isBold());
    }

    @Test
    void clippedTextKeepsTheStyleFromBeforeTheWindow() {
        List<Glyph> glyphs = glyphs(NameInputFormatter.format("&9Blue", "lue", 3));
        assertEquals("lue", text(glyphs));
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.BLUE), glyphs.get(0).style().getColor());
    }

    private static List<Glyph> glyphs(FormattedCharSequence sequence) {
        List<Glyph> glyphs = new ArrayList<>();
        sequence.accept((index, style, codepoint) -> {
            glyphs.add(new Glyph(style, codepoint));
            return true;
        });
        return glyphs;
    }

    private static String text(List<Glyph> glyphs) {
        StringBuilder text = new StringBuilder();
        for (Glyph glyph : glyphs) text.appendCodePoint(glyph.codepoint());
        return text.toString();
    }

    private record Glyph(Style style, int codepoint) {
    }
}
