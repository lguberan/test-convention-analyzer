package com.guberan.testanalyzer.util;

import java.util.Locale;
import java.util.Set;
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
     * Keywords commonly found in sentence-style test names
     */
    private static final Set<String> PHRASE_WORDS1 = Set.of(
            "given",
            "when",
            "then",
            "should",
            "expect",
            "expected",
            "throw",
            "throws",
            "thrown"
    );
    /**
     * Split camelCase and snake_case into tokens
     */
    private static final Pattern SPLIT_PATTERN =
            Pattern.compile("(?<!^)(?=[A-Z])|[_\\-]");

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
    public static boolean isPhraseLike1(String methodName) {
        if (methodName == null || methodName.isBlank()) return false;

        String[] tokens = SPLIT_PATTERN.split(methodName);

        int matches = 0;

        for (String token : tokens) {
            String lower = token.toLowerCase(Locale.ROOT);
            if (PHRASE_WORDS1.contains(lower)) {
                matches++;
                if (matches >= 2) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isPhraseLike(String methodName) {
        if (methodName == null || methodName.isBlank()) return false;

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

    public static boolean followsWhenThen(String name) {
        if (name == null || name.isBlank()) return false;

        int when = name.indexOf("When");
        if (when < 0) return false;

        int then = name.indexOf("Then", when + 4);
        return then >= 0;
    }
}