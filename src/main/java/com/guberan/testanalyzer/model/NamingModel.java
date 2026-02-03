package com.guberan.testanalyzer.model;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.guberan.testanalyzer.util.NamingUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class NamingModel {

    private static final int MAX_EXAMPLES = 50;

    private long totalTestMethods = 0;

    private long displayAnnotUsed = 0;
    private long startsWithTest = 0;
    private long hasWhenThen = 0; // test__When__Then__
    private long phraseLike = 0;
    private long containsUnderscore = 0;
    private long noCamelCase = 0;
    private long sameAsSourceMethod = 0;
    private long sourceContainsArrangeActAssert = 0; // arrange act assert

    private Set<String> displayAnnotUsedExamples = new HashSet<>();
    private Set<String> startsWithTestExamples = new HashSet<>();
    private Set<String> hasWhenThenExamples = new HashSet<>();
    private Set<String> phraseLikeExamples = new HashSet<>();
    private Set<String> sameAsSourceMethodExamples = new HashSet<>();
    private Set<String> containsUnderscoreExamples = new HashSet<>();
    private Set<String> noCamelCaseExamples = new HashSet<>();


    private boolean hasDisplayAnnotation(MethodDeclaration m) {
        return m.getAnnotations().stream().anyMatch(a -> "DisplayName".equals(a.getNameAsString()));
    }

    /**
     * Ingest one test method
     */
    public void acceptMethod(MethodDeclaration m, String testClass, Set<String> sourceMethods) {
        String name = m.getNameAsString();

        totalTestMethods++;

        if (hasDisplayAnnotation(m)) {
            displayAnnotUsed++;
            if (displayAnnotUsedExamples.size() <= MAX_EXAMPLES) {
                displayAnnotUsedExamples.add(testClass + "." + name);
            }
        }
        if (name.startsWith("test")) {
            startsWithTest++;
            if (startsWithTestExamples.size() <= MAX_EXAMPLES) {
                startsWithTestExamples.add(testClass + "." + name);
            }
        }
        if (NamingUtil.followsWhenThen(name)) {
            hasWhenThen++;
            if (hasWhenThenExamples.size() <= MAX_EXAMPLES) {
                hasWhenThenExamples.add(testClass + "." + name);
            }
        }
        if (NamingUtil.isBDDLike(name)) {
            phraseLike++;
            if (phraseLikeExamples.size() <= MAX_EXAMPLES) {
                phraseLikeExamples.add(testClass + "." + name);
            }
        }
        if (!sourceMethods.isEmpty() && sourceMethods.contains(name)) {
            sameAsSourceMethod++;
            if (sameAsSourceMethodExamples.size() <= MAX_EXAMPLES) {
                sameAsSourceMethodExamples.add(testClass + "." + name);
            }
        }
        if (name.contains("_")) {
            containsUnderscore++;
            if (containsUnderscoreExamples.size() <= MAX_EXAMPLES) {
                containsUnderscoreExamples.add(testClass + "." + name);
            }
        }
        if (NamingUtil.noUpperCase(name)) {
            noCamelCase++;
            if (noCamelCaseExamples.size() <= MAX_EXAMPLES) {
                noCamelCaseExamples.add(testClass + "." + name);
            }
        }
//        if (name.matches("(?si).*arrange.*act.*assert.*")) { // TODO source code
//           sourceContainsArrangeActAssert++;
//        }

    }

    public void createNamingReport(ProjectAnalysis projectAnalysis) {

        List<ProjectAnalysis.MetricRecord> metrics = new ArrayList<>();
        metrics.add(new ProjectAnalysis.MetricRecord("All tests", this.totalTestMethods, 1.0f, ""));
        metrics.add(new ProjectAnalysis.MetricRecord("@DisplayName annotation", displayAnnotUsed, (float) displayAnnotUsed / totalTestMethods, String.join("\n", displayAnnotUsedExamples)));
        metrics.add(new ProjectAnalysis.MetricRecord("Start with \"test\"", startsWithTest, (float) startsWithTest / totalTestMethods, String.join("\n", startsWithTestExamples)));
        metrics.add(new ProjectAnalysis.MetricRecord("follows pattern ..When..Then...", hasWhenThen, (float) hasWhenThen / totalTestMethods, String.join("\n", hasWhenThenExamples)));
        metrics.add(new ProjectAnalysis.MetricRecord("Is like a phrase", phraseLike, (float) phraseLike / totalTestMethods, String.join("\n", phraseLikeExamples)));
        metrics.add(new ProjectAnalysis.MetricRecord("same name for source and test", sameAsSourceMethod, (float) sameAsSourceMethod / totalTestMethods, String.join("\n", sameAsSourceMethodExamples)));
        metrics.add(new ProjectAnalysis.MetricRecord("contains \"_\"", containsUnderscore, (float) containsUnderscore / totalTestMethods, String.join("\n", containsUnderscoreExamples)));
        metrics.add(new ProjectAnalysis.MetricRecord("no CamelCase", noCamelCase, (float) noCamelCase / totalTestMethods, String.join("\n", noCamelCaseExamples)));


        projectAnalysis.addReport(
                new ProjectAnalysis.MetricsReport(
                        ProjectAnalysis.ReportId.TEST_METHOD_NAMING,
                        "Test naming",
                        "How test method names are written (prefix 'test', underscores, camelCase, @DisplayName, etc.).",
                        "",
                        metrics.size(),
                        metrics)
        );
    }
}