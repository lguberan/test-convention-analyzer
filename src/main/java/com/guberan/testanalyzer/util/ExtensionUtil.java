package com.guberan.testanalyzer.util;

public class ExtensionUtil {
    public static String extensionOf(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "(no-ext)";
        return filename.substring(idx + 1).toLowerCase();
    }
}