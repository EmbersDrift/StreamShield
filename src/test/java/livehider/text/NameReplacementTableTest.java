package livehider.text;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NameReplacementTableTest {
    @Test
    void replacementIsNotRescannedWhenSelfAliasIsAnotherKnownName() {
        NameReplacementTable names = new NameReplacementTable(Map.of("Streamer", "Alice", "Alice", "[Player]#7"), "Streamer");
        assertEquals("Alice thanked [Player]#7", names.replace("Streamer thanked Alice", true));
        assertEquals("Alice thanked Alice", names.replace("Streamer thanked Alice", false));
    }

    @Test
    void cyclicReplacementsOnlyTransformTheOriginalOccurrences() {
        NameReplacementTable names = new NameReplacementTable(Map.of("Alice", "Bob", "Bob", "Alice"), null);
        assertEquals("Bob Alice Bob", names.replace("Alice Bob Alice", true));
    }

    @Test
    void playerNamesHaveCaseInsensitiveAsciiUsernameBoundaries() {
        NameReplacementTable names = new NameReplacementTable(Map.of("Alice", "SAFE"), null);
        assertEquals("SAFE (SAFE) @SAFE SAFE玩家 Alice2 xAlice Alice_ _Alice",
            names.replace("Alice (ALICE) @alice Alice玩家 Alice2 xAlice Alice_ _Alice", true));
    }

    @Test
    void longestOverlappingLiteralWinsWithoutRegexInterpretation() {
        NameReplacementTable names = new NameReplacementTable(Map.of("Alice", "SHORT", "Alice-Bob", "LONG", "A.B", "DOT"), null);
        assertEquals("LONG SHORT DOT AxB", names.replace("Alice-Bob Alice A.B AxB", true));
    }

    @Test
    void dollarBackslashAndFormattingInReplacementAreLiteral() {
        NameReplacementTable names = new NameReplacementTable(Map.of("Alice", "$1\\folder &dAlias"), null);
        assertEquals("$1\\folder &dAlias", names.replace("Alice", true));
    }

    @Test
    void preservesFormattingAroundNamesWithoutMistakingColorCodeForNamePrefix() {
        NameReplacementTable names = new NameReplacementTable(Map.of("Alice", "SAFE"), null);
        assertEquals("§a§lSAFE§r x§aAlice §aAlice2", names.replace("§a§lAlice§r x§aAlice §aAlice2", true));
    }

    @Test
    void selfOnlyPolicyStillHidesSelfWhenOtherNamesAreDisabled() {
        NameReplacementTable names = new NameReplacementTable(Map.of("Streamer", "", "Alice", "SAFE"), "Streamer");
        assertEquals(" met Alice", names.replace("STREAMER met Alice", false));
        assertEquals("", names.replaceExact("streamer", false));
        assertEquals("Alice", names.replaceExact("Alice", false));
    }

    @Test
    void ownModeLeavesSelfIntactEvenWhenOtherNamesAreEnabled() {
        NameReplacementTable names = new NameReplacementTable(Map.of("Alice", "SAFE"), "Streamer");
        assertEquals("Streamer met SAFE", names.replace("Streamer met Alice", true));
        assertEquals("Streamer", names.replaceExact("Streamer", true));
        assertEquals("Streamer met Alice", names.replace("Streamer met Alice", false));
    }

    @Test
    void scoreboardNamesOnlyMatchAnEntireRow() {
        NameReplacementTable names = new NameReplacementTable(Map.of("Alice", "SAFE"), null);
        assertEquals("SAFE", names.replaceExact("alice", true));
        assertEquals("Alice: 7", names.replaceExact("Alice: 7", true));
        assertEquals(" Alice ", names.replaceExact(" Alice ", true));
    }

    @Test
    void compiledSnapshotDoesNotObserveUnsavedMutableMapEdits() {
        Map<String, String> source = new HashMap<>(Map.of("Alice", "OLD"));
        NameReplacementTable old = new NameReplacementTable(source, null);
        source.put("Alice", "NEW");
        assertEquals("OLD", old.replace("Alice", true));
        assertEquals("NEW", new NameReplacementTable(source, null).replace("Alice", true));
    }

    @Test
    void emptySessionAndNullInputAreSafe() {
        NameReplacementTable empty = new NameReplacementTable(Map.of(), null);
        assertNull(empty.replace(null, true));
        assertNull(empty.replaceExact(null, true));
        assertEquals("Alice", empty.replace("Alice", true));
    }
}
