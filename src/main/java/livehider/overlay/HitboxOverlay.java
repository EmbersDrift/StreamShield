package livehider.overlay;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.joml.Matrix4f;

/**
 * Owns only F3+B geometry. Never flushes Minecraft's buffer source or item queues.
 * The target is composited at native swap time, separately from the main world target.
 */
public final class HitboxOverlay implements AutoCloseable {
    private DrawableGizmoPrimitives pending = new DrawableGizmoPrimitives();
    private RenderTarget target;
    private ByteBufferBuilder vertices;
    private boolean drawing;
    private boolean dirty;

    public DrawableGizmoPrimitives primitives() { return pending; }

    public void beginFrame() {
        pending = new DrawableGizmoPrimitives();
        dirty = false;
    }

    public void render(PoseStack pose, CameraRenderState camera, Matrix4f modelView) {
        if (drawing || pending.isEmpty()) return;
        DrawableGizmoPrimitives geometry = pending;
        pending = new DrawableGizmoPrimitives();
        Minecraft mc = Minecraft.getInstance();
        if (!canPresent(mc)) {
            dirty = false;
            return;
        }
        if (target == null) {
            target = new TextureTarget("StreamShield hitboxes", mc.getWindow().getWidth(), mc.getWindow().getHeight(), true);
        }
        if (vertices == null) vertices = new ByteBufferBuilder(16384);
        // Use the active world's depth, without writing back into the world's depth buffer.
        GpuTextureView depth = RenderSystem.outputDepthTextureOverride;
        if (depth == null) depth = mc.getMainRenderTarget().getDepthTextureView();
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
            target.getColorTexture(), 0, target.getDepthTexture(), 1.0);
        if (depth != null) {
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(depth.texture(), target.getDepthTexture(),
                0, 0, 0, 0, 0, target.width, target.height);
        }
        // A NEW buffer source over our own storage cannot contain another player's held item.
        MultiBufferSource.BufferSource isolated = MultiBufferSource.immediate(vertices);
        PoseStack isolatedPose = new PoseStack();
        isolatedPose.mulPose(pose.last().pose());
        drawing = true;
        try (ScopedRenderState<TargetState> ignored = new ScopedRenderState<>(TargetState::current,
                TargetState::apply, new TargetState(target.getColorTextureView(), target.getDepthTextureView()))) {
            geometry.render(isolatedPose, isolated, camera, modelView);
            isolated.endBatch();
            dirty = true;
        } finally {
            drawing = false;
            vertices.clear();
        }
    }

    public RenderTarget takeFrame() {
        // Screens may open after world extraction, before the native swap callback.
        // Discard this frame rather than replaying it when the screen closes.
        if (!canPresent(Minecraft.getInstance())) {
            beginFrame();
            return null;
        }
        if (!dirty) return null;
        dirty = false;
        return target;
    }

    private static boolean canPresent(Minecraft mc) {
        return HitboxVisibility.canDraw(mc.level != null, mc.screen != null, mc.getOverlay() != null);
    }

    public void resize(int width, int height) {
        if (target != null) target.resize(width, height);
        beginFrame();
    }

    @Override public void close() {
        beginFrame();
        try {
            if (target != null) target.destroyBuffers();
        } finally {
            target = null;
            if (vertices != null) vertices.close();
            vertices = null;
        }
    }

    private record TargetState(GpuTextureView color, GpuTextureView depth) {
        static TargetState current() {
            return new TargetState(RenderSystem.outputColorTextureOverride, RenderSystem.outputDepthTextureOverride);
        }
        void apply() {
            RenderSystem.outputColorTextureOverride = color;
            RenderSystem.outputDepthTextureOverride = depth;
        }
    }
}
