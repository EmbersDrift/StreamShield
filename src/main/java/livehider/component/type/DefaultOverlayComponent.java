package livehider.component.type;

import livehider.LiveHiderConfig;
import livehider.component.IOverlayComponent;
import livehider.component.OverlayComponentRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/**
 * Base component with overlay/auto-hide state backed by config.
 * Ported from obs-overlay (MIT, author zziger).
 */
public abstract class DefaultOverlayComponent implements IOverlayComponent {
    private final Identifier id;
    private final boolean canAutoHide;
    private final boolean defaultOverlay;
    private boolean overlay;
    private boolean autoHide;

    public DefaultOverlayComponent(Identifier id, boolean defaultOverlay, boolean canAutoHide) {
        this.id = id;
        this.canAutoHide = canAutoHide;
        this.defaultOverlay = defaultOverlay;
        LiveHiderConfig config = LiveHiderConfig.get();
        this.overlay = config.overlayComponents.getOrDefault(this.getId(), defaultOverlay);
        this.autoHide = canAutoHide ? config.autoHideComponents.getOrDefault(this.getId(), true) : false;
    }

    @Override
    public boolean canAutoHide() {
        return this.canAutoHide;
    }

    @Override
    public String getId() {
        return this.id.toLanguageKey();
    }

    @Override
    public boolean isOverlayEnabledDefault() {
        return this.defaultOverlay;
    }

    @Override
    public boolean isOverlayEnabled() {
        return this.overlay;
    }

    @Override
    public void setOverlayEnabled(boolean value) {
        this.overlay = value;
        LiveHiderConfig.get().overlayComponents.put(this.getId(), value);
    }

    @Override
    public boolean isAutoHideEnabled() {
        return this.canAutoHide && this.autoHide;
    }

    @Override
    public void setAutoHideEnabled(boolean value) {
        if (this.canAutoHide) {
            this.autoHide = value;
            LiveHiderConfig.get().autoHideComponents.put(this.getId(), value);
        }
    }

    @Override
    public boolean isHidden() {
        if (!this.isAutoHideEnabled()) {
            return false;
        }
        Screen currentScreen = Minecraft.getInstance().gui.screen();
        return currentScreen != null && !OverlayComponentRegistry.ignoredScreens.contains(currentScreen.getClass());
    }
}
