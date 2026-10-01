package livehider.text;

import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScoreboardDiagnosticsTest {
    @Test void capturesSplitLettersWithoutChangingSource() {
        var source = Component.literal("A").withStyle(ChatFormatting.RED)
            .append(Component.literal("S").withStyle(ChatFormatting.GOLD))
            .append(Component.literal("T").withStyle(ChatFormatting.YELLOW));
        var report = ScoreboardDiagnostics.describe("row", source);
        assertEquals("AST", report.get("text").getAsString());
        var runs = report.getAsJsonArray("segments");
        assertEquals(3, runs.size());
        assertNotEquals(runs.get(0).getAsJsonObject().get("color"), runs.get(1).getAsJsonObject().get("color"));
        assertEquals("AST", source.getString());
        assertFalse(report.get("truncated").getAsBoolean());
    }

    @Test void legacyCodesAndNewlinesAreUnambiguousInSingleLineJson() {
        var report = ScoreboardDiagnostics.describe("row", Component.literal("\u00a7aIP:\nserver.net"));
        assertEquals("IP:\nserver.net", report.get("formatStripped").getAsString());
        assertTrue(report.getAsJsonArray("segments").get(0).getAsJsonObject().get("legacyEscaped").getAsString().contains("\\u00a7a"));
        assertFalse(report.toString().contains("\n"));
        assertEquals(report, JsonParser.parseString(report.toString()));
    }

    @Test void hugeInputsAreBoundedAndFlagged() {
        var report = ScoreboardDiagnostics.describe("row", Component.literal("X".repeat(10000)));
        assertTrue(report.get("truncated").getAsBoolean());
        assertEquals(256, report.get("text").getAsString().length());
        var source = Component.empty();
        for (int i = 0; i < 100; i++) source.append(Component.literal("X"));
        var split = ScoreboardDiagnostics.describe("row", source);
        assertTrue(split.get("truncated").getAsBoolean());
        assertTrue(split.getAsJsonArray("segments").size() <= 64);
    }
}
