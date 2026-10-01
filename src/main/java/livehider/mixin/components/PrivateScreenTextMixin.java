package livehider.mixin.components;

import livehider.LiveHiderConfig;
import livehider.text.SafeText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Render-only hooks. No mutations to menus, book items, signing or editing inputs. */
@Mixin(GuiGraphics.class)
public class PrivateScreenTextMixin {
    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component privateContainerTitle(Component text) {
        var config = LiveHiderConfig.get();
        var screen = Minecraft.getInstance().screen;
        return config != null && config.redactContainerTitles && screen instanceof AbstractContainerScreen<?>
            && text == screen.getTitle() ? SafeText.rewrite(text) : text;
    }
    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FormattedCharSequence privateBookText(FormattedCharSequence text) {
        var config = LiveHiderConfig.get();
        if (config == null || !config.redactBooks || !(Minecraft.getInstance().screen instanceof BookViewScreen)) return text;
        var root = Component.empty();
        Style[] previous = {null};
        StringBuilder run = new StringBuilder();
        text.accept((index, style, cp) -> {
            if (previous[0] != null && !previous[0].equals(style)) {
                root.append(Component.literal(run.toString()).setStyle(previous[0])); run.setLength(0);
            }
            previous[0] = style; run.appendCodePoint(cp); return true;
        });
        if (previous[0] != null) root.append(Component.literal(run.toString()).setStyle(previous[0]));
        return SafeText.rewrite(root).getVisualOrderText();
    }
}
