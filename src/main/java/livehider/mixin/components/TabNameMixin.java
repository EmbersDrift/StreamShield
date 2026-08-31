package livehider.mixin.components;

import livehider.text.NameAnonymizer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Anonymizes player names in the TAB player list, but honors the local player's self-name mode
 * (HIDE / CUSTOM / OWN), so your own TAB entry shows your real name when OWN is selected.
 */
@Mixin(PlayerTabOverlay.class)
public class TabNameMixin {
    @Inject(method = "getNameForDisplay(Lnet/minecraft/client/multiplayer/PlayerInfo;)Lnet/minecraft/network/chat/Component;", at = @At("RETURN"), cancellable = true)
    private void anonTabName(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        Component name = cir.getReturnValue();
        if (name == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean isSelf = mc.player != null && playerInfo.getProfile().id().equals(mc.player.getUUID());
        Component rewritten = NameAnonymizer.applyPlayerDisplayName(name, playerInfo.getProfile().id(), isSelf);
        if (rewritten != name) {
            cir.setReturnValue(rewritten == null ? Component.empty() : rewritten);
        }
    }
}
