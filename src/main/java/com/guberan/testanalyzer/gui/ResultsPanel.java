package com.guberan.testanalyzer.gui;

import com.guberan.testanalyzer.model.ProjectAnalysis;

import javax.swing.*;
import java.awt.*;

/**
 * Swing panel responsible for presenting analysis results.
 *
 * <p>This panel is a pure view: it renders an already computed {@link ProjectAnalysis}
 * (no scanning/parsing logic is done here).
 */
public class ResultsPanel extends JPanel {

    private final JTabbedPane tabs = new JTabbedPane();

    // Per-tab notes (short explanatory text shown above the main content)
    private final JTextArea summaryNotes = new JTextArea();
//    private final JTextArea fileTypesNotes = new JTextArea();
//    private final JTextArea javaNotes = new JTextArea();
//    private final JTextArea testNamingNotes = new JTextArea();
//    private final JTextArea patternsNotes = new JTextArea();
//    private final JTextArea tokenNotes = new JTextArea();

    // Text areas
    private final JTextArea summaryArea = new JTextArea();


    /**
     * Creates the results panel and initializes all Swing components.
     */
    public ResultsPanel() {
        setLayout(new BorderLayout(8, 8));

        configureTextArea(summaryArea);

        configureNotesArea(summaryNotes);
//        configureNotesArea(fileTypesNotes);
//        configureNotesArea(javaNotes);
//        configureNotesArea(testNamingNotes);
//        configureNotesArea(patternsNotes);
//        configureNotesArea(tokenNotes);

        tabs.addTab("Summary", tabWithNotes(summaryNotes, new JScrollPane(summaryArea)));


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
     * Resets the panel to an empty state (before any analysis was run).
     */
    private void setEmptyModels() {

        summaryArea.setText("No results yet.");
    }

    private void insertReportTab(ProjectAnalysis.MetricsReport report) {
        String name = report.getName();
        int index = tabs.indexOfTab(name);
        MetricsReportPanel reportPanel = new MetricsReportPanel(report);
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
    public void setResults(ProjectAnalysis projectAnalysis) {
        // Summary
        summaryArea.setText(projectAnalysis.prettySummary());
        summaryArea.setCaretPosition(0);

        for (ProjectAnalysis.MetricsReport report : projectAnalysis.getReports().values().stream().sorted().toList()) {
            insertReportTab(report);
        }
    }
}
