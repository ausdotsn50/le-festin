package com.lefestin.ui.panels;

import javax.swing.*;

import com.lefestin.ui.MainFrame;

import java.awt.*;

public class RecipeSuggestionsPanel extends JPanel {
    private final MainFrame frame;

    public RecipeSuggestionsPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        add(new JLabel("Suggestions panel — coming soon",
            SwingConstants.CENTER), BorderLayout.CENTER);
    }
}