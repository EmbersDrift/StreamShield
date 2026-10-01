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

@Mixin(LevelRenderer.class)
public abstract class HitboxRoutingMixin {
    @WrapOperation(method = "finalizeGizmoCollection", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/gizmos/Gizmo;emit(Lnet/minecraft/gizmos/GizmoPrimitives;F)V"))
    private void liveHider$route(Gizmo gizmo, GizmoPrimitives primitives, float alpha, Operation<Void> original) {
        OverlayRenderer renderer = LiveHider.getRenderer();
        // Fail open for world geometry: an unavailable overlay must not make hitboxes vanish locally.
        if (gizmo instanceof HitboxGizmo && renderer != null && AllDefaultOverlayComponents.hitboxes.isOverlayEnabled()) {
            original.call(gizmo, renderer.getHitboxOverlay().primitives(), alpha);
        } else {
            original.call(gizmo, primitives, alpha);
        }
    }
}
