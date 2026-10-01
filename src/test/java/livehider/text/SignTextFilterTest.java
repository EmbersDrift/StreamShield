package livehider.text;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignText;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 26.3 replaced the builder-style {@code SignText} API (no-arg ctor + {@code setMessage(int, …)} /
 * {@code getMessage(int, boolean)}) with an immutable list-based one: {@code SignText(front, back,
 * color, glowing)} + {@code getMessages(boolean front)}.
 */
class SignTextFilterTest {
    private static List<Component> lines(String text) {
        List<Component> list = new ArrayList<>();
        for (int i = 0; i < SignText.LINES; i++) {
            list.add(Component.literal(text));
        }
        return List.copyOf(list);
    }

    private static SignText of(String front, String back, DyeColor color, boolean glowing) {
        return new SignText(lines(front), lines(back), color, glowing);
    }

    @Test void disabledPreservesIdentity() {
        SignText source = of("secret", "secret", DyeColor.BLACK, false);
        assertSame(source, SignTextFilter.rewrite(source, false, false, false,
            value -> { throw new AssertionError("Must not rewrite"); }));
    }

    @Test void blankPreservesModelPropertiesAndSource() {
        SignText source = of("secret", "secret", DyeColor.RED, true);
        SignText display = SignTextFilter.rewrite(source, false, true, false,
            value -> { throw new AssertionError("Blank must override filtering"); });
        for (boolean front : new boolean[] { true, false }) {
            for (int i = 0; i < SignText.LINES; i++) {
                assertEquals("", display.getMessages(front).get(i).getString());
                assertEquals("secret", source.getMessages(front).get(i).getString());
            }
        }
        assertEquals(DyeColor.RED, display.getColor());
        assertTrue(display.hasGlowingText());
    }

    @Test void filtersSelectedVariantWithoutChangingEitherSourceArray() {
        // getMessages(true) is the variant SignTextFilter rewrites; the other must stay untouched.
        SignText source = of("unfiltered", "secret filtered", DyeColor.BLACK, false);
        SignText display = SignTextFilter.rewrite(source, true, false, true,
            value -> SafeText.rewriteNode(value, s -> s.replace("secret", "***")));
        assertEquals("*** filtered", display.getMessages(true).get(0).getString());
        assertEquals("secret filtered", source.getMessages(true).get(0).getString());
        assertEquals("unfiltered", source.getMessages(false).get(0).getString());
    }

    @Test void unchangedTextKeepsVanillaCache() {
        SignText source = of("safe", "safe", DyeColor.BLACK, false);
        assertSame(source, SignTextFilter.rewrite(source, false, false, true, value -> value.copy()));
    }

    @Test void changingRulesDoesNotReuseRedactedCacheOrMutateSource() {
        SignText source = of("secret", "secret", DyeColor.BLACK, false);
        SignText first = SignTextFilter.rewrite(source, false, false, true, value -> Component.literal("one"));
        SignText second = SignTextFilter.rewrite(source, false, false, true, value -> Component.literal("two"));
        assertNotSame(first, second);
        assertEquals("one", first.getMessages(false).get(0).getString());
        assertEquals("two", second.getMessages(false).get(0).getString());
        assertEquals("secret", source.getMessages(false).get(0).getString());
        assertSame(source, SignTextFilter.rewrite(source, false, false, false, value -> value));
    }
}
