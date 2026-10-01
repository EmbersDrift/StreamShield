package livehider.mixin.components;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import livehider.overlay.HitboxCaptureScope;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.debug.DebugValueAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityHitboxDebugRenderer.class)
public abstract class HitboxCaptureMixin {
    @WrapMethod(method = "emitGizmos")
    private void liveHider$tagHitboxes(double x, double y, double z, DebugValueAccess values,
                                      Frustum frustum, float partialTick, Operation<Void> original) {
        try (HitboxCaptureScope ignored = HitboxCaptureScope.enter()) {
            original.call(x, y, z, values, frustum, partialTick);
        }
    }
}
