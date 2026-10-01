package livehider.neoforge;

import livehider.LiveHider;
import livehider.LiveHiderConfig;
import livehider.client.LiveHiderConfigScreen;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/** NeoForge equivalent of the lightweight Fabric configuration watcher. */
final class NeoForgeConfigWatcher {
    private static final int CHECK_INTERVAL_TICKS = 40;
    private static long lastModified = -1;
    private static int tickCount;
    private static boolean started;

    private NeoForgeConfigWatcher() {
    }

    static void start() {
        if (started) {
            return;
        }
        started = true;
        lastModified = mtime(LiveHiderConfig.getPath());
    }

    static void tick() {
        if (!started || ++tickCount < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCount = 0;
        if (LiveHiderConfigScreen.isActiveConfigScreen(Minecraft.getInstance().gui.screen())) {
            return;
        }
        Path path = LiveHiderConfig.getPath();
        long now = mtime(path);
        if (now != lastModified) {
            lastModified = now;
            if (LiveHiderConfig.reload()) {
                LiveHider.LOGGER.info("[LiveHider] Config hot-reloaded from {}", path);
            }
        }
    }

    /** Record an in-game save so it is not mistaken for an external JSON edit. */
    static void markCurrentFileState() {
        lastModified = mtime(LiveHiderConfig.getPath());
    }

    private static long mtime(Path path) {
        try {
            FileTime time = Files.getLastModifiedTime(path);
            return time == null ? -1 : time.toMillis();
        } catch (Exception ignored) {
            return -1;
        }
    }
}
