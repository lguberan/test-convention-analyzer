package com.guberan.testanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ProjectStats {
    public static final String FILE_TYPES = "fileTypes";
    public static final String TEST_METHOD_NAMING = "testMethodNaming";

    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.00%");

    private String projectRoot;
    private long totalFiles;
    private long javaFiles;
    private long javaSourceFiles;
    private long javaTestFiles;
    //private List<FileTypeStat> topFileTypes;
    private TestNamingStats testNamingStats;
    private ConventionSummary conventionSummary;
    private String ngramSummary;
    private String patternSummary;
    private Map<String, MetricReport> reports = new HashMap<>();

    private MetricReport fileTypes = new MetricReport(
            FILE_TYPES,
            "Files Type (Top 10)",
            "Top file extensions by count (useful to understand the project composition).");

    private MetricReport testMethodNaming = new MetricReport(
            TEST_METHOD_NAMING,
            "Test Method Naming",
            "How test method names are written (prefix 'test', underscores, camelCase, @DisplayName, etc.).");


    public ProjectStats() {
        addReport(fileTypes);
        addReport(testMethodNaming);
    }

    public MetricReport addReport(MetricReport report) {
        reports.put(report.getId(), report);
        return report;
    }

    public MetricReport getReport(String id) {
        return reports.get(id);
    }


    public double ratio(long a, long b) {
        if (b == 0) return 0.0;
        return ((double) a) / ((double) b);
    }

    public String prettySummary() {
        var sb = new StringBuilder();
        sb.append("Project root: ").append(projectRoot).append("\n\n");

        sb.append("Files scanned: ").append(totalFiles).append("\n");
        sb.append(".java files:   ").append(javaFiles).append(" (").append(PERCENT_FORMAT.format(ratio(javaFiles, totalFiles))).append(")\n");
        sb.append("  - sources:   ").append(javaSourceFiles).append(" (").append(PERCENT_FORMAT.format(ratio(javaSourceFiles, javaFiles))).append(")\n");
        sb.append("  - tests:     ").append(javaTestFiles).append(" (").append(PERCENT_FORMAT.format(ratio(javaTestFiles, javaFiles))).append(")\n\n");

        if (testNamingStats != null) {
            var t = testNamingStats;
            sb.append("Test methods detected: ").append(t.getTotalTestMethods()).append("\n");
            sb.append("  startsWith 'test':   ").append(t.getStartsWithTest()).append(" (").append(PERCENT_FORMAT.format(ratio(t.getStartsWithTest(), t.getTotalTestMethods()))).append(")\n");
            sb.append("  contains '_':        ").append(t.getContainsUnderscore()).append(" (").append(PERCENT_FORMAT.format(ratio(t.getContainsUnderscore(), t.getTotalTestMethods()))).append(")\n");
            sb.append("  camelCase:           ").append(t.getCamelCase()).append(" (").append(PERCENT_FORMAT.format(ratio(t.getCamelCase(), t.getTotalTestMethods()))).append(")\n");
            sb.append("  @DisplayName used:   ").append(t.getDisplayNameUsed()).append(" (").append(PERCENT_FORMAT.format(ratio(t.getDisplayNameUsed(), t.getTotalTestMethods()))).append(")\n");
            sb.append("  same as source meth: ").append(t.getSameAsSourceMethod()).append(" (").append(PERCENT_FORMAT.format(ratio(t.getSameAsSourceMethod(), t.getTotalTestMethods()))).append(")\n\n");
        }

        if (conventionSummary != null) {
            sb.append("De-facto convention:\n");
            sb.append("  ").append(conventionSummary.getHeadline()).append("\n");
            sb.append("  ").append(conventionSummary.getRationale()).append("\n");
        }

        return sb.toString();
    }

    @Data
    @AllArgsConstructor
    public static final class MetricReport {
        private String id;
        private String name;
        private String summary;
        private String tooltip;
        private long totalCount;
        private List<MetricItem> items;

        public MetricReport(String id, String name, String summary) {
            this.name = name;
            this.summary = summary;
            this.tooltip = "";
            this.items = new ArrayList<>();
        }

        public boolean add(MetricItem metricItem) {
            return items.add(metricItem);
        }

        public MetricItem get(String name) {
            return items.stream().filter(item -> item.getName().equals(name)).findFirst().orElse(null);
        }

        public MetricReport computeRatios() {
            if (this.totalCount == 0) {
                long sum = 0;
                for (MetricItem item : items) {
                    sum += item.count;
                }
                this.totalCount = sum;
            }
            float floatSum = this.totalCount;
            for (MetricItem item : items) {
                item.percent = item.getCount() / floatSum;
            }
            return this;
        }

        public MetricReport addTotal() {
            computeRatios();
            add(new MetricItem("TOTAL", this.totalCount, 1.0f, "total of all lines"));
            return this;
        }
    }

    @Data
    @AllArgsConstructor
    public static final class MetricItem {
        private String name;
        private long count;
        private float percent;
        private String tooltip;

        public long inc() {
            return ++count;
        }
    }
}