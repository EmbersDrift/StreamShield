package livehider.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.network.chat.Component;

/**
 * Small helpers for the overlay layer. Ported from obs-overlay (MIT, author zziger).
 */
public final class OverlayUtils {
    private OverlayUtils() {
    }

    public static void showToast(Component title, Component description) {
        Minecraft.getInstance()
            .submit(() -> Minecraft.getInstance().getToastManager().addToast(new SystemToast(SystemToastId.LOW_DISK_SPACE, title, description)));
    }
}
