package com.lafestin.ui.panels;

import com.lafestin.ui.MainFrame;
import javax.swing.*;
import java.awt.*;

public class PantryPanel extends JPanel {
    private final MainFrame frame;

    public PantryPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        add(new JLabel("Pantry panel — coming soon",
            SwingConstants.CENTER), BorderLayout.CENTER);
    }
}