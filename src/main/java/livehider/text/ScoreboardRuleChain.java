package livehider.text;

import livehider.ScoreboardRule;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Compiled immutable snapshot; unlike player aliases, scoreboard replacements intentionally chain. */
final class ScoreboardRuleChain {
    private final List<CompiledRule> rules;

    ScoreboardRuleChain(List<ScoreboardRule> configuredRules) {
        List<CompiledRule> compiled = new ArrayList<>();
        if (configuredRules != null) {
            for (ScoreboardRule rule : configuredRules) {
                if (rule != null && rule.enabled && rule.key != null && !rule.key.isEmpty()) {
                    boolean unicode = rule.key.startsWith(ScoreboardUnicodeKey.PREFIX);
                    boolean glyph = rule.key.startsWith(ScoreboardUnicodeKey.GLYPH_PREFIX);
                    var glyphKey = glyph ? ScoreboardUnicodeKey.decodeGlyph(rule.key) : null;
                    if (glyph && glyphKey == null) continue;
                    String key = unicode ? ScoreboardUnicodeKey.decode(rule.key) : rule.key;
                    if (glyph) key = glyphKey.text();
                    // Malformed advanced keys must never become empty/global matches.
                    if (key == null || key.isEmpty()) continue;
                    Pattern pattern = Pattern.compile(unicode || glyph ? Pattern.quote(key)
                        : "(?i)(?<!\\w)" + Pattern.quote(key) + "(?!\\w)");
                    compiled.add(new CompiledRule(pattern, rule.replacement == null ? "" : rule.replacement,
                        glyph ? glyphKey.font() : null));
                }
            }
        }
        rules = List.copyOf(compiled);
    }

    String apply(String text) {
        if (text == null) {
            return null;
        }
        String result = text;
        for (CompiledRule rule : rules) {
            // Plain strings carry no font evidence; never guess for font-qualified rules.
            if (rule.font != null) continue;
            result = rule.pattern.matcher(result).replaceAll(Matcher.quoteReplacement(rule.replacement));
        }
        return result;
    }

    private record CompiledRule(Pattern pattern, String replacement, net.minecraft.resources.Identifier font) {
    }

    net.minecraft.network.chat.Component applyComponent(net.minecraft.network.chat.Component original) {
        if (original == null || rules.isEmpty()) return original;
        var result = original;
        var styled = new StyledScoreboardText(result);
        for (CompiledRule rule : rules) {
            var next = styled.replace(rule.pattern, rule.replacement, rule.font);
            if (next != result) {
                result = next;
                styled = new StyledScoreboardText(result);
            }
        }
        return result;
    }
}
