package livehider.client;

import com.mojang.blaze3d.platform.InputConstants;
import livehider.text.NameAnonymizer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * Registers the "hold to reveal real names in the chat input" keybinding.
 */
public final class LiveHiderKeybinds {
    private static KeyMapping revealKey;
    private static KeyMapping configKey;

    private LiveHiderKeybinds() {
    }

    public static void register() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("live_hider", "keybinds"));
        configKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.live_hider.open_config", InputConstants.Type.KEYBOARD, InputConstants.KEY_F8, category));
        revealKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.live_hider.reveal",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_LALT,
            category
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            NameAnonymizer.setRevealInput(revealKey.isDown());
            while (configKey.consumeClick()) {
                if (client.gui.screen() == null) client.gui.setScreen(LiveHiderConfigScreen.create(null));
            }
        });
    }
}
