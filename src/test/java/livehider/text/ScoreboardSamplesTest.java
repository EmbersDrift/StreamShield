package livehider.text;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ScoreboardSamplesTest {
    @Test void samplesAreBoundedAndCanBeCleared() {
        ScoreboardSamples.clear();
        for (int i = 0; i < 100; i++) ScoreboardSamples.capture(Component.literal("sample " + i));
        assertTrue(ScoreboardSamples.snapshot().size() <= 32);
        ScoreboardSamples.clear();
        assertTrue(ScoreboardSamples.snapshot().isEmpty());
    }
    @Test void sampleIncludesExactAndFontQualifiedKeys() {
        ScoreboardSamples.clear();
        ScoreboardSamples.capture(Component.literal("\ue001"));
        assertTrue(ScoreboardSamples.snapshot().stream().anyMatch(s -> s.key().equals("unicode:U+E001")));
        assertTrue(ScoreboardSamples.snapshot().stream().anyMatch(s -> s.key().startsWith("glyph:minecraft:default|")));
        ScoreboardSamples.clear();
    }
}
