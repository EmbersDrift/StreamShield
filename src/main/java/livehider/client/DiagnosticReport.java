package livehider.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Set;

/** Final privacy boundary for shared diagnostics, independent of the game and its config schema. */
final class DiagnosticReport {
    private static final Set<String> STATES = Set.of(
        "WAITING", "INITIALIZING", "READY", "FAILED", "UNSUPPORTED", "CLOSED");
    private static final Set<String> FAILURE_CODES = Set.of(
        "NONE", "UNSUPPORTED_PLATFORM", "INITIALIZATION_FAILED", "FRAME_PREPARE_FAILED",
        "GUI_RENDER_FAILED", "COMPOSITE_FAILED", "RESIZE_FAILED", "RESOURCE_RELOAD_FAILED",
        "MH_INITIALIZE_FAILED", "MH_CREATE_FAILED", "MH_ENABLE_FAILED", "MH_REMOVE_FAILED",
        "NATIVE_LIBRARY_FAILED", "NATIVE_SYMBOL_MISSING");
    private static final String[] SETTINGS = {
        "configurationAvailable", "anonymizeNames", "sanitizeChatInput", "redactEnabled",
        "autoGrabServer", "redactPresetEnabled", "redactStrictPresetEnabled", "normalizeItemNames",
        "scoreboardEnabled", "hideAllScreens", "overlayHandledScreensEnabled",
        "hideHudWhenOverlayUnavailable", "showTestIcon", "debugLog"
    };
    private static final String[] HUD_KEYS = {
        "debug_menu", "chat", "chat_bar", "player_list", "subtitles", "scoreboards",
        "actionbar", "title_subtitle", "effects", "main_hud"
    };

    private DiagnosticReport() {}

    static String status(String value) {
        return value != null && STATES.contains(value) ? value : "UNKNOWN";
    }

    static String failureCode(String value) {
        return value != null && FAILURE_CODES.contains(value) ? value : "UNKNOWN";
    }

    static String version(String value) {
        return value != null && value.matches("[A-Za-z0-9][A-Za-z0-9._+\\-]{0,95}")
            ? value : "unknown";
    }

    /** Unknown keys and non-boolean setting values are discarded, even after future collector edits. */
    static String serialize(JsonObject source) {
        JsonObject report = new JsonObject();
        report.addProperty("schema", 1);
        JsonObject build = new JsonObject();
        JsonObject sourceBuild = object(source, "build");
        for (String key : new String[] { "version", "minecraft", "loader", "commit" }) {
            build.addProperty(key, version(string(sourceBuild, key)));
        }
        report.add("build", build);
        JsonObject runtime = new JsonObject();
        JsonObject sourceRuntime = object(source, "runtime");
        for (String key : new String[] { "minecraft", "streamshield", "loaderVersion", "architectury", "clothConfig" }) {
            runtime.addProperty(key, version(string(sourceRuntime, key)));
        }
        String loader = string(sourceRuntime, "loader");
        runtime.addProperty("loader", "fabric".equals(loader) || "neoforge".equals(loader) ? loader : "unknown");
        int javaFeature = 0;
        JsonElement java = sourceRuntime.get("javaFeature");
        if (java != null && java.isJsonPrimitive() && java.getAsJsonPrimitive().isNumber()) {
            try {
                int value = java.getAsBigDecimal().intValueExact();
                if (value >= 8 && value <= 255) javaFeature = value;
            } catch (ArithmeticException | NumberFormatException ignored) {}
        }
        runtime.addProperty("javaFeature", javaFeature);
        report.add("runtime", runtime);
        JsonObject overlay = new JsonObject();
        JsonObject sourceOverlay = object(source, "overlay");
        overlay.addProperty("status", status(string(sourceOverlay, "status")));
        overlay.addProperty("failureCode", failureCode(string(sourceOverlay, "failureCode")));
        overlay.addProperty("obsCaptureAutomaticallyVerified", false);
        report.add("overlay", overlay);
        report.add("settings", booleans(object(source, "settings"), SETTINGS));
        JsonObject components = new JsonObject();
        JsonObject sourceComponents = object(source, "components");
        for (String key : HUD_KEYS) {
            if (sourceComponents.has(key) && sourceComponents.get(key).isJsonObject()) {
                components.add(key, booleans(object(sourceComponents, key), new String[] {
                    "selected", "autoHideSupported", "autoHide", "autoHiddenWhileThisScreenIsOpen"
                }));
            }
        }
        report.add("components", components);
        return new GsonBuilder().setPrettyPrinting().create().toJson(report);
    }

    private static JsonObject object(JsonObject source, String key) {
        JsonElement value = source.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }

    private static String string(JsonObject source, String key) {
        JsonElement value = source.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
            ? value.getAsString() : null;
    }

    private static JsonObject booleans(JsonObject source, String[] keys) {
        JsonObject result = new JsonObject();
        for (String key : keys) {
            JsonElement value = source.get(key);
            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
                result.addProperty(key, value.getAsBoolean());
            }
        }
        return result;
    }
}

