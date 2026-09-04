package livehider.skin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.blaze3d.platform.NativeImage;
import livehider.LiveHider;
import livehider.LiveHiderConfig;
import livehider.mixin.accessor.SkinManagerInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Client-only custom skin replacement. Each visible player is deterministically assigned one
 * account from the configured pool; assignments may repeat and stay stable while the pool is unchanged.
 * Mojang profile lookups are asynchronous and use Steve until the substitute is ready.
 */
public final class RandomSkinManager {
    private static final String ELYBY_URL_TEMPLATE = "https://skinsystem.ely.by/skins/{name}.png";
    private static final ConcurrentMap<String, ConcurrentMap<String, CandidateSkin>> cachedSkins = new ConcurrentHashMap<>();
    private static final Object LOCAL_POOL_LOCK = new Object();
    private static final Object CONFIG_CACHE_LOCK = new Object();
    private static volatile LiveHiderConfig cachedConfig;
    private static volatile List<String> accountPool = List.of();
    private static volatile String cachedSource = "MOJANG";
    private static volatile String cachedSourceKey = "";
    private static volatile String localPoolFolder = "";
    private static volatile List<String> localPool = List.of();
    private static volatile long nextLocalPoolScanNanos;
    private static volatile long sessionSalt = ThreadLocalRandom.current().nextLong();

    private RandomSkinManager() {
    }

    public static void start() {
        LiveHider.LOGGER.info("[LiveHider] Custom skin replacement is ready.");
    }

    public static boolean isActive() {
        LiveHiderConfig config = LiveHiderConfig.get();
        return config != null && (isReplacementMode(config.skinOtherMode) || isReplacementMode(config.skinSelfMode));
    }

    /** Start a new stable random assignment when joining another world/server. */
    public static void resetSessionAssignments() {
        sessionSalt = ThreadLocalRandom.current().nextLong();
    }

    /** Called after settings are saved or reloaded; no render-path list rebuilding is needed. */
    public static void invalidateConfigCache() {
        cachedConfig = null;
        nextLocalPoolScanNanos = 0L;
    }

    public static PlayerSkin getSkin(SkinManager skinManager, GameProfile profile) {
        if (!isActive() || profile == null || profile.id() == null) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LiveHiderConfig config = LiveHiderConfig.get();
        if (config == null) return null;
        ensureConfigCache(config);
        boolean self = minecraft.player != null && profile.id().equals(minecraft.player.getUUID());
        String mode = self ? config.skinSelfMode : config.skinOtherMode;
        if ("STEVE".equalsIgnoreCase(mode)) return SkinOverride.getSteveSkin();
        if (!"CUSTOM".equalsIgnoreCase(mode) && !"RANDOM".equalsIgnoreCase(mode)) return null;
        String source = cachedSource;
        String candidate = "CUSTOM".equalsIgnoreCase(mode) ? customCandidate(config, source) : randomCandidate(profile, config, source);
        if (candidate == null) {
            return SkinOverride.getSteveSkin();
        }
        String sourceKey = cachedSourceKey;
        CandidateSkin assignment = cachedSkins.computeIfAbsent(sourceKey, key -> new ConcurrentHashMap<>())
            .computeIfAbsent(candidate, key -> {
            CandidateSkin fresh = new CandidateSkin(candidate, source, sourceKey, SkinOverride.getSteveSkin());
            loadReplacement(skinManager, fresh);
            return fresh;
            });
        return assignment.skin;
    }

    private static boolean isReplacementMode(String mode) {
        return "STEVE".equalsIgnoreCase(mode) || "CUSTOM".equalsIgnoreCase(mode) || "RANDOM".equalsIgnoreCase(mode);
    }

    private static String customCandidate(LiveHiderConfig config, String source) {
        if ("LOCAL_FOLDER".equalsIgnoreCase(source)) {
            return LocalSkinLookup.resolveSkinFile(config.skinLocalFolder, config.skinCustomSkin);
        }
        String candidate = config.skinCustomSkin == null ? "" : config.skinCustomSkin.trim();
        return MojangSkinLookup.isValidPlayerName(candidate) ? candidate : null;
    }

    private static String randomCandidate(GameProfile profile, LiveHiderConfig config, String source) {
        List<String> pool = sourcePool(config, source);
        if (pool.isEmpty()) return null;
        long mixed = profile.id().getMostSignificantBits() ^ profile.id().getLeastSignificantBits() ^ sessionSalt;
        return pool.get((int) Math.floorMod(mixed, (long) pool.size()));
    }

    private static List<String> sourcePool(LiveHiderConfig config, String source) {
        if ("LOCAL_FOLDER".equalsIgnoreCase(source)) {
            return localFolderPool(config);
        }
        return accountPool;
    }

    private static List<String> sanitizedAccountPool(LiveHiderConfig config) {
        if (config == null || config.skinRandomPool == null || config.skinRandomPool.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String value : config.skinRandomPool) {
            if (MojangSkinLookup.isValidPlayerName(value)) {
                result.add(value.trim());
            }
        }
        return result;
    }

