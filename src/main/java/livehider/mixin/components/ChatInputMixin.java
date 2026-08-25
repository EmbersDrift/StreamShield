package livehider.mixin.components;

import livehider.text.NameAnonymizer;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Desensitizes the chat input box <em>display</em> by hooking the EditBox formatter entrypoint
 * ({@code applyFormat}), so a typed real player name is shown anonymized on screen (and thus not
 * leaked to OBS) while the underlying input value (and thus the sent message) keeps the real string.
 */
@Mixin(EditBox.class)
public class ChatInputMixin {
    @ModifyVariable(method = "applyFormat(Ljava/lang/String;I)Lnet/minecraft/util/FormattedCharSequence;", at = @At("HEAD"), argsOnly = true, index = 1)
    private String anonInput(String string) {
        // While the reveal key is held, show the real text so the streamer can verify what they typed.
        if (NameAnonymizer.isRevealInput()) {
            return string;
        }
        return NameAnonymizer.applyToText(string);
    }
}
