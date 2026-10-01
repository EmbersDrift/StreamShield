package livehider.mixin.accessor;

import com.mojang.renderpearl.backend.api.GpuSurfaceBackend;
import com.mojang.renderpearl.frontend.FrontendGpuSurface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Used only at the native OpenGL swap boundary, after the regular surface blit. */
@Mixin(FrontendGpuSurface.class)
public interface SurfaceAccessor {
    @Accessor("backend")
    GpuSurfaceBackend liveHider$backend();
}
