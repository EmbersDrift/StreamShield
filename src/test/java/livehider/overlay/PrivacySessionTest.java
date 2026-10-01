package livehider.overlay;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PrivacySessionTest {
    @Test void closingAUiDoesNotAutomaticallyResume() {
        var s = new PrivacySession(); s.arm();
        for (int i=0;i<10;i++) s.nextFrame(false);
        assertTrue(s.active());
    }
    @Test void explicitResumeRetainsSeveralProtectedFrames() {
        var s = new PrivacySession(); s.arm(); s.requestResume();
        s.nextFrame(false); assertTrue(s.active());
        s.nextFrame(false); assertTrue(s.active());
        s.nextFrame(false); assertFalse(s.active());
    }
    @Test void openingAnotherScreenCancelsRelease() {
        var s = new PrivacySession(); s.arm(); s.requestResume();
        s.nextFrame(true);
        for(int i=0;i<10;i++) s.nextFrame(false);
        assertTrue(s.active()); assertFalse(s.releasing());
    }
}
