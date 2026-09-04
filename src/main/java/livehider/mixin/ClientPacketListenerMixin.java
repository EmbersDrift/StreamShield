package livehider.mixin;

import livehider.text.NameAnonymizer;
import livehider.text.Redactor;
import livehider.skin.RandomSkinManager;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Resets the anonymizer session on join and refreshes the name map whenever the player
 * list changes, so new players get anonymized names and removed players drop out.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleLogin(Lnet/minecraft/network/protocol/game/ClientboundLoginPacket;)V", at = @At("RETURN"))
    private void onJoin(ClientboundLoginPacket pkt, CallbackInfo ci) {
        NameAnonymizer.resetSession();
        RandomSkinManager.resetSessionAssignments();
        NameAnonymizer.refreshFromConnection();
        Redactor.captureServer();
    }

    @Inject(method = "handlePlayerInfoUpdate(Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket;)V", at = @At("RETURN"))
    private void onPlayersUpdate(ClientboundPlayerInfoUpdatePacket pkt, CallbackInfo ci) {
        NameAnonymizer.refreshFromConnection();
    }

    @Inject(method = "handlePlayerInfoRemove(Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoRemovePacket;)V", at = @At("RETURN"))
    private void onPlayersRemove(ClientboundPlayerInfoRemovePacket pkt, CallbackInfo ci) {
        NameAnonymizer.refreshFromConnection();
    }
}
