package livehider.text;

/** Legacy Minecraft formatting utilities shared by name and scoreboard matching. */
final class LegacyFormatCodes {
    private LegacyFormatCodes() {
    }

    static String strip(String text) {
        if (text == null || text.indexOf('\u00a7') < 0) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length() && isFormattingCode(text.charAt(i + 1))) {
                i++;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static boolean isFormattingCode(char c) {
        char lower = Character.toLowerCase(c);
        return (lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f')
            || (lower >= 'k' && lower <= 'o') || lower == 'r';
    }
}
