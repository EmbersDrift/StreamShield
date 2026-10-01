package livehider.mixin.accessor;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessors for private fields of {@link GuiRenderer} needed to build the overlay renderer.
 * Ported from obs-overlay (MIT, author zziger).
 */
@Mixin(GuiRenderer.class)
public interface GuiRendererAccessor {
    @Accessor("bufferSource")
    BufferSource getBufferSource();

    @Accessor("submitNodeCollector")
    SubmitNodeCollector getSubmitNodeCollector();

    @Accessor("featureRenderDispatcher")
    FeatureRenderDispatcher getFeatureRenderDispatcher();

}
