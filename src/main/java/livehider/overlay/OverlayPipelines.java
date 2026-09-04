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
import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * The 26.1 "RenderPipeline" used to composite the overlay target onto the screen
 * at swap time. Ported from obs-overlay (MIT, author zziger).
 */
public final class OverlayPipelines {
    public static final RenderPipeline OVERLAY_COMPOSITE =
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
            .build();

    /** Registers the pipeline on Fabric, which has no loader event for this. */
    public static void registerWithMinecraft() {
        // NeoForge exposes this vanilla registry field as private while Fabric's development
        // environment permits direct access. Keep the integration loader-neutral without an
        // access transformer; failure is explicit rather than silently producing no overlay.
        try {
            Field field = RenderPipelines.class.getDeclaredField("PIPELINES_BY_LOCATION");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Identifier, RenderPipeline> pipelines = (Map<Identifier, RenderPipeline>) field.get(null);
            pipelines.put(OVERLAY_COMPOSITE.getLocation(), OVERLAY_COMPOSITE);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to register StreamShield overlay pipeline", e);
        }
    }

    /**
     * Forces class initialization without choosing a loader-specific registration path.
     */
    public static void initialize() {
        // Accessing this method initializes OVERLAY_COMPOSITE.
    }
}
