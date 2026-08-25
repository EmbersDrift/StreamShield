package livehider.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

/**
 * Integrates the {@link LiveHiderConfigScreen} into Mod Menu, so the mod appears with a "Configure"
 * button in the mods list. Only loads when Mod Menu is present (modmenu is an optional dependency).
 */
public class LiveHiderModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> createScreen(parent);
    }

    private static Screen createScreen(Screen parent) {
        return LiveHiderConfigScreen.create(parent);
    }
}
