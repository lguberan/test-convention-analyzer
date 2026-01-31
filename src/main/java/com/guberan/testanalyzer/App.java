package com.guberan.testanalyzer;

import com.formdev.flatlaf.FlatLightLaf;
import com.guberan.testanalyzer.gui.MainFrame;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            new MainFrame().setVisible(true);
        });
    }
}