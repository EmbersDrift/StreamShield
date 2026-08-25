package livehider.overlay;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.List;
import livehider.mixin.accessor.GuiGraphicsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.model.Model.Simple;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.profiling.ResultField;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A no-op GuiGraphics used when a hidden component should draw nothing at all.
 * Ported from obs-overlay (MIT, author zziger).
 */
public class DummyGuiGraphics extends GuiGraphics {
    public static final DummyGuiGraphics INSTANCE = new DummyGuiGraphics();

    private DummyGuiGraphics() {
        super(Minecraft.getInstance(), new GuiRenderState(), 0, 0);
    }

    public void nextStratum() {
        GuiGraphicsAccessor accessor = (GuiGraphicsAccessor) this;
        accessor.getGuiRenderState().reset();
    }

    public void blurBeforeThisStratum() {
    }

    public void fill(RenderPipeline pipeline, int minX, int minY, int maxX, int maxY, int color) {
    }

    public void fillGradient(int minX, int minY, int maxX, int maxY, int colorFrom, int colorTo) {
    }

    public void fill(RenderPipeline pipeline, TextureSetup textureSetup, int minX, int minY, int maxX, int maxY) {
    }

    public void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color) {
    }

    public void blitSprite(
        RenderPipeline pipeline, Identifier sprite, int textureWidth, int textureHeight, int u, int v, int x, int y, int width, int height, int color
    ) {
    }

    public void blitSprite(RenderPipeline pipeline, TextureAtlasSprite sprite, int x, int y, int width, int height, int color) {
    }

    public void blit(
        RenderPipeline pipeline,
        Identifier atlas,
        int x,
        int y,
        float u,
        float v,
        int width,
        int height,
        int uWidth,
        int vHeight,
        int textureWidth,
        int textureHeight,
        int color
    ) {
    }

    public void drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean drawShadow) {
    }

    public void submitMapRenderState(MapRenderState renderState) {
    }

    public void submitEntityRenderState(
        EntityRenderState renderState, float scale, Vector3f translation, Quaternionf rotation, Quaternionf overrideCameraAngle, int x0, int y0, int x1, int y1
    ) {
    }

    public void submitSkinRenderState(
        PlayerModel playerModel, Identifier texture, float rotationX, float rotationY, float pivotY, float x0, int y0, int x1, int y1, int scale
    ) {
    }

    public void submitBookModelRenderState(BookModel bookModel, Identifier texture, float open, float flip, float x0, int y0, int x1, int y1, int scale) {
    }

    public void submitBannerPatternRenderState(
        BannerFlagModel flag, DyeColor baseColor, BannerPatternLayers resultBannerPatterns, int x0, int y0, int x1, int y1
    ) {
    }

    public void submitSignRenderState(Simple signModel, float scale, WoodType woodType, int x0, int y0, int x1, int y1) {
    }

    public void submitProfilerChartRenderState(List<ResultField> chartData, int x0, int y0, int x1, int y1) {
    }
}
