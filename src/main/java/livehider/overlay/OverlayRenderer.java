package livehider.overlay;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.frontend.FrontendCommandEncoder;
import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import livehider.LiveHider;
import livehider.LiveHiderConfig;
import livehider.component.IOverlayComponent;
import livehider.mixin.accessor.GuiRendererAccessor;
import livehider.mixin.accessor.SurfaceAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Vector4f;

/** Off-screen HUD plus a private presentation copy; never modifies the captured main target. */
public class OverlayRenderer implements Closeable {
    private final OverlayHook.Handler swapHandler = this::renderFrame;
    private final GuiRenderState overlayGuiState = new GuiRenderState();
    private RenderTarget hudTarget;
    private RenderTarget presentationTarget;
    private RenderTarget hitboxTarget;
    private RenderBuffers hitboxBuffers;
    private FeatureRenderDispatcher hitboxDispatcher;
    private final SubmitNodeStorage hitboxSubmits = new SubmitNodeStorage();
    private DrawableGizmoPrimitives hitboxes = new DrawableGizmoPrimitives();
    private GuiGraphicsExtractor overlayGuiGraphics;
    private GuiRenderer overlayGuiRenderer;
    private boolean framebufferOverridden;
    private boolean dirty;
    private boolean closed;

    public OverlayRenderer() {
        // The existing native capture hook is OpenGL-only, not a Vulkan capture hook.
        if (!(Minecraft.getInstance().gameRenderer.mainRenderTarget().getColorTexture()
                instanceof com.mojang.renderpearl.backend.opengl.GlTexture)) {
            throw new OverlayHookException("UNSUPPORTED_RENDERER");
        }
        OverlayHook.init();
        try {
            Minecraft mc = Minecraft.getInstance();
            int width = mc.getWindow().getWidth(), height = mc.getWindow().getHeight();
            hudTarget = new TextureTarget("StreamShield HUD", width, height, GpuFormat.RGBA8_UNORM,
                mc.gameRenderer.mainRenderTarget().getDepthTexture().getFormat());
            presentationTarget = new TextureTarget("StreamShield presentation", width, height, GpuFormat.RGBA8_UNORM, null);
            hitboxTarget = new TextureTarget("StreamShield hitboxes", width, height, GpuFormat.RGBA8_UNORM,
                mc.gameRenderer.mainRenderTarget().getDepthTexture().getFormat());
            hitboxBuffers = new RenderBuffers(1);
            hitboxDispatcher = new FeatureRenderDispatcher(hitboxBuffers, mc.getModelManager(), mc.getAtlasManager(), mc.font,
                mc.gameRenderer.gameRenderState());
            beginFrame();
            OverlayHook.subscribe(swapHandler);
        } catch (RuntimeException | LinkageError error) {
            close();
            throw error;
        }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        OverlayHook.unsubscribe(swapHandler);
        framebufferOverridden = false;
        try {
            if (overlayGuiRenderer != null) {
                ((GuiRendererAccessor) overlayGuiRenderer).setPictureInPictureRenderers(Map.of());
                overlayGuiRenderer.close();
                overlayGuiRenderer = null;
            }
        } finally {
            if (hudTarget != null) hudTarget.destroyBuffers();
            if (presentationTarget != null) presentationTarget.destroyBuffers();
            if (hitboxTarget != null) hitboxTarget.destroyBuffers();
            if (hitboxDispatcher != null) hitboxDispatcher.close();
            if (hitboxBuffers != null) hitboxBuffers.close();
        }
    }

    public GuiRenderer getOverlayGuiRenderer(GuiRenderer original) {
        if (overlayGuiRenderer == null) {
            GuiRendererAccessor access = (GuiRendererAccessor) original;
            overlayGuiRenderer = new GuiRenderer(overlayGuiState, access.getFeatureRenderDispatcher(), List.of());
        }
        return overlayGuiRenderer;
    }

    public RenderTarget getGuiRenderTarget() {
        return framebufferOverridden ? hudTarget : Minecraft.getInstance().gameRenderer.mainRenderTarget();
    }

