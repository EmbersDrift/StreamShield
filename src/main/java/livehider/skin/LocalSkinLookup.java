package livehider.skin;

import net.minecraft.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

/** Read-only local skin-folder access. Files never leave the client. */
public final class LocalSkinLookup {
    private static final long MAX_IMAGE_BYTES = 1024 * 1024;

    private LocalSkinLookup() {
    }

    public static List<String> listPngFiles(String folder) {
        if (folder == null || folder.isBlank()) return List.of();
        try {
            Path root = Path.of(folder).toAbsolutePath().normalize();
            if (!Files.isDirectory(root)) return List.of();
            try (Stream<Path> paths = Files.list(root)) {
                return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png"))
                    .map(path -> path.toAbsolutePath().normalize().toString())
                    .sorted()
                    .toList();
            }
        } catch (Exception ignored) {
            return List.of();
        }
    }

    /** Resolves one direct-child PNG by filename without allowing paths outside the configured folder. */
    public static String resolveSkinFile(String folder, String filename) {
        if (folder == null || folder.isBlank() || filename == null || filename.isBlank()) return null;
        try {
            Path root = Path.of(folder).toAbsolutePath().normalize();
            Path candidate = root.resolve(filename.trim()).normalize();
            if (!candidate.startsWith(root) || !Files.isRegularFile(candidate)
                || !candidate.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
                return null;
            }
            return candidate.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    public static CompletableFuture<byte[]> lookupSkin(String filename) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path path = Path.of(filename);
                long size = Files.size(path);
                if (size <= 0 || size > MAX_IMAGE_BYTES) {
                    throw new IllegalArgumentException("Local skin file must be between 1 byte and 1 MiB");
                }
                return Files.readAllBytes(path);
            } catch (IOException | RuntimeException error) {
                throw new CompletionException(error);
            }
        }, Util.ioPool());
    }
}
