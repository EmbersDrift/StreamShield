package livehider.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.function.UnaryOperator;

/**
 * Central content-rewrite pipeline. Rewrites a {@link Component} tree, preserving formatting.
 * {@link #rewrite} applies the general redaction + name anonymization; {@link #rewriteScoreboard}
 * applies the scoreboard-specific keyword-to-replacement mapping + name anonymization.
 */
public final class SafeText {
    private SafeText() {
    }

    public static Component rewrite(Component original) {
        if (original == null || (!NameAnonymizer.isActive() && !Redactor.isActive())) {
            return original;
        }
        return rewriteNode(original, SafeText::rewriteText);
    }

    public static Component rewriteScoreboard(Component original) {
        ScoreboardDiagnostics.capture("title", original);
        if (original == null) {
            return original;
        }
        return Redactor.applyScoreboardToComponent(original);
    }

    /**
     * Matches the visible row across text leaves while retaining untouched styles and icon fonts.
     */
    public static Component rewriteScoreboardRecord(Component original) {
        ScoreboardDiagnostics.capture("row", original);
        if (original == null) {
            return original;
        }
        return Redactor.applyScoreboardToComponent(original);
    }

    /** Name anonymization then content redaction (general surfaces). */
    private static String rewriteText(String text) {
        return Redactor.applyToText(NameAnonymizer.applyToText(text));
    }

    /**
     * Parse a string containing legacy {@code §X} colour/format codes into a chain of styled
     * {@link MutableComponent} runs, so {@code §d§l布吉岛§r} renders as purple-bold "布吉岛" instead of
     * literal {@code §} characters. Also accepts {@code &} as an alias for {@code §} (easier to type on
     * any keyboard), with {@code &&} meaning a literal ampersand. Non-format text is kept as plain runs.
     */
    public static MutableComponent parseLegacyFormatting(String input) {
        return parseLegacyFormatting(input, Style.EMPTY);
    }

    static MutableComponent parseLegacyFormatting(String input, Style initialStyle) {
        MutableComponent root = Component.empty();
        StringBuilder plain = new StringBuilder();
        // Track the running style so codes accumulate until reset.
        Style style = initialStyle;
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '\u00A7' && i + 1 < input.length()) {
                ChatFormatting fmt = ChatFormatting.getByCode(input.charAt(i + 1));
                if (fmt != null) {
                    if (plain.length() > 0) {
                        String text = plain.toString();
                        plain.setLength(0);
                        MutableComponent run = Component.literal(text);
                        run.withStyle(style);
                        root.append(run);
                    }
                    style = applyLegacyFormat(style, fmt);
                    i += 2;
                    continue;
                }
            }
            if (c == '&' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                if (next == '&') {
                    plain.append('&');
                    i += 2;
                    continue;
                }
                ChatFormatting fmt = ChatFormatting.getByCode(next);
                if (fmt != null) {
                    if (plain.length() > 0) {
                        String text = plain.toString();
                        plain.setLength(0);
                        MutableComponent run = Component.literal(text);
                        run.withStyle(style);
                        root.append(run);
                    }
                    style = applyLegacyFormat(style, fmt);
                    i += 2;
                    continue;
                }
            }
            plain.append(c);
            i++;
        }
        if (plain.length() > 0) {
            MutableComponent run = Component.literal(plain.toString());
            run.withStyle(style);
            root.append(run);
        }
        return root;
    }

    private static Style applyLegacyFormat(Style style, ChatFormatting format) {
        // Explicitly clear visual flags on reset, including when the component is later appended
        // to a styled parent. Style.EMPTY would inherit that parent's bold/italic/color again.
        return style.applyLegacyFormat(format == ChatFormatting.RESET ? ChatFormatting.WHITE : format);
    }

    static Component rewriteNode(Component node, UnaryOperator<String> transform) {
        if (node == null) {
            return null;
        }
        ComponentContents contents = node.getContents();
        if (contents instanceof PlainTextContents ptc) {
            String text = ptc.text();
            String replaced = transform.apply(text);
            if (!replaced.equals(text)) {
                // Preserve legacy § formatting codes in the replacement (so §X renders as colour/format),
                // and re-append the original siblings so value/number parts are not dropped.
                if (containsLegacyFormatting(replaced)) {
                    MutableComponent nodeRoot = parseLegacyFormatting(replaced, node.getStyle());
                    for (Component sibling : node.getSiblings()) {
                        Component inheritedSibling = sibling.copy().withStyle(sibling.getStyle().applyTo(node.getStyle()));
                        nodeRoot.append(rewriteNode(inheritedSibling, transform));
                    }
                    return nodeRoot;
                }
                MutableComponent newLeaf = Component.literal(replaced).withStyle(node.getStyle());
                for (Component sibling : node.getSiblings()) {
                    newLeaf.append(rewriteNode(sibling, transform));
                }
                return newLeaf;
            }
        } else if (contents instanceof TranslatableContents tc) {
            Object[] args = tc.getArgs();
            if (args != null && args.length > 0) {
                Object[] newArgs = null;
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg instanceof Component c) {
                        Component r = rewriteNode(c, transform);
                        if (r != c) {
                            if (newArgs == null) {
                                newArgs = args.clone();
                            }
                            newArgs[i] = r;
                        }
                    } else if (arg instanceof String s) {
                        String r = transform.apply(s);
                        if (!r.equals(s)) {
                            if (newArgs == null) {
                                newArgs = args.clone();
                            }
                            newArgs[i] = containsLegacyFormatting(r)
                                ? parseLegacyFormatting(r, node.getStyle()) : r;
                        }
                    }
                }
                if (newArgs != null) {
                    String key = tc.getKey();
                    String fallback = tc.getFallback();
                    MutableComponent rewritten = fallback != null
                        ? Component.translatableWithFallback(key, fallback, newArgs)
                        : Component.translatable(key, newArgs);
                    rewritten.withStyle(node.getStyle());
                    for (Component sibling : node.getSiblings()) {
                        rewritten.append(rewriteNode(sibling, transform));
                    }
                    return rewritten;
                }
            }
        }
        if (node.getSiblings().isEmpty()) {
            return node;
        }
        MutableComponent result = MutableComponent.create(node.getContents()).withStyle(node.getStyle());
        for (Component sibling : node.getSiblings()) {
            result.append(rewriteNode(sibling, transform));
        }
        return result;
    }

    private static boolean containsLegacyFormatting(String text) {
        for (int i = 0; i + 1 < text.length(); i++) {
            char marker = text.charAt(i);
            if (marker == '\u00A7' || marker == '&') {
                if (marker == '&' && text.charAt(i + 1) == '&') {
                    return true;
                }
                if (ChatFormatting.getByCode(text.charAt(i + 1)) != null) {
                    return true;
                }
            }
        }
        return false;
    }
}
