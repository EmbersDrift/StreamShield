package livehider.mixin.components;

import livehider.LiveHiderConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * When enabled, renamed items (anvil / name tag) display their bound registry name
 * (e.g. {@code netherite_sword}) instead of the custom NBT name. This is a stream-safety
 * feature: malicious/NSFW anvil-renamed item names are hidden from viewers.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getHoverName", at = @At("HEAD"), cancellable = true)
    private void normalizeHoverName(CallbackInfoReturnable<Component> cir) {
        if (!LiveHiderConfig.get().normalizeItemNames) {
            return;
        }
        ItemStack self = (ItemStack) (Object) this;
        if (self.getCustomName() != null) {
            Identifier id = BuiltInRegistries.ITEM.getKey(self.getItem());
            if (id != null) {
                cir.setReturnValue(Component.literal(id.getPath()));
            }
        }
    }

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void normalizeDisplayName(CallbackInfoReturnable<Component> cir) {
        if (!LiveHiderConfig.get().normalizeItemNames) {
            return;
        }
        ItemStack self = (ItemStack) (Object) this;
        if (self.getCustomName() != null) {
            Identifier id = BuiltInRegistries.ITEM.getKey(self.getItem());
            if (id != null) {
                cir.setReturnValue(Component.literal(id.getPath()));
            }
        }
    }
}
