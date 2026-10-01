package livehider.mixin.components;

import livehider.overlay.HitboxCaptureScope;
import livehider.overlay.HitboxGizmo;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.Gizmos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gizmos.class)
public abstract class HitboxGizmosMixin {
    @ModifyVariable(method = "addGizmo", at = @At("HEAD"), argsOnly = true)
    private static Gizmo liveHider$mark(Gizmo gizmo) {
        return HitboxCaptureScope.active() && !(gizmo instanceof HitboxGizmo) ? new HitboxGizmo(gizmo) : gizmo;
    }
}
