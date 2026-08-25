package livehider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.architectury.platform.Platform;
import livehider.component.OverlayComponentRegistry;
import livehider.text.Redactor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import livehider.component.IOverlayComponent;

/**
 * Minimal config for M1 (Cloth Config UI screen added later). Ported from
 * obs-overlay (MIT, author zziger), mod id re-spaced to <code>live_hider</code>.
 */
public final class LiveHiderConfig {
    private static LiveHiderConfig INSTANCE;

    public HashMap<String, Boolean> overlayComponents = new HashMap<>();
    public HashMap<String, Boolean> autoHideComponents = new HashMap<>();
    public HashMap<String, Boolean> overlayScreensList = new HashMap<>();
    public boolean hideAllScreens = false;
    public boolean overlayHandledScreensEnabled = false;
    public HashSet<String> overlayHandledScreensList = new HashSet<>();
    public transient HashSet<Class<?>> overlayScreensClasses = new HashSet<>();
    public boolean showTestIcon;

    // Stream-safe item name normalization: renamed items show their bound registry name instead of the custom NBT name.
    public boolean normalizeItemNames = true;

    // ---- M2b-1: name anonymization ----
    public boolean anonymizeNames = true;
    public String nameTemplate = "[Player]#";
    public int nameDigits = 4;
    public String selfNameMode = "HIDE"; // HIDE | CUSTOM | RANDOM
    public String selfCustomName = "";
    public boolean sanitizeChatInput = true;

    // ---- M2b-2: content redaction engine ----
    public boolean redactEnabled = true;
    public java.util.ArrayList<String> redactPatterns = new java.util.ArrayList<>();
    public String redactReplacement = "***";
    public boolean autoGrabServer = true;
    public boolean redactPresetEnabled = true;

    // Scoreboard chain rules (ordered): replaced in list order, result feeds the next rule.
    public boolean scoreboardEnabled = true;
    public java.util.ArrayList<ScoreboardRule> scoreboardRules = new java.util.ArrayList<>();

    // Legacy (pre-chain) mapping; kept only to read old configs and migrate on load. Not serialized.
    public transient java.util.HashMap<String, String> scoreboardReplacements = new java.util.HashMap<>();

    // ---- M2c: skin obfuscation ----
    // OFF: keep real skins. STEVE: everyone renders with the default Steve skin (kills anything hidden
    // in a skin texture). RANDOM: each player gets one random skin from a skin-site API (cached per UUID),
    // with an optional player-name pool as fallback source. API/pool configured in the GUI later.
    public String skinMode = "OFF"; // OFF | STEVE | RANDOM
    public String skinApiBase = "";      // e.g. https://littleskin.cn
    public String skinApiToken = "";     // for authenticated texture retrieval if the API needs it
    public java.util.ArrayList<String> skinRandomPool = new java.util.ArrayList<>(); // player names to draw random skins from


    // Debug logging for matching (see logs for raw/stripped scoreboard text and match result).
    public boolean debugLog = false;

    public static void init() {
        Path configPath = getPath();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (Files.exists(configPath)) {
            try (BufferedReader reader = Files.newBufferedReader(configPath)) {
                INSTANCE = gson.fromJson(reader, LiveHiderConfig.class);
            } catch (JsonParseException | IOException e) {
                LiveHider.LOGGER.error("Failed to load config", e);
                INSTANCE = new LiveHiderConfig();
            }
        } else {
            INSTANCE = new LiveHiderConfig();
        }
        migrateLegacyScoreboardMap(configPath, gson);
        INSTANCE.updateCache();
    }

    /** One-time migration: convert the legacy {@code scoreboardReplacements} map into chain rules. */
    private static void migrateLegacyScoreboardMap(Path configPath, Gson gson) {
        if (!Files.exists(configPath) || INSTANCE == null || !INSTANCE.scoreboardRules.isEmpty()) {
            return;
        }
        try {
            String raw = Files.readString(configPath);
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
            if (obj.has("scoreboardReplacements")) {
                com.google.gson.JsonObject map = obj.getAsJsonObject("scoreboardReplacements");
                for (String key : map.keySet()) {
                    String value = map.get(key).getAsString();
                    INSTANCE.scoreboardRules.add(new ScoreboardRule(true, key, value));
                }
                save(); // rewrite config without the legacy field
                LiveHider.LOGGER.info("[LiveHider] Migrated {} scoreboard mapping(s) to chain rules", INSTANCE.scoreboardRules.size());
            }
        } catch (Exception e) {
            LiveHider.LOGGER.debug("[LiveHider] Legacy scoreboard migration skipped: {}", e.toString());
        }
    }

    private void updateCache() {
        this.overlayScreensClasses.clear();
        this.overlayScreensList.forEach((screenId, state) -> {
            if (state) {
                Class<?> clazz = OverlayComponentRegistry.hideableScreens.get(screenId);
                if (clazz != null) {
                    this.overlayScreensClasses.add(clazz);
                }
            }
        });
    }

    /**
     * Re-parse the config file from disk and swap the live singleton, so manual JSON edits take
     * effect without a restart. Refreshes the derived screen-class cache and the redactor pattern.
     * On any parse/IO failure the previous config is kept. Returns true if a reload happened.
     */
    public static boolean reload() {
        Path configPath = getPath();
        if (!Files.exists(configPath)) {
            return false;
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        LiveHiderConfig fresh;
        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            fresh = gson.fromJson(reader, LiveHiderConfig.class);
        } catch (JsonParseException | IOException e) {
            LiveHider.LOGGER.error("Failed to reload config (keeping current)", e);
            return false;
        }
        if (fresh == null) {
            LiveHider.LOGGER.warn("Config reload produced null (keeping current)");
            return false;
        }
        INSTANCE = fresh;
        INSTANCE.updateCache();
        Redactor.setup();
        return true;
    }

    public static LiveHiderConfig get() {
        return INSTANCE;
    }

    public static Path getPath() {
        return Platform.getConfigFolder().resolve("live_hider.json");
    }

    public static void save() {
        try {
            Path configPath = getPath();
            Files.createDirectories(configPath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(get(), writer);
            }
        } catch (IOException e) {
            LiveHider.LOGGER.error("Failed to save config", e);
        }
    }

    /** On first run, write a config file that lists every component so it can be toggled by hand. */
    public static void ensureFile() {
        if (Files.exists(getPath())) {
            return;
        }
        for (IOverlayComponent component : OverlayComponentRegistry.components) {
            get().overlayComponents.put(component.getId(), component.isOverlayEnabled());
            if (component.canAutoHide()) {
                get().autoHideComponents.put(component.getId(), component.isAutoHideEnabled());
            }
        }
        save();
    }

    public static boolean isScreenOverlayed(Screen screen) {
        LiveHiderConfig config = get();
        if (config.hideAllScreens && Minecraft.getInstance().level != null) {
            return true;
        }
        if (config.overlayScreensClasses.contains(screen.getClass())) {
            return true;
        }
        if (config.overlayHandledScreensEnabled && screen instanceof AbstractContainerScreen<?> handledScreen) {
            try {
                Identifier id = BuiltInRegistries.MENU.getKey(handledScreen.getMenu().getType());
                if (id != null && (config.overlayHandledScreensList.contains(id.toString()) || config.overlayHandledScreensList.contains(id.getPath()))) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