    /** Re-scan at most once every five seconds; rendering only reads the cached immutable list. */
    private static List<String> localFolderPool(LiveHiderConfig config) {
        String folder = config == null ? "" : config.skinLocalFolder.trim();
        long now = System.nanoTime();
        if (folder.equals(localPoolFolder) && now < nextLocalPoolScanNanos) return localPool;
        synchronized (LOCAL_POOL_LOCK) {
            now = System.nanoTime();
            if (!folder.equals(localPoolFolder) || now >= nextLocalPoolScanNanos) {
                localPool = LocalSkinLookup.listPngFiles(folder);
                localPoolFolder = folder;
                nextLocalPoolScanNanos = now + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            }
            return localPool;
        }
    }

    private static void loadReplacement(SkinManager skinManager, CandidateSkin assignment) {
        if ("LOCAL_FOLDER".equalsIgnoreCase(assignment.source)) {
            LocalSkinLookup.lookupSkin(assignment.candidate)
                .thenCompose(bytes -> registerRemoteSkinOnClient(assignment, bytes))
                .whenComplete((skin, error) -> completeReplacement(assignment, skin, error));
            return;
        }
        if (!"MOJANG".equalsIgnoreCase(assignment.source)) {
            LittleSkinLookup.lookupSkin(assignment.sourceKey, assignment.candidate)
                .thenCompose(bytes -> registerRemoteSkinOnClient(assignment, bytes))
                .whenComplete((skin, error) -> completeReplacement(assignment, skin, error));
            return;
        }
        MojangSkinLookup.lookupId(assignment.candidate)
            .thenCompose(sourceId -> MojangSkinLookup.lookupTextures(sourceId)
                .thenCompose(textures -> registerOnClient(skinManager, sourceId, textures)))
            .whenComplete((skin, error) -> completeReplacement(assignment, skin, error));
    }

    private static void ensureConfigCache(LiveHiderConfig config) {
        if (cachedConfig == config) return;
        synchronized (CONFIG_CACHE_LOCK) {
            if (cachedConfig == config) return;
            cachedSource = config == null || config.skinSource == null ? "MOJANG" : config.skinSource;
            cachedSourceKey = sourceKey(config, cachedSource);
            accountPool = sanitizedAccountPool(config);
            cachedConfig = config;
        }
    }

    private static String sourceKey(LiveHiderConfig config, String source) {
        if ("LITTLESKIN".equalsIgnoreCase(source)) return LittleSkinLookup.URL_TEMPLATE;
        if ("ELYBY".equalsIgnoreCase(source)) return ELYBY_URL_TEMPLATE;
        if ("LOCAL_FOLDER".equalsIgnoreCase(source)) return "local:" + (config == null ? "" : config.skinLocalFolder.trim());
        return config == null ? "" : config.skinUrlTemplate.trim();
    }

    private static void completeReplacement(CandidateSkin assignment, PlayerSkin skin, Throwable error) {
        if (error != null) {
            LiveHider.LOGGER.debug("[LiveHider] Could not load replacement skin for {}: {}",
                assignment.candidate, error.toString());
            return;
        }
        assignment.skin = skin != null ? skin : SkinOverride.getSteveSkin();
    }

    private static java.util.concurrent.CompletableFuture<PlayerSkin> registerOnClient(
        SkinManager skinManager, UUID targetId, MinecraftProfileTextures textures
    ) {
        java.util.concurrent.CompletableFuture<PlayerSkin> result = new java.util.concurrent.CompletableFuture<>();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            try {
                ((SkinManagerInvoker) skinManager).liveHider$registerTextures(targetId, textures)
                    .whenComplete((skin, error) -> {
                        if (error != null) result.completeExceptionally(error);
                        else result.complete(skin);
                    });
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        });
        return result;
    }

    private static java.util.concurrent.CompletableFuture<PlayerSkin> registerRemoteSkinOnClient(CandidateSkin assignment, byte[] bytes) {
        java.util.concurrent.CompletableFuture<PlayerSkin> result = new java.util.concurrent.CompletableFuture<>();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            NativeImage image = null;
            try {
                image = NativeImage.read(bytes);
                if (image.getWidth() != 64 || image.getHeight() != 64) {
                    throw new IllegalArgumentException("Skin image must be 64x64");
                }
                Identifier textureId = Identifier.fromNamespaceAndPath("live_hider",
                    "remote_skin/" + Integer.toUnsignedString((assignment.sourceKey + '\u0000' + assignment.candidate).hashCode()));
                DynamicTexture dynamicTexture = new DynamicTexture(() -> "Live Hider remote skin", image);
                dynamicTexture.upload();
                minecraft.getTextureManager().register(textureId, dynamicTexture);
                image = null; // DynamicTexture now owns and closes the image.
                ClientAsset.Texture texture = new ClientAsset.Texture() {
                    @Override public Identifier id() { return textureId; }
                    @Override public Identifier texturePath() { return textureId; }
                };
                PlayerSkin fallback = SkinOverride.getSteveSkin();
                result.complete(PlayerSkin.insecure(texture, fallback.cape(), fallback.elytra(), PlayerModelType.WIDE));
            } catch (Throwable error) {
                if (image != null) image.close();
                result.completeExceptionally(error);
            }
        });
        return result;
    }

    private static final class CandidateSkin {
        private final String candidate;
        private final String source;
        private final String sourceKey;
        private volatile PlayerSkin skin;

        private CandidateSkin(String candidate, String source, String sourceKey, PlayerSkin skin) {
            this.candidate = candidate;
            this.source = source;
            this.sourceKey = sourceKey;
            this.skin = skin;
        }
    }
}
