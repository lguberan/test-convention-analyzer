package com.guberan.testanalyzer.util;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility for detecting "phrase-like" test method names.
 *
 * <p>A phrase-like name follows a BDD/spec style such as:
 * <pre>
 *   givenInvalidInput_whenParsing_thenThrow
 *   convertWhenMissingDelimiterShouldFail
 * </pre>
 * <p>
 * The method returns {@code true} if the name contains at least two
 * semantic keywords typically used in test sentences, such as
 * "given", "when", "then", "should", "expect", or "throw".
 */
public class NamingUtil {

    /**
     * Split camelCase and snake_case into tokens
     */
    private static final Pattern SPLIT_PATTERN =
            Pattern.compile("(?<!^)(?=[A-Z])|[_\\-]");

    //    /**
//     * Keywords commonly found in sentence-style test names
//     */
//    private static final Set<String> PHRASE_WORDS1 = Set.of(
//            "given",
//            "when",
//            "then",
//            "should",
//            "expect",
//            "expected",
//            "throw",
//            "throws",
//            "thrown"
//    );
    private static final String[] PHRASE_WORDS = {
            "given", "when", "should", "then", "expect", "return",
            "throw", "fail", "error", "exception", "invalid", "null", "empty"
    };
    // IMPORTANT: longest first, otherwise UserServiceIntegrationTests -> "UserServiceIntegration"
    private static final String[] TEST_CLASS_SUFFIXES = {
            "IntegrationTest",
            "Test",
            "IntegrationTests",
            "Tests",
            "ITCase",
            "IT"
    };
    private static final Set<String> BDD_CORE = Set.of(
            "given", "when", "then", "should", "expect"
    );
    // “verbs” commonly used in tests (action/trigger)
    private static final Set<String> ACTION_WORDS = Set.of(
            "call", "calling",
            "invoke", "invoking",
            "execute", "executing",
            "create", "creating",
            "update", "updating",
            "delete", "deleting",
            "save", "saving",
            "load", "loading",
            "send", "sending",
            "receive", "receiving",
            "parse", "parsing",
            "convert", "converting",
            "process", "processing",
            "handle", "handling",
            "get", "getting",
            "set", "setting",
            "compute", "computing",
            "calculate", "calculating"
    );
    // “assertion/outcome” words (Then/Should style)
    private static final Set<String> OUTCOME_WORDS = Set.of(
            "then", "should", "expect",
            "return", "returns", "returned",
            "throw", "throws", "thrown",
            "fail", "fails", "failed",
            "error", "exception",
            "success", "succeeds", "succeeded",
            "true", "false",
            "null", "empty",
            "invalid", "valid",
            "contains", "equals", "matches",
            "not", "no"
    );

//    /**
//     * Returns true if the supplied method name looks like a sentence/BDD-style phrase.
//     *
//     * <p>The name is considered phrase-like if it contains at least two distinct
//     * keywords from the set {given, when, then, should, expect, throw}.
//     *
//     * @param methodName method name to analyze
//     * @return true if the name resembles a test phrase, false otherwise
//     */
//    public static boolean isPhraseLike1(String methodName) {
//        if (methodName == null || methodName.isBlank()) return false;
//
//        String[] tokens = SPLIT_PATTERN.split(methodName);
//
//        int matches = 0;
//
//        for (String token : tokens) {
//            String lower = token.toLowerCase(Locale.ROOT);
//            if (PHRASE_WORDS1.contains(lower)) {
//                matches++;
//                if (matches >= 2) {
//                    return true;
//                }
//            }
//        }
//
//        return false;
//    }
    // Splits on underscores / hyphens / spaces, and also camelCase boundaries and digit boundaries.
    private static final Pattern TOKEN_SPLIT = Pattern.compile(
            "(?<=[a-z])(?=[A-Z])" +  // fooBar -> foo|Bar
                    "|(?<=[A-Za-z])(?=\\d)" + // foo1 -> foo|1
                    "|(?<=\\d)(?=[A-Za-z])" + // 1foo -> 1|foo
                    "|[_\\-\\s]+"             // snake_case, kebab-case, spaces
    );

    private NamingUtil() {
    }

