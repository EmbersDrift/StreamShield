package livehider.mixin.accessor;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Map;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Uses Mixin remapping rather than development-only reflection names. */
@Mixin(RenderPipelines.class)
public interface RenderPipelinesAccessor {
    @Accessor("PIPELINES_BY_LOCATION")
    static Map<Identifier, RenderPipeline> liveHider$getPipelines() {
        throw new AssertionError("Mixin accessor was not applied");
    }
}
