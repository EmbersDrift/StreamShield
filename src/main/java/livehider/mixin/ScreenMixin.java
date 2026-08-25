package livehider.mixin;

import livehider.LiveHiderConfig;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels the blurred background of screens that should be hidden from the stream.
 * Ported from obs-overlay (MIT, author zziger).
 */
@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "renderBlurredBackground(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void renderBlur(CallbackInfo ci) {
        if (LiveHiderConfig.isScreenOverlayed((Screen) (Object) this)) {
            ci.cancel();
        }
    }
}
