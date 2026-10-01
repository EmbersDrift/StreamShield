package livehider.mixin;

import livehider.LiveHider;
import livehider.mixin.accessor.GuiGraphicsAccessor;
import com.mojang.blaze3d.platform.cursor.CursorType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiLifecycleMixin {
    @Inject(method = "setOverlay", at = @At("HEAD"))
    private void liveHider$loading(Overlay overlay, CallbackInfo ci) {
        if (overlay instanceof LoadingOverlay) LiveHider.resourcesLoading();
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void liveHider$cursor(CallbackInfo ci) {
        if (!LiveHider.getIsInitialized()) return;
        GuiGraphicsExtractor graphics = LiveHider.getAPI().getOverlayGuiGraphics();
        if (((GuiGraphicsAccessor) graphics).getPendingCursor() != CursorType.DEFAULT) {
            graphics.applyCursor(Minecraft.getInstance().getWindow());
        }
    }
}
