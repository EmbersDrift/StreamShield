package livehider.neoforge;

import livehider.LiveHider;
import livehider.client.LiveHiderConfigScreen;
import livehider.skin.RandomSkinManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = LiveHider.ID, dist = Dist.CLIENT)
public final class NeoForgeLiveHider {
    public NeoForgeLiveHider(IEventBus modBus, ModContainer container) {
        // Overlay mixins can run during the first rendered frame, before the
        // client-setup event is dispatched. Initialise their configuration now.
        LiveHider.init();
        NeoForgeConfigWatcher.start();
        RandomSkinManager.start();

        modBus.addListener(NeoForgeKeybinds::register);
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (mod, parent) -> LiveHiderConfigScreen.create(parent));
    }
}
