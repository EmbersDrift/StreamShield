package livehider;

/**
 * One scoreboard chain-rule. Rules are applied in list order: the result of one rule is passed to the
 * next, so {@code Level -> 宝马值} then {@code 宝马值 -> X} chains. {@code enabled} allows a rule to be
 * present but inactive. {@code key}/{@code replacement} may be any text (Chinese or English).
 */
public final class ScoreboardRule {
    public boolean enabled = true;
    public String key = "";
    public String replacement = "";

    public ScoreboardRule() {
    }

    public ScoreboardRule(boolean enabled, String key, String replacement) {
        this.enabled = enabled;
        this.key = key;
        this.replacement = replacement;
    }
}
