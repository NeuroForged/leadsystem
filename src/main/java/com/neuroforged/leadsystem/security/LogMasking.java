package com.neuroforged.leadsystem.security;

/**
 * Masks secrets / tokens for safe logging.
 *
 * <p>Logs go to Loki + stdout; anyone with log access gains access to every value emitted.
 * Every credential-like field in a log statement must go through {@link #mask(String)}.
 *
 * <p>Output shape: {@code "abcd…wxyz (len=64)"} — preserves enough to grep for known prefixes
 * without leaking the secret.
 *
 * @see <a href="https://alchemizeiq.atlassian.net/browse/LSB-157">LSB-157</a>
 */
public final class LogMasking {

    private static final int VISIBLE_HEAD = 4;
    private static final int VISIBLE_TAIL = 4;

    private LogMasking() {}

    /**
     * Returns {@code "<head>…<tail> (len=<n>)"}; or {@code "null"} / {@code "<too-short>"} for
     * inputs that can't be partially shown safely.
     */
    public static String mask(String secret) {
        if (secret == null) {
            return "null";
        }
        int len = secret.length();
        if (len < VISIBLE_HEAD + VISIBLE_TAIL + 4) {
            // Don't reveal anything if the secret is short enough that head+tail covers most of it.
            return "<redacted len=" + len + ">";
        }
        return secret.substring(0, VISIBLE_HEAD)
                + "…"
                + secret.substring(len - VISIBLE_TAIL)
                + " (len=" + len + ")";
    }
}
