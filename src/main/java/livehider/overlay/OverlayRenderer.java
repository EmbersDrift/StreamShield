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
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import livehider.component.IOverlayComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.jetbrains.annotations.NotNull;

/**
 * The 1.21.11 overlay renderer: draws redirected HUD components into an off-screen
 * target, then composited onto the screen at swap time so the player sees it but OBS
 * (which grabs the main target) does not. Ported from obs-overlay (MIT, author zziger).
 */
public class OverlayRenderer implements Closeable {
    private boolean framebufferOverridden = false;
    private OverlayFramebuffer overlayFramebuffer;
    private final GuiRenderState overlayGuiState = new GuiRenderState();
    private GuiGraphics overlayGuiGraphics;
    private GuiRenderer overlayGuiRenderer;

    public OverlayRenderer() {
        OverlayHook.init();
        OverlayHook.subscribe(this::renderFrame);
        this.initializeFramebuffers();
    }

    @Override
    public void close() {
        OverlayHook.unsubscribe(this::renderFrame);
    }

    private void initializeFramebuffers() {
        Minecraft client = Minecraft.getInstance();
        RenderTarget simpleFramebuffer = new TextureTarget("Overlay Target", client.getWindow().getWidth(), client.getWindow().getHeight(), true);
        clearFramebuffer(simpleFramebuffer);
        this.overlayFramebuffer = new OverlayFramebuffer(simpleFramebuffer);
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
            List<?> pipRenderers = isNeoForgePresent()
                ? List.of()
                : ((Map<?, ?>) getField(copyFrom, "pictureInPictureRenderers")).values().stream().toList();
            Constructor<GuiRenderer> constructor = GuiRenderer.class.getConstructor(
                GuiRenderState.class,
                net.minecraft.client.renderer.MultiBufferSource.BufferSource.class,
                net.minecraft.client.renderer.SubmitNodeCollector.class,
                net.minecraft.client.renderer.feature.FeatureRenderDispatcher.class,
                List.class
            );
            this.overlayGuiRenderer = constructor.newInstance(
                this.overlayGuiState,
                getField(copyFrom, "bufferSource"),
                getField(copyFrom, "submitNodeCollector"),
                getField(copyFrom, "featureRenderDispatcher"),
                pipRenderers
            );
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to create StreamShield overlay GUI renderer", e);
        }
        return this.overlayGuiRenderer;
    }

    private static boolean isNeoForgePresent() {
        try {
            Class.forName("net.neoforged.neoforge.common.NeoForge");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static Object getField(GuiRenderer renderer, String name) throws ReflectiveOperationException {
        Field field = GuiRenderer.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(renderer);
    }

    public RenderTarget getGuiRenderTarget() {
        return this.framebufferOverridden && this.overlayFramebuffer != null
            ? this.overlayFramebuffer.object
            : Minecraft.getInstance().getMainRenderTarget();
    }

    public GuiGraphics getGuiGraphics() {
        return this.overlayGuiGraphics;
    }

    @NotNull
    public GuiGraphics getGuiGraphics(IOverlayComponent component, GuiGraphics original) {
        if (!component.isOverlayEnabled()) {
            return original;
        }
        if (component.isHidden()) {
            return DummyGuiGraphics.INSTANCE;
        }
        GuiGraphics guiGraphics = this.getGuiGraphics();
        return guiGraphics != null ? guiGraphics : original;
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
                // NeoForge's resource reload can omit a mod-added full-screen shader from its
                // initial compilation cache. Its built-in outline blit has the same screenquad /
                // InSampler contract and alpha compositing semantics, so use that stable path.
                renderPass.setPipeline(isNeoForgePresent()
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
        this.overlayGuiGraphics = new GuiGraphics(minecraft, this.overlayGuiState, mouseX, mouseY);
    }

    public void renderFrame() {
        if (this.overlayFramebuffer != null && this.overlayFramebuffer.dirty) {
            this.overlayFramebuffer.dirty = false;
            renderQuad(this.overlayFramebuffer.object);
        }
    }
}
