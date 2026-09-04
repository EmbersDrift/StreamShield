package livehider.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import livehider.text.NameAnonymizer;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

public final class NeoForgeKeybinds {
    private static final KeyMapping REVEAL_KEY = new KeyMapping(
        "key.live_hider.reveal",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_LEFT_ALT,
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("live_hider", "keybinds"))
    );

    private NeoForgeKeybinds() {
    }

    static void register(RegisterKeyMappingsEvent event) {
        event.register(REVEAL_KEY);
    }

    @EventBusSubscriber(value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            NameAnonymizer.setRevealInput(REVEAL_KEY.isDown());
            NeoForgeConfigWatcher.tick();
        }
    }
}
