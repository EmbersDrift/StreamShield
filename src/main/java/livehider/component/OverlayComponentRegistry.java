package livehider.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * Registry of overlay components and screen-hiding targets.
 * Ported from obs-overlay (MIT, author zziger).
 */
public class OverlayComponentRegistry {
    public static ArrayList<IOverlayComponent> components = new ArrayList<>();
    public static ArrayList<Class<?>> ignoredScreens = new ArrayList<>(List.of(ChatScreen.class));
    public static HashMap<String, Class<?>> hideableScreens = new HashMap<String, Class<?>>() {
        {
            this.put("inventory", InventoryScreen.class);
            this.put("creative_inventory", CreativeModeInventoryScreen.class);
            this.put("pause_menu", PauseScreen.class);
            this.put("command_block", CommandBlockEditScreen.class);
        }
    };

    public static IOverlayComponent registerComponent(IOverlayComponent component) {
        components.add(component);
        return component;
    }

    public static void registerComponents(IOverlayComponent... components) {
        Stream.of(components).forEach(OverlayComponentRegistry::registerComponent);
    }

    public static void addIgnoredScreen(Class<?> screen) {
        ignoredScreens.add(screen);
    }

    public static void addHideableScreen(String id, Class<?> screen) {
        hideableScreens.put(id, screen);
    }
}
