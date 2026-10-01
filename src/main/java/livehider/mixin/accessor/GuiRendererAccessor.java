package livehider.mixin.accessor;

import java.util.Map;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessors for private fields of {@link GuiRenderer} needed to build the overlay renderer.
 * Ported from obs-overlay (MIT, author zziger).
 */
@Mixin(GuiRenderer.class)
public interface GuiRendererAccessor {
    @Accessor("featureRenderDispatcher")
    FeatureRenderDispatcher getFeatureRenderDispatcher();

    @Accessor("pictureInPictureRenderers")
    Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRenderer<?>> getPictureInPictureRenderers();

    @Mutable
    @Accessor("pictureInPictureRenderers")
    void setPictureInPictureRenderers(Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRenderer<?>> renderers);
}
