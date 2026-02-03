package com.guberan.testanalyzer.gui;

public enum Grade {
    ALWAYS("✅✅ Always"),
    OFTEN("✅ Often"),
    SOMETIMES("👍 Sometimes"),
    RARELY("⚠️ Rarely"),
    NEVER("❌ Never"),
    NA("• N/A");

    private final String label;

    Grade(String label) {
        this.label = label;
    }

    public static Grade fromRatio(double ratio) {
        // if (ratio == null) return NA;
        if (ratio >= 0.95) return ALWAYS;
        if (ratio >= 0.70) return OFTEN;
        if (ratio >= 0.30) return SOMETIMES;
        if (ratio > 0.05) return RARELY;
        return NEVER;
    }

    public String label() {
        return label;
    }
}
