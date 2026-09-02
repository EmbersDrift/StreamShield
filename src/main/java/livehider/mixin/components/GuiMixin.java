package livehider.mixin.components;

import livehider.LiveHider;
import livehider.component.AllDefaultOverlayComponents;
import livehider.text.ItemNameNormalizer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Redirects scoreboard/actionbar/title/effects/main-HUD onto the overlay target.
 * 26.1 renamed the per-feature Gui draw methods to {@code extract*} and switched
 * the draw context from GuiGraphics to GuiGraphicsExtractor.
 * Ported from obs-overlay (MIT, author zziger).
 */
@Mixin(Gui.class)
public class GuiMixin {
    @Redirect(
        method = "extractSelectedItemName(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;")
    )
    private Component normalizeSelectedItemName(ItemStack stack) {
        return ItemNameNormalizer.displayName(stack);
    }

    @ModifyVariable(
        method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private GuiGraphicsExtractor drawStartScoreboard(GuiGraphicsExtractor value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.scoreboards, value);
    }

    @ModifyVariable(
        method = "extractOverlayMessage(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private GuiGraphicsExtractor drawStartActionbar(GuiGraphicsExtractor value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.actionbar, value);
    }

    @ModifyVariable(method = "extractTitle(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor drawStartTitleSubtitle(GuiGraphicsExtractor value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.titleSubtitle, value);
    }

    @ModifyVariable(method = "extractEffects(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor drawStartEffects(GuiGraphicsExtractor value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.effects, value);
    }

    @ModifyVariable(
        method = "extractHotbarAndDecorations(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private GuiGraphicsExtractor drawStartMainHud(GuiGraphicsExtractor value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.mainHud, value);
    }
}
