package livehider.overlay;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import livehider.component.IOverlayComponent;
import livehider.LiveHider;
import livehider.LiveHiderConfig;
import livehider.mixin.accessor.GuiRendererAccessor;
import livehider.mixin.accessor.FabricGuiRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.jetbrains.annotations.NotNull;

/**
 * The 26.1 overlay renderer: extracts redirected HUD components into an off-screen
 * target, then composites it onto the screen at swap time so the player sees it but OBS
 * (which grabs the main target) does not. Ported from obs-overlay (MIT, author zziger).
 */
public class OverlayRenderer implements Closeable {
    private static final boolean NEOFORGE = detectNeoForge();
    private final OverlayHook.Handler swapHandler = this::renderFrame;
    private boolean closed;
    private boolean framebufferOverridden = false;
    private OverlayFramebuffer overlayFramebuffer;
    private final GuiRenderState overlayGuiState = new GuiRenderState();
    private GuiGraphicsExtractor overlayGuiGraphics;
    private GuiRenderer overlayGuiRenderer;
    private final HitboxOverlay hitboxOverlay = new HitboxOverlay();

    public HitboxOverlay getHitboxOverlay() { return this.hitboxOverlay; }

    public OverlayRenderer() {
        OverlayHook.init();
        try {
            this.initializeFramebuffers();
            this.resetGuiExtraction();
            OverlayHook.subscribe(this.swapHandler);
        } catch (RuntimeException | LinkageError error) {
            close();
            throw error;
        }
    }

    @Override
    public void close() {
        if (this.closed) return;
        this.closed = true;
        OverlayHook.unsubscribe(this.swapHandler);
        this.framebufferOverridden = false;
        try {
            if (this.overlayGuiRenderer != null) {
                // Fabric borrows vanilla's PiP renderers. Never close those shared instances.
                if (!NEOFORGE) {
                    ((FabricGuiRendererAccessor) this.overlayGuiRenderer).setPictureInPictureRenderers(Map.of());
                }
                this.overlayGuiRenderer.close();
                this.overlayGuiRenderer = null;
            }
        } finally {
            try {
                this.hitboxOverlay.close();
            } finally {
                if (this.overlayFramebuffer != null) {
                    this.overlayFramebuffer.object.destroyBuffers();
                    this.overlayFramebuffer = null;
                }
            }
        }
    }

    private void initializeFramebuffers() {
        Minecraft client = Minecraft.getInstance();
        RenderTarget simpleFramebuffer = new TextureTarget("Overlay Target", client.getWindow().getWidth(), client.getWindow().getHeight(), true);
        this.overlayFramebuffer = new OverlayFramebuffer(simpleFramebuffer);
        clearFramebuffer(simpleFramebuffer);
    }

    private void markOverlayDirty() {
        if (this.overlayFramebuffer != null) {
            this.overlayFramebuffer.dirty = true;
        }
    }

    private static void clearFramebuffer(RenderTarget target) {
        GpuTexture colorTexture = target.getColorTexture();
        if (colorTexture != null) {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            if (target.useDepth && target.getDepthTexture() != null) {
                encoder.clearColorAndDepthTextures(colorTexture, 0, target.getDepthTexture(), 1.0);
            } else {
                encoder.clearColorTexture(colorTexture, 0);
            }
        }
    }

    @NotNull
    public GuiRenderer getOverlayGuiRenderer(GuiRenderer copyFrom) {
        if (this.overlayGuiRenderer != null) {
            return this.overlayGuiRenderer;
        }
        try {
            // NeoForge 26.1 replaces vanilla's renderer list parameter with its own registration
            // records. Construct reflectively so the shared overlay code remains binary-compatible
            // with Fabric; PiP renderers are intentionally omitted on NeoForge because they are
            // unrelated to the HUD components StreamShield redirects.
            GuiRendererAccessor access = (GuiRendererAccessor) copyFrom;
            List<?> pipRenderers = NEOFORGE
                ? List.of()
                : ((FabricGuiRendererAccessor) copyFrom).getPictureInPictureRenderers().values().stream().toList();
            Constructor<GuiRenderer> constructor = GuiRenderer.class.getConstructor(
                GuiRenderState.class,
                net.minecraft.client.renderer.MultiBufferSource.BufferSource.class,
                net.minecraft.client.renderer.SubmitNodeCollector.class,
                net.minecraft.client.renderer.feature.FeatureRenderDispatcher.class,
                List.class
            );
            this.overlayGuiRenderer = constructor.newInstance(
                this.overlayGuiState,
                access.getBufferSource(),
                access.getSubmitNodeCollector(),
                access.getFeatureRenderDispatcher(),
                pipRenderers
            );
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to create StreamShield overlay GUI renderer", e);
        }
        return this.overlayGuiRenderer;
    }

