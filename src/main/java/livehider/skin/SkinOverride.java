package livehider.skin;

import livehider.LiveHiderConfig;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Supplies the skin/substitution used for the two obfuscation modes.
 *
 * <p>{@code STEVE}: every player is rendered with the default Steve {@link PlayerSkin} (returned
 * directly by {@code SkinManager.createLookup} override — the texture is a real built-in resource, so
 * it renders correctly). {@code RANDOM}: a per-player substitute {@link GameProfile} so the vanilla
 * pipeline downloads + registers a pool player's real skin with correct async handling.
 */
public final class SkinOverride {
    private static PlayerSkin steve;

    private SkinOverride() {
    }

    public static boolean isSteveActive() {
        LiveHiderConfig config = LiveHiderConfig.get();
        return config != null && "STEVE".equalsIgnoreCase(config.skinMode);
    }

    public static boolean isRandoActive() {
        LiveHiderConfig config = LiveHiderConfig.get();
        return config != null && "RANDOM".equalsIgnoreCase(config.skinMode);
    }

    public static boolean isActive() {
        return isSteveActive() || isRandoActive();
    }

    /** Lazily build (and cache) the Steve PlayerSkin. Safe to call on the render thread. */
    public static PlayerSkin getSteveSkin() {
        if (steve == null) {
            steve = DefaultPlayerSkin.getDefaultSkin();
        }
        return steve;
    }

    /** @return the Steve skin, or {@code null} when not in STEVE mode. */
    public static PlayerSkin steveSkinIfActive() {
        return isSteveActive() ? getSteveSkin() : null;
    }
}
