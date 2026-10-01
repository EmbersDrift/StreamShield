package livehider.text;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;

/** Bounded transient pre-rewrite samples for the rule assistant; never logged or persisted. */
public final class ScoreboardSamples {
    public record Sample(Component preview, String key) {}
    private static final LinkedHashMap<String, Sample> samples = new LinkedHashMap<>();
    private ScoreboardSamples() {}
    public static synchronized void clear() { samples.clear(); }
    public static synchronized List<Sample> snapshot() { return List.copyOf(samples.values()); }
    public static synchronized void capture(Component original) {
        if (original == null || original.getString().length() > 2048) return;
        String visible = LegacyFormatCodes.strip(original.getString());
        if (!visible.isBlank()) add(new Sample(original.copy(), ScoreboardUnicodeKey.encode(visible)));
        int[] count = {0};
        original.visit((style, value) -> {
            if (++count[0] > 32) return Optional.of(true);
            String text = LegacyFormatCodes.strip(value);
            if (!text.isBlank() && text.length() <= 256 && style.getFont() instanceof FontDescription.Resource font) {
                add(new Sample(Component.literal(text).setStyle(style), ScoreboardUnicodeKey.encodeGlyph(font.id(), text)));
            }
            return Optional.empty();
        }, Style.EMPTY);
    }
    private static void add(Sample sample) {
        samples.put(sample.key(), sample);
        while (samples.size() > 32) samples.remove(samples.keySet().iterator().next());
    }
}
