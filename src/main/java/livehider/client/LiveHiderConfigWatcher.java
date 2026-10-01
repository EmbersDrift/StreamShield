package livehider.client;

import livehider.LiveHider;
import livehider.LiveHiderConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

/**
 * Polls {@code live_hider.json} every couple seconds and hot-reloads the config when it changes, so
 * manual JSON edits apply without a restart. Lightweight: only a stat() per poll, and a full
 * re-parse + cache refresh only when the file actually changed.
 */
public final class LiveHiderConfigWatcher {
    private static final int CHECK_INTERVAL_TICKS = 40; // ~2 seconds at 20 TPS
    private static long lastModified = -1;
    private static int tickCount = 0;
    private static boolean started = false;

    private LiveHiderConfigWatcher() {
    }

    public static void start() {
        if (started) {
            return;
        }
        started = true;
        Path path = LiveHiderConfig.getPath();
        lastModified = mtime(path);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++tickCount < CHECK_INTERVAL_TICKS) {
                return;
            }
            tickCount = 0;
            if (LiveHiderConfigScreen.isActiveConfigScreen(client.gui.screen())) {
                return;
            }
            long now = mtime(path);
            if (now != lastModified) {
                lastModified = now;
                if (LiveHiderConfig.reload()) {
                    LiveHider.LOGGER.info("[LiveHider] Config hot-reloaded from {}", path);
                }
            }
        });
    }

    /** Record a successful in-game save so the watcher does not reload the same file afterwards. */
    public static void markCurrentFileState() {
        lastModified = mtime(LiveHiderConfig.getPath());
    }

    private static long mtime(Path path) {
        try {
            FileTime time = Files.getLastModifiedTime(path);
            return time == null ? -1 : time.toMillis();
        } catch (Exception e) {
            return -1;
        }
    }
}
