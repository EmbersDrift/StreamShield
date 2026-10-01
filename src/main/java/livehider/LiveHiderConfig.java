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
    // Optional local hiding while the capture-excluded overlay is unavailable.
    public boolean hideHudWhenOverlayUnavailable = false;

    // Stream-safe client display: renamed items show their localized vanilla name instead of the custom name.
    public boolean normalizeItemNames = true;
    public boolean filterSignText = true;
    public boolean hideSignText = false;

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
    // Broad profanity/sensitive-content list. Off by default because it intentionally trades
    // normal chat readability for stricter filtering.
    public boolean redactStrictPresetEnabled = false;

    // Scoreboard chain rules (ordered): replaced in list order, result feeds the next rule.
    public boolean scoreboardEnabled = true;
    public java.util.ArrayList<ScoreboardRule> scoreboardRules = new java.util.ArrayList<>();

    // Legacy (pre-chain) mapping; kept only to read old configs and migrate on load. Not serialized.
    public transient java.util.HashMap<String, String> scoreboardReplacements = new java.util.HashMap<>();

    // ---- M2c: skin obfuscation ----
    // Legacy global mode, retained to migrate existing config files.
    public String skinMode = "OFF"; // OFF | STEVE | CUSTOM (RANDOM remains a legacy alias)
    /** Per-target modes: ORIGINAL keeps the skin; STEVE, CUSTOM and RANDOM replace it. */
    public String skinOtherMode = null;
    public String skinSource = "MOJANG"; // MOJANG | LITTLESKIN | ELYBY | URL_TEMPLATE | LOCAL_FOLDER
    public String skinSelfMode = "KEEP"; // ORIGINAL | STEVE | CUSTOM | RANDOM; KEEP is legacy
    /** One fixed account ID (or a PNG filename for LOCAL_FOLDER) used by CUSTOM. */
    public String skinCustomSkin = "";
    /** HTTPS PNG endpoint for URL_TEMPLATE. {name} is replaced with a pool ID. */
    public String skinUrlTemplate = "";
    /** Folder containing 64x64 PNG skins for LOCAL_FOLDER. Only direct child PNG files are used. */
    public String skinLocalFolder = "";
    // Kept only so older manually edited configs remain readable. Tokens are intentionally unused:
    // this client-side display feature must not persist account credentials in its JSON config.
    @Deprecated public String skinApiBase = "";
    @Deprecated public String skinApiToken = "";
    public java.util.ArrayList<String> skinRandomPool = new java.util.ArrayList<>();


    // Diagnostic events only; never log original or replacement text.
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
        if (INSTANCE == null) {
            LiveHider.LOGGER.warn("Config file contained null; using defaults");
            INSTANCE = new LiveHiderConfig();
        }
        INSTANCE.normalize();
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
        fresh.normalize();
        INSTANCE = fresh;
        INSTANCE.updateCache();
        livehider.skin.RandomSkinManager.invalidateConfigCache();
        Redactor.setup();
        livehider.text.NameAnonymizer.invalidateDisplayNameCache();
        livehider.text.NameAnonymizer.refreshFromConnection();
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
            // Do not let the polling watcher replace the live config with an identical copy while
            // a Cloth Config screen still holds this instance for follow-up rule actions.
            markConfigWatcherCurrentFileState();
            livehider.skin.RandomSkinManager.invalidateConfigCache();
            Redactor.setup();
            livehider.text.NameAnonymizer.invalidateDisplayNameCache();
            livehider.text.NameAnonymizer.refreshFromConnection();
        } catch (IOException e) {
            LiveHider.LOGGER.error("Failed to save config", e);
        }
    }

    private static void markConfigWatcherCurrentFileState() {
        for (String watcherName : new String[] {
            "livehider.client.LiveHiderConfigWatcher",
            "livehider.neoforge.NeoForgeConfigWatcher"
        }) {
            try {
                Class<?> watcher = Class.forName(watcherName);
                java.lang.reflect.Method method = watcher.getDeclaredMethod("markCurrentFileState");
                method.setAccessible(true);
                method.invoke(null);
                return;
            } catch (ReflectiveOperationException ignored) {
                // Try the watcher supplied by the other loader, if present.
            }
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

    /** Keep manually edited or older JSON configurations safe to consume after loading. */
    private void normalize() {
        if (overlayComponents == null) overlayComponents = new HashMap<>();
        if (autoHideComponents == null) autoHideComponents = new HashMap<>();
        if (overlayScreensList == null) overlayScreensList = new HashMap<>();
        if (overlayHandledScreensList == null) overlayHandledScreensList = new HashSet<>();
        if (overlayScreensClasses == null) overlayScreensClasses = new HashSet<>();
        if (nameTemplate == null) nameTemplate = "[Player]#";
        if (selfNameMode == null) selfNameMode = "HIDE";
        if (selfCustomName == null) selfCustomName = "";
        if (redactPatterns == null) redactPatterns = new java.util.ArrayList<>();
        if (redactReplacement == null) redactReplacement = "***";
        if (scoreboardRules == null) scoreboardRules = new java.util.ArrayList<>();
        if (scoreboardReplacements == null) scoreboardReplacements = new java.util.HashMap<>();
        if (skinMode == null) skinMode = "OFF";
        String migratedLegacySkinMode = "STEVE".equalsIgnoreCase(skinMode) ? "STEVE"
            : ("CUSTOM".equalsIgnoreCase(skinMode) || "RANDOM".equalsIgnoreCase(skinMode)) ? "RANDOM" : "ORIGINAL";
        if (skinOtherMode == null) skinOtherMode = migratedLegacySkinMode;
        if (skinSource == null) skinSource = "MOJANG";
        if (skinSelfMode == null) skinSelfMode = "KEEP";
        if ("KEEP".equalsIgnoreCase(skinSelfMode)) {
            skinSelfMode = "STEVE".equalsIgnoreCase(migratedLegacySkinMode) ? "STEVE" : "ORIGINAL";
        }
        if ("OFF".equalsIgnoreCase(skinOtherMode)) skinOtherMode = "ORIGINAL";
        if ("OFF".equalsIgnoreCase(skinSelfMode)) skinSelfMode = "ORIGINAL";
        if (skinCustomSkin == null) skinCustomSkin = "";
        if (skinApiBase == null) skinApiBase = "";
        if (skinApiToken == null) skinApiToken = "";
        if (skinUrlTemplate == null) skinUrlTemplate = "";
        if (skinLocalFolder == null) skinLocalFolder = "";
        if (skinRandomPool == null) skinRandomPool = new java.util.ArrayList<>();
        overlayComponents.entrySet().removeIf(entry -> entry.getValue() == null);
        autoHideComponents.entrySet().removeIf(entry -> entry.getValue() == null);
        overlayScreensList.entrySet().removeIf(entry -> entry.getValue() == null);
        scoreboardRules.removeIf(rule -> rule == null);
        for (ScoreboardRule rule : scoreboardRules) {
            if (rule.key == null) rule.key = "";
            if (rule.replacement == null) rule.replacement = "";
        }
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
