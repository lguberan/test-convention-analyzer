package com.guberan.testanalyzer.model;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.guberan.testanalyzer.util.StringUtil;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Simple unigram token frequency model.
 *
 * <p>Tokenization is tailored for Java method names:
 * <ul>
 *   <li>splits on camelCase boundaries</li>
 *   <li>splits on '_' and '-'</li>
 *   <li>lower-cases tokens</li>
 * </ul>
 */
public final class TokenModel {

    private static final int DEFAULT_TOP_K = 50;

    /**
     * Split points:
     * - before an upper-case letter (but not at the start)
     * - on '_' or '-'
     */
    private static final Pattern TOKEN_SPLIT = Pattern.compile("(?<!^)(?=[A-Z])|[_\\-]");

    // private final Map<String, Long> tokenMap = new HashMap<>();
    private final Map<String, ProjectAnalysis.MetricItem> tokenMap = new HashMap<>();

    private long sequences = 0;
    private long totalTokens = 0;

    /**
     * Tokenize a method name into lowercase tokens.
     */
    public static List<String> tokenize(String s) {
        if (s == null || s.isBlank()) return List.of();
        String[] raw = TOKEN_SPLIT.split(s);
        List<String> out = new ArrayList<>(raw.length);
        for (String r : raw) {
            if (r == null || r.isBlank()) continue;
            out.add(r.toLowerCase(Locale.ROOT));
        }
        return out;
    }

    /**
     * Ingest one test method
     */
    public void acceptMethod(MethodDeclaration method) {
        String methodName = method.getNameAsString();
        List<String> tokens = tokenize(methodName);
        if (tokens.isEmpty()) return;

        sequences++;
        totalTokens += tokens.size();

        for (String t : tokens) {
            tokenMap.merge(t, new ProjectAnalysis.MetricItem(t, 1L, 0.0f, methodName), this::mergeMetrictems);
        }
    }

    private ProjectAnalysis.MetricItem mergeMetrictems(ProjectAnalysis.MetricItem item1, ProjectAnalysis.MetricItem item2) {

        return new ProjectAnalysis.MetricItem(item1.getName(),
                item1.getCount() + item2.getCount(),
                item1.getPercent() + item2.getPercent(),
                StringUtil.concatWithMaxLines(item1.getTooltip(), item2.getTooltip(), 20)
        );
    }

    public void createTokenReport(ProjectAnalysis projectAnalysis) {

        List<ProjectAnalysis.MetricItem> top50Tokens = tokenMap.values().stream()
                .sorted()
                .limit(DEFAULT_TOP_K)
                .toList();

        projectAnalysis.addReport(
                new ProjectAnalysis.MetricsReport(
                        ProjectAnalysis.ReportId.TOKENS.name(),
                        ProjectAnalysis.ReportId.TOKENS.ordinal(),
                        "Tokens",
                        "Top tokens (50)",
                        "",
                        totalTokens,
                        top50Tokens)
                        .computeRatios()
        );
    }

}