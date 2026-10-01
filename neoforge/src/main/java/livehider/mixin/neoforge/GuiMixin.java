package livehider.mixin.neoforge;

import livehider.LiveHider;
import livehider.component.AllDefaultOverlayComponents;
import livehider.text.ItemNameNormalizer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/** Routes every NeoForge 26.1 GUI layer belonging to the main HUD to the overlay. */
@Mixin(Gui.class)
public class GuiMixin {
    @WrapOperation(method = "extractSelectedItemName(Lnet/minecraft/client/gui/GuiGraphicsExtractor;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHoverName()Lnet/minecraft/network/chat/Component;"))
    private Component normalizeSelectedItemName(ItemStack stack, Operation<Component> original) {
        // Keep other mods' name hooks in the chain, even when replacing the displayed name.
        Component name = original.call(stack);
        return ItemNameNormalizer.shouldNormalize(stack) ? ItemNameNormalizer.displayName(stack) : name;
    }

    @ModifyVariable(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor scoreboard(GuiGraphicsExtractor value) { return graphics(AllDefaultOverlayComponents.scoreboards, value); }
    @ModifyVariable(method = "extractOverlayMessage(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor actionbar(GuiGraphicsExtractor value) { return graphics(AllDefaultOverlayComponents.actionbar, value); }
    @ModifyVariable(method = "extractTitle(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor title(GuiGraphicsExtractor value) { return graphics(AllDefaultOverlayComponents.titleSubtitle, value); }
    @ModifyVariable(method = "extractEffects(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor effects(GuiGraphicsExtractor value) { return graphics(AllDefaultOverlayComponents.effects, value); }

    @ModifyVariable(method = "extractHotbar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor hotbar(GuiGraphicsExtractor value) { return mainHud(value); }
    @ModifyVariable(method = "extractPlayerHealth(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor playerHealth(GuiGraphicsExtractor value) { return mainHud(value); }
    @ModifyVariable(method = "extractHealthLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor health(GuiGraphicsExtractor value) { return mainHud(value); }
    @ModifyVariable(method = "extractArmorLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor armor(GuiGraphicsExtractor value) { return mainHud(value); }
    @ModifyVariable(method = "extractFoodLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor food(GuiGraphicsExtractor value) { return mainHud(value); }
    @ModifyVariable(method = "extractAirLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor air(GuiGraphicsExtractor value) { return mainHud(value); }
    @ModifyVariable(method = "extractVehicleHealth(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor vehicleHealth(GuiGraphicsExtractor value) { return mainHud(value); }
    @ModifyVariable(method = "extractContextualInfoBarBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor experienceBar(GuiGraphicsExtractor value) { return mainHud(value); }
    @ModifyVariable(method = "extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor experienceLevel(GuiGraphicsExtractor value) { return mainHud(value); }
    @ModifyVariable(method = "extractContextualInfoBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), argsOnly = true)
    private GuiGraphicsExtractor contextualInfo(GuiGraphicsExtractor value) { return mainHud(value); }

    private static GuiGraphicsExtractor mainHud(GuiGraphicsExtractor value) { return graphics(AllDefaultOverlayComponents.mainHud, value); }
    private static GuiGraphicsExtractor graphics(livehider.component.IOverlayComponent component, GuiGraphicsExtractor value) {
        return LiveHider.getAPI().getGuiGraphics(component, value);
    }
}
