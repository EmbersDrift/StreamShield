package livehider.client;

import com.google.gson.JsonParser;
import livehider.LiveHiderConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrivacyPresetsTest {
    @Test void serverNamesNeverBecomePaths() {
        assertTrue(PrivacyPresets.serverKey("../../private:25565").matches("[a-f0-9]{64}"));
        assertEquals(PrivacyPresets.serverKey("example.org"), PrivacyPresets.serverKey(" EXAMPLE.ORG "));
        assertNotEquals(PrivacyPresets.serverKey("example.org:1"), PrivacyPresets.serverKey("example.org:2"));
    }
    @Test void exportedSettingsExcludeLocalPathsCredentialsAndSafetySwitches() {
        var config = new LiveHiderConfig();
        config.skinLocalFolder = "private/path";
        config.scoreboardDiagnostics = true;
        var settings = PrivacyPresets.settings(config);
        assertFalse(settings.has("skinLocalFolder"));
        assertFalse(settings.has("skinApiToken"));
        assertFalse(settings.has("scoreboardDiagnostics"));
        assertFalse(settings.has("protectConfigUi"));
        assertTrue(settings.has("scoreboardRules"));
    }
    @Test void importedSafetySwitchesAreIgnoredAndNullRulesNormalized() {
        var current = new LiveHiderConfig();
        var candidate = PrivacyPresets.candidate(current, JsonParser.parseString(
            "{\"protectConfigUi\":false,\"scoreboardDiagnostics\":true,\"scoreboardRules\":null,\"redactBooks\":true}").getAsJsonObject());
        assertTrue(candidate.protectConfigUi);
        assertFalse(candidate.scoreboardDiagnostics);
        assertTrue(candidate.redactBooks);
        assertTrue(candidate.scoreboardRules.isEmpty());
        assertFalse(current.redactBooks);
    }
    @Test void malformedSettingsCannotMutateOriginal() {
        var current = new LiveHiderConfig();
        assertThrows(RuntimeException.class, () -> PrivacyPresets.candidate(current,
            JsonParser.parseString("{\"scoreboardRules\":42}").getAsJsonObject()));
        assertTrue(current.scoreboardRules.isEmpty());
    }
}
