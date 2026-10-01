package livehider.text;

import java.util.function.UnaryOperator;
import livehider.LiveHiderConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignText;

/** Creates display-only copies; never changes block-entity data or the editor's text cache. */
public final class SignTextFilter {
    private SignTextFilter() {}

    public static SignText forDisplay(SignText text, boolean filtered) {
        LiveHiderConfig config = LiveHiderConfig.get();
        if (config == null) return text;
        return rewrite(text, filtered, config.hideSignText, config.filterSignText && Redactor.isActive(),
            component -> SafeText.rewriteNode(component, Redactor::applyToText));
    }

    static SignText rewrite(SignText text, boolean filtered, boolean blank, boolean filter,
                            UnaryOperator<Component> rewrite) {
        if (!blank && !filter) return text;
        java.util.List<Component> messages = new java.util.ArrayList<>(text.getMessages(filtered));
        boolean changed = false;
        for (int line = 0; line < SignText.LINES; line++) {
            Component source = messages.get(line);
            Component replacement = blank ? Component.empty() : rewrite.apply(source);
            if (!replacement.equals(source)) {
                // Update only the display copy. Both arrays use the selected filtering variant.
                messages.set(line, replacement);
                changed = true;
            }
        }
        return changed ? new SignText(java.util.List.copyOf(messages), java.util.List.copyOf(messages),
            text.getColor(), text.hasGlowingText()) : text;
    }
}
