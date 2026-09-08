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
                    Pattern pattern = Pattern.compile("(?i)(?<!\\w)" + Pattern.quote(rule.key) + "(?!\\w)");
                    compiled.add(new CompiledRule(pattern, rule.replacement == null ? "" : rule.replacement));
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
            result = rule.pattern.matcher(result).replaceAll(Matcher.quoteReplacement(rule.replacement));
        }
        return result;
    }

    private record CompiledRule(Pattern pattern, String replacement) {
    }
}
