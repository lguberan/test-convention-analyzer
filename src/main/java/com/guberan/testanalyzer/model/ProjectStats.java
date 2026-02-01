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

    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.00%");

    private final Map<String, MetricReport> reports = new HashMap<>();

    StringBuilder sb = new StringBuilder();

    private String projectRoot;
    private ConventionSummary conventionSummary;
    private String ngramSummary;
    private String patternSummary;


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
        sb.append("Project root: ").append(projectRoot).append("\n\n");

//        sb.append("Files scanned: ").append(totalFiles).append("\n");
//        sb.append(".java files:   ").append(totalJavaFiles).append(" (").append(PERCENT_FORMAT.format(ratio(totalJavaFiles, totalFiles))).append(")\n");
//        sb.append("  - sources:   ").append(totalJavaSourceFiles).append(" (").append(PERCENT_FORMAT.format(ratio(totalJavaSourceFiles, totalJavaFiles))).append(")\n");
//        sb.append("  - tests:     ").append(totalJavaTestFiles).append(" (").append(PERCENT_FORMAT.format(ratio(totalJavaTestFiles, totalJavaFiles))).append(")\n\n");

//        if (testNamingStats != null) {
//            var t = testNamingStats;
//            sb.append("Test methods detected: ").append(t.getTotalTestMethods()).append("\n");
//            sb.append("  startsWith 'test':   ").append(t.getStartsWithTest()).append(" (").append(PERCENT_FORMAT.format(ratio(t.getStartsWithTest(), t.getTotalTestMethods()))).append(")\n");
//            sb.append("  contains '_':        ").append(t.getContainsUnderscore()).append(" (").append(PERCENT_FORMAT.format(ratio(t.getContainsUnderscore(), t.getTotalTestMethods()))).append(")\n");
//            sb.append("  camelCase:           ").append(t.getCamelCase()).append(" (").append(PERCENT_FORMAT.format(ratio(t.getCamelCase(), t.getTotalTestMethods()))).append(")\n");
//            sb.append("  @DisplayName used:   ").append(t.getDisplayNameUsed()).append(" (").append(PERCENT_FORMAT.format(ratio(t.getDisplayNameUsed(), t.getTotalTestMethods()))).append(")\n");
//            sb.append("  same as source meth: ").append(t.getSameAsSourceMethod()).append(" (").append(PERCENT_FORMAT.format(ratio(t.getSameAsSourceMethod(), t.getTotalTestMethods()))).append(")\n\n");
//        }

        if (conventionSummary != null) {
            sb.append("De-facto convention:\n");
            sb.append("  ").append(conventionSummary.getHeadline()).append("\n");
            sb.append("  ").append(conventionSummary.getRationale()).append("\n");
        }

        return sb.toString();
    }

    public enum ReportEnum {SUMMARY, FILE_TYPES, SRC_VS_TEST, TEST_METHOD_NAMING, TOKENS, PATTERNS}

    @Data
    @AllArgsConstructor
    public static final class MetricReport implements Comparable<MetricReport> {
        private String id;
        private int order;
        private String name;
        private String summary;
        private String tooltip;
        private long totalCount;
        private List<MetricItem> items;

        public MetricReport(String id, int order, String name, String summary) {
            this.name = name;
            this.order = order;
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
            if (this.totalCount == 0) {
                for (MetricItem item : items) {
                    item.percent = 0.0f;
                }
            } else {
                float floatSum = this.totalCount;
                for (MetricItem item : items) {
                    item.percent = item.getCount() / floatSum;
                }
            }
            return this;
        }

        public MetricReport addTotal() {
            computeRatios();
            add(new MetricItem("TOTAL", this.totalCount, 1.0f, "total of all lines"));
            return this;
        }

        @Override
        public int compareTo(MetricReport other) {
            int c = Integer.compare(this.order, other.order);
            if (c != 0) return c;
            return this.name.compareTo(other.name);
        }
    }

    @Data
    @AllArgsConstructor
    public static final class MetricItem implements Comparable<MetricItem> {
        private String name;
        private long count;
        private float percent;
        private String tooltip;

        public static MetricItem of(String name, long count) {
            return new MetricItem(name, count, 0.0f, "");
        }

        public static MetricItem of(Map.Entry<String, Long> entry) {
            return new MetricItem(entry.getKey(), entry.getValue(), 0.0f, "");
        }

        public long inc() {
            return ++count;
        }

        @Override
        public int compareTo(MetricItem other) {
            // highest first
            return Long.compare(other.count, this.count);
        }
    }
}