package livehider.mixin.components;

import livehider.text.NameAnonymizer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Anonymizes entity name tags (other players globally; the local player per its self-name mode).
 */
@Mixin(EntityRenderer.class)
public abstract class NameTagMixin {
    @Inject(method = "getNameTag(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/chat/Component;", at = @At("RETURN"), cancellable = true)
    private void anonNameTag(Entity entity, CallbackInfoReturnable<Component> cir) {
        Component name = cir.getReturnValue();
        if (name == null) {
            return;
        }
        // Only player nametags can match the player-name map. Skipping mobs avoids running the
        // anonymizer for every custom-named entity in dense farms/lobbies.
        if (!(entity instanceof Player player)) {
            return;
        }
        boolean isSelf = entity == Minecraft.getInstance().player;
        Component rewritten = NameAnonymizer.applyPlayerDisplayName(name, player.getUUID(), isSelf);
        if (rewritten != name) {
            cir.setReturnValue(rewritten);
        }
    }
}
