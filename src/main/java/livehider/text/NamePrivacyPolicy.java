package livehider.text;

import java.util.function.Supplier;

/** Independent master switches; chat sanitization never grants extra permission. */
record NamePrivacyPolicy(boolean self, boolean others, boolean chat) {
    boolean enabled(boolean isSelf) { return isSelf ? self : others; }
    boolean chatEnabled() { return chat && (self || others); }
    String replacement(boolean isSelf, Supplier<String> generate) {
        return enabled(isSelf) ? generate.get() : null;
    }
}
