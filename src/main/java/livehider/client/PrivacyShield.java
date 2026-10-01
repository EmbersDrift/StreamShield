package livehider.client;

import livehider.LiveHider;
import livehider.LiveHiderConfig;
import livehider.overlay.OverlayHook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

/** Latched for the entire configuration session, including third-party child dialogs. */
public final class PrivacyShield {
    private static final livehider.overlay.PrivacySession SESSION = new livehider.overlay.PrivacySession();
    private static Screen safeDialog;
    private static Screen resumeScreen;
    private PrivacyShield() {}
    public static boolean active() { return SESSION.active(); }
    public static void arm() { SESSION.arm(); resumeScreen = null; }
    public static void protectConfiguration() {
        if (LiveHiderConfig.get().protectConfigUi && OverlayHook.isPlatformSupported()) arm();
    }
    public static boolean safeDialogVisible() {
        return safeDialog != null && Minecraft.getInstance().screen == safeDialog;
    }
    public static void toggleEmergency() {
        if (!active()) arm(); else confirmResume();
    }
    public static void confirmResume() {
        safeDialog = new ConfirmScreen(confirmed -> {
            // Never reveal the previous private screen, even for one frame.
            Minecraft.getInstance().setScreen(null);
            safeDialog = null;
            if (confirmed) {
                requestResume();
            }
        }, t("resume"), t("resume_warning"));
        Minecraft.getInstance().setScreen(safeDialog);
    }
    public static void checkAvailability() {
        SESSION.nextFrame(Minecraft.getInstance().screen != resumeScreen);
        if (!active() || SESSION.releasing() || LiveHider.getIsInitialized() || safeDialogVisible()) return;
        safeDialog = new ConfirmScreen(retry -> {
            Minecraft.getInstance().setScreen(null);
            safeDialog = null;
            if (retry) LiveHider.retryOverlayInitialization(); else {
                requestResume();
            }
        }, t("unavailable"), t("unavailable_hint"), t("retry"), t("close"));
        Minecraft.getInstance().setScreen(safeDialog);
    }
    private static Component t(String key) { return Component.translatable("live_hider.shield." + key); }
    private static void requestResume() {
        // setScreen(null) becomes a fresh TitleScreen when no world is loaded.
        resumeScreen = Minecraft.getInstance().screen;
        PrivacyPresets.tickBeforeResume();
        if (!PrivacyPresets.hasFailure()) SESSION.requestResume();
    }
}
