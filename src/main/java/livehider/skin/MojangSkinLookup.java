package livehider.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.SignatureState;
import net.minecraft.util.Util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Resolves a real Mojang player's skin textures by name (the "正版校验" path for random skins).
 * Flow: name → {@code api.mojang.com/users/profiles/minecraft/<name>} → UUID →
 * {@code sessionserver.mojang.com/session/minecraft/profile/<uuid>} → base64 "textures" property →
 * {@link MinecraftProfileTextures}. Async via {@link HttpClient}. Network only; the caller decides how
 * to register/render the resulting textures.
 */
public final class MojangSkinLookup {
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .build();
    private static final String USERS_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private MojangSkinLookup() {
    }

    /** Resolve a (dashed or undashed) UUID string to canonical dashed form. */
    private static String dashed(UUID id) {
        return id.toString();
    }

    public static CompletableFuture<UUID> lookupId(String name) {
        return Util.make(() -> CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(USERS_URL + name))
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();
                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) {
                    throw new CompletionException(new RuntimeException("users lookup HTTP " + resp.statusCode()));
                }
                JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
                String id = obj.get("id").getAsString();
                return uuidFromString(id);
            } catch (Exception e) {
                if (e instanceof CompletionException ce) {
                    throw ce;
                }
                throw new CompletionException(e);
            }
        }, Util.ioPool()));
    }

    public static CompletableFuture<MinecraftProfileTextures> lookupTextures(UUID id) {
        return Util.make(() -> CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(PROFILE_URL + dashed(id)))
                    .timeout(Duration.ofSeconds(12))
                    .header("Accept", "application/json")
                    .GET().build();
                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) {
                    throw new CompletionException(new RuntimeException("profile HTTP " + resp.statusCode()));
                }
                JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
                String value = null;
                if (obj.has("properties")) {
                    JsonArray props = obj.getAsJsonArray("properties");
                    for (JsonElement el : props) {
                        JsonObject p = el.getAsJsonObject();
                        if ("textures".equals(p.get("name").getAsString())) {
                            value = p.get("value").getAsString();
                            break;
                        }
                    }
                }
                if (value == null) {
                    return MinecraftProfileTextures.EMPTY;
                }
                return parseTextures(value);
            } catch (Exception e) {
                if (e instanceof CompletionException ce) {
                    throw ce;
                }
                throw new CompletionException(e);
            }
        }, Util.ioPool()));
    }

    /** Decode the base64 "textures" value into {@link MinecraftProfileTextures}. */
    private static MinecraftProfileTextures parseTextures(String base64) {
        byte[] decoded = Base64.getDecoder().decode(base64);
        String json = new String(decoded, StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        MinecraftProfileTexture skin = null;
        MinecraftProfileTexture cape = null;
        MinecraftProfileTexture elytra = null;
        if (root.has("textures")) {
            JsonObject textures = root.getAsJsonObject("textures");
            if (textures.has("SKIN")) {
                skin = toProfileTexture(textures.getAsJsonObject("SKIN"));
            }
            if (textures.has("CAPE")) {
                cape = toProfileTexture(textures.getAsJsonObject("CAPE"));
            }
            if (textures.has("ELYTRA")) {
                elytra = toProfileTexture(textures.getAsJsonObject("ELYTRA"));
            }
        }
        return new MinecraftProfileTextures(skin, cape, elytra, SignatureState.SIGNED);
    }

    private static MinecraftProfileTexture toProfileTexture(JsonObject obj) {
        String url = obj.has("url") ? obj.get("url").getAsString() : "";
        Map<String, String> metadata = new HashMap<>();
        if (obj.has("metadata")) {
            JsonObject meta = obj.getAsJsonObject("metadata");
            if (meta.has("model")) {
                metadata.put("model", meta.get("model").getAsString());
            }
        }
        return new MinecraftProfileTexture(url, metadata);
    }

    private static UUID uuidFromString(String s) {
        String trimmed = s.replace("-", "");
        if (trimmed.length() != 32) {
            throw new IllegalArgumentException("Bad UUID: " + s);
        }
        return UUID.fromString(
            trimmed.substring(0, 8) + "-" + trimmed.substring(8, 12) + "-" + trimmed.substring(12, 16)
                + "-" + trimmed.substring(16, 20) + "-" + trimmed.substring(20));
    }
}
