package livehider.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import livehider.text.NameAnonymizer;
import livehider.client.LiveHiderConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

public final class NeoForgeKeybinds {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("live_hider", "keybinds"));
    private static final KeyMapping CONFIG_KEY = new KeyMapping(
        "key.live_hider.open_config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, CATEGORY);
    private static final KeyMapping EMERGENCY_KEY = new KeyMapping(
        "key.live_hider.emergency", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F9, CATEGORY);
    private static final KeyMapping REVEAL_KEY = new KeyMapping(
        "key.live_hider.reveal",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_LEFT_ALT,
        CATEGORY
    );

    private NeoForgeKeybinds() {
    }

    static void register(RegisterKeyMappingsEvent event) {
        event.register(REVEAL_KEY);
        event.register(CONFIG_KEY);
        event.register(EMERGENCY_KEY);
    }

    @EventBusSubscriber(value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            NameAnonymizer.setRevealInput(REVEAL_KEY.isDown());
            while (EMERGENCY_KEY.consumeClick()) livehider.client.PrivacyShield.toggleEmergency();
            Minecraft client = Minecraft.getInstance();
            while (CONFIG_KEY.consumeClick()) {
                if (client.screen == null) client.setScreen(LiveHiderConfigScreen.create(null));
            }
            NeoForgeConfigWatcher.tick();
        }
    }
}
