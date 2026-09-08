package livehider.mixin;

import livehider.LiveHider;
import livehider.overlay.OverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lifecycle hooks run on the render thread; readiness follows successful resource loading. */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "onResourceLoadFinished", at = @At("RETURN"))
    private void resourcesLoaded(CallbackInfo ci) {
        LiveHider.resourcesLoaded();
    }

    @Inject(method = "setOverlay", at = @At("HEAD"))
    private void resourcesLoading(Overlay overlay, CallbackInfo ci) {
        if (overlay instanceof LoadingOverlay) LiveHider.resourcesLoading();
    }

    @Inject(method = "resizeGui()V", at = @At("RETURN"))
    private void onResolutionChanged(CallbackInfo ci) {
        OverlayRenderer renderer = LiveHider.getRenderer();
        if (renderer != null) {
            try {
                renderer.onResolutionChanged(Minecraft.getInstance());
            } catch (RuntimeException | LinkageError error) {
                LiveHider.reportOverlayFailure("RESIZE_FAILED");
            }
        }
    }

    @Inject(method = "runTick(Z)V", at = @At("HEAD"))
    private void onRender(boolean tick, CallbackInfo ci) {
        LiveHider.beginOverlayFrame();
    }

    @Inject(method = "close()V", at = @At("HEAD"))
    private void closeOverlay(CallbackInfo ci) {
        LiveHider.closeRender();
    }
}
