package livehider.client;

import com.mojang.blaze3d.platform.InputConstants;
import livehider.text.NameAnonymizer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Registers the "hold to reveal real names in the chat input" keybinding.
 */
public final class LiveHiderKeybinds {
    private static KeyMapping revealKey;

    private LiveHiderKeybinds() {
    }

    public static void register() {
        revealKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.live_hider.reveal",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("live_hider", "keybinds"))
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            NameAnonymizer.setRevealInput(revealKey.isDown());
        });
    }
}
