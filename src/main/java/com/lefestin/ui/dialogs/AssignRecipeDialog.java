package com.lefestin.ui.dialogs;

import javax.swing.*;

import com.lefestin.ui.MainFrame;

import java.time.LocalDate;

public class AssignRecipeDialog extends JDialog {

    private int    selectedRecipeId    = -1;
    private String selectedRecipeTitle = null;

    public AssignRecipeDialog(MainFrame frame,
                              LocalDate date,
                              String mealType) {
        super(frame, "Assign Recipe — " + mealType, true);
        // TODO: build full picker in Week 3
        setSize(480, 360);
        setLocationRelativeTo(frame);
        add(new JLabel("Recipe picker coming in Week 3",
            SwingConstants.CENTER));
    }

    public int    getSelectedRecipeId()    { return selectedRecipeId;    }
    public String getSelectedRecipeTitle() { return selectedRecipeTitle; }
}