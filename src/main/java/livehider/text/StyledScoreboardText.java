package livehider.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Matches visible text across components while retaining the style/font of every untouched run. */
final class StyledScoreboardText {
    private record Run(int start, int end, String text, Style style) {}
    private final Component original;
    private final StringBuilder plain = new StringBuilder();
    private final List<Run> runs = new ArrayList<>();

    StyledScoreboardText(Component original) {
        this.original = original;
        original.visit((style, text) -> {
            if (text.indexOf('\u00a7') < 0) {
                add(text, style);
            } else {
                // Only section codes are source formatting; ampersands in server text are literal.
                SafeText.parseLegacyFormatting(text.replace("&", "&&"), style).visit((parsedStyle, value) -> {
                    add(value, parsedStyle);
                    return Optional.empty();
                }, Style.EMPTY);
            }
            return Optional.empty();
        }, Style.EMPTY);
    }

    private void add(String text, Style style) {
        if (text.isEmpty()) return;
        int start = plain.length();
        plain.append(text);
        runs.add(new Run(start, plain.length(), text, style));
    }

    String text() { return plain.toString(); }

    Component replace(Pattern pattern, String replacement) {
        return replace(pattern, replacement, null);
    }

    Component replace(Pattern pattern, String replacement, net.minecraft.resources.Identifier font) {
        var matcher = pattern.matcher(plain);
        MutableComponent result = null;
        int cursor = 0;
        int searchFrom = 0;
        while (matcher.find(searchFrom)) {
            if (!matchesFont(matcher.start(), matcher.end(), font)) {
                searchFrom = matcher.start() + Character.charCount(Character.codePointAt(plain, matcher.start()));
                continue;
            }
            if (result == null) result = Component.empty();
            appendSlice(result, cursor, matcher.start());
            Style style = styleAt(matcher.start());
            result.append(SafeText.parseLegacyFormatting(replacement, style));
            cursor = matcher.end();
            if (cursor == plain.length()) break;
            searchFrom = cursor > matcher.start() ? cursor : cursor + 1;
        }
        if (result == null) return original;
        appendSlice(result, cursor, plain.length());
        return result;
    }

    private boolean matchesFont(int start, int end, net.minecraft.resources.Identifier font) {
        if (font == null) return true;
        for (Run run : runs) {
            if (run.end > start && run.start < end
                && (!(run.style.getFont() instanceof net.minecraft.network.chat.FontDescription.Resource resource)
                    || !font.equals(resource.id()))) return false;
        }
        return true;
    }

    private Style styleAt(int index) {
        for (Run run : runs) if (index >= run.start && index < run.end) return run.style;
        return Style.EMPTY;
    }

    private void appendSlice(MutableComponent output, int start, int end) {
        for (Run run : runs) {
            int from = Math.max(start, run.start);
            int to = Math.min(end, run.end);
            if (from < to) output.append(Component.literal(run.text.substring(from - run.start, to - run.start))
                .setStyle(run.style));
        }
    }
}
