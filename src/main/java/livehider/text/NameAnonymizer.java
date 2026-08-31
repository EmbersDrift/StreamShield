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
        String clean = realName.replaceAll("§.", "");
        if (clean.isEmpty()) {
            return;
        }
        // OWN self-name mode: the local player's own name must never be anonymized. Every
        // anonymization surface (TAB list, chat, scoreboard rows, chat input display) goes
        // through this map, so excluding the own name here covers them all. Register() runs on
        // every tick and on player-list changes, so toggling the mode also self-heals the map.
        if ("OWN".equalsIgnoreCase(config.selfNameMode) && isOwnRealName(clean)) {
            if (map.remove(clean) != null || patternCache.remove(clean) != null) {
                rebuildSortedNames();
            }
            return;
        }
        if (map.containsKey(clean)) {
            return;
        }
        int digits = Math.min(9, Math.max(1, config.nameDigits));
        long h = (id != null ? id.getLeastSignificantBits() : clean.hashCode()) ^ salt;
        int n = Math.abs((int) h);
        int modulo = (int) Math.pow(10, digits);
        String num = String.format("%0" + digits + "d", n % modulo);
        map.put(clean, config.nameTemplate + num);
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

    private static void rebuildSortedNames() {
        sortedNames = new ArrayList<>(map.keySet());
        sortedNames.sort((a, b) -> b.length() - a.length());
    }

    public static boolean isActive() {
        LiveHiderConfig config = LiveHiderConfig.get();
        return active && config != null && config.anonymizeNames && !map.isEmpty();
    }

    public static void setRevealInput(boolean value) {
        revealInput = value;
    }

    public static boolean isRevealInput() {
        return revealInput;
    }

    /** Replace every known player name in a plain string (longest names first, word boundaries). */
    public static String applyToText(String text) {
        if (text == null || !isActive()) {
            return text;
        }
        List<String> names = sortedNames;
        String result = text;
        for (String name : names) {
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
        String striped = text.replaceAll("§.", "");
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
            return Component.literal(config.selfCustomName);
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
}
