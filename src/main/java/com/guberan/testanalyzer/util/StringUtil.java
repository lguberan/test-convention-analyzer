package com.guberan.testanalyzer.util;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringUtil {
    
    public static String extensionOf(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "(no-ext)";
        return filename.substring(idx + 1).toLowerCase();
    }

    public static String concatWithMaxLines(String a, String b, int maxLines) {
        return Stream.concat(a.lines(), b.lines())
                .limit(maxLines)
                .collect(Collectors.joining("\n"));
    }
}