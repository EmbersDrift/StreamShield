package livehider.text;

import livehider.ScoreboardRule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreboardRuleChainTest {
    @Test
    void rulesDeliberatelyChainInConfiguredOrder() {
        ScoreboardRule level = new ScoreboardRule(true, "Level", "Rank");
        ScoreboardRule rank = new ScoreboardRule(true, "Rank", "Tier");
        assertEquals("Tier: 94", new ScoreboardRuleChain(List.of(level, rank)).apply("Level: 94"));
        assertEquals("Rank: 94", new ScoreboardRuleChain(List.of(rank, level)).apply("Level: 94"));
    }

    @Test
    void skipsDisabledEmptyAndMalformedEntries() {
        List<ScoreboardRule> rules = Arrays.asList(null, new ScoreboardRule(false, "Level", "Wrong"),
            new ScoreboardRule(true, "", "Wrong"), new ScoreboardRule(true, null, "Wrong"),
            new ScoreboardRule(true, "Level", "Rank"));
        assertEquals("Rank", new ScoreboardRuleChain(rules).apply("Level"));
    }

    @Test
    void matchingIsLiteralCaseInsensitiveAndRespectsBoundaries() {
        ScoreboardRuleChain rules = new ScoreboardRuleChain(List.of(new ScoreboardRule(true, "Rank+", "$1\\safe")));
        assertEquals("$1\\safe Ranks Rank+2", rules.apply("rANK+ Ranks Rank+2"));
    }

    @Test
    void nullReplacementDeletesOnlyMatchedContent() {
        ScoreboardRuleChain rules = new ScoreboardRuleChain(List.of(new ScoreboardRule(true, "secret", null)));
        assertEquals("Score:  42", rules.apply("Score: secret 42"));
    }

    @Test
    void compiledRulesAreUnaffectedByUnsavedEditsUntilRecompiled() {
        ScoreboardRule rule = new ScoreboardRule(true, "Level", "Rank");
        List<ScoreboardRule> source = new ArrayList<>(List.of(rule));
        ScoreboardRuleChain compiled = new ScoreboardRuleChain(source);
        rule.replacement = "Tier";
        rule.enabled = false;
        source.clear();
        assertEquals("Rank", compiled.apply("Level"));
        assertEquals("Level", new ScoreboardRuleChain(source).apply("Level"));
    }

    @Test
    void emptyConfigurationAndNullInputAreSafe() {
        ScoreboardRuleChain rules = new ScoreboardRuleChain(null);
        assertEquals("Level: 94", rules.apply("Level: 94"));
        assertNull(rules.apply(null));
    }
}
