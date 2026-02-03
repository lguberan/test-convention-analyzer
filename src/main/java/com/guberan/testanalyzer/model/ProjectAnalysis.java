package com.guberan.testanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ProjectAnalysis {

    private final Map<ReportId, MetricsReport> reports = new HashMap<>();

    private String projectRoot;
    private ConventionSummary conventionSummary;


    public MetricsReport addReport(MetricsReport report) {
        reports.put(report.getId(), report);
        return report;
    }

    public MetricsReport getReport(String id) {
        return reports.get(id);
    }

    public String prettySummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Project root: ").append(projectRoot).append("\n\n");

        if (conventionSummary != null) {
            sb.append("De-facto convention:\n");
            sb.append("  ").append(conventionSummary.getHeadline()).append("\n");
            sb.append("  ").append(conventionSummary.getRationale()).append("\n");
        }

        return sb.toString();
    }

    public enum ReportId {SUMMARY, FILE_TYPES, SRC_VS_TEST, TEST_METHOD_NAMING, TOKENS, PATTERNS}

    @Data
    @AllArgsConstructor
    public static final class MetricsReport implements Comparable<MetricsReport> {
        private ReportId id;
        //       private int order;
        private String name;
        private String summary;
        private String helpText;
        private long totalCount;
        private List<MetricRecord> items;

        public MetricsReport(ReportId id, int order, String name, String summary) {
            this.id = id;
            this.name = name;
            //         this.order = order;
            this.summary = summary;
            this.helpText = "";
            this.totalCount = 0;
            this.items = new ArrayList<>();
        }

        public boolean add(MetricRecord metricRecord) {
            return items.add(metricRecord);
        }

//        public MetricRecord get(String name) {
//            return items.stream().filter(item -> item.getName().equals(name)).findFirst().orElse(null);
//        }

        public MetricsReport computeRatios() {
            if (this.totalCount == 0) {
                long sum = 0;
                for (MetricRecord item : items) {
                    sum += item.count;
                }
                this.totalCount = sum;
            }
            if (this.totalCount == 0) {
                for (MetricRecord item : items) {
                    item.percent = 0.0f;
                }
            } else {
                float floatSum = this.totalCount;
                for (MetricRecord item : items) {
                    item.percent = item.getCount() / floatSum;
                }
            }
            return this;
        }

//        public MetricsReport addTotal() {
//            computeRatios();
//            add(new MetricRecord("TOTAL", this.totalCount, 1.0f, "total of all lines"));
//            return this;
//        }

        @Override
        public int compareTo(MetricsReport other) {
            // sort by enum declaration order (ReportId ordinal), then by name
            int c = Integer.compare(this.id.ordinal(), other.id.ordinal());
            if (c != 0) return c;
            return this.name.compareTo(other.name);
        }
    }

    @Data
    @AllArgsConstructor
    public static final class MetricRecord implements Comparable<MetricRecord> {
        private String name;
        private long count;
        private float percent;
        private String samples;

        public static MetricRecord of(String name, long count) {
            return new MetricRecord(name, count, 0.0f, "");
        }

        public static MetricRecord of(Map.Entry<String, Long> entry) {
            return new MetricRecord(entry.getKey(), entry.getValue(), 0.0f, "");
        }

        public long inc() {
            return ++count;
        }

        @Override
        public int compareTo(MetricRecord other) {
            // highest count first; then name for stable ordering
            int c = Long.compare(other.count, this.count);
            if (c != 0) return c;
            return this.name.compareTo(other.name);
        }
    }
}