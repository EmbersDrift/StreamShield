package livehider;

import livehider.client.LiveHiderConfigWatcher;
import livehider.client.LiveHiderKeybinds;
import livehider.skin.RandomSkinManager;
import livehider.overlay.OverlayPipelines;
import net.fabricmc.api.ClientModInitializer;

public class LiveHiderClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LiveHider.LOGGER.info("Live Hider initializing");
        OverlayPipelines.registerWithMinecraft();
        LiveHider.init();
        LiveHiderKeybinds.register();
        LiveHiderConfigWatcher.start();
        RandomSkinManager.start();
    }
}