    // "Camel-like": starts with letter, contains at least one uppercase later, no underscores
    // Also counts lowerCamelCase / UpperCamelCase.
    public static boolean isCamelLike(String s) {
        if (s == null || s.isBlank()) return false;
        if (s.contains("_")) return false;
        if (!Character.isLetter(s.charAt(0))) return false;

        boolean hasUpperInside = false;
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c)) {
                hasUpperInside = true;
                break;
            }
        }
        return hasUpperInside;
    }

    /* ---- BDD kike ----------------------------------------------------------*/

    // no upper case (but allows "_" etc...)
    public static boolean noUpperCase(String s) {

        if (s == null || s.isBlank()) return false;
        return s.chars().noneMatch(Character::isUpperCase);
    }

    /**
     * Returns true if the supplied method name looks like a sentence/BDD-style phrase.
     *
     * <p>The name is considered phrase-like if it contains at least two distinct
     * keywords from the set {given, when, then, should, expect, throw}.
     *
     * @param methodName method name to analyze
     * @return true if the name resembles a test phrase, false otherwise
     */
    public static boolean isPhraseLike(String methodName) {
        if (methodName != null && !methodName.isBlank()) {

            String lower = methodName.toLowerCase();

            int matches = 0;

            for (String w : PHRASE_WORDS) {
                if (lower.contains(w)) {
                    matches++;
                    if (matches >= 2) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Removes common test-class suffixes and returns the corresponding
     * production class name.
     *
     * <p>Examples:
     * <pre>
     *   UserServiceTest              -> UserService
     *   UserServiceTests             -> UserService
     *   UserServiceIT                -> UserService
     *   UserServiceIntegrationTests  -> UserService
     * </pre>
     * <p>
     * Returns {@code null} if the class name does not look like a test class.
     */
    public static String sourceClassNameFromTestClass(String testClass) {
        if (testClass == null || testClass.isBlank()) {
            return null;
        }

        int len = testClass.length();
        for (String suffix : TEST_CLASS_SUFFIXES) {
            if (testClass.endsWith(suffix) && len > suffix.length()) {
                return testClass.substring(0, len - suffix.length());
            }
        }

        return null;
    }

    // ---- Tokenization ----------------------------------------------------------

    public static boolean followsWhenThen(String name) {
        if (name == null || name.isBlank()) return false;

        int when = name.indexOf("When");
        if (when < 0) return false;

        int then = name.indexOf("Then", when + 4);
        return then >= 0;
    }

    private static List<String> tokensOf(String methodName) {
        String[] raw = TOKEN_SPLIT.split(methodName);
        List<String> out = new ArrayList<>(raw.length);
        for (String r : raw) {
            if (r == null || r.isBlank()) continue;
            out.add(r.toLowerCase(Locale.ROOT));
        }
        return out;
    }

    // ---- Main heuristic --------------------------------------------------------

    /**
     * Heuristic: BDD-like if it contains (>=1 action word) AND (>=1 outcome word).
     * Additionally, if the name uses explicit BDD core words (given/when/then/should/expect),
     * we consider it BDD-like when it contains at least 2 distinct core words.
     * <p>
     * This avoids false positives such as "testNullValue" (outcome-only) and
     * catches "parseInvalidJsonThrowsException" (action + outcome).
     */
    public static boolean isBDDLike(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            return false;
        }

        List<String> tokens = tokensOf(methodName);

        // Distinct matches so we don't inflate counts with repeated words
        boolean hasAction = false;
        boolean hasOutcome = false;

        int coreDistinct = 0;
        // Use a tiny set for distinct core tracking
        Set<String> coreSeen = null;

        for (String t : tokens) {
            if (!hasAction && ACTION_WORDS.contains(t)) {
                hasAction = true;
            }
            if (!hasOutcome && OUTCOME_WORDS.contains(t)) {
                hasOutcome = true;
            }

            if (BDD_CORE.contains(t)) {
                if (coreSeen == null) coreSeen = new HashSet<>(4);
                if (coreSeen.add(t)) {
                    coreDistinct++;
                }
            }

            // Fast early exits
            if ((hasAction && hasOutcome) || coreDistinct >= 2) {
                return true;
            }
        }

        return false;
    }
}