    private static boolean detectNeoForge() {
        try {
            Class.forName("net.neoforged.neoforge.common.NeoForge");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public RenderTarget getGuiRenderTarget() {
        return this.framebufferOverridden && this.overlayFramebuffer != null
            ? this.overlayFramebuffer.object
            : Minecraft.getInstance().getMainRenderTarget();
    }

    public GuiGraphicsExtractor getGuiGraphics() {
        return this.overlayGuiGraphics;
    }

    @NotNull
    public GuiGraphicsExtractor getGuiGraphics(IOverlayComponent component, GuiGraphicsExtractor original) {
        if (!component.isOverlayEnabled()) {
            return original;
        }
        if (component.isHidden()) {
            return DummyGuiGraphics.INSTANCE;
        }
        GuiGraphicsExtractor guiGraphics = this.getGuiGraphics();
        return guiGraphics != null ? guiGraphics
            : LiveHiderConfig.get().hideHudWhenOverlayUnavailable ? DummyGuiGraphics.INSTANCE : original;
    }

    public void beginDraw() {
        if (this.overlayFramebuffer != null) {
            this.framebufferOverridden = true;
            this.markOverlayDirty();
        }
    }

    public void endDraw() {
        if (this.overlayFramebuffer != null) {
            this.framebufferOverridden = false;
        }
    }

    public void onResolutionChanged(Minecraft client) {
        this.hitboxOverlay.resize(client.getWindow().getWidth(), client.getWindow().getHeight());
        if (this.overlayFramebuffer != null) {
            this.overlayFramebuffer.object.resize(client.getWindow().getWidth(), client.getWindow().getHeight());
        }
    }

    private static void renderQuad(RenderTarget framebuffer) {
        if (framebuffer.getColorTexture() != null) {
            Minecraft minecraft = Minecraft.getInstance();
            int width = minecraft.getWindow().getWidth();
            int height = minecraft.getWindow().getHeight();
            RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "Overlay Screen", new OverlayScreenTextureView(width, height), OptionalInt.empty());
            try {
                // On NeoForge use a vanilla full-screen blit pipeline.  It has
                // the same screenquad/InSampler contract but is compiled by the
                // game's own resource reload, rather than relying on a mod shader.
                renderPass.setPipeline(NEOFORGE
                    ? RenderPipelines.ENTITY_OUTLINE_BLIT
                    : OverlayPipelines.OVERLAY_COMPOSITE);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.bindTexture("InSampler", framebuffer.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                renderPass.draw(0, 3);
            } catch (Throwable t) {
                if (renderPass != null) {
                    try {
                        renderPass.close();
                    } catch (Throwable close) {
                        t.addSuppressed(close);
                    }
                }
                throw t;
            }
            if (renderPass != null) {
                renderPass.close();
            }
        }
    }

    public void beginFrame() {
        this.hitboxOverlay.beginFrame();
        if (this.overlayFramebuffer != null) {
            clearFramebuffer(this.overlayFramebuffer.object);
            this.resetGuiExtraction();
        }
    }

    private void resetGuiExtraction() {
        Minecraft minecraft = Minecraft.getInstance();
        int mouseX = (int) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        int mouseY = (int) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
        this.overlayGuiState.reset();
        this.overlayGuiGraphics = new GuiGraphicsExtractor(minecraft, this.overlayGuiState, mouseX, mouseY);
    }

    public void renderFrame() {
        if (!this.closed && LiveHider.getIsInitialized()) {
            RenderTarget hitboxes = this.hitboxOverlay.takeFrame();
            if (hitboxes != null) renderQuad(hitboxes);
        }
        if (!this.closed && LiveHider.getIsInitialized() && this.overlayFramebuffer != null && this.overlayFramebuffer.dirty) {
            this.overlayFramebuffer.dirty = false;
            renderQuad(this.overlayFramebuffer.object);
        }
    }
}
