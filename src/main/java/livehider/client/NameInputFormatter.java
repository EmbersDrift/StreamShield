package livehider.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Formatting for the name-privacy text fields only. The stored EditBox value is never changed. */
final class NameInputFormatter {
    private NameInputFormatter() {
    }

    static FormattedCharSequence format(String raw, String visible, int offset) {
        int end = Math.min(raw.length(), offset + visible.length());
        List<FormattedCharSequence> runs = new ArrayList<>();
        StringBuilder pending = new StringBuilder();
        Style pendingStyle = Style.EMPTY;
        Style currentStyle = Style.EMPTY;

        for (int i = 0; i < end; i++) {
            char c = raw.charAt(i);
            if (i + 1 < raw.length() && (c == '\u00a7' || c == '&')) {
                char next = raw.charAt(i + 1);
                if (c == '&' && next == '&') {
                    pendingStyle = append(runs, pending, pendingStyle, currentStyle, '&', i, offset, end);
                    i++;
                    continue;
                }
                ChatFormatting code = ChatFormatting.getByCode(next);
                if (code != null) {
                    currentStyle = currentStyle.applyLegacyFormat(
                        code == ChatFormatting.RESET ? ChatFormatting.WHITE : code);
                    i++;
                    continue;
                }
            }
            pendingStyle = append(runs, pending, pendingStyle, currentStyle, c, i, offset, end);
        }
        flush(runs, pending, pendingStyle);
        return runs.isEmpty() ? FormattedCharSequence.EMPTY : FormattedCharSequence.composite(runs);
    }

    private static Style append(List<FormattedCharSequence> runs, StringBuilder pending,
                                Style previous, Style style, char c, int index, int start, int end) {
        if (index < start || index >= end) return previous;
        if (!previous.equals(style)) flush(runs, pending, previous);
        pending.append(c);
        return style;
    }

    private static void flush(List<FormattedCharSequence> runs, StringBuilder pending, Style style) {
        if (!pending.isEmpty()) {
            runs.add(FormattedCharSequence.forward(pending.toString(), style));
            pending.setLength(0);
        }
    }
}
