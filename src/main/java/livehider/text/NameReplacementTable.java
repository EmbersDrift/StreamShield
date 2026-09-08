package livehider.text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable name snapshot. A single matcher only visits the original input, so an alias that
 * happens to contain another known player's name is never anonymized a second time.
 */
final class NameReplacementTable {
    private final Map<String, String> replacements;
    private final Pattern allNames;
    private final Pattern selfName;
    private final String ownName;

    NameReplacementTable(Map<String, String> replacements, String ownName) {
        Map<String, String> normalized = new HashMap<>();
        replacements.forEach((name, replacement) -> {
            if (name != null && !name.isEmpty() && replacement != null) {
                normalized.put(name.toLowerCase(Locale.ROOT), replacement);
            }
        });
        this.replacements = Map.copyOf(normalized);
        this.ownName = ownName == null ? null : ownName.toLowerCase(Locale.ROOT);
        ArrayList<String> names = new ArrayList<>(normalized.keySet());
        names.sort(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()));
        this.allNames = names.isEmpty() ? null : compileNames(names.stream().map(Pattern::quote).toList());
        this.selfName = this.ownName != null && normalized.containsKey(this.ownName)
            ? compileNames(java.util.List.of(Pattern.quote(this.ownName))) : null;
    }

    private static Pattern compileNames(java.util.List<String> quotedNames) {
        // Minecraft profile names contain ASCII letters, digits and underscores.
        return Pattern.compile("(?<![A-Za-z0-9_])((?:\u00a7[0-9a-fk-or])*)(" + String.join("|", quotedNames) + ")(?![A-Za-z0-9_])",
            Pattern.CASE_INSENSITIVE);
    }

    String replace(String text, boolean includeOtherPlayers) {
        Pattern pattern = includeOtherPlayers ? allNames : selfName;
        if (text == null || pattern == null) {
            return text;
        }
        return pattern.matcher(text).replaceAll(match ->
            Matcher.quoteReplacement(match.group(1) + replacements.get(match.group(2).toLowerCase(Locale.ROOT))));
    }

    String replaceExact(String text, boolean includeOtherPlayers) {
        if (text == null) {
            return null;
        }
        String key = text.toLowerCase(Locale.ROOT);
        if (!includeOtherPlayers && !key.equals(ownName)) {
            return text;
        }
        return replacements.getOrDefault(key, text);
    }
}
