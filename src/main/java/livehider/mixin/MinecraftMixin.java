package livehider.mixin;

import livehider.LiveHider;
import livehider.overlay.OverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wires the renderer lifecycle into Minecraft: init, resize, and per-tick frame begin.
 * Ported from obs-overlay (MIT, author zziger).
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    /**
     * NeoForge starts the Minecraft constructor while the first resource reload is
     * still assembling the shader-source cache.  Starting the native swap hook at
     * that point can make its first frame request GUI/blit pipelines too early.
     */
    @Unique
    private int liveHider$neoForgeReadyTicks;

    @Inject(method = "<init>(Lnet/minecraft/client/main/GameConfig;)V", at = @At("RETURN"))
    private void constructor(GameConfig args, CallbackInfo ci) {
        if (!liveHider$isNeoForgePresent()) {
            LiveHider.initRender();
        }
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
        if (liveHider$isNeoForgePresent() && !LiveHider.getIsInitialized()) {
            // Let the initial resource reload (including vanilla GUI shaders) finish
            // before the overlay's native swap callback can submit a render pass.
            if (++this.liveHider$neoForgeReadyTicks >= 80) {
                LiveHider.initRender();
            }
        }
        OverlayRenderer renderer = LiveHider.getRenderer();
        if (renderer != null) {
            renderer.beginFrame();
        }
    }

    @Unique
    private static boolean liveHider$isNeoForgePresent() {
        try {
            Class.forName("net.neoforged.neoforge.common.NeoForge");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
