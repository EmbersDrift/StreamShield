package livehider.overlay;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import livehider.mixin.accessor.RenderPipelinesAccessor;
import net.minecraft.resources.Identifier;

/**
 * The 1.21.11 "RenderPipeline" used to composite the overlay target onto the screen
 * at swap time. Ported from obs-overlay (MIT, author zziger).
 */
public final class OverlayPipelines {
    public static final RenderPipeline OVERLAY_COMPOSITE =
        RenderPipeline.builder(new Snippet[0])
            .withLocation(Identifier.fromNamespaceAndPath("live_hider", "pipeline/overlay_composite"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(Identifier.fromNamespaceAndPath("live_hider", "core/overlay"))
            .withSampler("InSampler")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withColorWrite(true, false)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.EMPTY, Mode.TRIANGLES)
            .build();

    /** Registers the pipeline on Fabric, which has no loader event for this. */
    public static void registerWithMinecraft() {
        RenderPipelinesAccessor.liveHider$getPipelines()
            .put(OVERLAY_COMPOSITE.getLocation(), OVERLAY_COMPOSITE);
    }

    /**
     * Forces class initialization without choosing a loader-specific registration path.
     */
    public static void initialize() {
        // Accessing this method initializes OVERLAY_COMPOSITE.
    }
}
