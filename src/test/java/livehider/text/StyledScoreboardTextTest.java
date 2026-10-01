package livehider.text;

import livehider.ScoreboardRule;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class StyledScoreboardTextTest {
    private Component rewrite(Component source, String key, String replacement) {
        return new ScoreboardRuleChain(List.of(new ScoreboardRule(true, key, replacement))).applyComponent(source);
    }

    @Test void gradientAddressMatchesAcrossEveryLetter() {
        var source = Component.literal("ip: ");
        for (char c : "astrummc.net".toCharArray())
            source.append(Component.literal(String.valueOf(c)).setStyle(Style.EMPTY.withColor(0xff7800 - c)));
        source.append(Component.literal("\u00a70\u00a74\u00a7r"));
        assertEquals("ip: hidden", rewrite(source, "ASTRUMMC.NET", "hidden").getString());
        assertTrue(source.getString().contains("astrummc.net"));
    }

    @Test void unmatchedInputRetainsOriginalIdentity() {
        var source = Component.literal("other server\u00a7r");
        assertSame(source, rewrite(source, "astrummc.net", "hidden"));
    }

    @Test void keepsUntouchedStylesAndReplacementInheritsFirstMatchedStyle() {
        var iconStyle = Style.EMPTY.withColor(0x123456).withBold(true).withInsertion("icon")
            .withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("server", "icons")));
        var firstStyle = Style.EMPTY.withColor(0xff7800);
        var source = Component.literal("\ue001").setStyle(iconStyle)
            .append(Component.literal(" secret ").setStyle(firstStyle))
            .append(Component.literal("\ue002").setStyle(iconStyle));
        var result = rewrite(source, "secret", "safe");
        assertEquals("\ue001 safe \ue002", result.getString());
        result.visit((style, text) -> {
            if (text.contains("\ue001") || text.contains("\ue002")) assertEquals(iconStyle, style);
            if (text.equals("safe")) assertEquals(firstStyle.applyTo(iconStyle), style);
            return Optional.empty();
        }, Style.EMPTY);
    }

    @Test void multipleMatchesDeletionBoundariesAndLiteralReplacement() {
        assertEquals("$1\\safe $1\\safe secret2", rewrite(Component.literal("secret SECRET secret2"), "secret", "$1\\safe").getString());
        assertEquals("a  b", rewrite(Component.literal("a secret b"), "secret", "").getString());
    }

    @Test void rulesChainAcrossFormattedReplacements() {
        var rules = new ScoreboardRuleChain(List.of(new ScoreboardRule(true, "secret", "&dRank"),
            new ScoreboardRule(true, "Rank", "Tier")));
        assertEquals("Tier", rules.applyComponent(Component.literal("secret")).getString());
    }

    @Test void sourceLegacyCodesAreIgnoredForMatchingButAmpersandsRemainLiteral() {
        assertEquals("A&B hidden", rewrite(Component.literal("A&B as\u00a7ctrummc.net"), "astrummc.net", "hidden").getString());
        assertEquals("&a safe", rewrite(Component.literal("&a secret\u00a7r"), "secret", "safe").getString());
    }

    @Test void unicodeTitleNeedsItsActualCharacters() {
        var source = Component.empty();
        "ᴀꜱᴛʀᴜᴍ".codePoints().forEach(c -> source.append(Component.literal(new String(Character.toChars(c)))));
        assertSame(source, rewrite(source, "ASTRUM", "hidden"));
        assertEquals("hidden", rewrite(source, "ᴀꜱᴛʀᴜᴍ", "hidden").getString());
    }
}
