package com.guberan.testanalyzer.gui;

import com.guberan.testanalyzer.model.ProjectAnalysis.MetricRecord;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;


public class MetricTableModel extends AbstractTableModel {

    private final List<MetricRecord> rows = new ArrayList<>();
    private String[] columnNames = {"Metric", "Value", "Percent"};

    // ---------- basic model ----------

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return switch (column) {
            case 0 -> String.class;
            case 1 -> Long.class;
            case 2 -> Float.class; // Float.class ?
            default -> Object.class;
        };
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        MetricRecord item = rows.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> item.getName();
            case 1 -> item.getCount();
            case 2 -> item.getPercent() * 100;// (String) PCT.format(item.getPercent());
            default -> null;
        };
    }

    // ---------- convenience API (important part) ----------

    public MetricRecord getRow(int row) {
        return rows.get(row);
    }

    public void setRows(List<MetricRecord> items) {
        rows.clear();
        rows.addAll(items);
        fireTableDataChanged();
    }

    public void addRow(MetricRecord item) {
        int r = rows.size();
        rows.add(item);
        fireTableRowsInserted(r, r);
    }

    public void clear() {
        rows.clear();
        fireTableDataChanged();
    }
}