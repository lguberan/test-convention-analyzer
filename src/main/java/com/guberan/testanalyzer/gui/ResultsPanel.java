package com.guberan.testanalyzer.gui;

import com.guberan.testanalyzer.model.ProjectStats;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Swing panel responsible for presenting analysis results.
 *
 * <p>This panel is a pure view: it renders an already computed {@link ProjectStats}
 * (no scanning/parsing logic is done here).
 */
public class ResultsPanel extends JPanel {

    /**
     * Percent formatter shared by all tables.
     */
    private static final DecimalFormat PCT = new DecimalFormat("0.00%");

    private final JTabbedPane tabs = new JTabbedPane();

    // Per-tab notes (short explanatory text shown above the main content)
    private final JTextArea summaryNotes = new JTextArea();
    private final JTextArea fileTypesNotes = new JTextArea();
    private final JTextArea javaNotes = new JTextArea();
    private final JTextArea testNamingNotes = new JTextArea();
    private final JTextArea patternsNotes = new JTextArea();
    private final JTextArea tokenNotes = new JTextArea();

    // Text areas
    private final JTextArea summaryArea = new JTextArea();


    /**
     * Creates the results panel and initializes all Swing components.
     */
    public ResultsPanel() {
        setLayout(new BorderLayout(8, 8));

        configureTextArea(summaryArea);

        configureNotesArea(summaryNotes);
        configureNotesArea(fileTypesNotes);
        configureNotesArea(javaNotes);
        configureNotesArea(testNamingNotes);
        configureNotesArea(patternsNotes);
        configureNotesArea(tokenNotes);

        tabs.addTab("Summary", tabWithNotes(summaryNotes, new JScrollPane(summaryArea)));
//        tabs.addTab("File Types (Top 10)", tabWithNotes(fileTypesNotes, new JScrollPane(fileTypesTable)));
//        tabs.addTab("Java Source vs Test", tabWithNotes(javaNotes, new JScrollPane(javaTable)));
//        tabs.addTab("Test Method Naming", tabWithNotes(testNamingNotes, new JScrollPane(testNamingTable)));
//        tabs.addTab("Patterns", tabWithNotes(patternsNotes, new JScrollPane(patternTable)));
//        tabs.addTab("Tokens", tabWithNotes(tokenNotes, new JScrollPane(tokenTable)));


        add(tabs, BorderLayout.CENTER);

        setEmptyModels();
    }

    /**
     * Applies common configuration to all text areas (read-only + monospaced font).
     */
    private static void configureTextArea(JTextArea area) {
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }

    /**
     * Configures a small non-editable note area used at the top of each tab.
     */
    private static void configureNotesArea(JTextArea area) {
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        area.setBorder(BorderFactory.createEmptyBorder(2, 2, 6, 2));
    }

