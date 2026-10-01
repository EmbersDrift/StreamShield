package livehider.mixin.components;

import livehider.text.NameAnonymizer;
import livehider.text.SafeText;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Keep original history so changing the privacy switches can rebuild the displayed lines. */
@Mixin(ChatComponent.class)
public class ChatNameMixin {
    @ModifyVariable(method = {"addMessageToDisplayQueue", "logChatMessage"},
        at = @At("HEAD"), argsOnly = true, index = 1)
    private GuiMessage anonMessage(GuiMessage message) {
        NameAnonymizer.refreshFromConnection();
        Component rewritten = SafeText.rewrite(message.content());
        if (rewritten == message.content()) return message;
        return new GuiMessage(message.addedTime(), rewritten, message.signature(), message.tag());
    }
}
