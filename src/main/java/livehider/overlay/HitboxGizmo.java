package livehider.overlay;

import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoPrimitives;

/** Marker travels with vanilla's frame state, avoiding a global cross-frame geometry queue. */
public record HitboxGizmo(Gizmo delegate) implements Gizmo {
    @Override public void emit(GizmoPrimitives primitives, float alpha) { delegate.emit(primitives, alpha); }
}
