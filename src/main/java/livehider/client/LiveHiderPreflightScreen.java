package livehider.client;

import livehider.LiveHider;
import livehider.LiveHiderConfig;
import livehider.component.IOverlayComponent;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/** Local readiness checks; this screen cannot inspect or certify the OBS capture pixels. */
public final class LiveHiderPreflightScreen {
    private static Screen activeScreen;

    private LiveHiderPreflightScreen() {}

    private static Component t(String key, Object... args) {
        return Component.translatable("live_hider.preflight." + key, args);
    }

    private static Component toggle(boolean enabled) {
        return Component.translatable(enabled ? "options.on" : "options.off")
            .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    public static boolean isPreflightScreen(Screen screen) {
        return screen != null && screen == activeScreen;
    }

    public static Component overlayStatusText() {
        String status = LiveHiderDiagnostics.status();
        ChatFormatting color = switch (status) {
            case "READY" -> ChatFormatting.GREEN;
            case "FAILED", "UNSUPPORTED", "UNKNOWN" -> ChatFormatting.RED;
            default -> ChatFormatting.YELLOW;
        };
        return t("status", t("status." + status.toLowerCase(Locale.ROOT)))
            .copy().withStyle(color);
    }

    public static Screen create(Screen parent) {
        PrivacyShield.protectConfiguration();
        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent)
            .setTitle(t("title")).setDoesConfirmSave(false);
        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(t("title"));
        category.addEntry(entries.startTextDescription(t("manual_check")).build());
        category.addEntry(new LiveStatusEntry(t("title"), LiveHiderPreflightScreen::overlayStatusText));
        category.addEntry(new LiveStatusEntry(t("failure_code"),
            () -> t("failure_code", LiveHiderDiagnostics.failureCode())));
        category.addEntry(entries.startTextDescription(t("build",
            LiveHiderDiagnostics.buildValue("version"), LiveHiderDiagnostics.buildValue("minecraft"),
            LiveHiderDiagnostics.buildValue("loader"), LiveHiderDiagnostics.buildValue("commit"))).build());
        category.addEntry(new LiveStatusEntry(t("fallback"),
            () -> t("fallback", toggle(LiveHiderConfig.get().hideHudWhenOverlayUnavailable))));
        category.addEntry(entries.startTextDescription(t("fallback_hint")).build());
        category.addEntry(new ActionButtonEntry(t("retry"), t("retry"), LiveHider::retryOverlayInitialization,
            () -> "FAILED".equals(LiveHiderDiagnostics.status())));
        category.addEntry(entries.startTextDescription(t("components_hint")).build());
        for (LiveHiderDiagnostics.HudComponent hud : LiveHiderDiagnostics.HudComponent.values()) {
            Component label = Component.translatable("live_hider.overlay." + hud.key());
            category.addEntry(new LiveStatusEntry(label, () -> {
                IOverlayComponent component = hud.component();
                return t("component", label, toggle(component.isOverlayEnabled()),
                    component.canAutoHide() ? toggle(component.isAutoHideEnabled()) : t("not_applicable"));
            }));
        }
        category.addEntry(new LiveStatusEntry(t("privacy"),
            () -> t("privacy", toggle(LiveHiderConfig.get().anonymizeNames),
                toggle(LiveHiderConfig.get().sanitizeChatInput), toggle(LiveHiderConfig.get().redactEnabled))));
        category.addEntry(new LiveStatusEntry(t("screens"),
            () -> t("screens", toggle(LiveHiderConfig.get().hideAllScreens))));
        category.addEntry(entries.startTextDescription(t("preview_steps")).build());
        category.addEntry(entries.startTextDescription(t("diagnostic_hint")).build());
        AtomicReference<Component> result = new AtomicReference<>(Component.empty());
        category.addEntry(new ActionButtonEntry(t("copy"), t("copy"), () -> {
            try {
                Minecraft.getInstance().keyboardHandler.setClipboard(LiveHiderDiagnostics.report());
                result.set(t("copied"));
            } catch (RuntimeException ignored) {
                result.set(t("copy_failed"));
            }
        }));
        category.addEntry(new ActionButtonEntry(t("export"), t("export"), () -> {
            try {
                result.set(t("exported", LiveHiderDiagnostics.export()));
            } catch (IOException | RuntimeException ignored) {
                result.set(t("export_failed"));
            }
        }));
        category.addEntry(new LiveStatusEntry(t("diagnostic_result"), result::get));
        activeScreen = builder.build();
        return activeScreen;
    }
}
