package livehider.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyFormatCodesTest {
    @Test
    void stripsValidCodesRegardlessOfCaseButKeepsIconsAndUnicode() {
        assertEquals("Alice \ue001 玩家", LegacyFormatCodes.strip("§a§LAlice§r \ue001 玩家"));
    }

    @Test
    void invalidAndIncompleteCodesRemainLiteral() {
        assertEquals("§zAlice § &dBob &&", LegacyFormatCodes.strip("§zAlice § &dBob &&"));
    }

    @Test
    void allowsExactMatchingOfAFormattedScoreboardName() {
        NameReplacementTable names = new NameReplacementTable(java.util.Map.of("Alice", "SAFE"), null);
        assertEquals("SAFE", names.replaceExact(LegacyFormatCodes.strip("§aAlice§r"), true));
        assertEquals("Alice: 7", names.replaceExact(LegacyFormatCodes.strip("§aAlice§r: 7"), true));
    }

    @Test
    void nullAndUnformattedInputArePreserved() {
        assertNull(LegacyFormatCodes.strip(null));
        assertEquals("", LegacyFormatCodes.strip(""));
        String text = "ordinary";
        assertSame(text, LegacyFormatCodes.strip(text));
    }
}
