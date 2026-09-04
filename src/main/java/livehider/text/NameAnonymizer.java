package livehider.text;

import livehider.LiveHider;
import livehider.LiveHiderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps real player names to anonymized {@code [Player]#nnn} names for a single session.
 * Names are stable within a session (keyed by UUID + session salt) and change each time the
 * world/server is joined. Used by {@link SafeText} to rewrite rendered text (stream safety).
 */
public final class NameAnonymizer {
    private static final Map<String, String> map = new HashMap<>();
    private static final Map<String, Pattern> patternCache = new HashMap<>();
    // Name tags and the TAB overlay request the same display component every render frame. Keep
    // their rewritten form per player instead of running the full text/regex pipeline every time.
    private static final Map<UUID, DisplayNameCacheEntry> displayNameCache = new HashMap<>();
    private static List<String> sortedNames = new ArrayList<>();
    private static String ownName;
    private static long salt;
    private static boolean active;
    private static boolean revealInput;

    private NameAnonymizer() {
    }

    public static void resetSession() {
        salt = new Random().nextLong();
        map.clear();
        patternCache.clear();
        displayNameCache.clear();
        sortedNames = new ArrayList<>();
        ownName = null;
        active = true;
    }

    /** Drop render-path display-name results after a configuration reload/save. */
    public static void invalidateDisplayNameCache() {
        displayNameCache.clear();
    }

    /** Register a player's real name so it can be anonymized everywhere it is rendered. */
    public static void register(String realName, UUID id) {
        LiveHiderConfig config = LiveHiderConfig.get();
        if (!active || realName == null || config == null) {
            return;
        }
        // Strip Minecraft § format codes so the key is the readable name; skip pure-format strings.
        String clean = stripFormatCodes(realName);
        if (clean.isEmpty()) {
            return;
        }
        // Self-name mode has priority over global player anonymization on every text surface.
        // Keep its replacement in the same map used by chat, scoreboards and input fields, while
        // name tags/TAB additionally use applySelfMode for their null/display-component behavior.
        if (isOwnRealName(clean)) {
            ownName = clean;
            String replacement = selfNameReplacement(config, clean, id);
            String previous = replacement == null ? map.remove(clean) : map.put(clean, replacement);
            patternCache.remove(clean);
            if (!Objects.equals(previous, replacement)) {
                displayNameCache.clear();
                rebuildSortedNames();
            }
            return;
        }
        if (map.containsKey(clean)) {
            return;
        }
        map.put(clean, anonymousReplacement(config, clean, id));
        patternCache.remove(clean);
        // A newly known player can appear inside another player's decorated nickname.
        displayNameCache.clear();
        // Re-sort the cached name list (build after current size, amortised — cheap relative to old per-call sort).
        rebuildSortedNames();
    }

    /** True when {@code name} is the local player's own real (profile) name. */
    private static boolean isOwnRealName(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.player.getGameProfile() == null) {
            return false;
        }
        String own = mc.player.getGameProfile().name();
        return own != null && own.equalsIgnoreCase(name);
    }

    /** Null means keep the real name; an empty string means hide it in text-only surfaces. */
    private static String selfNameReplacement(LiveHiderConfig config, String realName, UUID id) {
        if ("OWN".equalsIgnoreCase(config.selfNameMode)) {
            return null;
        }
        if ("HIDE".equalsIgnoreCase(config.selfNameMode)) {
            return "";
        }
        if ("CUSTOM".equalsIgnoreCase(config.selfNameMode)) {
            return config.selfCustomName != null ? config.selfCustomName : "";
        }
        return anonymousReplacement(config, realName, id);
    }

    private static String anonymousReplacement(LiveHiderConfig config, String name, UUID id) {
        int digits = Math.min(9, Math.max(1, config.nameDigits));
        long h = (id != null ? id.getLeastSignificantBits() : name.hashCode()) ^ salt;
        int modulo = (int) Math.pow(10, digits);
        long n = Math.floorMod(h, (long) modulo);
        String num = String.format("%0" + digits + "d", n);
        return config.nameTemplate + num;
    }

    private static void rebuildSortedNames() {
        sortedNames = new ArrayList<>(map.keySet());
        sortedNames.sort((a, b) -> b.length() - a.length());
    }

    public static boolean isActive() {
        LiveHiderConfig config = LiveHiderConfig.get();
        return active
            && config != null
            && !map.isEmpty()
            && (config.anonymizeNames || hasSelfReplacement());
    }

    public static void setRevealInput(boolean value) {
        revealInput = value;
    }

    public static boolean isRevealInput() {
        return revealInput;
    }

    /** Replace every known player name in a plain string (longest names first, word boundaries). */
    public static String applyToText(String text) {
        LiveHiderConfig config = LiveHiderConfig.get();
        return applyNames(text, config != null && config.anonymizeNames);
    }

    /**
     * Sanitizes a chat input field independently of the global name-anonymization display mode.
     * This deliberately affects known player names only; generic redaction is not applied to text
     * that the player is about to send.
     */
    public static String applyToChatInput(String text) {
        return applyNames(text, true);
    }

    private static boolean hasSelfReplacement() {
        return ownName != null && map.containsKey(ownName);
    }

    /** @param includeAllNames false restricts rewriting to the local player's configured replacement. */
    private static String applyNames(String text, boolean includeAllNames) {
        if (text == null || !active || map.isEmpty() || (!includeAllNames && !hasSelfReplacement())) {
            return text;
        }
        List<String> names = sortedNames;
        String result = text;
        for (String name : names) {
            if (!includeAllNames && !name.equals(ownName)) {
                continue;
            }
            String anon = map.get(name);
            Pattern p = patternCache.computeIfAbsent(name, n -> Pattern.compile("(?<!\\w)" + Pattern.quote(n) + "(?!\\w)"));
            result = p.matcher(result).replaceAll(Matcher.quoteReplacement(anon));
        }
        LiveHiderConfig config = LiveHiderConfig.get();
        if (config != null && config.debugLog && !result.equals(text)) {
            LiveHider.LOGGER.info("[LiveHider][name] \"{}\" -> \"{}\"", text, result);
        }
        return result;
    }

    /**
     * Replace a string only when it <em>exactly</em> equals a known player name (whole line).
     * Used by the scoreboard path so a sidebar entry is only anonymized when it is precisely a
     * player name, never when a name merely appears as a substring.
     */
    public static String applyExact(String text) {
        if (text == null || !isActive()) {
            return text;
        }
        LiveHiderConfig config = LiveHiderConfig.get();
        // A self-name replacement keeps the anonymizer active even when global anonymization is
        // disabled. In that mode, exact scoreboard rows may rewrite the local player only.
        if (config == null || (!config.anonymizeNames && !text.equals(ownName))) {
            return text;
        }
        String anon = map.get(text);
        return anon != null ? anon : text;
    }

    /**
     * Like {@link #applyExact(String)} but tolerant of Minecraft {@code §} format codes in the input:
     * we compare the format-stripped text against known names, but if it exactly matches we return
     * the anonymized name (dropping the colour codes). Used for whole-sidebar-row lines that carry
     * leading colour/style codes around the player name.
     */
    public static String applyExactStrip(String text) {
        if (text == null || !isActive()) {
            return text;
        }
        String striped = stripFormatCodes(text);
        LiveHiderConfig config = LiveHiderConfig.get();
        if (config == null || (!config.anonymizeNames && !striped.equals(ownName))) {
            return text;
        }
        String anon = map.get(striped);
        return anon != null ? anon : text;
    }

    /** (Re)populate the name map from the connection's online players and the world's player entities. */
    public static void refreshFromConnection() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        if (mc.player.connection != null) {
            Collection<PlayerInfo> players = mc.player.connection.getOnlinePlayers();
            for (PlayerInfo info : players) {
                register(info.getProfile().name(), info.getProfile().id());
            }
        }
        if (mc.level != null) {
            for (net.minecraft.world.entity.player.Player entity : mc.level.players()) {
                register(entity.getName().getString(), entity.getUUID());
            }
        }
    }

    /**
     * Apply the self-name mode (HIDE / CUSTOM / RANDOM) to a nametag, or anonymize other
     * players' nametags. Returns null to hide the nametag entirely.
     */
    public static Component applySelfMode(Component name, boolean isSelf) {
        if (!isSelf) {
            return SafeText.rewrite(name);
        }
        LiveHiderConfig config = LiveHiderConfig.get();
        if (config == null) {
            return name;
        }
        if ("HIDE".equalsIgnoreCase(config.selfNameMode)) {
            return null;
        }
        if ("CUSTOM".equalsIgnoreCase(config.selfNameMode)) {
            // Supports both § codes and & aliases while keeping ordinary Unicode/special
            // characters literal. Parsing happens once per display-name cache entry.
            return SafeText.parseLegacyFormatting(config.selfCustomName == null ? "" : config.selfCustomName);
        }
        if ("OWN".equalsIgnoreCase(config.selfNameMode)) {
            // Show the player's own real name unchanged (escape hatch to get back to normal).
            return name;
        }
        return SafeText.rewrite(name);
    }

    /**
     * Render-path variant for player name tags and TAB entries. The source display component is
     * normally stable between network updates, so caching here prevents a per-frame scan of every
     * known player name and the associated regex/string allocations.
     */
    public static Component applyPlayerDisplayName(Component name, UUID playerId, boolean isSelf) {
        if (name == null || playerId == null) {
            return name;
        }
        DisplayNameCacheEntry cached = displayNameCache.get(playerId);
        if (cached != null && cached.source.equals(name) && cached.isSelf == isSelf) {
            return cached.result;
        }
        Component result = applySelfMode(name, isSelf);
        displayNameCache.put(playerId, new DisplayNameCacheEntry(name, isSelf, result));
        return result;
    }

    private record DisplayNameCacheEntry(Component source, boolean isSelf, Component result) {
    }

    /** Remove complete legacy formatting pairs without compiling a regex on player-list updates. */
    private static String stripFormatCodes(String text) {
        int marker = text.indexOf('§');
        if (marker < 0) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            result.append(c);
        }
        return result.toString();
    }
}
