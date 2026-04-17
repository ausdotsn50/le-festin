package com.lafestin.ui.panels;

import com.lafestin.ui.MainFrame;
import javax.swing.*;
import java.awt.*;

public class GroceryListPanel extends JPanel {
    private final MainFrame frame;

    public GroceryListPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        add(new JLabel("Grocery list — coming soon",
            SwingConstants.CENTER), BorderLayout.CENTER);
    }
}