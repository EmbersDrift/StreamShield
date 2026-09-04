package livehider.mixin.neoforge;

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
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * NeoForge 1.21.11 registers the vanilla HUD as separate GUI layers.  Unlike
 * Fabric's single method, health, food, air and experience do not pass through
 * {@code renderHotbar}, so every main-HUD layer must be redirected explicitly.
 */
@Mixin(Gui.class)
public class GuiMixin {
    @Redirect(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;"))
    private Component normalizeSelectedItemName(ItemStack stack) {
        return ItemNameNormalizer.displayName(stack);
    }

    @ModifyVariable(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphics drawStartScoreboard(GuiGraphics value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.scoreboards, value);
    }

    @ModifyVariable(method = "renderOverlayMessage(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
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

    @ModifyVariable(method = "renderHotbar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphics drawStartMainHud(GuiGraphics value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.mainHud, value);
    }

    @ModifyVariable(method = "renderPlayerHealth(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphics drawPlayerHealth(GuiGraphics value) {
        return mainHudGraphics(value);
    }

    @ModifyVariable(method = "renderHealthLevel(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphics drawHealthLevel(GuiGraphics value) {
        return mainHudGraphics(value);
    }

    @ModifyVariable(method = "renderArmorLevel(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphics drawArmorLevel(GuiGraphics value) {
        return mainHudGraphics(value);
    }

    @ModifyVariable(method = "renderFoodLevel(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphics drawFoodLevel(GuiGraphics value) {
        return mainHudGraphics(value);
    }

    @ModifyVariable(method = "renderAirLevel(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphics drawAirLevel(GuiGraphics value) {
        return mainHudGraphics(value);
    }

    @ModifyVariable(method = "renderVehicleHealth(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphics drawVehicleHealth(GuiGraphics value) {
        return mainHudGraphics(value);
    }

    @ModifyVariable(method = "renderContextualInfoBarBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphics drawExperienceBar(GuiGraphics value) {
        return mainHudGraphics(value);
    }

    @ModifyVariable(method = "renderExperienceLevel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphics drawExperienceLevel(GuiGraphics value) {
        return mainHudGraphics(value);
    }

    @ModifyVariable(method = "renderContextualInfoBar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphics drawContextualInfoBar(GuiGraphics value) {
        return mainHudGraphics(value);
    }

    private static GuiGraphics mainHudGraphics(GuiGraphics value) {
        return LiveHider.getAPI().getGuiGraphics(AllDefaultOverlayComponents.mainHud, value);
    }
}
