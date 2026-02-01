package com.guberan.testanalyzer.service;

import com.guberan.testanalyzer.model.ConventionSummary;
import com.guberan.testanalyzer.model.NamingModel;

public class ConventionInferer {

    public ConventionSummary infer(NamingModel s) {
        long total = 0; // s.getTotalTestMethods();
        if (total == 0) {
            return new ConventionSummary(
                    "No test methods detected",
                    "No methods annotated with JUnit @Test-like annotations were found."
            );
        }

//        double pctTestPrefix = (double) s.getStartsWithTest() / total;
//        double pctUnderscore = (double) s.getContainsUnderscore() / total;
//        double pctCamel = (double) s.getCamelCase() / total;
//        double pctDisplay = (double) s.getDisplayNameUsed() / total;
//        double pctPhraseLike = (double) s.getPhraseLike() / total;

        // Very simple “winner take most”
        String headline = "";
        String rationale = "";
//
//        if (pctCamel >= 0.60 && pctTestPrefix < 0.30 && pctUnderscore < 0.20) {
//            headline = "lowerCamelCase method names, no 'test' prefix, minimal underscores";
//            rationale = String.format("camel-like: %.1f%%, startsWith 'test': %.1f%%, underscores: %.1f%%, @DisplayName: %.1f%%",
//                    pctCamel * 100, pctTestPrefix * 100, pctUnderscore * 100, pctDisplay * 100);
//        } else if (pctTestPrefix >= 0.50) {
//            headline = "Legacy 'test' prefix style is dominant";
//            rationale = String.format("startsWith 'test': %.1f%% (camel-like: %.1f%%, underscores: %.1f%%, @DisplayName: %.1f%%)",
//                    pctTestPrefix * 100, pctCamel * 100, pctUnderscore * 100, pctDisplay * 100);
//        } else if (pctUnderscore >= 0.40) {
//            headline = "Underscore-based naming (snake-ish) is common";
//            rationale = String.format("contains '_': %.1f%% (camel-like: %.1f%%, startsWith 'test': %.1f%%, @DisplayName: %.1f%%)",
//                    pctUnderscore * 100, pctCamel * 100, pctTestPrefix * 100, pctDisplay * 100);
//        } else {
//            headline = "Mixed naming conventions";
//            rationale = String.format("camel-like: %.1f%%, startsWith 'test': %.1f%%, underscores: %.1f%%, @DisplayName: %.1f%%",
//                    pctCamel * 100, pctTestPrefix * 100, pctUnderscore * 100, pctDisplay * 100);
//        }

        return new ConventionSummary(headline, rationale);
    }
}