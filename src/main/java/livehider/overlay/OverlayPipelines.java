package livehider.overlay;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * The 26.1 "RenderPipeline" used to composite the overlay target onto the screen
 * at swap time. Ported from obs-overlay (MIT, author zziger).
 */
public final class OverlayPipelines {
    public static final RenderPipeline OVERLAY_COMPOSITE = register(
        RenderPipeline.builder(new Snippet[0])
            .withLocation(Identifier.fromNamespaceAndPath("live_hider", "pipeline/overlay_composite"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(Identifier.fromNamespaceAndPath("live_hider", "core/overlay"))
            .withSampler("InSampler")
            // 26.1 replaced the old blend/depth/color-write builder calls with state records.
            .withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT), ColorTargetState.WRITE_COLOR))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.EMPTY, Mode.TRIANGLES)
            .build()
    );

    private static RenderPipeline register(RenderPipeline pipeline) {
        RenderPipelines.PIPELINES_BY_LOCATION.put(pipeline.getLocation(), pipeline);
        return pipeline;
    }
}
