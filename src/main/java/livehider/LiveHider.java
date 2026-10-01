package livehider;

import livehider.api.IOverlayAPI;
import livehider.api.impl.DummyOverlayAPI;
import livehider.api.impl.NormalOverlayAPI;
import livehider.component.AllDefaultOverlayComponents;
import livehider.overlay.OverlayHook;
import livehider.overlay.OverlayHookException;
import livehider.overlay.OverlayLifecycle;
import livehider.overlay.OverlayRenderer;
import livehider.overlay.OverlayPipelines;
import livehider.overlay.OverlayUtils;
import livehider.text.Redactor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/** Shared configuration and resource-driven overlay lifecycle for both loaders. */
public final class LiveHider {
    public static final String ID = "live_hider";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    private static final OverlayLifecycle LIFECYCLE = new OverlayLifecycle();
    private static OverlayRenderer renderer;
    private static IOverlayAPI api = new DummyOverlayAPI();

    private LiveHider() {}

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ID, path); }
    public static boolean getIsInitialized() { return LIFECYCLE.state() == OverlayLifecycle.State.READY; }
    public static String getOverlayStatus() { return LIFECYCLE.state().name(); }
    public static String getOverlayFailureCode() { return LIFECYCLE.failureCode(); }
    public static IOverlayAPI getAPI() { return api; }

    @Nullable
    public static OverlayRenderer getRenderer() { return getIsInitialized() ? renderer : null; }

    public static void init() {
        OverlayPipelines.initialize();
        LiveHiderConfig.init();
        AllDefaultOverlayComponents.init();
        LiveHiderConfig.ensureFile();
        Redactor.setup();
        Properties build = new Properties();
        try (InputStream stream = LiveHider.class.getResourceAsStream("/live_hider-build.properties")) {
            if (stream != null) build.load(stream);
        } catch (Exception ignored) {
            // Build identity is diagnostic only; missing metadata must not prevent startup.
        }
        LOGGER.info("StreamShield version={} minecraft={} loader={} commit={}",
            build.getProperty("version", "unknown"), build.getProperty("minecraft", "unknown"),
            build.getProperty("loader", "unknown"), build.getProperty("commit", "unknown"));
    }

    /** Called by Minecraft's successful resource-load callback, including reloads. */
    public static void resourcesLoaded() {
        LIFECYCLE.resourcesLoaded();
    }

    public static void resourcesLoading() {
        LIFECYCLE.resourcesLoading();
        api = new DummyOverlayAPI();
        // Dispose at the next frame boundary, never in the middle of a GUI render/reload callback.
    }

    /** Called on the render thread before GUI extraction each frame. */
    public static void beginOverlayFrame() {
        if (!getIsInitialized()) disposeRenderer();
        Minecraft minecraft = Minecraft.getInstance();
        if (LIFECYCLE.beginAttempt(OverlayHook.isPlatformSupported(),
                minecraft.gui.overlay() instanceof LoadingOverlay)) {
            initRender();
        }
        if (getIsInitialized()) {
            try {
                renderer.beginFrame();
            } catch (RuntimeException | LinkageError error) {
                reportOverlayFailure("FRAME_PREPARE_FAILED");
            }
        }
    }

    private static void initRender() {
        try {
            renderer = new OverlayRenderer();
            api = new NormalOverlayAPI(renderer);
            LIFECYCLE.initialized();
            LOGGER.info("StreamShield overlay initialized; verify the OBS preview before streaming.");
        } catch (Throwable error) {
            String code = error instanceof OverlayHookException hookError
                ? hookError.getCode() : "INITIALIZATION_FAILED";
            reportOverlayFailure(code);
            disposeRenderer();
        }
    }

    /** Safe at the native callback boundary: report once and defer GPU disposal until next frame. */
    public static void reportOverlayFailure(String code) {
        if (LIFECYCLE.fail(code)) {
            api = new DummyOverlayAPI();
            LOGGER.error("StreamShield overlay unavailable: {}", code);
            OverlayUtils.showToast(Component.literal("StreamShield overlay unavailable"),
                Component.literal(code + " - open the pre-stream check."));
        }
    }

    public static void retryOverlayInitialization() {
        LIFECYCLE.retry();
    }

    private static void disposeRenderer() {
        OverlayRenderer previous = renderer;
        renderer = null;
        api = new DummyOverlayAPI();
        if (previous != null) {
            try {
                previous.close();
            } catch (RuntimeException | LinkageError error) {
                LOGGER.warn("StreamShield overlay cleanup failed (details omitted).");
            }
        }
    }

    public static void closeRender() {
        LIFECYCLE.close();
        disposeRenderer();
        OverlayHook.close();
    }
}
