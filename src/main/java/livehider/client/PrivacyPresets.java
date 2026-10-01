package livehider.client;

import com.google.gson.*;
import livehider.LiveHiderConfig;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;

/** Data-only presets: no executable rules, local skin paths, tokens or diagnostic opt-ins. */
public final class PrivacyPresets {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> FIELDS = Set.of("anonymizeNames", "anonymizeSelfName", "nameTemplate", "nameDigits",
        "selfNameMode", "selfCustomName", "sanitizeChatInput", "redactEnabled", "redactPatterns", "redactReplacement",
        "autoGrabServer", "redactPresetEnabled", "redactStrictPresetEnabled", "scoreboardEnabled", "scoreboardRules",
        "normalizeItemNames", "filterSignText", "hideSignText", "redactItemTooltips", "redactContainerTitles", "redactBooks",
        "overlayComponents", "autoHideComponents", "hideHudWhenOverlayUnavailable");
    private static JsonObject baseline;
    private static String activeServer;
    private static String failedServer;
    private static boolean failurePending;
    private static Object connection;
    private static final long MAX_BYTES = 1024 * 1024;
    private PrivacyPresets() {}
    private static Path directory() { return LiveHiderConfig.getPath().getParent().resolve("streamshield"); }
    public static String serverKey(String address) {
        try {
            String normalized = address.trim().toLowerCase(java.util.Locale.ROOT);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public static JsonObject settings(LiveHiderConfig config) {
        var all = GSON.toJsonTree(config).getAsJsonObject();
        var result = new JsonObject();
        for (String key : FIELDS) if (all.has(key)) result.add(key, all.get(key).deepCopy());
        return result;
    }
    public static void apply(JsonObject source) {
        // Validate all fields on a detached copy before changing the live instance.
        LiveHiderConfig candidate = candidate(LiveHiderConfig.get(), source);
        try {
            for (String key : FIELDS) {
                var field = LiveHiderConfig.class.getField(key);
                field.set(LiveHiderConfig.get(), field.get(candidate));
            }
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        LiveHiderConfig.refreshPrivacyState();
    }
    static LiveHiderConfig candidate(LiveHiderConfig current, JsonObject source) {
        if (source == null) throw new IllegalArgumentException("Missing settings");
        JsonObject merged = GSON.toJsonTree(current).getAsJsonObject();
        for (String key : FIELDS) if (source.has(key)) merged.add(key, source.get(key));
        LiveHiderConfig result = GSON.fromJson(merged, LiveHiderConfig.class);
        result.normalize();
        return result;
    }
    static JsonObject read(Path file) throws java.io.IOException {
        if (Files.size(file) > MAX_BYTES) throw new java.io.IOException("Preset too large");
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("schema") || root.get("schema").getAsInt() != 1) throw new java.io.IOException("Unsupported preset schema");
            return root.getAsJsonObject("settings");
        }
    }
    static void write(Path file, JsonObject settings, boolean replace) throws java.io.IOException {
        Files.createDirectories(file.getParent());
        var root = new JsonObject();
        root.addProperty("schema", 1);
        root.add("settings", settings);
        Path temp = Files.createTempFile(file.getParent(), "preset-", ".tmp");
        try {
            Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
            if (replace) {
                try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException e) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
            } else Files.move(temp, file);
        } finally { Files.deleteIfExists(temp); }
    }
    public static Path exportPreset() throws java.io.IOException {
        Path path = directory().resolve("exports").resolve("privacy-" + java.util.UUID.randomUUID() + ".json");
        write(path, settings(LiveHiderConfig.get()), false);
        return path;
    }
    public static void importPreset() throws java.io.IOException {
        // Fixed filename: imported data never selects filesystem paths.
        JsonObject incoming = read(directory().resolve("import.json"));
        write(directory().resolve("backups").resolve("before-import-" + java.util.UUID.randomUUID() + ".json"), settings(LiveHiderConfig.get()), false);
        apply(incoming);
        LiveHiderConfig.save();
    }
    public static JsonObject globalSnapshot() {
        var result = GSON.toJsonTree(LiveHiderConfig.get()).getAsJsonObject();
        if (baseline != null) for (String key : FIELDS) if (baseline.has(key)) result.add(key, baseline.get(key));
        return result;
    }
    public static void saveActive() throws java.io.IOException {
        if (activeServer != null) write(directory().resolve("profiles").resolve(activeServer + ".json"), settings(LiveHiderConfig.get()), true);
    }
    public static void tick() {
        tick(false);
    }
    public static void tickBeforeResume() { tick(true); }
    private static void tick(boolean finishing) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var config = LiveHiderConfig.get();
        if (config == null) return;
        if (connection != mc.getConnection()) {
            connection = mc.getConnection();
            livehider.text.ScoreboardSamples.clear();
            failedServer = null;
            failurePending = false;
        }
        String wanted = config.serverProfiles && mc.level != null && mc.getCurrentServer() != null
            ? serverKey(mc.getCurrentServer().ip) : null;
        if (wanted == null && activeServer == null) { failurePending = false; failedServer = null; }
        if (java.util.Objects.equals(wanted, activeServer)) return;
        if (failurePending && java.util.Objects.equals(wanted, failedServer)) return;
        // Never replace unsaved fields while any protected configuration dialog is open.
        if (!finishing && mc.screen != null && (PrivacyShield.active() || LiveHiderConfigScreen.isActiveConfigScreen(mc.screen))) return;
        try {
            saveActive();
            if (baseline != null) apply(baseline);
            baseline = null;
            activeServer = null;
            livehider.text.ScoreboardSamples.clear();
            if (wanted != null && !wanted.equals(failedServer)) {
                JsonObject original = settings(config);
                Path file = directory().resolve("profiles").resolve(wanted + ".json");
                if (Files.exists(file)) apply(read(file));
                baseline = original;
                activeServer = wanted;
                failedServer = null;
            }
        } catch (Exception e) {
            failedServer = wanted;
            failurePending = true;
            PrivacyShield.arm();
            livehider.LiveHider.LOGGER.warn("StreamShield profile switch failed; no server address logged.");
        }
    }
    public static boolean isServerActive() { return activeServer != null; }
    public static boolean hasFailure() { return failurePending; }
    public static void retrySwitch() { failurePending = false; failedServer = null; }
}
