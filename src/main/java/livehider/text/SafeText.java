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
        if (original == null) {
            return original;
        }
        return rewriteNode(original, Redactor::applyScoreboardToText);
    }

    /**
     * Structure-preserving rewrite for an assembled sidebar-row {@link Component}. Unlike
     * {@link #rewriteScoreboard}, this treats the <em>whole row</em> as a single unit to map on its
     * raw string (colour codes + PUA icon codepoints preserved) and anonymize an exact player-name
     * line, then rebuilds only the text leaves. Siblings that carry server resource-pack icon styling
     * are left untouched, so PUA glyphs keep rendering as their custom images instead of boxes.
     */
    /**
     * Structure-preserving rewrite for an assembled sidebar-row {@link Component}. The whole row's
     * plain string is run through {@link Redactor#rewriteScoreboardRow} (chain rules + name anonymization)
     * as one unit, then, if it changed and contains {@code §}/{@code &} colour codes, parsed into styled
     * runs. Server resource-pack icon siblings are preserved by only substituting the row text itself;
     * if the composed string did not change, the original component is returned untouched.
     */
    public static Component rewriteScoreboardRecord(Component original) {
        if (original == null) {
            return original;
        }
        // Per-leaf, structure-preserving rewrite (the same transform the title uses). Each plain-text
        // leaf is rewritten individually while its sibling components keep their original style/font,
        // so server resource-pack PUA icon siblings still render as their custom images instead of
        // degrading to boxes. The earlier whole-row flatten (flatString -> parseLegacyFormatting) is
        // intentionally dropped: rebuilding the entire row as Component.literal runs stripped the icon
        // siblings' styling and turned their PUA glyphs into boxes.
        return rewriteNode(original, Redactor::applyScoreboardToText);
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
        MutableComponent root = Component.empty();
        MutableComponent current = null;
        StringBuilder plain = new StringBuilder();
        // Track the running style so codes accumulate until reset.
        Style style = Style.EMPTY;
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
                        if (current == null) {
                            root = run;
                            current = run;
                        } else {
                            current.append(run);
                        }
                    }
                    style = style.applyLegacyFormat(fmt);
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
                        if (current == null) {
                            root = run;
                            current = run;
                        } else {
                            current.append(run);
                        }
                    }
                    style = style.applyLegacyFormat(fmt);
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
            if (current == null) {
                root = run;
            } else {
                current.append(run);
            }
        }
        return root;
    }

    private static Component rewriteNode(Component node, UnaryOperator<String> transform) {
        if (node == null) {
            return null;
        }
        ComponentContents contents = node.getContents();
        if (contents instanceof PlainTextContents ptc) {
            String text = ptc.text();
            String replaced = transform.apply(text);
            if (!replaced.equals(text)) {
                livehider.LiveHider.LOGGER.info("[LiveHider][fmt] leaf replaced: \"{}\" -> \"{}\"", text, replaced);
                // Preserve legacy § formatting codes in the replacement (so §X renders as colour/format),
                // and re-append the original siblings so value/number parts are not dropped.
                if (replaced.indexOf('\u00A7') >= 0) {
                    MutableComponent nodeRoot = parseLegacyFormatting(replaced);
                    for (Component sibling : node.getSiblings()) {
                        nodeRoot.append(rewriteNode(sibling, transform));
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
                            newArgs[i] = r;
                        }
                    }
                }
                if (newArgs != null) {
                    String key = tc.getKey();
                    String fallback = tc.getFallback();
                    MutableComponent rewritten = fallback != null
                        ? Component.translatable(key, fallback, newArgs)
                        : Component.translatable(key, newArgs);
                    return rewritten.withStyle(node.getStyle());
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
}