    public GuiGraphicsExtractor getGuiGraphics() { return overlayGuiGraphics; }

    public GuiGraphicsExtractor getGuiGraphics(IOverlayComponent component, GuiGraphicsExtractor original) {
        if (!component.isOverlayEnabled()) return original;
        if (component.isHidden()) return DummyGuiGraphics.INSTANCE;
        return overlayGuiGraphics != null ? overlayGuiGraphics
            : LiveHiderConfig.get().hideHudWhenOverlayUnavailable ? DummyGuiGraphics.INSTANCE : original;
    }

    public void beginDraw() { framebufferOverridden = true; dirty = true; }
    public void endDraw() { framebufferOverridden = false; }

    public void onResolutionChanged(Minecraft mc) {
        hudTarget.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        presentationTarget.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        hitboxTarget.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
    }

    public void beginFrame() {
        dirty = false;
        framebufferOverridden = false;
        hitboxes = new DrawableGizmoPrimitives();
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
            hitboxTarget.getColorTexture(), new Vector4f(), hitboxTarget.getDepthTexture(), RenderSystem.DEFAULT_DEPTH_CLEAR_VALUE);
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
            hudTarget.getColorTexture(), new Vector4f(), hudTarget.getDepthTexture(), RenderSystem.DEFAULT_DEPTH_CLEAR_VALUE);
        Minecraft mc = Minecraft.getInstance();
        overlayGuiState.reset();
        overlayGuiGraphics = new GuiGraphicsExtractor(mc, overlayGuiState,
            (int) mc.mouseHandler.getScaledXPos(mc.getWindow()), (int) mc.mouseHandler.getScaledYPos(mc.getWindow()));
    }

    public void renderFrame() {
        if (closed || !LiveHider.getIsInitialized() || !dirty) return;
        dirty = false;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.gameRenderer.mainRenderTarget();
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(main.getColorTexture(), presentationTarget.getColorTexture(),
            0, 0, 0, 0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());
        try (RenderPass pass = encoder.createRenderPass(() -> "StreamShield composite", presentationTarget.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(RenderSystem.getCompiledPipeline(OverlayPipelines.OVERLAY_COMPOSITE));
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("InSampler", hitboxTarget.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.draw(0, 3, 0, 1);
            pass.setUniform("InSampler", hudTarget.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.draw(0, 3, 0, 1);
        }
        // Native swap callback: frontend has already blitted once. Do not recursively present.
        ((SurfaceAccessor) mc.windowSurface()).liveHider$backend().blitFromTexture(
            ((FrontendCommandEncoder) encoder).backend(), presentationTarget.getColorTextureView());
    }

    public DrawableGizmoPrimitives hitboxPrimitives() { return hitboxes; }

    /** Dedicated submits, dispatcher and vertex buffer: held items cannot enter this pass. */
    public void renderHitboxes() {
        if (hitboxes.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(mc.gameRenderer.mainRenderTarget().getDepthTexture(), hitboxTarget.getDepthTexture(),
            0, 0, 0, 0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());
        hitboxSubmits.setUseImprovedTransparency(false);
        hitboxes.submit(hitboxSubmits, mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState, false);
        try (FeatureRenderDispatcher.PreparedFrame frame = hitboxDispatcher.prepareFrame(hitboxSubmits)) {
            RenderSystem.resizeAllAutoStorageIndexBuffers();
            try (RenderPass pass = encoder.createRenderPass(() -> "StreamShield isolated hitboxes", hitboxTarget.getColorTextureView(),
                    Optional.empty(), hitboxTarget.getDepthTextureView(), OptionalDouble.empty())) {
                RenderSystem.bindDefaultUniforms(pass);
                FeatureRenderDispatcher.renderAllFeatures(pass, frame);
            }
            dirty = true;
        } finally {
            hitboxBuffers.endFrame();
            hitboxes = new DrawableGizmoPrimitives();
        }
    }
}
