package livehider.client;

import livehider.LiveHiderConfig;
import livehider.ScoreboardRule;
import livehider.text.*;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import java.util.concurrent.atomic.AtomicReference;

/** Assistant, local coverage samples and explicit preset transfer actions, under the same shield. */
public final class PrivacyToolsScreen {
    private PrivacyToolsScreen() {}
    private static Component t(String key) { return Component.translatable("live_hider.tools." + key); }
    public static Screen create(Screen parent) {
        PrivacyShield.protectConfiguration();
        var builder = ConfigBuilder.create().setParentScreen(parent).setTitle(t("title")).setDoesConfirmSave(false);
        var entries = builder.entryBuilder();
        var rules = builder.getOrCreateCategory(t("assistant"));
        rules.addEntry(entries.startTextDescription(t("assistant_hint")).build());
        var status = new AtomicReference<Component>(Component.empty());
        rules.addEntry(new LiveStatusEntry(t("status"), status::get));
        var snapshot = ScoreboardSamples.snapshot();
        if (snapshot.isEmpty()) rules.addEntry(entries.startTextDescription(t("empty")).build());
        rules.addEntry(new ActionButtonEntry(t("refresh"), t("refresh"), () -> Minecraft.getInstance().setScreen(create(parent))));
        rules.addEntry(new ActionButtonEntry(t("clear"), t("clear"), () -> {
            ScoreboardSamples.clear(); Minecraft.getInstance().setScreen(create(parent));
        }));
        int index = 0;
        for (var sample : snapshot) {
            var group = entries.startSubCategory(Component.translatable("live_hider.tools.sample", ++index));
            var key = entries.startTextField(t("key"), sample.key()).build();
            for (var child : key.children()) if (child instanceof net.minecraft.client.gui.components.EditBox edit) edit.setMaxLength(32768);
            key.setValue(sample.key());
            group.add(key);
            group.add(new ScoreboardPreviewEntry(key::getValue));
            var replacement = entries.startTextField(t("replacement"), "").build();
            group.add(replacement);
            group.add(new ActionButtonEntry(t("add"), t("add"), () -> {
                if (key.getValue().isEmpty()) return;
                LiveHiderConfig.get().scoreboardRules.add(new ScoreboardRule(true, key.getValue(), replacement.getValue()));
                LiveHiderConfig.save();
                status.set(t("added"));
            }));
            rules.addEntry(group.build());
        }
        var checks = builder.getOrCreateCategory(t("checks"));
        checks.addEntry(entries.startTextDescription(t("checks_hint")).build());
        String word = LiveHiderConfig.get().redactPatterns.stream().filter(s -> s != null && !s.isEmpty()).findFirst().orElse("example.private");
        Component example = Component.literal("Example: " + word);
        checks.addEntry(entries.startTextDescription(Component.translatable("live_hider.tools.original", example)).build());
        checks.addEntry(new LiveStatusEntry(t("general"), () -> Component.translatable("live_hider.tools.general", SafeText.rewrite(example))));
        checks.addEntry(new LiveStatusEntry(t("scoreboard"), () -> Component.translatable("live_hider.tools.scoreboard", Redactor.applyScoreboardToComponent(example))));
        checks.addEntry(new LiveStatusEntry(t("name"), () -> Component.translatable("live_hider.tools.name",
            NameAnonymizer.applyToText(Minecraft.getInstance().getUser().getName()))));
        checks.addEntry(new LiveStatusEntry(t("sign"), () -> Component.translatable("live_hider.tools.sign",
            LiveHiderConfig.get().hideSignText ? "" : LiveHiderConfig.get().filterSignText
                ? Redactor.applyToText(example.getString()) : example.getString())));
        checks.addEntry(new LiveStatusEntry(t("extra"), () -> Component.translatable("live_hider.tools.extra",
            LiveHiderConfig.get().redactItemTooltips ? SafeText.rewrite(example) : example,
            LiveHiderConfig.get().redactContainerTitles ? SafeText.rewrite(example) : example,
            LiveHiderConfig.get().redactBooks ? SafeText.rewrite(example) : example)));
        checks.addEntry(new ActionButtonEntry(t("preflight"), t("preflight"), () -> Minecraft.getInstance().setScreen(LiveHiderPreflightScreen.create(create(parent)))));
        var presets = builder.getOrCreateCategory(t("presets"));
        presets.addEntry(entries.startTextDescription(t("presets_hint")).build());
        presets.addEntry(new LiveStatusEntry(t("profile_status"), () -> t(PrivacyPresets.hasFailure()
            ? "profile_failed" : PrivacyPresets.isServerActive() ? "profile_active" : "profile_global")));
        presets.addEntry(new ActionButtonEntry(t("profile_retry"), t("profile_retry"), PrivacyPresets::retrySwitch));
        var transfer = new AtomicReference<Component>(Component.empty());
        presets.addEntry(new LiveStatusEntry(t("status"), transfer::get));
        presets.addEntry(new ActionButtonEntry(t("export"), t("export"), () -> confirm(parent, true, transfer)));
        presets.addEntry(new ActionButtonEntry(t("import"), t("import"), () -> confirm(parent, false, transfer)));
        return builder.build();
    }
    private static void confirm(Screen parent, boolean export, AtomicReference<Component> status) {
        Screen returnTo = Minecraft.getInstance().screen;
        Minecraft.getInstance().setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                try {
                    if (export) PrivacyPresets.exportPreset(); else PrivacyPresets.importPreset();
                    status.set(t("transfer_ok"));
                    if (!export) {
                        // Rebuild parent widgets, otherwise their stale values could undo the import.
                        Minecraft.getInstance().setScreen(create(LiveHiderConfigScreen.create(null)));
                        return;
                    }
                } catch (Exception error) { status.set(t("transfer_failed")); }
            }
            Minecraft.getInstance().setScreen(returnTo);
        }, t(export ? "export" : "import"), t("transfer_warning")));
    }
}
