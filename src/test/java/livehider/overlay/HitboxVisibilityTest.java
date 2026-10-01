package livehider.overlay;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HitboxVisibilityTest {
    @Test void onlyUnobstructedWorldAllowsHitboxes() {
        for (boolean world : new boolean[]{false, true})
            for (boolean screen : new boolean[]{false, true})
                for (boolean loading : new boolean[]{false, true})
                    assertEquals(world && !screen && !loading, HitboxVisibility.canDraw(world, screen, loading));
    }

    @Test void openingAndClosingScreenRestoresVisibility() {
        assertTrue(HitboxVisibility.canDraw(true, false, false));
        assertFalse(HitboxVisibility.canDraw(true, true, false));
        assertTrue(HitboxVisibility.canDraw(true, false, false));
    }
}
