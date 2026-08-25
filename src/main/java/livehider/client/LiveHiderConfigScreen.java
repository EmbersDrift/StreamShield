package livehider.client;

import livehider.LiveHiderConfig;
import livehider.ScoreboardRule;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * In-game configuration screen built with Cloth Config. All labels use {@code translatable} keys so the
 * UI follows the game language (zh_cn / en_us). Scoreboard rules render as collapsible sub-categories;
 * rules chain in list order. Overlay components are listed from the registry; skin mode sits in General.
 */
public final class LiveHiderConfigScreen {
    private static Screen parentScreen;

    private LiveHiderConfigScreen() {
    }

    private static Component t(String key) {
        return Component.translatable(key);
    }

    private static Component t(String key, Object... args) {
        return Component.translatable(key, args);
    }

    /** Recreate the config screen immediately (used after add/delete rule so ordering refreshes). */
    private static void reopen() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc != null) {
            mc.setScreen(create(parentScreen));
        }
    }

    /** Extract the last path segment (after the final '.') from a component id, e.g. "live_hider.live_hider.debug_menu" -> "debug_menu". */
    private static String lastSegment(String id) {
        int dot = id.lastIndexOf('.');
        return dot >= 0 ? id.substring(dot + 1) : id;
    }

    public static Screen create(Screen parent) {
        parentScreen = parent;
        LiveHiderConfig config = LiveHiderConfig.get();
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(t("live_hider.title"));
        builder.setSavingRunnable(LiveHiderConfig::save);

        ConfigEntryBuilder e = builder.entryBuilder();

        // ---- General / redaction + skin mode ----
        ConfigCategory general = builder.getOrCreateCategory(t("live_hider.category.general"));
        general.addEntry(e.startBooleanToggle(t("live_hider.redact_enabled"), config.redactEnabled)
            .setDefaultValue(true)
            .setTooltip(t("live_hider.redact_enabled.tooltip"))
            .setSaveConsumer(v -> config.redactEnabled = v)
            .build());
        general.addEntry(e.startTextField(t("live_hider.redact_replacement"), config.redactReplacement)
            .setDefaultValue("***")
            .setTooltip(t("live_hider.redact_replacement.tooltip"))
            .setSaveConsumer(v -> config.redactReplacement = v)
            .build());
        general.addEntry(e.startBooleanToggle(t("live_hider.auto_grab_server"), config.autoGrabServer)
            .setDefaultValue(true)
            .setTooltip(t("live_hider.auto_grab_server.tooltip"))
            .setSaveConsumer(v -> config.autoGrabServer = v)
            .build());
        general.addEntry(e.startBooleanToggle(t("live_hider.redact_preset"), config.redactPresetEnabled)
            .setDefaultValue(true)
            .setTooltip(t("live_hider.redact_preset.tooltip"))
            .setSaveConsumer(v -> config.redactPresetEnabled = v)
            .build());
        // The string-list works by clicking "Add" then typing into the box below; make that explicit.
        general.addEntry(e.startTextDescription(t("live_hider.redact_words.hint")).build());
        general.addEntry(e.startStrList(t("live_hider.redact_words"), config.redactPatterns)
            .setDefaultValue(new java.util.ArrayList<>())
            .setTooltip(t("live_hider.redact_words.tooltip"))
            .setSaveConsumer(v -> {
                config.redactPatterns = new java.util.ArrayList<>(v);
                livehider.text.Redactor.setup();
            })
            .build());
        general.addEntry(e.startBooleanToggle(t("live_hider.debug_log"), config.debugLog)
            .setDefaultValue(false)
            .setSaveConsumer(v -> config.debugLog = v)
            .build());

        // ---- Names ----
        ConfigCategory names = builder.getOrCreateCategory(t("live_hider.category.names"));
        names.addEntry(e.startBooleanToggle(t("live_hider.anonymize_names"), config.anonymizeNames)
            .setDefaultValue(true)
            .setTooltip(t("live_hider.anonymize_names.tooltip"))
            .setSaveConsumer(v -> config.anonymizeNames = v)
            .build());
        names.addEntry(e.startTextField(t("live_hider.name_template"), config.nameTemplate)
            .setDefaultValue("[Player]#")
            .setTooltip(t("live_hider.name_template.tooltip"))
            .setSaveConsumer(v -> config.nameTemplate = v)
            .build());
        names.addEntry(e.startIntField(t("live_hider.name_digits"), config.nameDigits)
            .setDefaultValue(4)
            .setTooltip(t("live_hider.name_digits.tooltip"))
            .setSaveConsumer(v -> config.nameDigits = v)
            .build());
        names.addEntry(e.startStringDropdownMenu(t("live_hider.self_name_mode"), config.selfNameMode)
            .setDefaultValue("HIDE")
            .setSelections(java.util.List.of("HIDE", "CUSTOM", "OWN", "RANDOM"))
            .setTooltip(t("live_hider.self_name_mode.tooltip"))
            .setSaveConsumer(v -> config.selfNameMode = v)
            .build());
        names.addEntry(e.startTextField(t("live_hider.self_custom_name"), config.selfCustomName)
            .setDefaultValue("")
            .setTooltip(t("live_hider.self_custom_name.tooltip"))
            .setSaveConsumer(v -> config.selfCustomName = v)
            .build());
        names.addEntry(e.startBooleanToggle(t("live_hider.sanitize_chat"), config.sanitizeChatInput)
            .setDefaultValue(true)
            .setTooltip(t("live_hider.sanitize_chat.tooltip"))
            .setSaveConsumer(v -> config.sanitizeChatInput = v)
            .build());
        // Skin obfuscation grouped under names (it hides what a player's skin could reveal).
        names.addEntry(e.startStringDropdownMenu(t("live_hider.skin_mode"), config.skinMode)
            .setDefaultValue("OFF")
            .setSelections(java.util.List.of("OFF", "STEVE"))
            .setTooltip(t("live_hider.skin_mode.tooltip"))
            .setSaveConsumer(v -> config.skinMode = v)
            .build());

        // ---- Items ----
        ConfigCategory items = builder.getOrCreateCategory(t("live_hider.category.items"));
        items.addEntry(e.startBooleanToggle(t("live_hider.normalize_items"), config.normalizeItemNames)
            .setDefaultValue(true)
            .setTooltip(t("live_hider.normalize_items.tooltip"))
            .setSaveConsumer(v -> config.normalizeItemNames = v)
            .build());

        // ---- Scoreboard chain rules ----
        ConfigCategory scoreboard = builder.getOrCreateCategory(t("live_hider.category.scoreboard"));
        scoreboard.addEntry(e.startBooleanToggle(t("live_hider.scoreboard.enabled_group"), config.scoreboardEnabled)
            .setDefaultValue(true)
            .setSaveConsumer(v -> config.scoreboardEnabled = v)
            .setTooltip(t("live_hider.scoreboard.enabled_group.tooltip"))
            .build());
        scoreboard.addEntry(e.startTextDescription(t("live_hider.scoreboard.desc")).build());
        scoreboard.addEntry(e.startTextDescription(t("live_hider.scoreboard.disclaimer")).build());
        for (int i = 0; i < config.scoreboardRules.size(); i++) {
            ScoreboardRule rule = config.scoreboardRules.get(i);
            final int idx = i;
            SubCategoryBuilder sub = e.startSubCategory(t("live_hider.scoreboard.rule", i + 1));
            sub.setExpanded(false);
            sub.add(e.startBooleanToggle(t("live_hider.scoreboard.enabled"), rule.enabled)
                .setDefaultValue(true)
                .setTooltip(t("live_hider.scoreboard.enabled.tooltip"))
                .setSaveConsumer(v -> config.scoreboardRules.get(idx).enabled = v)
                .build());
            sub.add(e.startTextField(t("live_hider.scoreboard.key"), rule.key)
                .setDefaultValue("")
                .setTooltip(t("live_hider.scoreboard.key.tooltip"))
                .setSaveConsumer(v -> config.scoreboardRules.get(idx).key = v)
                .build());
            sub.add(e.startTextField(t("live_hider.scoreboard.replacement"), rule.replacement)
                .setDefaultValue("")
                .setTooltip(t("live_hider.scoreboard.replacement.tooltip"))
                .setSaveConsumer(v -> config.scoreboardRules.get(idx).replacement = v)
                .build());
            // Delete button for this rule.
            sub.add(new ActionButtonEntry(
                t("live_hider.scoreboard.delete_rule"),
                t("live_hider.scoreboard.delete_rule"),
                () -> {
                    if (idx < config.scoreboardRules.size()) {
                        config.scoreboardRules.remove(idx);
                        reopen();
                    }
                }));
            scoreboard.addEntry(sub.build());
        }
        // Add rule button (as an entry row, not a sub-category).
        scoreboard.addEntry(new ActionButtonEntry(
            t("live_hider.scoreboard.add_rule"),
            t("live_hider.scoreboard.add_rule"),
            () -> {
                config.scoreboardRules.add(new ScoreboardRule(true, "", ""));
                livehider.LiveHider.LOGGER.info("[LiveHider] Add rule clicked, size now {}", config.scoreboardRules.size());
                reopen();
            }));

        // ---- Overlay (obs-overlay HUD concealer) ----
        ConfigCategory overlayCat = builder.getOrCreateCategory(t("live_hider.category.overlay"));
        overlayCat.addEntry(e.startBooleanToggle(t("live_hider.overlay.hide_all_screens"), config.hideAllScreens)
            .setDefaultValue(false)
            .setSaveConsumer(v -> config.hideAllScreens = v)
            .build());
        for (livehider.component.IOverlayComponent comp : livehider.component.OverlayComponentRegistry.components) {
            String labelKey = "live_hider.overlay." + lastSegment(comp.getId());
            overlayCat.addEntry(e.startBooleanToggle(Component.translatable(labelKey), comp.isOverlayEnabled())
                .setDefaultValue(comp.isOverlayEnabledDefault())
                .setTooltip(Component.translatable(labelKey + ".tooltip"))
                .setSaveConsumer(comp::setOverlayEnabled)
                .build());
            if (comp.canAutoHide()) {
                overlayCat.addEntry(e.startBooleanToggle(
                        Component.translatable(labelKey + ".autohide"), comp.isAutoHideEnabled())
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable(labelKey + ".autohide.tooltip"))
                    .setSaveConsumer(comp::setAutoHideEnabled)
                    .build());
            }
        }
        overlayCat.addEntry(e.startBooleanToggle(t("live_hider.overlay.show_test_icon"), config.showTestIcon)
            .setDefaultValue(false)
            .setSaveConsumer(v -> config.showTestIcon = v)
            .build());

        return builder.build();
    }
}
