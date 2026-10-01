package livehider.mixin.components;

import livehider.LiveHider;
import livehider.component.AllDefaultOverlayComponents;
import livehider.text.ItemNameNormalizer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/**
 * Redirects scoreboard/actionbar/title/effects/main-HUD onto the overlay target.
 * Ported from obs-overlay (MIT, author zziger).
 */
@Mixin(Gui.class)
public class GuiMixin {
    @WrapOperation(
        method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;")
    )
    private Component normalizeSelectedItemName(ItemStack stack, Operation<Component> original) {
        // Keep other mods' name hooks in the chain, even when replacing the displayed name.
        Component name = original.call(stack);
        return ItemNameNormalizer.shouldNormalize(stack) ? ItemNameNormalizer.displayName(stack) : name;
    }

    @ModifyVariable(
        method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private GuiGraphics drawStartScoreboard(GuiGraphics value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.scoreboards, value);
    }

    @ModifyVariable(
        method = "renderOverlayMessage(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private GuiGraphics drawStartActionbar(GuiGraphics value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.actionbar, value);
    }

    @ModifyVariable(method = "renderTitle(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphics drawStartTitleSubtitle(GuiGraphics value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.titleSubtitle, value);
    }

    @ModifyVariable(method = "renderEffects(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphics drawStartEffects(GuiGraphics value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.effects, value);
    }

    @ModifyVariable(
        method = "renderHotbarAndDecorations(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private GuiGraphics drawStartMainHud(GuiGraphics value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.mainHud, value);
    }
}
