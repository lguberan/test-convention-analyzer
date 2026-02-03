package com.guberan.testanalyzer.gui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        super("Test Convention Analyzer");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);

        Image icon = new ImageIcon(MainFrame.class.getResource("/icons/icon.png")).getImage();
        setIconImage(icon);

        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                taskbar.setIconImage(icon);
            }
        }

        var runPanel = new RunPanel();
        var resultsPanel = new ResultsPanel();

        runPanel.setOnResults(resultsPanel::setResults);

        setLayout(new BorderLayout(8, 8));
        add(runPanel, BorderLayout.NORTH);
        add(resultsPanel, BorderLayout.CENTER);
    }
}