package livehider.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import livehider.LiveHider;
import livehider.overlay.OverlayRenderer;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Routes the GUI render target to our overlay target when redirected components draw.
 * Ported from obs-overlay (MIT, author zziger).
 */
@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @ModifyExpressionValue(
        method = "draw(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget overriddenTarget(RenderTarget original) {
        OverlayRenderer renderer = LiveHider.getRenderer();
        return renderer != null ? renderer.getGuiRenderTarget() : original;
    }
}
