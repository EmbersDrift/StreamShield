package livehider.mixin.components;

import livehider.LiveHider;
import livehider.component.AllDefaultOverlayComponents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Redirects the chat input bar onto the overlay target. Ported from obs-overlay (MIT, author zziger).
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @ModifyVariable(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("HEAD"), argsOnly = true, index = 1)
    private GuiGraphicsExtractor drawStart(GuiGraphicsExtractor value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.chatBar, value);
    }
}