    /**
     * Creates a tab panel with an explanatory note area on top and the main component below.
     */
    private static JPanel tabWithNotes(JTextArea notes, JComponent main) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.add(notes, BorderLayout.NORTH);
        p.add(main, BorderLayout.CENTER);
        return p;
    }

    /**
     * Parses the textual pattern report and fills a Metric/Value/Percent table.
     * Expected line format: "  58 (12.4%) : When <any> Then <any>".
     */
    private static DefaultTableModel patternTableModelFromReport(String report) {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Metric", "Value", "Percent"}, 0);
        if (report == null || report.isBlank()) return model;

        for (String line : report.split("\\R")) {
            String s = line.trim();
            if (s.isEmpty()) continue;

            int pctOpen = s.indexOf('(');
            int pctClose = s.indexOf(')');
            int colon = s.indexOf(':');
            if (pctOpen < 0 || pctClose < 0 || colon < 0 || pctClose < pctOpen) {
                continue;
            }

            String countPart = s.substring(0, pctOpen).trim();
            String pctPart = s.substring(pctOpen + 1, pctClose).trim();
            String pattern = s.substring(colon + 1).trim();

            long count;
            try {
                count = Long.parseLong(countPart);
            } catch (NumberFormatException ignore) {
                continue;
            }

            String pctNormalized = pctPart.endsWith("%") ? pctPart : (pctPart + "%");
            model.addRow(new Object[]{pattern, count, pctNormalized});
        }

        return model;
    }

    /**
     * Resets the panel to an empty state (before any analysis was run).
     */
    private void setEmptyModels() {
//        fileTypesTable.setModel(new DefaultTableModel(new Object[]{"Extension", "Files", "Percent"}, 0));
//        javaTable.setModel(new DefaultTableModel(new Object[]{"Metric", "Value", "Percent"}, 0));
//        testNamingTable.setModel(new DefaultTableModel(new Object[]{"Metric", "Value", "Percent"}, 0));
//        patternTable.setModel(new DefaultTableModel(new Object[]{"Metric", "Value", "Percent"}, 0));

        summaryNotes.setText("High-level report and key totals for the analyzed project.");
        fileTypesNotes.setText("Top file extensions by count (useful to understand the project composition).");
        javaNotes.setText("Breakdown of Java files into production sources vs test sources (based on src/main/java and src/test/java).");
        testNamingNotes.setText("How test method names are written (prefix 'test', underscores, camelCase, @DisplayName, etc.).");
        patternsNotes.setText("Most common phrase templates extracted from test method names (e.g., 'When <any> Then <any>').");
        tokenNotes.setText("Token-level statistics. Useful for exploratory analysis, but less actionable than phrase patterns.");

        summaryArea.setText("No results yet.");
    }

    private void insertReportTab(ProjectStats.MetricReport report) {
        String name = report.getName();
        int index = tabs.indexOfTab(name);
        MetricReportPanel reportPanel = new MetricReportPanel(report);
        if (index >= 0) {
            tabs.remove(index);
            tabs.insertTab(name, null, reportPanel, null, index);
        } else {
            tabs.addTab(name, reportPanel);
        }
    }

    /**
     * Populates the panel with fresh analysis results.
     */
    public void setResults(ProjectStats stats) {
        // Summary
        summaryArea.setText(stats.prettySummary());
        summaryArea.setCaretPosition(0);


        for (ProjectStats.MetricReport report : stats.getReports().values().stream().sorted().toList()) {
            insertReportTab(report);
        }


        // Test naming metrics
        // var tn = stats.getTestNamingStats();
        //    var tnModel = new DefaultTableModel(new Object[]{"Metric", "Value", "Percent"}, 0);
//        tnModel.addRow(new Object[]{"Test methods (detected)", tn.getTotalTestMethods(), PCT.format(1.0)});
//        tnModel.addRow(new Object[]{"Start with 'test'", tn.getStartsWithTest(), PCT.format(stats.ratio(tn.getStartsWithTest(), tn.getTotalTestMethods()))});
//        tnModel.addRow(new Object[]{"Contain '_'", tn.getContainsUnderscore(), PCT.format(stats.ratio(tn.getContainsUnderscore(), tn.getTotalTestMethods()))});
//        tnModel.addRow(new Object[]{"CamelCase / lowerCamel", tn.getCamelCase(), PCT.format(stats.ratio(tn.getCamelCase(), tn.getTotalTestMethods()))});
//        tnModel.addRow(new Object[]{"Phrase-like (BDD/spec)", tn.getPhraseLike(), PCT.format(stats.ratio(tn.getPhraseLike(), tn.getTotalTestMethods()))});
//        tnModel.addRow(new Object[]{"@DisplayName present", tn.getDisplayNameUsed(), PCT.format(stats.ratio(tn.getDisplayNameUsed(), tn.getTotalTestMethods()))});
//        tnModel.addRow(new Object[]{"Same as source method (heuristic)", tn.getSameAsSourceMethod(), PCT.format(stats.ratio(tn.getSameAsSourceMethod(), tn.getTotalTestMethods()))});
        //      testNamingTable.setModel(tnModel);

        // Patterns (rendered as table)
        //  patternTable.setModel(patternTableModelFromReport(stats.getPatternSummary()));

    }
}
