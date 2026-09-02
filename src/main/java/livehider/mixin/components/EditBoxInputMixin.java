package livehider.mixin.components;

import livehider.client.LiveHiderConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.StringUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

/**
 * Lets the legacy {@code §} colour/format code be typed or pasted into an {@link EditBox}. This is used
 * by the Cloth Config text fields that hold scoreboard key/replacement values.
 *
 * <p>Vanilla {@code EditBox.insertText} always runs {@link StringUtil#filterText}, which strips {@code §}
 * because it is not a valid chat character — so the config GUI could never accept a colour code. We
 * intercept exactly that call site and, when the inserted text contains {@code §}, return the original
 * text instead of the filtered copy, so {@code §d§l布吉岛§r} survives into the stored value (it is later
 * parsed into styled runs at render time). All other text is sanitised exactly as before.</p>
 */
@Mixin(EditBox.class)
public class EditBoxInputMixin {
    @ModifyExpressionValue(
            method = "insertText",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StringUtil;filterText(Ljava/lang/String;)Ljava/lang/String;")
    )
    private String preserveFormattingCodes(String filtered, String text) {
        if (text != null
            && text.indexOf('\u00A7') >= 0
            && LiveHiderConfigScreen.isActiveConfigScreen(Minecraft.getInstance().screen)) {
            return text;
        }
        return filtered;
    }
}
