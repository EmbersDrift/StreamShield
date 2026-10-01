package livehider.mixin.components;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import livehider.LiveHider;
import livehider.component.AllDefaultOverlayComponents;
import livehider.overlay.HitboxGizmo;
import livehider.overlay.OverlayRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class HitboxRenderMixin {
    @WrapOperation(method = "finalizeGizmoCollection", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/gizmos/Gizmo;emit(Lnet/minecraft/gizmos/GizmoPrimitives;F)V"))
    private void liveHider$routeHitbox(Gizmo gizmo, GizmoPrimitives originalPrimitives, float alpha, Operation<Void> original) {
        OverlayRenderer renderer = LiveHider.getRenderer();
        if (gizmo instanceof HitboxGizmo && renderer != null && AllDefaultOverlayComponents.hitboxes.isOverlayEnabled()) {
            original.call(gizmo, renderer.hitboxPrimitives(), alpha);
        } else {
            original.call(gizmo, originalPrimitives, alpha);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;"))
    private void liveHider$drawHitboxes(CallbackInfo ci) {
        OverlayRenderer renderer = LiveHider.getRenderer();
        if (renderer == null) return;
        try {
            renderer.renderHitboxes();
        } catch (RuntimeException | LinkageError error) {
            LiveHider.reportOverlayFailure("HITBOX_RENDER_FAILED");
        }
    }
}
