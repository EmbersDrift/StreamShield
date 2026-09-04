package livehider.skin;

import net.minecraft.util.Util;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Read-only LittleSkin legacy PNG lookup. No account, cookie, or token is used. */
public final class LittleSkinLookup {
    public static final String URL_TEMPLATE = "https://littleskin.cn/skin/{name}.png";
    private static final int MAX_IMAGE_BYTES = 1024 * 1024;
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private LittleSkinLookup() {
    }

    public static CompletableFuture<byte[]> lookupSkin(String name) {
        return lookupSkin(URL_TEMPLATE, name);
    }

    /**
     * Fetches a public skin PNG from an explicitly configured HTTPS template. The only supported
     * placeholder is {name}; accepting credentials here would expose them in the local config.
     */
    public static CompletableFuture<byte[]> lookupSkin(String urlTemplate, String name) {
        if (!MojangSkinLookup.isValidPlayerName(name)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid Minecraft account name"));
        }
        URI uri;
        try {
            if (urlTemplate == null || !urlTemplate.contains("{name}")) {
                throw new IllegalArgumentException("Skin URL template must contain {name}");
            }
            uri = URI.create(urlTemplate.replace("{name}", URLEncoder.encode(name.trim(), StandardCharsets.UTF_8)));
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("Skin URL template must be an HTTPS URL without credentials");
            }
        } catch (Exception error) {
            return CompletableFuture.failedFuture(error);
        }
        String cleanName = name.trim();
        URI requestUri = uri;
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(requestUri)
                    .timeout(Duration.ofSeconds(12))
                    .header("Accept", "image/png")
                    .GET()
                    .build();
                HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("Skin endpoint HTTP " + response.statusCode());
                }
                String type = response.headers().firstValue("Content-Type").orElse("");
                byte[] body = response.body();
                if (!type.toLowerCase(java.util.Locale.ROOT).startsWith("image/png")
                    || body == null || body.length == 0 || body.length > MAX_IMAGE_BYTES) {
                    throw new IllegalStateException("Invalid skin image response");
                }
                return body;
            } catch (Exception error) {
                throw new CompletionException(error);
            }
        }, Util.ioPool());
    }
}
