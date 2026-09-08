package livehider.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Privacy regression: even accidental extra collector fields must not enter a shared report. */
class DiagnosticReportTest {
    @Test
    void exportsKnownSettingsWithoutSensitiveTextOrDynamicComponentNames() {
        JsonObject snapshot = new JsonObject();
        snapshot.addProperty("rawLog", "private-chat-sentinel");
        snapshot.addProperty("serverAddress", "private-server.example");
        JsonObject settings = new JsonObject();
        settings.addProperty("anonymizeNames", true);
        settings.addProperty("hideHudWhenOverlayUnavailable", false);
        settings.addProperty("selfCustomName", "private-name-sentinel");
        settings.addProperty("skinUrlTemplate", "https://private-server.example/token");
        settings.addProperty("skinLocalFolder", "C:/Users/private-user/skins");
        settings.addProperty("scoreboardRules", "private-rule-sentinel");
        // Config text accidentally copied under a boolean setting must not pass through, either.
        settings.addProperty("sanitizeChatInput", "private-chat-sentinel");
        snapshot.add("settings", settings);
        JsonObject components = new JsonObject();
        JsonObject chat = new JsonObject();
        chat.addProperty("selected", true);
        chat.addProperty("autoHide", false);
        chat.addProperty("rawText", "private-chat-sentinel");
        components.add("chat", chat);
        components.add("private-name-sentinel", chat.deepCopy());
        snapshot.add("components", components);

        String text = DiagnosticReport.serialize(snapshot);
        JsonObject report = JsonParser.parseString(text).getAsJsonObject();

        assertFalse(text.contains("private-"));
        assertEquals(Set.of("schema", "build", "runtime", "overlay", "settings", "components"), report.keySet());
        assertEquals(Set.of("anonymizeNames", "hideHudWhenOverlayUnavailable"),
            report.getAsJsonObject("settings").keySet());
        assertEquals(Set.of("chat"), report.getAsJsonObject("components").keySet());
        assertEquals(Set.of("selected", "autoHide"), report.getAsJsonObject("components").getAsJsonObject("chat").keySet());
        assertTrue(report.getAsJsonObject("settings").get("anonymizeNames").getAsBoolean());
        assertFalse(report.getAsJsonObject("settings").get("hideHudWhenOverlayUnavailable").getAsBoolean());
    }

    @Test
    void turnsExceptionMessagesIntoUnknownCodesAndNeverCertifiesObs() {
        JsonObject snapshot = new JsonObject();
        JsonObject overlay = new JsonObject();
        overlay.addProperty("status", "error reading C:/Users/private-user/config.json");
        overlay.addProperty("failureCode", "failed with token private-secret");
        overlay.addProperty("obsCaptureAutomaticallyVerified", true);
        overlay.addProperty("exception", "private-secret");
        snapshot.add("overlay", overlay);

        String text = DiagnosticReport.serialize(snapshot);
        JsonObject output = JsonParser.parseString(text).getAsJsonObject().getAsJsonObject("overlay");

        assertFalse(text.contains("private-"));
        assertEquals("UNKNOWN", output.get("status").getAsString());
        assertEquals("UNKNOWN", output.get("failureCode").getAsString());
        assertFalse(output.get("obsCaptureAutomaticallyVerified").getAsBoolean());
        assertEquals(Set.of("status", "failureCode", "obsCaptureAutomaticallyVerified"), output.keySet());
    }

    @Test
    void keepsVersionIdentifiersAndKnownFailureCodes() {
        JsonObject snapshot = new JsonObject();
        JsonObject build = new JsonObject();
        build.addProperty("version", "1.1.2");
        build.addProperty("minecraft", "26.1.2");
        build.addProperty("loader", "neoforge");
        build.addProperty("commit", "a1b2c3d-dirty");
        snapshot.add("build", build);
        JsonObject runtime = new JsonObject();
        runtime.addProperty("minecraft", "26.1.2");
        runtime.addProperty("loader", "neoforge");
        runtime.addProperty("loaderVersion", "26.1.2.102");
        runtime.addProperty("javaFeature", 25);
        snapshot.add("runtime", runtime);
        JsonObject overlay = new JsonObject();
        overlay.addProperty("status", "FAILED");
        overlay.addProperty("failureCode", "MH_ENABLE_FAILED");
        snapshot.add("overlay", overlay);

        JsonObject output = JsonParser.parseString(DiagnosticReport.serialize(snapshot)).getAsJsonObject();

        assertEquals(build, output.get("build"));
        assertEquals("26.1.2.102", output.getAsJsonObject("runtime").get("loaderVersion").getAsString());
        assertEquals(25, output.getAsJsonObject("runtime").get("javaFeature").getAsInt());
        assertEquals("FAILED", output.getAsJsonObject("overlay").get("status").getAsString());
        assertEquals("MH_ENABLE_FAILED", output.getAsJsonObject("overlay").get("failureCode").getAsString());
    }

    @Test
    void rejectsPathsUrlsAndUnexpectedMetadataTypes() {
        JsonObject snapshot = new JsonObject();
        JsonObject build = new JsonObject();
        build.addProperty("version", "https://private-server.example");
        build.addProperty("commit", "C:/Users/private-user/worktree");
        build.add("minecraft", new JsonObject());
        build.addProperty("environment", "private-secret");
        snapshot.add("build", build);
        JsonObject runtime = new JsonObject();
        runtime.addProperty("loader", "private-user");
        runtime.addProperty("javaFeature", "private-secret");
        snapshot.add("runtime", runtime);

        String text = DiagnosticReport.serialize(snapshot);
        JsonObject output = JsonParser.parseString(text).getAsJsonObject();

        assertFalse(text.contains("private-"));
        assertEquals("unknown", output.getAsJsonObject("build").get("version").getAsString());
        assertEquals("unknown", output.getAsJsonObject("build").get("commit").getAsString());
        assertEquals("unknown", output.getAsJsonObject("build").get("minecraft").getAsString());
        assertEquals("unknown", output.getAsJsonObject("runtime").get("loader").getAsString());
        assertEquals(0, output.getAsJsonObject("runtime").get("javaFeature").getAsInt());
    }

    @Test
    void ignoresMalformedNestedObjectsAndOversizedNumericValues() {
        JsonObject snapshot = new JsonObject();
        snapshot.addProperty("settings", "private-secret");
        snapshot.addProperty("components", true);
        JsonObject runtime = new JsonObject();
        runtime.addProperty("javaFeature", new java.math.BigInteger("99999999999999999999999"));
        snapshot.add("runtime", runtime);

        JsonObject output = JsonParser.parseString(DiagnosticReport.serialize(snapshot)).getAsJsonObject();

        assertTrue(output.getAsJsonObject("settings").isEmpty());
        assertTrue(output.getAsJsonObject("components").isEmpty());
        assertEquals(0, output.getAsJsonObject("runtime").get("javaFeature").getAsInt());
    }
}

