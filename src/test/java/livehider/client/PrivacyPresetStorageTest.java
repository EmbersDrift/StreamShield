package livehider.client;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class PrivacyPresetStorageTest {
    @TempDir Path directory;
    @Test void roundTripAndExplicitReplace() throws Exception {
        var settings = new JsonObject(); settings.addProperty("redactBooks", true);
        Path file = directory.resolve("preset.json");
        PrivacyPresets.write(file, settings, false);
        assertEquals(settings, PrivacyPresets.read(file));
        assertThrows(FileAlreadyExistsException.class, () -> PrivacyPresets.write(file, new JsonObject(), false));
        assertEquals(settings, PrivacyPresets.read(file));
        PrivacyPresets.write(file, new JsonObject(), true);
        assertEquals(new JsonObject(), PrivacyPresets.read(file));
        try (var paths=Files.list(directory)) { assertEquals(1, paths.count()); }
    }
    @Test void invalidSchemaAndOversizedImportAreRejected() throws Exception {
        Path file=directory.resolve("input.json");
        Files.writeString(file,"{\"schema\":2,\"settings\":{}}");
        assertThrows(java.io.IOException.class, () -> PrivacyPresets.read(file));
        Files.writeString(file," ".repeat(1024 * 1024 + 1));
        assertThrows(java.io.IOException.class, () -> PrivacyPresets.read(file));
    }
}
