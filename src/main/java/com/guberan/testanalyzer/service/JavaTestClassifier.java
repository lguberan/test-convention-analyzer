package com.guberan.testanalyzer.service;

import java.nio.file.Path;

public class JavaTestClassifier {

    private static final String[] TEST_SUFFIXES = {"Test.java", "Tests.java", "IT.java", "IntegrationTest.java"};

    public boolean isTestSource(Path file) {
        String norm = file.toString().replace('\\', '/');
        if (norm.contains("/src/test/java/")) return true;
        if (norm.contains("/src/main/java/")) return false;

        // fallback heuristic
        String name = file.getFileName().toString();
        for (String suffix : TEST_SUFFIXES) {
            if (name.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTestClass(Path file) {
        String name = file.getFileName().toString();
        for (String suffix : TEST_SUFFIXES) {
            if (name.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

}