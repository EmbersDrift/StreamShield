package livehider.component;

import livehider.LiveHider;
import livehider.component.type.HUDOverlayComponent;

/**
 * The default obs-overlay HUD components. Ported from obs-overlay (MIT, author zziger).
 */
public final class AllDefaultOverlayComponents {
    public static IOverlayComponent hitboxes = new HUDOverlayComponent(LiveHider.id("hitboxes"), false, false);
    public static IOverlayComponent debugMenu = new HUDOverlayComponent(LiveHider.id("debug_menu"), true, false);
    public static IOverlayComponent chat = new HUDOverlayComponent(LiveHider.id("chat"), false, true);
    public static IOverlayComponent chatBar = new HUDOverlayComponent(LiveHider.id("chat_bar"), false, false);
    public static IOverlayComponent playerList = new HUDOverlayComponent(LiveHider.id("player_list"), false, false);
    public static IOverlayComponent subtitles = new HUDOverlayComponent(LiveHider.id("subtitles"), false, true);
    public static IOverlayComponent scoreboards = new HUDOverlayComponent(LiveHider.id("scoreboards"), false, true);
    public static IOverlayComponent actionbar = new HUDOverlayComponent(LiveHider.id("actionbar"), false, true);
    public static IOverlayComponent titleSubtitle = new HUDOverlayComponent(LiveHider.id("title_subtitle"), false, true);
    public static IOverlayComponent effects = new HUDOverlayComponent(LiveHider.id("effects"), false, true);
    public static IOverlayComponent mainHud = new HUDOverlayComponent(LiveHider.id("main_hud"), false, true);

    private AllDefaultOverlayComponents() {
    }

    public static void init() {
        OverlayComponentRegistry.registerComponents(debugMenu, chat, chatBar, playerList, subtitles, scoreboards, actionbar, titleSubtitle, effects, mainHud, hitboxes);
    }
}
