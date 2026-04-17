package com.lafestin.ui.panels;

import com.lafestin.ui.MainFrame;
import javax.swing.*;
import java.awt.*;

public class RecipeListPanel extends JPanel {
    private final MainFrame frame;

    public RecipeListPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        
        // TODO: 
        add(new JLabel("Recipes panel — coming soon",
            SwingConstants.CENTER), BorderLayout.CENTER);
    }
}