package com.guberan.testanalyzer.service;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.guberan.testanalyzer.model.ConventionSummary;
import com.guberan.testanalyzer.model.ProjectStats;
import com.guberan.testanalyzer.model.TestNamingStats;
import com.guberan.testanalyzer.model.TokenNgramModel;
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
    private final TokenNgramModel model = new TokenNgramModel();

    private static String buildNgramSummary(List<TokenNgramModel.Trigram> topTrigrams,
                                            List<TokenNgramModel.Bigram> topBigrams,
                                            List<Map.Entry<String, Long>> topTokens,
                                            List<String> typicalTokens) {

        StringBuilder sb = new StringBuilder(8_192);

        sb.append("Typical tokens (greedy, trigram model):\n");
        sb.append(String.join(" ", typicalTokens));
        sb.append("\n\n");

        sb.append("Top tokens (100):\n");
        for (var e : topTokens) {
            sb.append(String.format("- %-24s %d\n", e.getKey(), e.getValue()));
        }
        sb.append("\n");

        sb.append("Top bigrams (100):\n");
        for (var b : topBigrams) {
            sb.append(String.format("- %-16s -> %-16s %d\n", b.prev(), b.next(), b.count()));
        }
        sb.append("\n");

        sb.append("Top trigrams (100):\n");
        for (var t : topTrigrams) {
            sb.append(String.format("- %-16s %-16s -> %-16s %d\n", t.prev1(), t.prev2(), t.next(), t.count()));
        }

        return sb.toString();
    }

    public ProjectStats analyze(Path projectRoot, Consumer<String> progress) {
        progress.accept("Scanning files…");
        ScanResult scan = scanner.scan(projectRoot);

        // Top extensions
        List<ProjectStats.MetricItem> topFileTypes = scan.extensionCounts().entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> new ProjectStats.MetricItem(e.getKey(), e.getValue(), 0f, ""))
                .collect(Collectors.toList());


        // classify java
        progress.accept("Classifying Java files…");
        long javaFiles = scan.javaFiles().size();
        Map<Boolean, List<Path>> parts = scan.javaFiles().stream().collect(Collectors.partitioningBy(classifier::isTestSource));

        List<Path> javaTestFiles = parts.get(true);
        List<Path> javaSourceFiles = parts.get(false);

        // build quick index for source files by “class” path heuristic
        progress.accept("Indexing source classes…");
        Map<String, Path> sourceByFqn = buildSourceIndex(projectRoot, javaSourceFiles);

        // analyze tests
        progress.accept("Analyzing test methods…");
        var testNaming = new TestNamingStats();
        PatternStats patternStats = analyzeTestFiles(projectRoot, javaTestFiles, sourceByFqn, testNaming, progress);

        // infer convention
        progress.accept("Inferring convention…");
        ConventionSummary convention = new ConventionInferer().infer(testNaming);

        // assemble
        ProjectStats stats = new ProjectStats();
        stats.setProjectRoot(projectRoot.toString());
        stats.setTotalFiles(scan.totalFiles());
        stats.setJavaFiles(javaFiles);
        stats.setJavaSourceFiles(javaSourceFiles.size());
        stats.setJavaTestFiles(javaTestFiles.size());

        ProjectStats.MetricReport fileTypes = new ProjectStats.MetricReport(
                "fileTypes",
                "Files Type (Top 10)",
                "Top file extensions by count (useful to understand the project composition).",
                "",
                scan.totalFiles(),
                topFileTypes)
                .addTotal();
        stats.addReport(fileTypes);

        stats.setTestNamingStats(testNaming);

        // reporting
        var topTrigrams = model.topTrigrams(100);
        var topBigrams = model.topBigrams(100);
        var topTokens = model.topUnigrams(100);
        var typical = model.generateGreedy(12);

        String ngramSummary = buildNgramSummary(topTrigrams, topBigrams, topTokens, typical);
        String patternSummary = patternStats.renderTopPatterns(50);

        stats.setConventionSummary(convention);
        stats.setNgramSummary(ngramSummary);
        stats.setPatternSummary(patternSummary);

        return stats;
    }

    private Map<String, Path> buildSourceIndex(Path root, List<Path> sourceFiles) {
        Map<String, Path> map = new HashMap<>();
        for (Path p : sourceFiles) {
            String fullyQualifiedClassName = PathUtil.fqnFromMainJavaPath(root, p).orElse(null);
            if (fullyQualifiedClassName != null) map.put(fullyQualifiedClassName, p);
        }
        return map;
    }

    private PatternStats analyzeTestFiles(Path root,
                                          List<Path> testFiles,
                                          Map<String, Path> sourceByFqn,
                                          TestNamingStats out,
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

            for (var m : methods) {
                if (!isTestMethod(m)) continue;

                out.setTotalTestMethods(out.getTotalTestMethods() + 1);

                String name = m.getNameAsString();
                patternStats.accept(name);
                model.acceptMethodName(name);

                if (name.startsWith("test")) {
                    out.incStartsWithTest();
                }
                if (name.contains("_")) {
                    out.incContainsUnderscore();
                }
                if (NamingUtil.isCamelLike(name)) {
                    out.incCamelCase();
                }
                if (NamingUtil.isPhraseLike(name)) {
                    out.incPhraseLike();
                }
                if (hasDisplayName(m)) {
                    out.incDisplayNameUsed();
                }
                if (!sourceMethods.isEmpty() && sourceMethods.contains(name)) {
                    out.incSameAsSourceMethod();
                }
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

    private boolean hasDisplayName(MethodDeclaration m) {
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
            List<String> tokens = TokenNgramModel.tokenize(methodName);
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