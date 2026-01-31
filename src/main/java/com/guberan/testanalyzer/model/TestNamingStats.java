package com.guberan.testanalyzer.model;

import lombok.Data;

@Data
public class TestNamingStats {
    private long totalTestMethods;
    private long startsWithTest;
    private long containsUnderscore;
    private long camelCase;
    private long phraseLike;
    private long displayNameUsed;
    private long sameAsSourceMethod;
    

    public void incTotalTestMethods() {
        totalTestMethods++;
    }

    public void incStartsWithTest() {
        startsWithTest++;
    }

    public void incContainsUnderscore() {
        containsUnderscore++;
    }

    public void incCamelCase() {
        camelCase++;
    }

    public void incPhraseLike() {
        phraseLike++;
    }

    public void incDisplayNameUsed() {
        displayNameUsed++;
    }

    public void incSameAsSourceMethod() {
        sameAsSourceMethod++;
    }
}