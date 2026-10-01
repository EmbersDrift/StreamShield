package livehider.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SafeTextTest {
    @Test
    void parsesSectionAndAmpersandCodesAndEscapes() {
        Component parsed = SafeText.parseLegacyFormatting("§d&l名字&r && &z §");
        assertEquals("名字 & &z §", parsed.getString());
        List<Run> runs = runs(parsed);
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE), runs.get(0).style.getColor());
        assertTrue(runs.get(0).style.isBold());
        assertFalse(runs.get(1).style.isBold());
    }

    @Test
    void resetDoesNotInheritBoldFromFirstRunOrOuterParent() {
        Component parsed = SafeText.parseLegacyFormatting("&lBold&rPlain");
        Component parent = Component.empty().withStyle(ChatFormatting.BOLD, ChatFormatting.RED).append(parsed);
        List<Run> runs = runs(parent);
        assertTrue(runs.get(0).style.isBold());
        assertFalse(runs.get(1).style.isBold());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.WHITE), runs.get(1).style.getColor());
    }

    @Test
    void colorCodeClearsEarlierVisualFlags() {
        List<Run> runs = runs(SafeText.parseLegacyFormatting("&lBold&aGreen"));
        assertTrue(runs.get(0).style.isBold());
        assertFalse(runs.get(1).style.isBold());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GREEN), runs.get(1).style.getColor());
    }

    @Test
    void rewritingTranslationPreservesFallbackArgumentsStyleAndSiblings() {
        Component source = Component.translatableWithFallback("streamshield.test.unknown", "Hello %s (%s)", "Alice", 7)
            .withStyle(ChatFormatting.GREEN)
            .append(Component.literal(" / Alice").withStyle(ChatFormatting.ITALIC));
        Component rewritten = SafeText.rewriteNode(source, text -> text.replace("Alice", "SAFE"));
        TranslatableContents contents = (TranslatableContents) rewritten.getContents();
        assertEquals("Hello %s (%s)", contents.getFallback());
        assertArrayEquals(new Object[]{"SAFE", 7}, contents.getArgs());
        assertEquals(source.getStyle(), rewritten.getStyle());
        assertEquals("Hello SAFE (7) / SAFE", rewritten.getString());
        assertTrue(rewritten.getSiblings().get(0).getStyle().isItalic());
        assertEquals("Hello Alice (7) / Alice", source.getString());
    }

    @Test
    void translationComponentArgumentsAreRewrittenWithoutMutatingTheSource() {
        Component sourceArg = Component.literal("Alice").withStyle(ChatFormatting.BOLD);
        Component source = Component.translatableWithFallback("streamshield.test.argument", "%s", sourceArg);
        Component rewritten = SafeText.rewriteNode(source, text -> text.replace("Alice", "SAFE"));
        Component rewrittenArg = (Component) ((TranslatableContents) rewritten.getContents()).getArgs()[0];
        assertEquals("SAFE", rewrittenArg.getString());
        assertTrue(rewrittenArg.getStyle().isBold());
        assertEquals("Alice", sourceArg.getString());
    }

    @Test
    void formattedReplacementKeepsUnaffectedSiblingAndInheritedStyle() {
        Component source = Component.literal("Alice").withStyle(ChatFormatting.ITALIC)
            .append(Component.literal(" \ue001").withStyle(Style.EMPTY.withInsertion("icon-marker")));
        Component rewritten = SafeText.rewriteNode(source, text -> text.equals("Alice") ? "&lSAFE" : text);
        assertEquals("SAFE \ue001", rewritten.getString());
        List<Run> runs = runs(rewritten);
        assertTrue(runs.get(0).style.isBold());
        assertTrue(runs.get(0).style.isItalic());
        assertTrue(runs.get(1).style.isItalic());
        assertEquals("icon-marker", runs.get(1).style.getInsertion());
    }

    @Test
    void escapedAmpersandOnlyReplacementIsParsedToo() {
        Component result = SafeText.rewriteNode(Component.literal("Alice"), text -> "Name && Co");
        assertEquals("Name & Co", result.getString());
    }

    private static List<Run> runs(Component text) {
        List<Run> result = new ArrayList<>();
        text.visit((style, value) -> {
            if (!value.isEmpty()) {
                result.add(new Run(value, style));
            }
            return Optional.<Void>empty();
        }, Style.EMPTY);
        return result;
    }

    private record Run(String text, Style style) {
    }
}
