package com.guberan.testanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileTypeStat {
    private String extension; // e.g. "java", "kt", "(no-ext)"
    private long count;
    private double percent;
}