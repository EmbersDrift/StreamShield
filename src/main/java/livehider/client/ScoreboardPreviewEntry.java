package livehider.client;

import livehider.text.ScoreboardKeyPreview;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.function.Supplier;

/** Fixed-size clipped preview: resource packs may define enormous or negative-advance glyphs. */
final class ScoreboardPreviewEntry extends AbstractConfigListEntry<Void> {
    private final Supplier<String> key;
    private String lastKey;
    private Component preview;

    ScoreboardPreviewEntry(Supplier<String> key) {
        super(Component.translatable("live_hider.scoreboard.preview"), false);
        this.key = key;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int index, int top, int left,
            int width, int itemHeight, int mouseX, int mouseY, boolean hovered, float partialTicks) {
        String value = key.get();
        if (preview == null || !Objects.equals(value, lastKey)) {
            lastKey = value;
            preview = ScoreboardKeyPreview.create(value);
        }
        var font = Minecraft.getInstance().font;
        graphics.enableScissor(left, top, left + Math.max(0, width), top + getItemHeight());
        try {
            graphics.text(font, getFieldName(), left + 4, top + 3, 0xffaaaaaa);
            graphics.enableScissor(left + 4, top + 16, left + Math.max(4, width - 4), top + 68);
            try {
                graphics.text(font, preview, left + 8, top + 36, 0xffffffff);
            } finally {
                graphics.disableScissor();
            }
        } finally {
            graphics.disableScissor();
        }
    }

    @Override public int getItemHeight() { return 72; }
    @Override public Void getValue() { return null; }
    @Override public Optional<Void> getDefaultValue() { return Optional.empty(); }
    @Override public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() { return List.of(); }
    @Override public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() { return List.of(); }
}
