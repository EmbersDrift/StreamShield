package livehider.client;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/** Read-only, wrapped status text refreshed every frame, including while the game is paused. */
final class LiveStatusEntry extends AbstractConfigListEntry<Void> {
    private final Supplier<Component> text;
    private Component lastText;
    private int lastWidth = 240;
    private List<FormattedCharSequence> lines = List.of();

    LiveStatusEntry(Component label, Supplier<Component> text) {
        super(label, false);
        this.text = text;
    }

    private void refresh(int width) {
        Component current = text.get();
        int wrapWidth = Math.max(40, width);
        if (!current.equals(lastText) || wrapWidth != lastWidth) {
            lastText = current;
            lastWidth = wrapWidth;
            lines = Minecraft.getInstance().font.split(current, wrapWidth);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int index, int top, int left,
            int width, int itemHeight, int mouseX, int mouseY, boolean hovered, float partialTicks) {
        refresh(width);
        int y = top + 5;
        for (FormattedCharSequence line : lines) {
            graphics.text(Minecraft.getInstance().font, line, left, y, 0xFFFFFFFF);
            y += 12;
        }
    }

    @Override
    public int getItemHeight() {
        refresh(lastWidth);
        return Math.max(24, lines.size() * 12 + 10);
    }

    @Override
    public Void getValue() { return null; }

    @Override
    public Optional<Void> getDefaultValue() { return Optional.empty(); }

    @Override
    public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        return List.of();
    }

    @Override
    public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return List.of();
    }
}

