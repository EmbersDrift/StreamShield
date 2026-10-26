package livehider.overlay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OverlayGuiFrameTest {
    @Test
    void cachedHudFramesKeepTheCompletedTargetAvailableForEverySwap() {
        var frame = new OverlayGuiFrame();
        frame.beginFrame(true);
        assertTrue(frame.needsRender());
        assertTrue(frame.beginDraw());
        frame.endDraw();

        for (int i = 0; i < 10; i++) {
            frame.beginFrame(false);
            assertFalse(frame.needsRender());
            assertTrue(frame.canPresent());
            assertTrue(frame.canPresent());
        }
    }

    @Test
    void aRealEmptyHudUpdateStillClearsThePreviousSnapshot() {
        var frame = completedFrame();
        frame.beginFrame(false);
        frame.extracted();
        assertTrue(frame.needsRender());
        assertTrue(frame.beginDraw());
        assertFalse(frame.canPresent());
        frame.endDraw();
        assertTrue(frame.canPresent());
    }

    @Test
    void screenWorldAndHudVisibilityChangesInvalidateOldPixels() {
        var frame = completedFrame();
        frame.beginFrame(true);
        assertFalse(frame.canPresent());
        assertTrue(frame.needsRender());
        frame.beginFrame(false);
        assertFalse(frame.canPresent());
        assertTrue(frame.needsRender());
    }

    @Test
    void privateGuiAndRedirectedHudShareOneClearWithinAFrame() {
        var frame = new OverlayGuiFrame();
        frame.beginFrame(true);
        assertTrue(frame.beginDraw());
        frame.endDraw();
        assertFalse(frame.beginDraw());
        frame.endDraw();
        assertTrue(frame.canPresent());
    }

    @Test
    void resizingRequiresANewCompletedTarget() {
        var frame = completedFrame();
        frame.invalidate();
        assertFalse(frame.canPresent());
        frame.beginFrame(false);
        assertTrue(frame.needsRender());
        assertTrue(frame.beginDraw());
        frame.endDraw();
        assertTrue(frame.canPresent());
    }

    private static OverlayGuiFrame completedFrame() {
        var frame = new OverlayGuiFrame();
        frame.beginFrame(true);
        frame.beginDraw();
        frame.endDraw();
        return frame;
    }
}
