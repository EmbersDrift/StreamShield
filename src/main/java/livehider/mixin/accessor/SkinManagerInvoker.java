package livehider.mixin.accessor;

import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Exposes the package-private {@link SkinManager#registerTextures(UUID, MinecraftProfileTextures)}, the
 * vanilla method that downloads a real player's skin, registers it into the {@code TextureManager} and
 * builds a renderable {@link PlayerSkin}. Reusing it (instead of hand-registering a texture) guarantees
 * the random skin renders correctly rather than as a purple/black missing texture.
 */
@Mixin(SkinManager.class)
public interface SkinManagerInvoker {
    @Invoker("registerTextures")
    CompletableFuture<PlayerSkin> liveHider$registerTextures(UUID ownerId, MinecraftProfileTextures textures);
}
