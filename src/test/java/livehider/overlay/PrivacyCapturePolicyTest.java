package livehider.overlay;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PrivacyCapturePolicyTest {
    @Test void unavailableOverlayNeverRendersPrivateGuiNormally() {
        assertEquals(PrivacyCapturePolicy.Route.BLOCK, PrivacyCapturePolicy.route(true, false, false));
        assertEquals(PrivacyCapturePolicy.Route.SAFE_RECOVERY, PrivacyCapturePolicy.route(true, false, true));
    }
    @Test void readyOverlayAndUnprotectedSessionsRouteCorrectly() {
        assertEquals(PrivacyCapturePolicy.Route.PRIVATE_OVERLAY, PrivacyCapturePolicy.route(true, true, false));
        assertEquals(PrivacyCapturePolicy.Route.NORMAL, PrivacyCapturePolicy.route(false, false, false));
        assertEquals(PrivacyCapturePolicy.Route.NORMAL, PrivacyCapturePolicy.route(false, true, true));
    }
}
