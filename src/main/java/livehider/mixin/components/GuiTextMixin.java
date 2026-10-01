package livehider.mixin.components;

import livehider.text.SafeText;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Redacts title / subtitle / actionbar content at the point it is set, so sensitive text
 * (e.g. server IP/name) is replaced by the placeholder before it is stored/rendered.
 */
@Mixin(Hud.class)
public class GuiTextMixin {
    @ModifyVariable(method = "setTitle(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), argsOnly = true, index = 1)
    private Component redactTitle(Component component) {
        return SafeText.rewrite(component);
    }

    @ModifyVariable(method = "setSubtitle(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), argsOnly = true, index = 1)
    private Component redactSubtitle(Component component) {
        return SafeText.rewrite(component);
    }

    @ModifyVariable(method = "setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"), argsOnly = true, index = 1)
    private Component redactOverlay(Component component) {
        return SafeText.rewrite(component);
    }
}
