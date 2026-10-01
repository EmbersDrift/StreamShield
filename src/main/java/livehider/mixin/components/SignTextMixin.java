package livehider.mixin.components;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.Function;
import livehider.text.SignTextFilter;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Shared by standing, wall, hanging and wall-hanging signs, for both faces. */
@Mixin(AbstractSignRenderer.class)
public abstract class SignTextMixin {
    @WrapOperation(method = "submitSignText", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/entity/SignText;getRenderMessages(ZLjava/util/function/Function;)[Lnet/minecraft/util/FormattedCharSequence;"))
    private FormattedCharSequence[] liveHider$filterSign(SignText text, boolean filtered,
            Function<Component, FormattedCharSequence> formatter, Operation<FormattedCharSequence[]> original) {
        return original.call(SignTextFilter.forDisplay(text, filtered), filtered, formatter);
    }
}
