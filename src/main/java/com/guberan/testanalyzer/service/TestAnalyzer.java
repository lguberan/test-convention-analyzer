package com.guberan.testanalyzer.service;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.guberan.testanalyzer.model.*;
import com.guberan.testanalyzer.service.ProjectScanner.ScanResult;
import com.guberan.testanalyzer.util.NamingUtil;
import com.guberan.testanalyzer.util.PathUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class TestAnalyzer {

    private final ProjectScanner scanner = new ProjectScanner();
    private final JavaTestClassifier classifier = new JavaTestClassifier();
    private final JavaAstService ast = new JavaAstService();
    private final TokenModel tokenModel = new TokenModel();
    private final NamingModel namingModel = new NamingModel();
    private final PhrasePatternModel patternModel = new PhrasePatternModel(false);


    public ProjectAnalysis analyze(Path projectRoot, Consumer<String> progress) {
        progress.accept("Scanning files…");
        ScanResult scan = scanner.scan(projectRoot);

        // classify java
        progress.accept("Classifying Java files…");
        long totalJavaFiles = scan.javaFiles().size();
        Map<Boolean, List<Path>> parts = scan.javaFiles().stream()
                .collect(Collectors.partitioningBy(classifier::isTestSource));

        List<Path> javaTestFiles = parts.get(true);
        List<Path> javaSourceFiles = parts.get(false);

        // build quick index for source files by “class” path heuristic
        progress.accept("Indexing source classes…");
        Map<String, Path> sourceByFqn = buildSourceIndex(projectRoot, javaSourceFiles);

        // analyze tests
        progress.accept("Analyzing test methods…");
        PatternStats patternStats = analyzeTestFiles(projectRoot, javaTestFiles, sourceByFqn, progress);

        // infer convention
        progress.accept("Inferring convention…");
        //ConventionSummary convention = new ConventionInferer().infer(testNaming);

        // assemble
        ProjectAnalysis projectAnalysis = new ProjectAnalysis();
        projectAnalysis.setProjectRoot(projectRoot.toString());

        createSourceVsTestReport(projectAnalysis, scan, javaSourceFiles.size(), javaTestFiles.size());
        createExtensionReport(projectAnalysis, scan);

        // reporting
        tokenModel.createTokenReport(projectAnalysis);
        namingModel.createNamingReport(projectAnalysis);
        patternModel.createPatternReport(projectAnalysis);

        return projectAnalysis;
    }

    private Map<String, Path> buildSourceIndex(Path root, List<Path> sourceFiles) {
        Map<String, Path> map = new HashMap<>();
        for (Path p : sourceFiles) {
            PathUtil.fqnFromMainJavaPath(root, p).ifPresent(fullyQualifiedClassName -> map.put(fullyQualifiedClassName, p));
        }
        return map;
    }

    void createExtensionReport(ProjectAnalysis projectAnalysis, ScanResult scan) {

        // Top extensions
        List<ProjectAnalysis.MetricItem> topFileTypes = scan.extensionCounts()
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(ProjectAnalysis.MetricItem::of)
                .collect(Collectors.toList());


        projectAnalysis.addReport(new ProjectAnalysis.MetricsReport(
                ProjectAnalysis.ReportId.FILE_TYPES.name(),
                ProjectAnalysis.ReportId.FILE_TYPES.ordinal(),
                "Files Type (Top 10)",
                "Top file extensions by count (useful to understand the project composition).",
                "",
                scan.totalFiles(),
                topFileTypes));
    }

    void createSourceVsTestReport(ProjectAnalysis projectAnalysis, ScanResult scan, long totalSrcFiles, long totalTestFiles) {

        long totalFiles = scan.totalFiles();
        long totalJavaFiles = scan.javaFiles().size();

        List<ProjectAnalysis.MetricItem> metrics = new ArrayList<>();
        metrics.add(new ProjectAnalysis.MetricItem("Total files", totalFiles, 1.0f, ""));
        metrics.add(new ProjectAnalysis.MetricItem(".java files", totalJavaFiles, (float) totalJavaFiles / totalFiles, ""));
        metrics.add(new ProjectAnalysis.MetricItem("Java sources", totalSrcFiles, (float) totalSrcFiles / totalJavaFiles, ""));
        metrics.add(new ProjectAnalysis.MetricItem("Java tests", totalTestFiles, (float) totalTestFiles / totalJavaFiles, ""));

        projectAnalysis.addReport(new ProjectAnalysis.MetricsReport(
                ProjectAnalysis.ReportId.SRC_VS_TEST.name(),
                ProjectAnalysis.ReportId.SRC_VS_TEST.ordinal(),
                "Java Source vs Test",
                "Breakdown of Java files into production sources vs test sources (based on src/main/java and src/test/java).",
                "",
                totalFiles,
                metrics));
    }


    private PatternStats analyzeTestFiles(Path root,
                                          List<Path> testFiles,
                                          Map<String, Path> sourceByFqn,
                                          Consumer<String> progress) {

        // cache parsed source method sets
        Map<Path, Set<String>> sourceMethodsCache = new HashMap<>();
        PatternStats patternStats = new PatternStats();

        for (int i = 0; i < testFiles.size(); i++) {
            Path testFile = testFiles.get(i);
            if (i % 200 == 0) progress.accept("Parsing tests… " + i + "/" + testFiles.size());

            Optional<CompilationUnit> cuOpt = ast.parse(testFile);
            if (cuOpt.isEmpty()) continue;
            CompilationUnit compilationUnit = cuOpt.get();

            String pkg = compilationUnit.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
            Set<String> typeNames = compilationUnit.getTypes().stream().map(t -> t.getNameAsString()).collect(Collectors.toSet());

            // determine "primary" test class name (heuristic: first type ending with Test else first)
//            String testClass = typeNames.stream().filter(n -> n.endsWith("Test")).findFirst()
//                    .orElse(typeNames.stream().findFirst().orElse(null));

            String testClass = typeNames.stream()
                    .filter(n -> NamingUtil.sourceClassNameFromTestClass(n) != null)
                    .findFirst()
                    .orElse(typeNames.stream().findFirst().orElse(null));

            String sourceClass = NamingUtil.sourceClassNameFromTestClass(testClass);

            Path sourceFile = null;
            if (sourceClass != null) {
                String fqn = pkg.isBlank() ? sourceClass : (pkg + "." + sourceClass);
                sourceFile = sourceByFqn.get(fqn);
            }

            Set<String> sourceMethods = Collections.emptySet();
            if (sourceFile != null) {
                sourceMethods = sourceMethodsCache.computeIfAbsent(sourceFile, sf -> parseMethodNames(sf));
            }

            // test methods: methods with @Test-ish annotations
            var methods = compilationUnit.findAll(MethodDeclaration.class);

            for (MethodDeclaration m : methods) {
                if (!isTestMethod(m)) continue;

                String name = m.getNameAsString();
                patternStats.accept(name);
                tokenModel.acceptMethod(m);
                namingModel.acceptMethod(m, sourceMethods);
                patternModel.acceptMethod(m);
            }
        }
        return patternStats;
    }

    private Set<String> parseMethodNames(Path sourceFile) {
        var cuOpt = ast.parse(sourceFile);
        if (cuOpt.isEmpty()) return Set.of();
        var cu = cuOpt.get();
        return cu.findAll(MethodDeclaration.class).stream()
                .filter(m -> !m.isPrivate()) // optionnel : filtre léger
                .map(m -> m.getNameAsString())
                .collect(Collectors.toSet());
    }

    private boolean isTestMethod(MethodDeclaration m) {
        // JUnit5 + common variants.
        return m.getAnnotations().stream().anyMatch(a -> {
            String n = a.getNameAsString();
            return n.equals("Test")
                    || n.equals("ParameterizedTest")
                    || n.equals("RepeatedTest")
                    || n.equals("TestFactory")
                    || n.equals("TestTemplate");
        });
    }

    private boolean hasDisplayAnnotation(MethodDeclaration m) {
        return m.getAnnotations().stream().anyMatch(a -> a.getNameAsString().equals("DisplayName"));
    }

    /* ---- */

    private static class PatternStats {
        private static final Set<String> KEYWORDS = Set.of(
                "given", "when", "then", "if", "should",
                "expect",
                "throws",
                "exception", "error",
                "fail", "fails", "return", "returns"
        );

        private static final int MAX_EXAMPLES_PER_PATTERN = 20;
        private final Map<String, List<String>> examplesByPattern = new HashMap<>();
        private final Map<String, Long> patternCounts = new HashMap<>();
        private long totalAnalyzed = 0;

        /**
         * Tokenizes a method name and applies light canonicalization so that equivalent constructs
         * map to the same pattern (reduces stats fragmentation).
         */
        private static List<String> tokenizeAndNormalize(String methodName) {
            List<String> tokens = TokenModel.tokenize(methodName);
            if (tokens.isEmpty()) return tokens;

            for (int i = 0; i < tokens.size(); i++) {
                String t = tokens.get(i);

                // throw / throws / thrown => throws
                if (t.equals("throw") || t.equals("throws") || t.equals("thrown")) {
                    tokens.set(i, "throws");
                    continue;
                }

                // assert* and expect* => expect
                if (t.equals("assert") || t.equals("asserts") || t.equals("asserted")
                        || t.equals("expect") || t.equals("expects") || t.equals("expected")) {
                    tokens.set(i, "expect");
                }
            }

            return tokens;
        }

        private void addExample(String pattern, String example) {
            List<String> list = examplesByPattern.computeIfAbsent(pattern, k -> new ArrayList<>());
            if (list.size() < MAX_EXAMPLES_PER_PATTERN) list.add(example);
        }

        void accept(String methodName) {
            List<String> tokens = tokenizeAndNormalize(methodName);
            long keywordsCount = tokens.stream().filter(KEYWORDS::contains).count();
            if (keywordsCount < 1) {
                return;
            }

            totalAnalyzed++;

            StringBuilder sb = new StringBuilder();
            boolean inAnyRun = false;

            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i);
                boolean isKeyword = KEYWORDS.contains(token);
                if (isKeyword) {
                    // Special-case for exception or error
                    if (token.equals("exception") || token.equals("error")) {
                        // Check if previous tokens were non-keyword run
                        if (inAnyRun) {
                            // Close the <any> run before adding Exception/Error
                            // Already appended <any>, so just append Exception/Error
                            sb.append(" ");
                            sb.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1));
                        } else {
                            // No prior <any>, just append Exception/Error
                            if (sb.length() > 0) sb.append(" ");
                            sb.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1));
                        }
                        inAnyRun = false;
                    } else {
                        // Normal keyword
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1));
                        inAnyRun = false;
                    }
                } else {
                    // Non-keyword token
                    // Look ahead to see if next keyword is exception/error
                    boolean nextIsExceptionOrError = false;
                    if (i + 1 < tokens.size()) {
                        String nextToken = tokens.get(i + 1);
                        if (nextToken.equals("exception") || nextToken.equals("error")) {
                            nextIsExceptionOrError = true;
                        }
                    }
                    if (nextIsExceptionOrError) {
                        // Do nothing here, placeholder will be before Exception/Error
                        if (!inAnyRun) {
                            if (sb.length() > 0) sb.append(" ");
                            sb.append("<any>");
                            inAnyRun = true;
                        }
                    } else {
                        if (!inAnyRun) {
                            if (sb.length() > 0) sb.append(" ");
                            sb.append("<any>");
                            inAnyRun = true;
                        }
                    }
                }
            }

            String pattern = sb.toString().trim().replaceAll("\\s+", " ");
            patternCounts.merge(pattern, 1L, Long::sum);
            addExample(pattern, methodName); // TODO testClassFqn + "#" +
        }

        List<Map.Entry<String, Long>> topPatterns(int k) {
            return patternCounts.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(k)
                    .toList();
        }

        String renderTopPatterns(int k) {
            StringBuilder sb = new StringBuilder(4096);
            List<Map.Entry<String, Long>> top = topPatterns(k);
            for (var e : top) {
                long count = e.getValue();
                double percent = totalAnalyzed > 0 ? (100.0 * count / totalAnalyzed) : 0.0;
                sb.append(String.format("%d (%.1f%%) : %s\n", count, percent, e.getKey()));
            }
            return sb.toString();
        }
    }
}