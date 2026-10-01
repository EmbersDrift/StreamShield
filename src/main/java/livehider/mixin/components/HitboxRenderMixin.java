package livehider.mixin.components;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import livehider.LiveHider;
import livehider.overlay.OverlayRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DrawableGizmoPrimitives.class)
public abstract class HitboxRenderMixin {
    @WrapMethod(method = "render")
    private void liveHider$draw(PoseStack pose, MultiBufferSource sharedBuffers, CameraRenderState camera,
                                Matrix4fc modelView, Operation<Void> original) {
        // All non-hitbox gizmos retain their original buffer, target and mod wrappers.
        original.call(pose, sharedBuffers, camera, modelView);
        OverlayRenderer renderer = LiveHider.getRenderer();
        if (renderer == null) return;
        try {
            renderer.getHitboxOverlay().render(pose, camera, modelView);
        } catch (RuntimeException | LinkageError error) {
            LiveHider.reportOverlayFailure("HITBOX_RENDER_FAILED");
        }
    }
}
