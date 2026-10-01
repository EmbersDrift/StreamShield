package livehider.client;

import com.google.gson.JsonObject;
import dev.architectury.platform.Platform;
import livehider.LiveHider;
import livehider.LiveHiderConfig;
import livehider.component.AllDefaultOverlayComponents;
import livehider.component.IOverlayComponent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Explicit diagnostic allowlist. Never serialize the config, arbitrary component registrations,
 * exception messages, system properties, account profiles, paths, server details or logs.
 */
public final class LiveHiderDiagnostics {
    private static final Properties BUILD = loadBuild();

    private LiveHiderDiagnostics() {}

    /** Only built-in HUD components can appear in a shared report. */
    public enum HudComponent {
        DEBUG_MENU("debug_menu"), CHAT("chat"), CHAT_BAR("chat_bar"), PLAYER_LIST("player_list"),
        SUBTITLES("subtitles"), SCOREBOARDS("scoreboards"), ACTIONBAR("actionbar"),
        TITLE_SUBTITLE("title_subtitle"), EFFECTS("effects"), MAIN_HUD("main_hud"), HITBOXES("hitboxes");

        private final String key;

        HudComponent(String key) { this.key = key; }

        public String key() { return key; }

        public IOverlayComponent component() {
            return switch (this) {
                case DEBUG_MENU -> AllDefaultOverlayComponents.debugMenu;
                case CHAT -> AllDefaultOverlayComponents.chat;
                case CHAT_BAR -> AllDefaultOverlayComponents.chatBar;
                case PLAYER_LIST -> AllDefaultOverlayComponents.playerList;
                case SUBTITLES -> AllDefaultOverlayComponents.subtitles;
                case SCOREBOARDS -> AllDefaultOverlayComponents.scoreboards;
                case ACTIONBAR -> AllDefaultOverlayComponents.actionbar;
                case TITLE_SUBTITLE -> AllDefaultOverlayComponents.titleSubtitle;
                case EFFECTS -> AllDefaultOverlayComponents.effects;
                case MAIN_HUD -> AllDefaultOverlayComponents.mainHud;
                case HITBOXES -> AllDefaultOverlayComponents.hitboxes;
            };
        }
    }

    public static String status() {
        return DiagnosticReport.status(LiveHider.getOverlayStatus());
    }

    public static String failureCode() {
        return DiagnosticReport.failureCode(LiveHider.getOverlayFailureCode());
    }

    public static String buildValue(String key) {
        return DiagnosticReport.version(BUILD.getProperty(key));
    }

    private static Properties loadBuild() {
        Properties properties = new Properties();
        try (InputStream stream = LiveHiderDiagnostics.class.getResourceAsStream("/live_hider-build.properties")) {
            if (stream != null) properties.load(stream);
        } catch (IOException | IllegalArgumentException ignored) {
            // Do not include exception messages, which may contain local filesystem details.
        }
        return properties;
    }

    private static String modVersion(String id) {
        try {
            return Platform.getOptionalMod(id).map(mod -> DiagnosticReport.version(mod.getVersion())).orElse("unknown");
        } catch (RuntimeException ignored) {
            return "unknown";
        }
    }

    public static String report() {
        LiveHiderConfig config = LiveHiderConfig.get();
        JsonObject report = new JsonObject();
        report.addProperty("schema", 1);
        JsonObject build = new JsonObject();
        for (String key : new String[] { "version", "minecraft", "loader", "commit" }) {
            build.addProperty(key, buildValue(key));
        }
        report.add("build", build);
        JsonObject runtime = new JsonObject();
        runtime.addProperty("minecraft", modVersion("minecraft"));
        runtime.addProperty("streamshield", modVersion(LiveHider.ID));
        boolean neoForge = Platform.isNeoForge();
        runtime.addProperty("loader", neoForge ? "neoforge" : "fabric");
        runtime.addProperty("loaderVersion", modVersion(neoForge ? "neoforge" : "fabricloader"));
        runtime.addProperty("architectury", modVersion("architectury"));
        runtime.addProperty("clothConfig", modVersion(neoForge ? "cloth_config" : "cloth-config"));
        runtime.addProperty("javaFeature", Runtime.version().feature());
        report.add("runtime", runtime);
        JsonObject overlay = new JsonObject();
        overlay.addProperty("status", status());
        overlay.addProperty("failureCode", failureCode());
        overlay.addProperty("obsCaptureAutomaticallyVerified", false);
        report.add("overlay", overlay);
        JsonObject settings = new JsonObject();
        settings.addProperty("configurationAvailable", config != null);
        if (config != null) {
            settings.addProperty("anonymizeNames", config.anonymizeNames);
            settings.addProperty("sanitizeChatInput", config.sanitizeChatInput);
            settings.addProperty("redactEnabled", config.redactEnabled);
            settings.addProperty("filterSignText", config.filterSignText);
            settings.addProperty("hideSignText", config.hideSignText);
            settings.addProperty("autoGrabServer", config.autoGrabServer);
            settings.addProperty("redactPresetEnabled", config.redactPresetEnabled);
            settings.addProperty("redactStrictPresetEnabled", config.redactStrictPresetEnabled);
            settings.addProperty("normalizeItemNames", config.normalizeItemNames);
            settings.addProperty("scoreboardEnabled", config.scoreboardEnabled);
            settings.addProperty("hideAllScreens", config.hideAllScreens);
            settings.addProperty("overlayHandledScreensEnabled", config.overlayHandledScreensEnabled);
            settings.addProperty("hideHudWhenOverlayUnavailable", config.hideHudWhenOverlayUnavailable);
            settings.addProperty("showTestIcon", config.showTestIcon);
            settings.addProperty("debugLog", config.debugLog);
        }
        report.add("settings", settings);
        JsonObject components = new JsonObject();
        if (config != null) {
            for (HudComponent hud : HudComponent.values()) {
                IOverlayComponent component = hud.component();
                JsonObject state = new JsonObject();
                state.addProperty("selected", component.isOverlayEnabled());
                state.addProperty("autoHideSupported", component.canAutoHide());
                state.addProperty("autoHide", component.isAutoHideEnabled());
                state.addProperty("autoHiddenWhileThisScreenIsOpen",
                    component.isOverlayEnabled() && component.isHidden());
                components.add(hud.key(), state);
            }
        }
        report.add("components", components);
        return DiagnosticReport.serialize(report);
    }

    /** Returns only the generated filename; no machine-specific path is shown or shared. */
    public static String export() throws IOException {
        Path directory = Platform.getGameFolder().resolve("streamshield-diagnostics");
        Files.createDirectories(directory);
        Path file = Files.createTempFile(directory, "streamshield-", ".json");
        Files.writeString(file, report(), StandardCharsets.UTF_8);
        return file.getFileName().toString();
    }
}
