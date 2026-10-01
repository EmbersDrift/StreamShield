package livehider.text;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ItemNameNormalizerTest {
    @Test void keepsNativeTranslationKeyInsteadOfFreezingALiteralName() {
        Component result = ItemNameNormalizer.resourceName("item.minecraft.diamond_sword");
        assertEquals("item.minecraft.diamond_sword", ((TranslatableContents) result.getContents()).getKey());
    }

    @Test void existingComponentFollowsResourcePackLanguageReload() {
        Language original = Language.getInstance();
        String key = "item.minecraft.diamond_sword";
        Component name = ItemNameNormalizer.resourceName(key);
        try {
            Language.inject(language(key, "Pack sword", original));
            assertEquals("Pack sword", name.getString());
            Language.inject(language(key, "Another pack sword", original));
            assertEquals("Another pack sword", name.getString());
        } finally {
            Language.inject(original);
        }
    }

    private static Language language(String key, String value, Language fallback) {
        return new Language() {
            @Override public String getOrDefault(String name, String defaultValue) {
                return name.equals(key) ? value : fallback.getOrDefault(name, defaultValue);
            }
            @Override public boolean has(String name) { return name.equals(key) || fallback.has(name); }
            @Override public boolean isDefaultRightToLeft() { return false; }
            @Override public FormattedCharSequence getVisualOrder(FormattedText text) { return fallback.getVisualOrder(text); }
        };
    }
}
