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

    private static final int MAX_TOKEN = 50;
    private static final int MAX_EXAMPLE = 50;

    /**
     * Split points:
     * - before an upper-case letter (but not at the start)
     * - on '_' or '-'
     */
    private static final Pattern TOKEN_SPLIT = Pattern.compile("(?<!^)(?=[A-Z])|[_\\-]");

    // private final Map<String, Long> tokenMap = new HashMap<>();
    private final Map<String, ProjectAnalysis.MetricRecord> tokenMap = new HashMap<>();

    private long totalMethods = 0;
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
    public void acceptMethod(MethodDeclaration method, String testClass) {
        String methodName = method.getNameAsString();
        List<String> tokens = tokenize(methodName);
        if (tokens.isEmpty()) return;

        totalMethods++;
        totalTokens += tokens.size();

        for (String t : tokens) {
            tokenMap.merge(t, new ProjectAnalysis.MetricRecord(t, 1L, 0.0f, testClass + "." + methodName), this::mergeMetrictems);
        }
    }

    private ProjectAnalysis.MetricRecord mergeMetrictems(ProjectAnalysis.MetricRecord item1, ProjectAnalysis.MetricRecord item2) {

        return new ProjectAnalysis.MetricRecord(item1.getName(),
                item1.getCount() + item2.getCount(),
                item1.getPercent() + item2.getPercent(),
                StringUtil.concatWithMaxLines(item1.getSamples(), item2.getSamples(), MAX_EXAMPLE)
        );
    }

    public void createTokenReport(ProjectAnalysis projectAnalysis) {

        List<ProjectAnalysis.MetricRecord> topTokens = tokenMap.values().stream()
                .sorted()
                .limit(MAX_TOKEN)
                .toList();

        ProjectAnalysis.MetricsReport report = new ProjectAnalysis.MetricsReport(
                ProjectAnalysis.ReportId.TOKENS,
                "Tokens",
                "Top 50 tokens (%,d total tokens across %,d test methods)".formatted(totalTokens, totalMethods),
                "",
                totalTokens,
                topTokens)
                .computeRatios();

        projectAnalysis.addReport(report);
    }

}