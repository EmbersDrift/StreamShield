package livehider.text;

import dev.architectury.platform.Platform;
import livehider.LiveHider;
import livehider.LiveHiderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Content redaction engine: replaces configured literal keywords with a safe placeholder
 * (e.g. {@code ***}). Words come from (1) the bundled safetext preset list, (2) a word file
 * the user drops in {@code config/live_hider_words.txt}, (3) {@code redactPatterns} in the
 * config, and (4) the current server IP/name (auto-grab). Used by {@link SafeText}.
 */
public final class Redactor {
    private static final Set<String> staticWords = new HashSet<>();
    private static final Set<String> runtimeWords = new HashSet<>();
    private static Pattern combinedPattern;
    private static ScoreboardRuleChain scoreboardRules = new ScoreboardRuleChain(List.of());
    private static String replacement = "***";
    private static boolean patternDirty = true;

    private Redactor() {
    }

    /** Load one bundled word list into the static word set. */
    private static void loadPreset(String resourcePath) {
        try (InputStream in = Redactor.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                LiveHider.LOGGER.warn("Bundled redaction preset not found: {}", resourcePath);
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String word = line.trim();
                    if (!word.isEmpty() && !word.startsWith("#")) {
                        staticWords.add(word);
                    }
                }
            }
        } catch (IOException e) {
            LiveHider.LOGGER.error("Failed to load redact preset", e);
        }
    }

    /** Load the bundled preset (if enabled) and a word file from config/ into the static word set. */
    public static void setup() {
        staticWords.clear();
        LiveHiderConfig config = LiveHiderConfig.get();
        if (config != null && config.redactPresetEnabled) {
            loadPreset("/assets/live_hider/redact_preset.txt");
        }
        if (config != null && config.redactStrictPresetEnabled) {
            loadPreset("/assets/live_hider/redact_strict_preset.txt");
        }
        Path wordsFile = Platform.getConfigFolder().resolve("live_hider_words.txt");
        if (Files.exists(wordsFile)) {
            try (BufferedReader reader = Files.newBufferedReader(wordsFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String word = line.trim();
                    if (!word.isEmpty() && !word.startsWith("#")) {
                        staticWords.add(word);
                    }
                }
            } catch (IOException e) {
                LiveHider.LOGGER.error("Failed to load word list", e);
            }
        }
        compileScoreboardRules(config);
        patternDirty = true;
    }

    /** Compile literal scoreboard rules when configuration changes, never while rendering HUD text. */
    private static void compileScoreboardRules(LiveHiderConfig config) {
        scoreboardRules = new ScoreboardRuleChain(config == null ? null : config.scoreboardRules);
    }

    public static void addRuntimeWord(String word) {
        if (word != null && !word.isEmpty()) {
            runtimeWords.add(word);
            patternDirty = true;
        }
    }

    public static void clearRuntimeWords() {
        runtimeWords.clear();
        patternDirty = true;
    }

    /** On join, capture the current server's IP and name into the runtime word list. */
    public static void captureServer() {
        LiveHiderConfig config = LiveHiderConfig.get();
        if (config != null && !config.autoGrabServer) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null) {
            ServerData sd = mc.getCurrentServer();
            addRuntimeWord(sd.ip);
            addRuntimeWord(sd.name);
        }
    }

    public static boolean isActive() {
        LiveHiderConfig config = LiveHiderConfig.get();
        return config != null && config.redactEnabled;
    }

    /** Build (and cache) a single case-insensitive literal alternation over all marked text. */
    private static Pattern combinedPattern() {
        if (combinedPattern == null || patternDirty) {
            LiveHiderConfig config = LiveHiderConfig.get();
            Set<String> all = new HashSet<>();
            if (config != null && config.redactPatterns != null) {
                all.addAll(config.redactPatterns);
            }
            all.addAll(staticWords);
            all.addAll(runtimeWords);
            combinedPattern = compileMarkedWords(all);
            replacement = config != null && config.redactReplacement != null ? config.redactReplacement : "***";
            patternDirty = false;
        }
        return combinedPattern;
    }

    static Pattern compileMarkedWords(Set<String> all) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        // Prefer full addresses/phrases over shorter overlapping entries.
        List<String> ordered = all.stream().filter(java.util.Objects::nonNull)
            .sorted(java.util.Comparator.comparingInt(String::length).reversed()
                .thenComparing(java.util.Comparator.naturalOrder())).toList();
        for (String word : ordered) {
            if (word.isEmpty()) {
                continue;
            }
            if (!first) {
                sb.append('|');
            }
            sb.append(Pattern.quote(word));
            first = false;
        }
        return first ? null : Pattern.compile("(?:" + sb + ")",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /** Replace every configured/ preset/ file/ runtime word in a plain string with the placeholder. */
    public static String applyToText(String text) {
        if (text == null || !isActive()) {
            return text;
        }
        Pattern pattern = combinedPattern();
        if (pattern == null) {
            return text;
        }
        String result = pattern.matcher(text).replaceAll(Matcher.quoteReplacement(replacement));
        LiveHiderConfig config = LiveHiderConfig.get();
        if (config != null && config.debugLog && !result.equals(text)) {
            LiveHider.LOGGER.info("[LiveHider][redact] Rewrote rendered text (content omitted)");
        }
        return result;
    }

    /** Match the visible row across styled runs, retaining untouched glyph fonts and colors. */
    public static net.minecraft.network.chat.Component applyScoreboardToComponent(net.minecraft.network.chat.Component original) {
        LiveHiderConfig config = LiveHiderConfig.get();
        if (original == null || config == null) return original;
        // Rules must leave original player-name spans intact. Apply names last so a
        // custom rule cannot consume a real name or rewrite a generated alias.
        Pattern protectedNames = NameAnonymizer.scoreboardProtectedNames();
        var result = config.scoreboardEnabled ? scoreboardRules.applyComponent(original, protectedNames) : original;
        if (config.redactEnabled) {
            Pattern pattern = combinedPattern();
            if (pattern != null) {
                result = new StyledScoreboardText(result).replace(pattern, replacement, null, protectedNames);
            }
        }
        result = NameAnonymizer.applyScoreboardToComponent(result);
        if (config.debugLog && result != original) {
            LiveHider.LOGGER.info("[LiveHider][scoreboard] Rewrote rendered text (content omitted)");
        }
        return result;
    }

    /** Plain-string compatibility path for assembled scoreboard text. */
    public static String applyScoreboardToText(String text) {
        if (text == null) return null;
        var source = net.minecraft.network.chat.Component.literal(text);
        var result = applyScoreboardToComponent(source);
        return result == source ? text : result.getString();
    }

    /** Plain-string compatibility alias for {@link #applyScoreboardToText(String)}. */
    public static String rewriteScoreboardRow(String text) {
        return applyScoreboardToText(text);
    }
}
