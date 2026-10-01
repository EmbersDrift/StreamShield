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

    /** Build (and cache) a single case-insensitive, word-boundary alternation over all words. */
    private static Pattern combinedPattern() {
        if (combinedPattern == null || patternDirty) {
            LiveHiderConfig config = LiveHiderConfig.get();
            Set<String> all = new HashSet<>();
            if (config != null && config.redactPatterns != null) {
                all.addAll(config.redactPatterns);
            }
            all.addAll(staticWords);
            all.addAll(runtimeWords);
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            // Prefer full addresses/phrases over shorter overlapping entries.
            List<String> ordered = all.stream().filter(java.util.Objects::nonNull)
                .sorted(java.util.Comparator.comparingInt(String::length).reversed()
                    .thenComparing(java.util.Comparator.naturalOrder())).toList();
            for (String word : ordered) {
                if (word == null || word.isEmpty()) {
                    continue;
                }
                if (!first) {
                    sb.append('|');
                }
                sb.append("(?<!\\w)").append(Pattern.quote(word)).append("(?!\\w)");
                first = false;
            }
            combinedPattern = first ? null : Pattern.compile("(?i)(?:" + sb + ")");
            replacement = config != null && config.redactReplacement != null ? config.redactReplacement : "***";
            patternDirty = false;
        }
        return combinedPattern;
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
        var result = config.scoreboardEnabled ? scoreboardRules.applyComponent(original) : original;
        if (NameAnonymizer.isActive()) {
            var styled = new StyledScoreboardText(result);
            String plain = styled.text();
            String anonymous = NameAnonymizer.applyExact(plain);
            if (!anonymous.equals(plain)) {
                result = styled.replace(Pattern.compile("\\A" + Pattern.quote(plain) + "\\z"), anonymous);
            }
        }
        if (config.debugLog && result != original) {
            LiveHider.LOGGER.info("[LiveHider][scoreboard] Rewrote rendered text (content omitted)");
        }
        return result;
    }

    private static String applyScoreboardRules(String text, LiveHiderConfig config) {
        if (config == null || config.scoreboardRules == null || !config.scoreboardEnabled) {
            return text;
        }
        return scoreboardRules.apply(text);
    }

    /**
     * Scoreboard-specific rewrite, deliberately narrow (the huge safetext preset and the
     * auto-grabbed server name/IP are NOT applied here, so a sidebar title/line is never
     * mangled just because it matches a server name). Only two things are done:
     * (1) the configured keyword-&gt;replacement-word mapping ({@code scoreboardReplacements});
     * (2) player-name anonymization, but only on an exact whole-string match.
     * Matching/result are computed on a copy with {@code §} format codes stripped; the original
     * (formatting intact) is returned when nothing matched.
     */
    public static String applyScoreboardToText(String text) {
        if (text == null) {
            return text;
        }
        String stripped = LegacyFormatCodes.strip(text);
        String result = stripped;
        LiveHiderConfig config = LiveHiderConfig.get();
        if (config != null) {
            // 1. Static-content chain mapping, so a mapped keyword gets its own replacement word.
            result = applyScoreboardRules(result, config);
            // 2. Player-name anonymization, exact whole-line only.
            result = NameAnonymizer.applyExact(result);
        }
        if (config != null && config.debugLog && !result.equals(stripped)) {
            LiveHider.LOGGER.info("[LiveHider][scoreboard] Rewrote rendered text (content omitted)");
        }
        // Only rewrite if something actually matched; otherwise keep the original (formatting intact).
        return result.equals(stripped) ? text : result;
    }

    /**
     * Rewrite one assembled sidebar row line (the raw string that already carries {@code §} colour
     * codes and possibly PUA icon codepoints). Matching is done on a format-stripped copy so colour
     * codes don't break word boundaries, but the replacement is applied to the original string so
     * {@code §} codes and icon codepoints that were not matched are preserved. Applies, in order:
     * {@code scoreboardReplacements} word mapping, then exact whole-line player-name anonymization.
     */
    public static String rewriteScoreboardRow(String text) {
        if (text == null) {
            return text;
        }
        String result = text;
        LiveHiderConfig config = LiveHiderConfig.get();
        if (config != null) {
             // 1. Static-content chain mapping, on the raw string (keeps §/icon codepoints intact).
            result = applyScoreboardRules(result, config);
            // 2. Exact whole-line player-name anonymization (tolerant of colour codes).
            result = NameAnonymizer.applyExactStrip(result);
        }
        if (config != null && config.debugLog && !result.equals(text)) {
            LiveHider.LOGGER.info("[LiveHider][scoreboard-row] Rewrote rendered text (content omitted)");
        }
        return result;
    }
}
