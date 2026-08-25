package livehider.overlay;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import org.jetbrains.annotations.NotNull;

/**
 * A screen-sized texture view used as the render pass output for the overlay composite.
 * Ported from obs-overlay (MIT, author zziger).
 */
public class OverlayScreenTextureView extends GlTextureView {
    private final int width;
    private final int height;

    public OverlayScreenTextureView(int width, int height) {
        super(new OverlayScreenTextureView.ScreenTexture(width, height), 0, 1);
        this.width = width;
        this.height = height;
    }

    public int getFbo(@NotNull DirectStateAccess dsa, GpuTexture depth) {
        return 0;
    }

    public void close() {
    }

    public boolean isClosed() {
        return false;
    }

    public int getWidth(int mipLevel) {
        return this.width;
    }

    public int getHeight(int mipLevel) {
        return this.height;
    }

    private static class ScreenTexture extends GlTexture {
        public ScreenTexture(int width, int height) {
            super(8, "Screen", TextureFormat.RGBA8, width, height, 1, 1, 0);
        }

        public void close() {
        }

        public boolean isClosed() {
            return false;
        }

        public void addViews() {
        }

        public void removeViews() {
        }
    }
}
