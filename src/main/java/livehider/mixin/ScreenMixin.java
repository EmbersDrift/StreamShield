package livehider.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import livehider.LiveHiderConfig;
import livehider.text.ItemNameNormalizer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Cancels the blurred background of screens that should be hidden from the stream.
 * Ported from obs-overlay (MIT, author zziger).
 */
@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "extractBlurredBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At("HEAD"), cancellable = true)
    private void renderBlur(CallbackInfo ci) {
        if (LiveHiderConfig.isScreenOverlayed((Screen) (Object) this)) {
            ci.cancel();
        }
    }

    /** Tooltip construction is client-only, so normalization cannot affect container/anvil logic. */
    @ModifyReturnValue(method = "getTooltipFromItem", at = @At("RETURN"))
    private static List<Component> normalizeItemTooltip(List<Component> original, Minecraft minecraft, ItemStack stack) {
        return ItemNameNormalizer.normalizeTooltip(original, stack);
    }
}
