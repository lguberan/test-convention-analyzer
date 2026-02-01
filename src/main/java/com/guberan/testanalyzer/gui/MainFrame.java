package com.guberan.testanalyzer.gui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        super("Test Convention Analyzer");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);

        var runPanel = new RunPanel();
        var resultsPanel = new ResultsPanel();

        runPanel.setOnResults(resultsPanel::setResults);

        setLayout(new BorderLayout(8, 8));
        add(runPanel, BorderLayout.NORTH);
        add(resultsPanel, BorderLayout.CENTER);
    }
}