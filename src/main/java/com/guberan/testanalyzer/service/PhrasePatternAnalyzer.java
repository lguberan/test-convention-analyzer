package com.guberan.testanalyzer.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Extracts "phrase-like" templates from test method names.
 * <p>
 * Example:
 * whenInputInvalidThenThrowsIllegalArgumentException
 * tokens:
 * when input invalid then throws illegal argument exception
 * template (granular):
 * When <w> <w> Then Throws <w> <w> Exception
 * Example normalization: assert/expect =&gt; Expect, throw/throws/thrown =&gt; Throws
 * <p>
 * template (compressed):
 * When <any> Then Throws <any> Exception
 */
public final class PhrasePatternAnalyzer {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("(?<!^)(?=[A-Z])|[_\\-]");

    /**
     * Words we want to keep as "anchors" in the template.
     * Keep this set small-ish; otherwise everything becomes a keyword and you lose abstraction.
     */
    private static final Set<String> KEYWORDS = Set.of(
            "given", "when", "then", "should", "if", "throws",
            "expect", "exception", "error", "fail", "fails", "failed",
            "return", "returns", "not", "no", "null", "empty", "missing", "invalid", "valid"
    );

    /**
     * Placeholder for a single unknown token.
     */
    private static final String WORD = "<w>";

    /**
     * Placeholder for a run of unknown tokens.
     */
    private static final String ANY = "<any>";

    private final Map<String, Long> patternCounts = new HashMap<>();
    private final boolean granular; // true => <w> <w> ; false => <any>
    private long total = 0;

    public PhrasePatternAnalyzer(boolean granular) {
        this.granular = granular;
    }

    /**
     * Tokenizes a method name into lowercase tokens (camelCase + '_' + '-' split)
     * and applies a small canonicalization step so similar constructs map to the same pattern.
     */
    static List<String> tokenizeAndNormalize(String s) {
        if (s == null || s.isBlank()) return List.of();

        String[] raw = TOKEN_SPLIT.split(s);
        List<String> out = new ArrayList<>(raw.length);

        for (String r : raw) {
            if (r == null || r.isBlank()) continue;
            String t = r.toLowerCase(Locale.ROOT);

            // Normalize throw/throws/thrown -> throws
            if (t.equals("throw") || t.equals("throws") || t.equals("thrown")) {
                t = "throws";
            }
            // Normalize assert* and expect* -> expect
            else if (t.equals("assert") || t.equals("asserts") || t.equals("asserted")
                    || t.equals("expect") || t.equals("expects") || t.equals("expected")) {
                t = "expect";
            }

            out.add(t);
        }

        return out;
    }

    /**
     * Tokenizer without canonicalization (kept for experiments).
     */
    static List<String> tokenize(String s) {
        if (s == null || s.isBlank()) return List.of();
        String[] raw = TOKEN_SPLIT.split(s);
        List<String> out = new ArrayList<>(raw.length);
        for (String r : raw) {
            if (r == null || r.isBlank()) continue;
            out.add(r.toLowerCase(Locale.ROOT));
        }
        return out;
    }

    static String toPattern(List<String> tokens, boolean granular) {
        StringBuilder sb = new StringBuilder(tokens.size() * 6);

        boolean previousWasPlaceholder = false;

        for (String t : tokens) {
            boolean keyword = KEYWORDS.contains(t);

            if (keyword) {
                // Keep anchors
                sb.append(capitalize(t)).append(' ');
                previousWasPlaceholder = false;
            } else {
                // Replace unknown words
                if (granular) {
                    sb.append(WORD).append(' ');
                    previousWasPlaceholder = true;
                } else {
                    // compressed: merge consecutive unknowns into a single <any>
                    if (!previousWasPlaceholder) {
                        sb.append(ANY).append(' ');
                        previousWasPlaceholder = true;
                    }
                }
            }
        }

        // cleanup trailing space
        return sb.toString().trim();
    }

    static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Accept one method name (already filtered to executable test methods, ideally).
     */
    public void acceptMethodName(String methodName) {
        List<String> tokens = tokenizeAndNormalize(methodName);
        if (tokens.isEmpty()) return;

        total++;

        String pattern = toPattern(tokens, granular);
        patternCounts.merge(pattern, 1L, Long::sum);
    }

    // ----------------- Core logic -----------------

    /**
     * Returns the top patterns, ordered by count desc.
     */
    public List<Entry<String, Long>> topPatterns(int k) {
        return patternCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(k)
                .toList();
    }

    /**
     * Renders a compact report: "count (pct%) : pattern".
     */
    public String renderTop(int k) {
        StringBuilder sb = new StringBuilder(8_192);
        for (var e : topPatterns(k)) {
            long c = e.getValue();
            double pct = total == 0 ? 0.0 : (100.0 * c / total);
            sb.append(String.format(Locale.ROOT, "%5d (%5.1f%%) : %s%n", c, pct, e.getKey()));
        }
        return sb.toString();
    }

    public long totalAnalyzed() {
        return total;
    }
}