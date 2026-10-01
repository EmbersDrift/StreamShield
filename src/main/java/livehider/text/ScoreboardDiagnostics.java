package livehider.text;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import livehider.LiveHider;
import livehider.LiveHiderConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Opt-in sensitive diagnostics of text entering our scoreboard hooks, not a packet dump. */
public final class ScoreboardDiagnostics {
    private static final int MAX_SAMPLES = 32;
    private static final Set<String> seen = new HashSet<>();
    private static boolean enabled;

    private ScoreboardDiagnostics() {}

    public static synchronized void configure(boolean value) {
        enabled = value;
        seen.clear();
        if (value) LiveHider.LOGGER.warn("[LiveHider][scoreboard-diagnostic] ARMED: sensitive text logging enabled; up to 32 distinct samples. Disable after testing. Samples are assembled text before rewriting, not raw packets.");
    }

    public static synchronized void capture(String source, Component original) {
        ScoreboardSamples.capture(original);
        LiveHiderConfig config = LiveHiderConfig.get();
        boolean requested = config != null && config.scoreboardDiagnostics;
        if (requested != enabled) configure(requested);
        if (!enabled || original == null || seen.size() >= MAX_SAMPLES) return;
        String sample = describe(source, original).toString();
        if (!seen.add(sample)) return;
        LiveHider.LOGGER.info("[LiveHider][scoreboard-diagnostic] {}", sample);
        if (seen.size() == MAX_SAMPLES)
            LiveHider.LOGGER.info("[LiveHider][scoreboard-diagnostic] LIMIT reached; further samples suppressed. Save settings to start another capture.");
    }

    static JsonObject describe(String source, Component original) {
        JsonObject report = new JsonObject();
        report.addProperty("source", source);
        StringBuilder flat = new StringBuilder();
        JsonArray segments = new JsonArray();
        boolean[] truncated = {false};
        original.visit((style, text) -> {
            if (segments.size() >= 64 || flat.length() >= 2048) {
                truncated[0] = true;
                return Optional.of(Boolean.TRUE);
            }
            int remaining = 2048 - flat.length();
            int end = Math.min(text.length(), Math.min(remaining, 256));
            if (end > 0 && end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))
                && Character.isLowSurrogate(text.charAt(end))) end--;
            String bounded = text.substring(0, end);
            truncated[0] |= bounded.length() != text.length();
            flat.append(bounded);
            JsonObject segment = new JsonObject();
            segment.addProperty("text", bounded);
            segment.addProperty("unicodeMatchKey", ScoreboardUnicodeKey.encode(LegacyFormatCodes.strip(bounded)));
            // Explicit escape makes embedded legacy codes distinguishable from style metadata.
            segment.addProperty("legacyEscaped", bounded.replace("\u00a7", "\\u00a7"));
            segment.addProperty("color", style.getColor() == null ? "inherited/default" : style.getColor().toString());
            segment.addProperty("font", String.valueOf(style.getFont()));
            if (style.getFont() instanceof net.minecraft.network.chat.FontDescription.Resource resource) {
                segment.addProperty("fontId", resource.id().toString());
                segment.addProperty("glyphMatchKey", ScoreboardUnicodeKey.encodeGlyph(resource.id(), LegacyFormatCodes.strip(bounded)));
            }
            segment.addProperty("bold", style.isBold());
            segment.addProperty("italic", style.isItalic());
            segment.addProperty("obfuscated", style.isObfuscated());
            segment.addProperty("underlined", style.isUnderlined());
            segment.addProperty("strikethrough", style.isStrikethrough());
            segments.add(segment);
            return Optional.empty();
        }, Style.EMPTY);
        report.addProperty("text", flat.toString());
        report.addProperty("formatStripped", LegacyFormatCodes.strip(flat.toString()));
        report.addProperty("unicodeMatchKey", ScoreboardUnicodeKey.encode(LegacyFormatCodes.strip(flat.toString())));
        report.add("segments", segments);
        report.addProperty("truncated", truncated[0]);
        return report;
    }
}
