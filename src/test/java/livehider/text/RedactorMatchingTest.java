package livehider.text;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RedactorMatchingTest {
    @Test void markedWordsMatchEveryOccurrenceInChatText() {
        var pattern = Redactor.compileMarkedWords(Set.of("你好", "Vlyen"));
        assertEquals("***世界 *** ***123", pattern.matcher("你好世界 VLYEN Vlyen123")
            .replaceAll(java.util.regex.Matcher.quoteReplacement("***")));
    }
}
