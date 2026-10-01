package livehider.text;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NamePrivacyPolicyTest {
    @Test void disabledMasterNeverCallsAliasGenerator() {
        for (boolean self : new boolean[]{false, true}) {
            for (boolean others : new boolean[]{false, true}) {
                var policy = new NamePrivacyPolicy(self, others, true);
                for (boolean isSelf : new boolean[]{false, true}) {
                    var calls = new AtomicInteger();
                    String result = policy.replacement(isSelf, () -> {
                        calls.incrementAndGet();
                        return "alias";
                    });
                    boolean enabled = isSelf ? self : others;
                    assertEquals(enabled ? 1 : 0, calls.get());
                    assertEquals(enabled ? "alias" : null, result);
                }
            }
        }
    }

    @Test void chatToggleCannotOverrideEitherMaster() {
        for (boolean self : new boolean[]{false, true}) {
            for (boolean others : new boolean[]{false, true}) {
                for (boolean chat : new boolean[]{false, true}) {
                    var policy = new NamePrivacyPolicy(self, others, chat);
                    assertEquals(chat && (self || others), policy.chatEnabled());
                }
            }
        }
    }

    @Test void staleAliasesAreIgnoredOnEverySwitchCombination() {
        var table = new NameReplacementTable(Map.of("Self", "Mine", "Other", "Theirs"), "Self");
        for (boolean self : new boolean[]{false, true}) {
            for (boolean others : new boolean[]{false, true}) {
                assertEquals((self ? "Mine" : "Self") + " " + (others ? "Theirs" : "Other"),
                    table.replace("Self Other", others, self));
                assertEquals(self ? "Mine" : "Self", table.replaceExact("Self", others, self));
                assertEquals(others ? "Theirs" : "Other", table.replaceExact("Other", others, self));
            }
        }
    }
}
