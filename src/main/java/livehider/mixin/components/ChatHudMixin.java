package livehider.mixin.components;

import livehider.LiveHider;
import livehider.component.AllDefaultOverlayComponents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Redirects chat onto the overlay target. Ported from obs-overlay (MIT, author zziger).
 */
@Mixin(ChatComponent.class)
public class ChatHudMixin {
    @ModifyVariable(
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 1
    )
    private GuiGraphics drawStart(GuiGraphics value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.chat, value);
    }
}
