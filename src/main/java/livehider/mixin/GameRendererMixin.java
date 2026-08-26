package livehider.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redirects the GUI render pass to draw the overlay, and fixes the cursor state.
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
                GuiGraphicsExtractor overlayGraphics = Objects.requireNonNull(LiveHider.getRenderer()).getGuiGraphics();
                Minecraft mc = this.getMinecraft();
                String lang = mc.getLanguageManager().getSelected();
                String label = (lang != null && lang.startsWith("zh")) ? "屏幕测试" : "Screen Test";
                overlayGraphics.text(mc.font, label, 2, 2, 0xFFFFFFFF, true);
            } catch (Exception ignored) {
            }
        }
    }

    @Redirect(
        method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V")
    )
    private void redirectGuiRendering(GuiRenderer instance, GpuBufferSlice fogBuffer) {
        instance.render(fogBuffer);
        if (LiveHider.getIsInitialized()) {
            OverlayRenderer overlayRenderer = LiveHider.getRenderer();
            assert overlayRenderer != null;
            GuiRenderer custom = overlayRenderer.getOverlayGuiRenderer(instance);
            overlayRenderer.beginDraw();
            custom.render(fogBuffer);
            overlayRenderer.endDraw();
        }
    }

    @Shadow
    public abstract Minecraft getMinecraft();

    @Inject(
        method = "extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;applyCursor(Lcom/mojang/blaze3d/platform/Window;)V", shift = Shift.AFTER)
    )
    private void fixCursor(DeltaTracker deltaTracker, boolean renderLevel, boolean extractGuiArg, CallbackInfo ci) {
        if (LiveHider.getIsInitialized()) {
            GuiGraphicsExtractor guiGraphics = LiveHider.getAPI().getOverlayGuiGraphics();
            if (((GuiGraphicsAccessor) guiGraphics).getPendingCursor() != CursorType.DEFAULT) {
                guiGraphics.applyCursor(this.getMinecraft().getWindow());
            }
        }
    }
}
