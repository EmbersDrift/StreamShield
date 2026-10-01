package livehider.client;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * A Cloth Config list entry that renders a vanilla {@link Button}; clicking it invokes
 * {@code onClick}. Used for the scoreboard "add rule" and per-rule "delete" actions. The button is
 * returned from {@link #children()} so the container forwards mouse/keyboard to it, and it is drawn in
 * {@link #render(GuiGraphics, int, int, int, int, int, int, int, boolean, float)}.
 */
public class ActionButtonEntry extends AbstractConfigListEntry<Void> {
    private final Component buttonText;
    private final Runnable onClick;
    private final BooleanSupplier enabled;
    private Button button;
    private int lastLeft;
    private int lastTop;
    private int lastWidth;
    private int lastItemHeight;

    public ActionButtonEntry(Component fieldName, Component buttonText, Runnable onClick) {
        this(fieldName, buttonText, onClick, () -> true);
    }

    public ActionButtonEntry(Component fieldName, Component buttonText, Runnable onClick, BooleanSupplier enabled) {
        super(fieldName, false);
        this.buttonText = buttonText;
        this.onClick = onClick;
        this.enabled = enabled;
    }

    @Override
    public boolean isRequiresRestart() {
        return false;
    }

    @Override
    public void setRequiresRestart(boolean value) {
    }

    @Override
    public Component getFieldName() {
        return super.getFieldName();
    }

    @Override
    public Optional<Void> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        int w = Math.min(160, Math.max(80, lastWidth - 10));
        int x = lastLeft + (lastWidth - w) / 2;
        int y = lastTop + (lastItemHeight - 20) / 2;
        if (button == null) {
            button = Button.builder(buttonText, b -> onClick.run()).bounds(x, y, w, 20).build();
        } else {
            button.setX(x);
            button.setY(y);
            button.setWidth(w);
        }
        button.active = enabled.getAsBoolean();
        return java.util.Collections.singletonList(button);
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        if (button == null) {
            children();
        }
        return java.util.Collections.singletonList(button);
    }

    @Override
    public void render(GuiGraphics graphics, int index, int top, int left, int width, int itemHeight, int mouseX, int mouseY, boolean hovered, float partialTicks) {
        lastLeft = left;
        lastTop = top;
        lastWidth = width;
        lastItemHeight = itemHeight;
        int w = Math.min(160, Math.max(80, width - 10));
        int x = left + (width - w) / 2;
        int y = top + (itemHeight - 20) / 2;
        if (button == null) {
            button = Button.builder(buttonText, b -> onClick.run()).bounds(x, y, w, 20).build();
        } else {
            button.setX(x);
            button.setY(y);
            button.setWidth(w);
        }
        button.active = enabled.getAsBoolean();
        button.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public int getItemHeight() {
        return 24;
    }
}
