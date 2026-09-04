package livehider.mixin.components;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import livehider.skin.RandomSkinManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

/**
 * Skin obfuscation via {@code SkinManager.createLookup}, the shared spine both {@code PlayerInfo} and
 * {@code PlayerSkinRenderCache} use to resolve a skin. {@code STEVE} mode returns a supplier yielding
 * the default Steve skin — a real built-in resource, so it renders correctly.
 */
@Mixin(SkinManager.class)
public class PlayerSkinMixin {
    @ModifyReturnValue(method = "createLookup", at = @At("RETURN"))
    private Supplier<PlayerSkin> overrideSkinLookup(Supplier<PlayerSkin> original, GameProfile profile, boolean secure) {
        // Keep the wrapper even while OFF so switching modes affects existing cached lookups.
        return () -> {
            // Some servers/client-side model extensions attach state while resolving the original
            // supplier. Always run it before substituting the final texture; skipping it made heads
            // disappear on servers with such skin/model integrations.
            PlayerSkin originalSkin = original.get();
            PlayerSkin custom = RandomSkinManager.getSkin((SkinManager) (Object) this, profile);
            return custom != null ? custom : originalSkin;
        };
    }
}
