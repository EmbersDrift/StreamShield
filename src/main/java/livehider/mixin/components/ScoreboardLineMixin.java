package livehider.mixin.components;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import livehider.text.SafeText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Rewrites the assembled sidebar-row text. Each visible sidebar row is produced (in
 * {@code Gui.extractScoreboardSidebar}) as {@code PlayerTeam.formatNameForTeam(team, ownerName())} and then
 * stored into an immutable {@code Gui$1DisplayEntry} record — so the record's field cannot be
 * changed after construction. Instead we rewrite the value at its source: the
 * {@code formatNameForTeam} return, a writable normal method return. In 26.1 the first parameter
 * type widened from {@code PlayerTeam} to {@code Team}.
 *
 * <p>We use {@link SafeText#rewriteScoreboardRecord} which rewrites only the plain-text leaves and
 * preserves the component structure/style, so server resource-pack icon siblings keep rendering as
 * their custom glyphs instead of degrading to boxes. {@code formatNameForTeam} is {@code static}, so
 * the handler must be {@code static} too.
 */
@Mixin(PlayerTeam.class)
public class ScoreboardLineMixin {
    @ModifyReturnValue(method = "formatNameForTeam(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;", at = @At("RETURN"))
    private static MutableComponent rewriteRowText(MutableComponent original) {
        if (original == null) {
            return original;
        }
        net.minecraft.network.chat.Component rewritten = SafeText.rewriteScoreboardRecord(original);
        return rewritten instanceof MutableComponent mc ? mc : original;
    }
}
