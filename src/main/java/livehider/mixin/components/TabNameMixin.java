package livehider.mixin.components;

import livehider.text.SafeText;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Anonymizes player names in the TAB player list.
 */
@Mixin(PlayerTabOverlay.class)
public class TabNameMixin {
    @Inject(method = "getNameForDisplay(Lnet/minecraft/client/multiplayer/PlayerInfo;)Lnet/minecraft/network/chat/Component;", at = @At("RETURN"), cancellable = true)
    private void anonTabName(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        Component name = cir.getReturnValue();
        if (name != null) {
            Component rewritten = SafeText.rewrite(name);
            if (rewritten != name) {
                cir.setReturnValue(rewritten);
            }
        }
    }
}
