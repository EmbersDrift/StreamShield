package livehider.overlay;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
/** Reuse the game's premultiplied-alpha blit on both loaders. */
public final class OverlayPipelines {
    public static final RenderPipeline OVERLAY_COMPOSITE = RenderPipelines.ENTITY_OUTLINE_BLIT;
    private OverlayPipelines() {}
    public static void initialize() {}
    public static void registerWithMinecraft() {}
}
