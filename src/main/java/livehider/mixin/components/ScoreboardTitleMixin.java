package livehider.mixin.components;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import livehider.text.SafeText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Redacts/ anonymizes the scoreboard sidebar title (objective display name), so a server
 * IP/name or sensitive keyword shown there is replaced before it is rendered.
 */
@Mixin(Objective.class)
public class ScoreboardTitleMixin {
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Component redactTitle(Component original) {
        return SafeText.rewriteScoreboard(original);
    }
}
