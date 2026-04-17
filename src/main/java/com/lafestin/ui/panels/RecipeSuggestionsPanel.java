package com.lafestin.ui.panels;

import com.lafestin.ui.MainFrame;
import javax.swing.*;
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