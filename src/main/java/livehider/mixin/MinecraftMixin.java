package livehider.mixin;

import livehider.LiveHider;
import livehider.overlay.OverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wires the renderer lifecycle into Minecraft: init, resize, and per-tick frame begin.
 * Ported from obs-overlay (MIT, author zziger).
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "<init>(Lnet/minecraft/client/main/GameConfig;)V", at = @At("RETURN"))
    private void constructor(GameConfig args, CallbackInfo ci) {
        LiveHider.initRender();
    }

    @Inject(method = "resizeDisplay()V", at = @At("RETURN"))
    private void onResolutionChanged(CallbackInfo ci) {
        OverlayRenderer renderer = LiveHider.getRenderer();
        if (renderer != null) {
            renderer.onResolutionChanged(Minecraft.getInstance());
        }
    }

    @Inject(method = "runTick(Z)V", at = @At("HEAD"))
    private void onRender(boolean tick, CallbackInfo ci) {
        OverlayRenderer renderer = LiveHider.getRenderer();
        if (renderer != null) {
            renderer.beginFrame();
        }
    }
}
