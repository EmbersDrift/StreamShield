package livehider.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.cursor.CursorType;
import java.util.Objects;
import livehider.LiveHider;
import livehider.LiveHiderConfig;
import livehider.mixin.accessor.GuiGraphicsAccessor;
import livehider.overlay.OverlayRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wraps the GUI render pass to draw the overlay, and fixes the cursor state.
 * Ported from obs-overlay (MIT, author zziger).
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(
        method = "render()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V", shift = Shift.BEFORE)
    )
    private void renderTestIcon(CallbackInfo ci) {
        if (LiveHiderConfig.get().showTestIcon && LiveHider.getIsInitialized()) {
            try {
                GuiGraphicsExtractor overlayGraphics = Objects.requireNonNull(LiveHider.getRenderer()).getGuiGraphics();
                Minecraft mc = Minecraft.getInstance();
                String lang = mc.getLanguageManager().getSelected();
                String label = (lang != null && lang.startsWith("zh")) ? "屏幕测试" : "Screen Test";
                overlayGraphics.text(mc.font, label, 2, 2, 0xFFFFFFFF, true);
            } catch (Exception ignored) {
            }
        }
    }

    @WrapOperation(
        method = "render()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V")
    )
    private void wrapGuiRendering(GuiRenderer instance, Operation<Void> original) {
        // Preserve other mods' wrappers and invoke the normal GUI render chain exactly once.
        original.call(instance);
        if (LiveHider.getIsInitialized()) {
            OverlayRenderer overlayRenderer = LiveHider.getRenderer();
            assert overlayRenderer != null;
            try {
                GuiRenderer custom = overlayRenderer.getOverlayGuiRenderer(instance);
                overlayRenderer.beginDraw();
                try {
                    custom.render();
                    custom.endFrame();
                } finally {
                    overlayRenderer.endDraw();
                }
            } catch (RuntimeException | LinkageError error) {
                LiveHider.reportOverlayFailure("GUI_RENDER_FAILED");
            }
        }
    }

}
