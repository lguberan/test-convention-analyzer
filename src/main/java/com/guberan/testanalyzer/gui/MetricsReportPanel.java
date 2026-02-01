package com.guberan.testanalyzer.gui;

import com.guberan.testanalyzer.model.ProjectAnalysis.MetricItem;
import com.guberan.testanalyzer.model.ProjectAnalysis.MetricsReport;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;

/**
 * Generic panel that renders a {@link MetricsReport} as a JTable.
 *
 * <p>Intended as a reusable "report tab" view: one report = one table.</p>
 */
public class MetricsReportPanel extends JPanel {

    private final JTextArea notes = new JTextArea();
    private final MetricTableModel model = new MetricTableModel();
    private final JTable table = new JTable(model) {
        @Override
        public String getToolTipText(MouseEvent e) {
            int viewRow = rowAtPoint(e.getPoint());
            if (viewRow < 0) return null;

            int modelRow = convertRowIndexToModel(viewRow);
            MetricItem item = model.getRow(modelRow);

            if (item.getTooltip() != null && !item.getTooltip().isBlank()) {
                return item.getTooltip();
            }
            return null;
        }
    };

    public MetricsReportPanel(MetricsReport report) {
        super(new BorderLayout(8, 8));

        configureNotesArea(notes);

        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setRowSorter(new TableRowSorter<>(model));

        // Percent rendering (MetricTableModel returns percent*100 as Float)
        table.setDefaultRenderer(Float.class, new DecimalRenderer("#0.00' %'"));

        add(notes, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        setReport(report);
    }

    private static void configureNotesArea(JTextArea area) {
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        area.setBorder(BorderFactory.createEmptyBorder(2, 2, 6, 2));
    }

    public void setReport(MetricsReport report) {
        if (report == null) {
            notes.setText("No report.");
            model.clear();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(report.getName() == null ? "" : report.getName());

        if (report.getSummary() != null && !report.getSummary().isBlank()) {
            sb.append("\n").append(report.getSummary().trim());
        }
        if (report.getTooltip() != null && !report.getTooltip().isBlank()) {
            sb.append("\n").append(report.getTooltip().trim());
        }
        notes.setText(sb.toString());

        model.setRows(report.getItems());
    }

    /* -- DecimalRenderer -- */
    public static class DecimalRenderer extends DefaultTableCellRenderer {

        private final DecimalFormat fmt;

        public DecimalRenderer(String pattern) {
            this.fmt = new DecimalFormat(pattern);
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        protected void setValue(Object value) {
            setText(value == null ? "" : fmt.format(((Number) value).doubleValue()));
        }
    }
}