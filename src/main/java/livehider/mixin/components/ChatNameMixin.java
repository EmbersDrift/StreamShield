package livehider.mixin.components;

import livehider.text.NameAnonymizer;
import livehider.text.SafeText;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Rewrites incoming chat messages so real player names are anonymized for stream safety.
 */
@Mixin(ChatComponent.class)
public class ChatNameMixin {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), argsOnly = true, index = 1)
    private Component anonMessage(Component component) {
        NameAnonymizer.refreshFromConnection();
        return SafeText.rewrite(component);
    }

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 1
    )
    private Component anonMessage3(Component component) {
        NameAnonymizer.refreshFromConnection();
        return SafeText.rewrite(component);
    }
}
