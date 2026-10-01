package livehider.text;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignText;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SignTextFilterTest {
    @Test void disabledPreservesIdentity() {
        SignText source = new SignText().setMessage(0, Component.literal("secret"));
        assertSame(source, SignTextFilter.rewrite(source, false, false, false,
            value -> { throw new AssertionError("Must not rewrite"); }));
    }

    @Test void blankPreservesModelPropertiesAndSource() {
        SignText source = new SignText().setColor(DyeColor.RED).setHasGlowingText(true);
        for (int i = 0; i < SignText.LINES; i++) source = source.setMessage(i, Component.literal("secret"));
        SignText display = SignTextFilter.rewrite(source, false, true, false,
            value -> { throw new AssertionError("Blank must override filtering"); });
        for (int i = 0; i < SignText.LINES; i++) {
            assertEquals("", display.getMessage(i, false).getString());
            assertEquals("secret", source.getMessage(i, false).getString());
        }
        assertEquals(DyeColor.RED, display.getColor());
        assertTrue(display.hasGlowingText());
    }

    @Test void filtersSelectedVariantWithoutChangingEitherSourceArray() {
        SignText source = new SignText().setMessage(0, Component.literal("unfiltered"),
            Component.literal("secret filtered"));
        SignText display = SignTextFilter.rewrite(source, true, false, true,
            value -> SafeText.rewriteNode(value, s -> s.replace("secret", "***")));
        assertEquals("*** filtered", display.getMessage(0, true).getString());
        assertEquals("unfiltered", source.getMessage(0, false).getString());
        assertEquals("secret filtered", source.getMessage(0, true).getString());
    }

    @Test void unchangedTextKeepsVanillaCache() {
        SignText source = new SignText().setMessage(0, Component.literal("safe"));
        assertSame(source, SignTextFilter.rewrite(source, false, false, true, value -> value.copy()));
    }

    @Test void changingRulesDoesNotReuseRedactedCacheOrMutateSource() {
        SignText source = new SignText().setMessage(0, Component.literal("secret"));
        SignText first = SignTextFilter.rewrite(source, false, false, true, value -> Component.literal("one"));
        first.getRenderMessages(false, Component::getVisualOrderText);
        SignText second = SignTextFilter.rewrite(source, false, false, true, value -> Component.literal("two"));
        assertNotSame(first, second);
        assertEquals("two", second.getMessage(0, false).getString());
        assertEquals("secret", source.getMessage(0, false).getString());
        assertSame(source, SignTextFilter.rewrite(source, false, false, false, value -> value));
    }
}
