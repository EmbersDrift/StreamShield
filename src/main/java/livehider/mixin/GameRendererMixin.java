package livehider.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.cursor.CursorType;
import java.util.Objects;
import livehider.LiveHider;
import livehider.LiveHiderConfig;
import livehider.mixin.accessor.GuiGraphicsAccessor;
import livehider.overlay.OverlayRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
        method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = Shift.BEFORE)
    )
    private void renderTestIcon(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        if (LiveHiderConfig.get().showTestIcon && LiveHider.getIsInitialized()) {
            try {
                GuiGraphics overlayGraphics = Objects.requireNonNull(LiveHider.getRenderer()).getGuiGraphics();
                Minecraft mc = this.getMinecraft();
                String lang = mc.getLanguageManager().getSelected();
                String label = (lang != null && lang.startsWith("zh")) ? "屏幕测试" : "Screen Test";
                overlayGraphics.drawString(mc.font, label, 2, 2, 0xFFFFFFFF, true);
            } catch (Exception ignored) {
            }
        }
    }

    @WrapOperation(
        method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V")
    )
    private void wrapGuiRendering(GuiRenderer instance, GpuBufferSlice fogBuffer, Operation<Void> original) {
        // Preserve other mods' wrappers and invoke the normal GUI render chain exactly once.
        boolean shield = livehider.client.PrivacyShield.active();
        var privacyRoute = livehider.overlay.PrivacyCapturePolicy.route(shield, LiveHider.getIsInitialized(),
            livehider.client.PrivacyShield.safeDialogVisible());
        if (shield) {
            clearProtectedCapture();
            if (privacyRoute == livehider.overlay.PrivacyCapturePolicy.Route.PRIVATE_OVERLAY) {
                OverlayRenderer renderer = LiveHider.getRenderer();
                renderer.beginDraw();
                try {
                    original.call(instance, fogBuffer);
                } catch (RuntimeException | LinkageError error) {
                    LiveHider.reportOverlayFailure("PRIVATE_GUI_FAILED");
                } finally {
                    renderer.endDraw();
                    clearProtectedCapture();
                }
            } else if (privacyRoute == livehider.overlay.PrivacyCapturePolicy.Route.SAFE_RECOVERY) {
                // Only a fixed, non-sensitive recovery dialog may draw to the capture target.
                original.call(instance, fogBuffer);
            }
        } else {
            original.call(instance, fogBuffer);
        }
        if (LiveHider.getIsInitialized()) {
            OverlayRenderer overlayRenderer = LiveHider.getRenderer();
            assert overlayRenderer != null;
            try {
                GuiRenderer custom = overlayRenderer.getOverlayGuiRenderer(instance);
                overlayRenderer.beginDraw();
                try {
                    custom.render(fogBuffer);
                } finally {
                    overlayRenderer.endDraw();
                }
            } catch (RuntimeException | LinkageError error) {
                LiveHider.reportOverlayFailure("GUI_RENDER_FAILED");
            }
        }
    }

    @Shadow
    public abstract Minecraft getMinecraft();

    private void clearProtectedCapture() {
        var target = getMinecraft().getMainRenderTarget();
        if (target.getColorTexture() != null)
            com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder()
                .clearColorTexture(target.getColorTexture(), 0xff101018);
    }

    @Inject(
        method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;applyCursor(Lcom/mojang/blaze3d/platform/Window;)V", shift = Shift.AFTER)
    )
    private void fixCursor(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        if (LiveHider.getIsInitialized()) {
            GuiGraphics guiGraphics = LiveHider.getAPI().getOverlayGuiGraphics();
            if (((GuiGraphicsAccessor) guiGraphics).getPendingCursor() != CursorType.DEFAULT) {
                guiGraphics.applyCursor(this.getMinecraft().getWindow());
            }
        }
    }
}
