package com.pvpclub.easyinv.config;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User-configurable chat-line template. The user writes the format they see
 * in chat, with two placeholders:
 * <ul>
 *   <li>{@code {name}} — the player name (this is what we extract)</li>
 *   <li>{@code {message}} — the rest of the line, ignored by the matcher</li>
 * </ul>
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>{@code "{name} » {message}"} — ncpvp.club default</li>
 *   <li>{@code "<{name}> {message}"} — many vanilla servers</li>
 *   <li>{@code "[{name}] {message}"} — guild/clan style</li>
 * </ul>
 *
 * <p>Backslash-escape any literal {@code {} } you need in the prefix/
 * suffix, e.g. {@code "\[{name}\] {message}"} for {@code [Name] msg}.</p>
 */
public final class ChatPattern {

    private final String format;
    private final Pattern compiled;

    public ChatPattern(String format) {
        this.format = format;
        this.compiled = compile(format);
    }

    public String getFormat() {
        return format;
    }

    public Pattern getCompiled() {
        return compiled;
    }

    /**
     * Tries to extract the player name from a single chat line. Returns
     * empty when the line doesn't match the configured format.
     */
    public Optional<String> extractName(String chatLine) {
        if (chatLine == null || chatLine.isEmpty()) {
            return Optional.empty();
        }
        Matcher m = compiled.matcher(chatLine);
        if (m.find()) {
            String captured = m.group(1);
            if (captured != null) {
                String trimmed = captured.trim();
                if (!trimmed.isEmpty()) {
                    return Optional.of(trimmed);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns a string suitable for putting into a JSON config file —
     * a minimal default that works on ncpvp.club out of the box.
     */
    public static ChatPattern defaultPattern() {
        return new ChatPattern("{name} » {message}");
    }

    // ---- Regex compilation ------------------------------------------------

    /**
     * Translate the user-friendly format string into a regex.
     * <p>{@code {name}} becomes a lazy capture group, {@code {message}}
     * becomes {@code .*?}, every other character is regex-escaped.</p>
     */
    private static Pattern compile(String format) {
        if (format == null || format.isBlank()) {
            format = "{name} » {message}";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < format.length()) {
            char c = format.charAt(i);
            if (c == '{') {
                int close = format.indexOf('}', i + 1);
                if (close > i + 1) {
                    String placeholder = format.substring(i + 1, close);
                    if (placeholder.equals("name")) {
                        sb.append("(.+?)");
                        i = close + 1;
                        continue;
                    }
                    if (placeholder.equals("message")) {
                        sb.append(".*?");
                        i = close + 1;
                        continue;
                    }
                }
            }
            // Regex-escape this char.
            if ("\\.^$|*+?{}[]()".indexOf(c) >= 0) {
                sb.append('\\').append(c);
            } else {
                sb.append(c);
            }
            i++;
        }
        return Pattern.compile(sb.toString(), Pattern.DOTALL);
    }
}