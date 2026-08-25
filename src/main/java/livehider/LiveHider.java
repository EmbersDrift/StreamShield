package livehider;

import livehider.api.IOverlayAPI;
import livehider.api.impl.DummyOverlayAPI;
import livehider.api.impl.NormalOverlayAPI;
import livehider.component.AllDefaultOverlayComponents;
import livehider.overlay.OverlayRenderer;
import livehider.overlay.OverlayUtils;
import livehider.text.Redactor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main orchestrator for Live Hider. Retains the obs-overlay overlay mechanism
 * (MIT, author zziger) plus the added stream-safe content rewriting layer.
 */
public final class LiveHider {
    public static final String ID = "live_hider";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    private static OverlayRenderer renderer = null;
    private static IOverlayAPI api = new DummyOverlayAPI();
    private static boolean initialized = false;

    private LiveHider() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    public static boolean getIsInitialized() {
        return initialized;
    }

    public static IOverlayAPI getAPI() {
        return api;
    }

    @Nullable
    public static OverlayRenderer getRenderer() {
        return renderer;
    }

    public static void init() {
        LiveHiderConfig.init();
        AllDefaultOverlayComponents.init();
        LiveHiderConfig.ensureFile();
        Redactor.setup();
    }

    public static void initRender() {
        try {
            renderer = new OverlayRenderer();
            api = new NormalOverlayAPI(renderer);
            initialized = true;
        } catch (Throwable e) {
            LOGGER.error("Failed to initialize Live Hider render", e);
            OverlayUtils.showToast(Component.literal("Failed to initialize Live Hider"), Component.literal(e.getMessage()));
            renderer = null;
            api = new DummyOverlayAPI();
        }
    }
}
