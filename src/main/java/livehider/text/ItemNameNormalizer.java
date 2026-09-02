package livehider.text;

import livehider.LiveHiderConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-display-only normalization for renamed items. Never modifies an ItemStack or its core
 * name accessors: menus such as the anvil use those accessors as gameplay input.
 */
public final class ItemNameNormalizer {
    private ItemNameNormalizer() {
    }

    public static boolean shouldNormalize(ItemStack stack) {
        LiveHiderConfig config = LiveHiderConfig.get();
        return config != null && config.normalizeItemNames && stack != null && stack.getCustomName() != null;
    }

    /** Returns the vanilla localized item name, e.g. Chinese when the client language is Chinese. */
    public static Component displayName(ItemStack stack) {
        return shouldNormalize(stack) ? stack.getItemName() : stack.getHoverName();
    }

    /** Replaces only the title line of a client tooltip, preserving all other tooltip content. */
    public static List<Component> normalizeTooltip(List<Component> tooltip, ItemStack stack) {
        if (!shouldNormalize(stack) || tooltip.isEmpty()) {
            return tooltip;
        }
        List<Component> normalized = new ArrayList<>(tooltip);
        normalized.set(0, stack.getItemName().copy().withStyle(stack.getRarity().color()));
        return normalized;
    }
}
