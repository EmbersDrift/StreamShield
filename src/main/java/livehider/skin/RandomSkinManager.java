package livehider.skin;

import livehider.LiveHider;
import livehider.LiveHiderConfig;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Random-skin support (currently parked). {@code RANDOM} mode is not active: {@link #start()} records
 * the flag but registers no client callbacks, so nothing here can interfere with the working
 * {@code STEVE} mode. If {@code RANDOM} is ever enabled it simply has no effect (falls back to the
 * real skin) rather than causing network side effects. Kept minimal and side-effect-free.
 */
public final class RandomSkinManager {
    private static boolean started = false;

    private RandomSkinManager() {
    }

    public static void start() {
        // Parked: do NOT register JOIN/DISCONNECT/CLIENT_STOPPING or do any network work.
        started = true;
        LiveHider.LOGGER.info("[LiveHider] RandomSkinManager parked (RANDOM mode disabled).");
    }

    public static boolean isStarted() {
        return started;
    }

    public static boolean isActive() {
        LiveHiderConfig config = LiveHiderConfig.get();
        return config != null && "RANDOM".equalsIgnoreCase(config.skinMode);
    }

    public static boolean isEnabled() {
        return isActive();
    }
}
