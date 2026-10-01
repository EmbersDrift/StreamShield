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
        return config != null && config.normalizeItemNames && stack != null && !stack.isEmpty();
    }

    /** Returns the vanilla localized item name, e.g. Chinese when the client language is Chinese. */
    public static Component displayName(ItemStack stack) {
        return shouldNormalize(stack) ? resourceName(stack) : stack.getHoverName();
    }

    /** Resolve through the current Language, including resource-pack translations; never cache a literal. */
    static Component resourceName(ItemStack stack) {
        return resourceName(stack.getItem().getDescriptionId());
    }

    static Component resourceName(String translationKey) {
        return Component.translatable(translationKey);
    }

    /** Replaces only the title line of a client tooltip, preserving all other tooltip content. */
    public static List<Component> normalizeTooltip(List<Component> tooltip, ItemStack stack) {
        if (!shouldNormalize(stack) || tooltip.isEmpty()) {
            return tooltip;
        }
        List<Component> normalized = new ArrayList<>(tooltip);
        normalized.set(0, resourceName(stack).copy().withStyle(stack.getRarity().color()));
        return normalized;
    }
}
