package com.lafestin.ui.panels;

import com.lafestin.ui.MainFrame;
import javax.swing.*;
import java.awt.*;

public class WeeklyPlannerPanel extends JPanel {
    private final MainFrame frame;

    public WeeklyPlannerPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        add(new JLabel("Meal planner — coming soon",
            SwingConstants.CENTER), BorderLayout.CENTER);
    }
}