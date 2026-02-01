package com.guberan.testanalyzer.model;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.guberan.testanalyzer.util.StringUtil;

import java.util.*;
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
public final class PhrasePatternModel {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("(?<!^)(?=[A-Z])|[_\\-]");
    private static final int DEFAULT_TOP_K = 50;

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

    private final Map<String, ProjectStats.MetricItem> patternMap = new HashMap<>();
    private final boolean granular; // true => <w> <w> ; false => <any>
    private long total = 0;

    public PhrasePatternModel(boolean granular) {
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

    // ----------------- acceptMethod -----------------

    /**
     * Accept one method (already filtered to executable test methods, ideally).
     */
    public void acceptMethod(MethodDeclaration method) {
        String methodName = method.getNameAsString();
        List<String> tokens = tokenizeAndNormalize(methodName);
        if (tokens.isEmpty()) return;

        total++;

        String pattern = toPattern(tokens, granular);
        patternMap.merge(pattern, new ProjectStats.MetricItem(pattern, 1L, 0.0f, methodName), this::mergeMetrictems);
    }

    private ProjectStats.MetricItem mergeMetrictems(ProjectStats.MetricItem item1, ProjectStats.MetricItem item2) {

        return new ProjectStats.MetricItem(item1.getName(),
                item1.getCount() + item2.getCount(),
                item1.getPercent() + item2.getPercent(),
                StringUtil.concatWithMaxLines(item1.getTooltip(), item2.getTooltip(), 20)
        );
    }


    public void createPatternReport(ProjectStats stats) {

        List<ProjectStats.MetricItem> top50Patterns = patternMap.values().stream()
                .sorted()
                .limit(DEFAULT_TOP_K)
                .toList();

        stats.addReport(
                new ProjectStats.MetricReport(
                        ProjectStats.ReportEnum.PATTERNS.name(),
                        ProjectStats.ReportEnum.PATTERNS.ordinal(),
                        "Patterns",
                        "Builds common phrase templates from test method names. Method names are tokenized (camelCase, _, -), normalized (e.g., throw/throws → Throws, assert/expect → Expect), keywords are preserved as anchors, and all other words are replaced with <w> (granular) or <any> (compressed). The most frequent patterns are then reported.",
                        "",
                        total,
                        top50Patterns)
                        .computeRatios()
        );
    }

}