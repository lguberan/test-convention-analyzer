package com.guberan.testanalyzer.gui;

import com.guberan.testanalyzer.model.ProjectAnalysis.MetricRecord;
import com.guberan.testanalyzer.model.ProjectAnalysis.MetricsReport;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
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
    private final JTextArea details = new JTextArea();
    private final MetricTableModel model = new MetricTableModel();
    private final JTable table = new JTable(model) {
        @Override
        public String getToolTipText(MouseEvent e) {
            int viewRow = rowAtPoint(e.getPoint());
            if (viewRow < 0) return null;

            int modelRow = convertRowIndexToModel(viewRow);
            MetricRecord item = model.getRow(modelRow);

//            if (item.getTooltip() != null && !item.getTooltip().isBlank()) {
//                return item.getTooltip();
//            }
            return null;
        }
    };

    public MetricsReportPanel(MetricsReport report) {
        super(new BorderLayout(8, 8));

        configureNotesArea(notes);
        configureDetailsArea(details);

        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setRowSorter(new TableRowSorter<>(model));

        // Renderers
        table.setDefaultRenderer(Float.class, new DecimalRenderer("#0.00' %'"));
        table.setDefaultRenderer(Long.class, new DecimalRenderer("#,##0"));

        // Column sizing: keep numeric columns narrow, let the "name" column take the rest
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        configureColumnWidths();

        // Selection -> update persistent details area
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(this::onSelectionChanged);

        // ESC clears selection + details (works regardless of scroll position)
        InputMap im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = table.getActionMap();
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "clearSelection");
        am.put("clearSelection", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                // Clear selection
                table.clearSelection();

                // Also clear lead/anchor indices so the JTable doesn't keep a focused cell rectangle
                ListSelectionModel rsm = table.getSelectionModel();
                rsm.setAnchorSelectionIndex(-1);
                rsm.setLeadSelectionIndex(-1);

                ListSelectionModel csm = table.getColumnModel().getSelectionModel();
                csm.setAnchorSelectionIndex(-1);
                csm.setLeadSelectionIndex(-1);

                // Reset details
                details.setText("Select a row to display examples here.");
                details.setCaretPosition(0);

                // Move focus away from the table to remove the focus border/rectangle
                details.requestFocusInWindow();
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);

        JScrollPane detailsScroll = new JScrollPane(details);
        detailsScroll.setBorder(new TitledBorder("Examples / details"));
        detailsScroll.setMinimumSize(new Dimension(260, 120));
        detailsScroll.setPreferredSize(new Dimension(340, 200));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, detailsScroll);

        // Give more space to the right (details) panel.
        // resizeWeight = proportion kept by LEFT component on resize.
        // 0.35 => ~35% left (table), ~65% right (details)
        split.setResizeWeight(0.35);
        split.setContinuousLayout(true);

        // Set an initial divider position favoring the details area
        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.35));

        add(notes, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

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

    private static void configureDetailsArea(JTextArea area) {
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        area.setText("Select a row to display examples here.");
        area.setCaretPosition(0);
    }

    private void configureColumnWidths() {
        // Assumes 3 columns: [0]=name, [1]=count (Long), [2]=percent (Float)
        TableColumnModel cm = table.getColumnModel();

        // Column 0: Name (wide, flexible)
        TableColumn c0 = cm.getColumn(0);
        c0.setMinWidth(200);
        c0.setPreferredWidth(360);

        // Column 1: Count (narrow)
        TableColumn c1 = cm.getColumn(1);
        c1.setMinWidth(80);
        c1.setPreferredWidth(95);
        c1.setMaxWidth(140);

        // Column 2: Percent (narrow)
        TableColumn c2 = cm.getColumn(2);
        c2.setMinWidth(80);
        c2.setPreferredWidth(95);
        c2.setMaxWidth(140);
    }

    private void onSelectionChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        updateDetailsFromSelection();
    }

    private void updateDetailsFromSelection() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            details.setText("Select a row to display examples here.");
            details.setCaretPosition(0);
            return;
        }

        int modelRow = table.convertRowIndexToModel(viewRow);
        MetricRecord item = model.getRow(modelRow);

        String examples = item.getSamples(); // re-used field: contains example method names/lines
        if (examples == null || examples.isBlank()) {
            details.setText("No examples available for: " + item.getName());
            details.setCaretPosition(0);
            return;
        }

        details.setText(examples);
//        StringBuilder sb = new StringBuilder();
//        sb.append(item.getName()).append("\n\n");
//        sb.append(examples.trim());
//
//        details.setText(sb.toString());
        details.setCaretPosition(0);
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
        if (report.getHelpText() != null && !report.getHelpText().isBlank()) {
            sb.append("\n").append(report.getHelpText().trim());
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
            setText(value == null ? "" : fmt.format(value));
        }
    }
}